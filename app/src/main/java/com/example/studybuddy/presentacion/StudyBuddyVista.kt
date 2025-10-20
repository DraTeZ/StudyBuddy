    package com.example.studybuddy.presentacion

    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.studybuddy.ai.GeminiAssistantService
    import com.example.studybuddy.data.PomodoroSession
    import com.example.studybuddy.data.Task
    import com.example.studybuddy.data.enums.PomodoroState
    import com.example.studybuddy.data.enums.TaskStatus
    import com.google.firebase.auth.auth
    import com.google.firebase.firestore.firestore
    import com.google.firebase.Firebase
    import com.google.firebase.firestore.CollectionReference
    import com.google.firebase.firestore.FieldValue
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.update
    import kotlinx.coroutines.launch
    import java.util.Date
    import java.util.UUID

    class StudyBuddyViewModel(
        private val geminiService: GeminiAssistantService
    ) : ViewModel() {

        private val auth = Firebase.auth
        private val db = Firebase.firestore
        private var userId: String? = null
        private lateinit var tasksCollection: CollectionReference

        // --- Variables para la sesión de estudio actual ---
        private var sessionStartTime: Date? = null
        private var sessionCyclesCompleted: Int = 0
        private var sessionTimeSpentMs: Long = 0L
        private var stateBeforePause: PomodoroState = PomodoroState.IDLE



        // Estado de las tareas
        private val _tasks = MutableStateFlow(emptyList<Task>())
        val tasks: StateFlow<List<Task>> = _tasks

        // Estado del temporizador
        private val _timerState = MutableStateFlow(PomodoroState.IDLE)
        val timerState: StateFlow<PomodoroState> = _timerState

        private val _currentTask: MutableStateFlow<Task?> = MutableStateFlow(null)
        val currentTask: StateFlow<Task?> = _currentTask

        private val _timeRemainingMs = MutableStateFlow(0L)
        val timeRemainingMs: StateFlow<Long> = _timeRemainingMs

        // Estado para el contenido generado (Consejos/Resúmenes de IA)
        private val _aiContentResult = MutableStateFlow<String?>(null)
        val aiContentResult: StateFlow<String?> = _aiContentResult // ESTE ES OBSERVADO POR LA UI

        // Configuración Pomodoro (25/5 minutos)
        private val workDurationMs = 25 * 60 * 1000L
        private val shortBreakMs = 5 * 60 * 1000L
        private val longBreakMs = 15 * 60 * 1000L
        private val cyclesBeforeLongBreak = 4

        private var timerJob: Job? = null

        // Helper para agrupar y ordenar tareas
        val sortedTasks: StateFlow<Map<TaskStatus, List<Task>>> = MutableStateFlow(emptyMap())

        fun onUserAuthenticated() {
            userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("Firestore", "User authenticated but UID is null. Cannot fetch data.")
                return
            }
            tasksCollection = db.collection("users").document(userId!!).collection("tasks")
            Log.d("Firestore", "User authenticated with UID: $userId. Listening for task updates.")
            listenForTaskUpdates()
        }

        private fun listenForTaskUpdates() {
            val safeUserId = userId ?: return // Seguridad por si se llama antes de tiempo

            // Nos suscribimos a la colección de tareas del usuario.
            // `addSnapshotListener` se ejecutará cada vez que los datos cambien en Firestore.
            tasksCollection
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("Firestore", "Listen failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // Convertimos los documentos de Firestore en nuestra clase de datos `Task`
                        val firestoreTasks = snapshot.toObjects(Task::class.java)
                        _tasks.value = firestoreTasks // Actualizamos nuestro StateFlow local
                        updateSortedTasks()
                        Log.d("Firestore", "Tasks updated from Firestore: ${firestoreTasks.size} tasks loaded.")
                    }
                }
        }

        private fun updateSortedTasks() {
            (sortedTasks as MutableStateFlow).value = _tasks.value
                .groupBy { it.status }
                .mapValues { (_, taskList) ->
                    taskList.sortedBy { it.dueDate }
                }
        }

        // --- TASK MANAGEMENT ---

        /** Añade una nueva tarea y ejecuta el análisis de IA. */

        fun addTask(task: Task) {
            // Aseguramos que la tarea tenga un ID único antes de guardarla.
            // Esto es importante para poder actualizarla después con los datos de la IA.
            val taskWithId = task.copy(id = task.id.ifBlank { UUID.randomUUID().toString() })

            // 1. Guarda la tarea INMEDIATAMENTE en Firestore.
            // El 'addSnapshotListener' se activará y mostrará la tarea en la UI.
            tasksCollection.document(taskWithId.id).set(taskWithId)
                .addOnSuccessListener {
                    Log.d("Firestore", "Paso 1/2: Tarea base añadida con ID: ${taskWithId.id}")

                    // 2. UNA VEZ guardada, ejecuta el análisis de IA.
                    // Lo hacemos en un viewModelScope para no bloquear el hilo principal.
                    viewModelScope.launch {
                        analyzeAndSaveAIData(taskWithId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error al añadir la tarea base", e)
                    // Opcional: Mostrar un error al usuario.
                }
        }

        /** Cambia el estado de una tarea. */
        fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
            tasksCollection.document(taskId).update("status", newStatus)
                .addOnSuccessListener { Log.d("Firestore", "Task $taskId status updated.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Error updating task status.", e) }
        }

        /** Elimina una tarea de la lista. */
        fun deleteTask(task: Task) {
            tasksCollection.document(task.id).delete()
                .addOnSuccessListener { Log.d("Firestore", "Task ${task.id} deleted.") }
                .addOnFailureListener { e -> Log.w("Firestore", "Error deleting task.", e) }
        }

        private fun savePomodoroSession(task: Task) {
            if (userId == null || task.id.isBlank() || sessionStartTime == null) {
                Log.w("Firestore", "Cannot save session, missing data.")
                return
            }

            val sessionToSave = PomodoroSession(
                startDatetime = sessionStartTime,
                finishDatetime = Date(), // Ahora
                completedCycles = sessionCyclesCompleted,
                timeUsed = sessionTimeSpentMs
            )
            Log.d("Firestore", "Saving Pomodoro session: $sessionToSave")


            // 1. Guarda la sesión detallada en la subcolección
            tasksCollection.document(task.id).collection("pomodoroSessions").add(sessionToSave)
                .addOnSuccessListener { Log.d("Firestore", "Pomodoro session saved for task ${task.id}") }
                .addOnFailureListener { e -> Log.w("Firestore", "Error saving Pomodoro session", e) }

            // 2. Actualiza los totales en el documento principal de la tarea de forma atómica,
            // el tiempo que haya faltado entre el último ciclo completado y si se termino la sesión pomodoro en mitad de un ciclo.
            // En caso de que se termine la sesión pomodoro en un descanso, no suma nada, ya que sessionTimeSpentMs = ciclos*duración de un ciclo
//
            updateTaskTimeSpent(task, sessionTimeSpentMs-(sessionCyclesCompleted*workDurationMs), 0)
        }


        /** Actualiza el tiempo dedicado de una tarea (llamado por el temporizador). */
        private fun updateTaskTimeSpent(task: Task, timeToAddMs: Long, cyclesToAdd: Int) {
            // Actualiza Firestore de forma atómica
            tasksCollection.document(task.id).update(
                mapOf(
                    "totalTimeSpentMs" to FieldValue.increment(timeToAddMs),
                    "totalPomodoroCycles" to FieldValue.increment(cyclesToAdd.toLong())
                )
            ).addOnFailureListener { e ->
                Log.w("Firestore", "Error updating time for ${task.id}", e)
            }

            // También actualiza la tarea actual del temporizador si es la misma
            if (_currentTask.value?.id == task.id) {
                _currentTask.value = _tasks.value.find { it.id == task.id }
            }
            updateSortedTasks()
        }

        // --- POMODORO TIMER (Adaptado para persistencia) ---

        /**
         * Inicia una NUEVA sesión de estudio para una tarea.
         * Limpia cualquier estado anterior y comienza desde cero.
         */
        fun startPomodoro(task: Task) {
            Log.d("Pomodoro", "Starting new Pomodoro session for task: ${task.name}")
            if (_timerState.value != PomodoroState.IDLE && _currentTask.value?.id == task.id) {
                // Si se presiona play sobre una tarea que ya está en curso (pausada), reanudamos.
                resumePomodoro()
                return
            }

            timerJob?.cancel()

            // *** Inicia una nueva sesión de estudio ***
            sessionStartTime = Date()
            sessionCyclesCompleted = 0
            sessionTimeSpentMs = 0L

            // Configura el estado para el nuevo ciclo de trabajo
            _currentTask.value = task
            updateTaskStatus(task.id, TaskStatus.IN_PROGRESS) // Actualiza el estado en Firestore
            _timerState.value = PomodoroState.WORK
            _timeRemainingMs.value = workDurationMs

            runTimer()
        }

        /** Pausa la sesión de estudio actual. */
        fun pausePomodoro() {
            Log.d("Pomodoro", "Pausing timer.")
            if (_timerState.value == PomodoroState.PAUSED || _timerState.value == PomodoroState.IDLE) return
            timerJob?.cancel()
            stateBeforePause = _timerState.value
            _timerState.value = PomodoroState.PAUSED
            Log.d("Pomodoro", "Timer paused, partial time registered: $sessionTimeSpentMs ms")
        }

        fun resumePomodoro(){
            if (_timerState.value != PomodoroState.PAUSED) return // Solo reanuda si está en pausa
            _timerState.value = stateBeforePause
            Log.d("Pomodoro", "Resuming timer.")
            runTimer()
        }

        /** Detiene la sesión Pomodoro actual, la persiste en Firestore y resetea el estado. */
        fun resetPomodoro() {
            Log.d("Pomodoro", "Resetting timer.")
            timerJob?.cancel()
            val task = _currentTask.value
            //val timeElapsedMs = workDurationMs - _timeRemainingMs.value

            if (task != null) {
                Log.d("Pomodoro", "Updating task time spent.")
                savePomodoroSession(task)

                // Regresamos el estado de la tarea a PENDIENTE, solo si no está ya completada
                if (task.status != TaskStatus.COMPLETED) {
                    updateTaskStatus(task.id, TaskStatus.TODO)
                }
            }

            // Reseteo final de estados locales
            _timerState.value = PomodoroState.IDLE
            _timeRemainingMs.value = 0L
            _currentTask.value = null
            sessionStartTime = null
            sessionCyclesCompleted = 0
            sessionTimeSpentMs = 0L
        }

        /** Lógica principal del temporizador. */
        private fun runTimer() {
            val working = _timerState.value == PomodoroState.WORK
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _timeRemainingMs.update { it - 1000L }
                    if(working) sessionTimeSpentMs += 1000L
                    if (_timeRemainingMs.value <= 0) {
                        handleTimerEnd()
                        break
                    }
                }
            }
        }

        /** Maneja el cambio de estado cuando el tiempo se agota. */
        private fun handleTimerEnd() {
            val currentTask = _currentTask.value ?: return

            when (_timerState.value) {
                PomodoroState.WORK -> {
                    sessionCyclesCompleted++ // Un ciclo de trabajo completado
                    updateTaskTimeSpent(currentTask, workDurationMs, 1)
                    // Decidir si es descanso corto o largo
                    val isLongBreak = (sessionCyclesCompleted % cyclesBeforeLongBreak == 0)
                    if (isLongBreak) {
                        _timerState.value = PomodoroState.LONG_BREAK
                        _timeRemainingMs.value = longBreakMs
                    } else {
                        _timerState.value = PomodoroState.SHORT_BREAK
                        _timeRemainingMs.value = shortBreakMs
                    }
                    runTimer() // Inicia el temporizador del descanso
                }
                PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> {
                    _timerState.value = PomodoroState.WORK
                    _timeRemainingMs.value = workDurationMs
                    runTimer() // Inicia un nuevo ciclo de trabajo
                }
                else -> {}
            }
        }

        override fun onCleared() {
            super.onCleared()
            timerJob?.cancel()
        }

        /** Limpia el resultado del contenido de IA para cerrar el diálogo en la UI. */
        fun clearAIContentResult() {
            _aiContentResult.value = null
        }

        private fun analyzeAndSaveAIData(task: Task) {
            viewModelScope.launch {
                try {
                    val result = geminiService.analyzeDifficulty(task)

                    // 3. Prepara un mapa solo con los campos de la IA para actualizar.
                    val aiDataMap = mapOf(
                        "aiDifficulty" to result.difficulty,
                        "recommendedTimeMin" to result.recommendedTimeMin,
                        "aiReasoning" to result.reasoning
                    )

                    // 4. Actualiza el documento existente en Firestore SOLO con los nuevos datos de la IA.
                    // El 'addSnapshotListener' se activará DE NUEVO y actualizará la UI con los datos de la IA.
                    tasksCollection.document(task.id).update(aiDataMap)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Paso 2/2: Datos de IA actualizados para la tarea ${task.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error al actualizar la tarea con datos de IA", e)
                        }

                } catch (e: Exception) {
                    println("Error al analizar la dificultad con Gemini: ${e.message}")
                }
            }
        }

        /** Llama al servicio de IA para generar consejos de estudio. */
        fun getAITips(task: Task) {
            viewModelScope.launch {
                // 1. Mensaje de carga
                _aiContentResult.value = "Generando consejos de estudio para ${task.name}..."

                try {
                    // 2. Llama a la función suspend del servicio
                    val tips = geminiService.generateStudyTips(task)

                    // 3. ACTUALIZA EL STATEFLOW para mostrar el resultado en la UI.
                    _aiContentResult.value = tips

                    // 4. (Opcional) Persistir los consejos en la tarea
                    _tasks.update { list ->
                        list.map { if (it.id == task.id) it.copy(aiStudyTips = tips) else it }
                    }

                } catch (e: Exception) {
                    println("Error al obtener consejos de IA: ${e.message}")
                    // 5. Muestra el error en la UI
                    _aiContentResult.value = "Error al obtener consejos de IA: ${e.message}. Inténtalo de nuevo."
                }
            }
        }

        /** Llama al servicio de IA para generar contenido educativo (resumen, flashcards, etc.). */
        fun generateContentForTask(task: Task, contentType: String) {
            viewModelScope.launch {
                _aiContentResult.value = "Generando $contentType para ${task.name}..."
                try {
                    val content = geminiService.generateContent(task, contentType)
                    _aiContentResult.value = content
                } catch (e: Exception) {
                    println("Error al generar contenido con Gemini: ${e.message}")
                    _aiContentResult.value = "Error al generar contenido: ${e.message}"
                }
            }
        }
    }