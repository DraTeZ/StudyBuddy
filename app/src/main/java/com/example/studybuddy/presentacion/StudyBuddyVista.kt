package com.example.studybuddy.presentacion

import android.util.Log
import kotlinx.coroutines.flow.asStateFlow
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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

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

    // --- ESTADO DE AUTENTICACIÓN ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // --- (ERROR 1) AÑADIDO: ESTADO PARA MODO OSCURO ---
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }
    // --- FIN DE LA ADICIÓN ---


    // 2. Función para manejar el login (Email/Pass)
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (email.isBlank() || pass.length <= 5) {
                _authState.value = AuthState.Error("Correo o contraseña inválidos.")
                return@launch
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ViewModel", "signInWithEmailAndPassword:success")
                        onUserAuthenticated()
                        _authState.value = AuthState.Success
                    } else {
                        Log.w("ViewModel", "signInWithEmailAndPassword:failure", task.exception)
                        _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Fallo al iniciar sesión.")
                    }
                }
        }
    }

    // Función para el inicio de sesión con Google (usando el ID Token)
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ViewModel", "signInWithCredential (Google): success")
                        onUserAuthenticated()
                        _authState.value = AuthState.Success
                    } else {
                        Log.w("ViewModel", "signInWithCredential (Google): failure", task.exception)
                        _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Fallo al iniciar sesión con Google.")
                    }
                }
        }
    }

    // Maneja errores que vienen de MainActivity (ej. Credential Manager)
    fun handleAuthError(message: String) {
        _authState.value = AuthState.Error(message)
        viewModelScope.launch { delay(5000); if (_authState.value is AuthState.Error) resetAuthState() }
    }

    // Forza el estado de éxito si la sesión ya existe al abrir la app
    fun forceAuthStateSuccess() {
        if (auth.currentUser != null) {
            _authState.value = AuthState.Success
        }
    }

    // 3. Función para manejar el registro
    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (name.isBlank() || !email.contains("@") || pass.length <= 5) {
                _authState.value = AuthState.Error("Por favor, completa todos los campos correctamente.")
                return@launch
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ViewModel", "createUserWithEmailAndPassword:success")

                        val user = auth.currentUser

                        // Creamos la solicitud de cambio de perfil para agregar el Nombre
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Log.d("ViewModel", "User profile updated.")
                                    onUserAuthenticated() // Inicializa listeners de Firestore
                                    _authState.value = AuthState.Success
                                } else {
                                    Log.w("ViewModel", "Error updating profile", updateTask.exception)
                                    onUserAuthenticated()
                                    _authState.value = AuthState.Success
                                }
                            }

                    } else {
                        Log.w("ViewModel", "createUserWithEmailAndPassword:failure", task.exception)
                        _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Fallo al registrar.")
                    }
                }
        }
    }

    // 4. Función para resetear el estado (útil para la navegación)
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
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
        val safeUserId = userId ?: return

        tasksCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Firestore", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val firestoreTasks = snapshot.toObjects(Task::class.java)
                    _tasks.value = firestoreTasks
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
    fun addTask(task: Task) {
        val taskWithId = task.copy(id = task.id.ifBlank { UUID.randomUUID().toString() })
        tasksCollection.document(taskWithId.id).set(taskWithId)
            .addOnSuccessListener {
                Log.d("Firestore", "Paso 1/2: Tarea base añadida con ID: ${taskWithId.id}")
                viewModelScope.launch {
                    analyzeAndSaveAIData(taskWithId)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al añadir la tarea base", e)
            }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        tasksCollection.document(taskId).update("status", newStatus)
            .addOnSuccessListener { Log.d("Firestore", "Task $taskId status updated.") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error updating task status.", e) }
    }

    fun deleteTask(task: Task) {
        if (_currentTask.value?.id == task.id && _timerState.value != PomodoroState.IDLE) {
            Log.w("ViewModel", "Intento de borrar la tarea activa bloqueado.")
            return
        }
        tasksCollection.document(task.id).delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Task ${task.id} deleted.")
                // Opcional: Si borramos una tarea que estaba seleccionada pero en IDLE, limpiamos la selección
                if (_currentTask.value?.id == task.id) {
                    _currentTask.value = null
                }
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error deleting task.", e) }
    }

    private fun savePomodoroSession(task: Task) {
        if (userId == null || task.id.isBlank() || sessionStartTime == null) {
            Log.w("Firestore", "Cannot save session, missing data.")
            return
        }

        val sessionToSave = PomodoroSession(
            startDatetime = sessionStartTime,
            finishDatetime = Date(),
            completedCycles = sessionCyclesCompleted,
            timeUsed = sessionTimeSpentMs
        )
        Log.d("Firestore", "Saving Pomodoro session: $sessionToSave")

        tasksCollection.document(task.id).collection("pomodoroSessions").add(sessionToSave)
            .addOnSuccessListener { Log.d("Firestore", "Pomodoro session saved for task ${task.id}") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error saving Pomodoro session", e) }

        updateTaskTimeSpent(task, sessionTimeSpentMs-(sessionCyclesCompleted*workDurationMs), 0)
    }

    private fun updateTaskTimeSpent(task: Task, timeToAddMs: Long, cyclesToAdd: Int) {
        tasksCollection.document(task.id).update(
            mapOf(
                "totalTimeSpentMs" to FieldValue.increment(timeToAddMs),
                "totalPomodoroCycles" to FieldValue.increment(cyclesToAdd.toLong())
            )
        ).addOnFailureListener { e ->
            Log.w("Firestore", "Error updating time for ${task.id}", e)
        }

        if (_currentTask.value?.id == task.id) {
            _currentTask.value = _tasks.value.find { it.id == task.id }
        }
        updateSortedTasks()
    }

    // --- POMODORO TIMER ---
    fun startPomodoro(task: Task) {
        Log.d("Pomodoro", "Starting new Pomodoro session for task: ${task.name}")
        if (_timerState.value != PomodoroState.IDLE && _currentTask.value?.id == task.id) {
            resumePomodoro()
            return
        }
        timerJob?.cancel()

        sessionStartTime = Date()
        sessionCyclesCompleted = 0
        sessionTimeSpentMs = 0L

        _currentTask.value = task
        updateTaskStatus(task.id, TaskStatus.IN_PROGRESS)
        _timerState.value = PomodoroState.WORK
        _timeRemainingMs.value = workDurationMs
        runTimer()
    }

    fun pausePomodoro() {
        Log.d("Pomodoro", "Pausing timer.")
        if (_timerState.value == PomodoroState.PAUSED || _timerState.value == PomodoroState.IDLE) return
        timerJob?.cancel()
        stateBeforePause = _timerState.value
        _timerState.value = PomodoroState.PAUSED
        Log.d("Pomodoro", "Timer paused, partial time registered: $sessionTimeSpentMs ms")
    }

    fun resumePomodoro(){
        if (_timerState.value != PomodoroState.PAUSED) return
        _timerState.value = stateBeforePause
        Log.d("Pomodoro", "Resuming timer.")
        runTimer()
    }

    fun resetPomodoro() {
        Log.d("Pomodoro", "Resetting timer.")
        timerJob?.cancel()
        val task = _currentTask.value

        if (task != null) {
            Log.d("Pomodoro", "Updating task time spent.")
            savePomodoroSession(task)
            if (task.status != TaskStatus.COMPLETED) {
                updateTaskStatus(task.id, TaskStatus.TODO)
            }
        }

        _timerState.value = PomodoroState.IDLE
        _timeRemainingMs.value = 0L
        _currentTask.value = null
        sessionStartTime = null
        sessionCyclesCompleted = 0
        sessionTimeSpentMs = 0L
    }

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

    private fun handleTimerEnd() {
        val currentTask = _currentTask.value ?: return
        when (_timerState.value) {
            PomodoroState.WORK -> {
                sessionCyclesCompleted++
                updateTaskTimeSpent(currentTask, workDurationMs, 1)
                val isLongBreak = (sessionCyclesCompleted % cyclesBeforeLongBreak == 0)
                if (isLongBreak) {
                    _timerState.value = PomodoroState.LONG_BREAK
                    _timeRemainingMs.value = longBreakMs
                } else {
                    _timerState.value = PomodoroState.SHORT_BREAK
                    _timeRemainingMs.value = shortBreakMs
                }
                runTimer()
            }
            PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> {
                _timerState.value = PomodoroState.WORK
                _timeRemainingMs.value = workDurationMs
                runTimer()
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun clearAIContentResult() {
        _aiContentResult.value = null
    }

    // --- FUNCIONES DE IA ---
    private fun analyzeAndSaveAIData(task: Task) {
        viewModelScope.launch {
            try {
                val result = geminiService.analyzeDifficulty(task)
                val aiDataMap = mapOf(
                    "aiDifficulty" to result.difficulty,
                    "recommendedTimeMin" to result.recommendedTimeMin,
                    "aiReasoning" to result.reasoning
                )
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

    fun getAITips(task: Task) {
        viewModelScope.launch {
            _aiContentResult.value = "Generando consejos de estudio para ${task.name}..."
            try {
                val tips = geminiService.generateStudyTips(task)
                _aiContentResult.value = tips
                _tasks.update { list ->
                    list.map { if (it.id == task.id) it.copy(aiStudyTips = tips) else it }
                }
            } catch (e: Exception) {
                println("Error al obtener consejos de IA: ${e.message}")
                _aiContentResult.value = "Error al obtener consejos de IA: ${e.message}. Inténtalo de nuevo."
            }
        }
    }

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

    // --- OTROS ESTADOS DE UI (FEEDBACK, PASSWORD RESET) ---
    private val _resetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val resetState: StateFlow<PasswordResetState> = _resetState.asStateFlow()

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetState.value = PasswordResetState.Loading
            if (email.isBlank() || !email.contains("@")) {
                _resetState.value = PasswordResetState.Error("Correo electrónico inválido.")
                return@launch
            }
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("ViewModel", "Password reset email sent to $email")
                        _resetState.value = PasswordResetState.Success
                    } else {
                        Log.w("ViewModel", "sendPasswordResetEmail:failure", task.exception)
                        _resetState.value = PasswordResetState.Error(task.exception?.localizedMessage ?: "Fallo al enviar correo.")
                    }
                }
        }
    }

    fun resetPasswordResetState() {
        _resetState.value = PasswordResetState.Idle
    }

    private val _feedbackState = MutableStateFlow<FeedbackState>(FeedbackState.Idle)
    val feedbackState: StateFlow<FeedbackState> = _feedbackState.asStateFlow()

    fun sendFeedback(type: String, description: String) {
        viewModelScope.launch {
            _feedbackState.value = FeedbackState.Loading
            delay(1500) // Simulación

            if (description.length > 10) {
                Log.d("ViewModel", "Enviando feedback: $type - $description")
                _feedbackState.value = FeedbackState.Success
            } else {
                _feedbackState.value = FeedbackState.Error("La descripción es muy corta.")
            }
        }
    }

    fun resetFeedbackState() {
        _feedbackState.value = FeedbackState.Idle
    }

    fun logout() {
        auth.signOut()
        userId = null
        timerJob?.cancel()
        _tasks.value = emptyList()
        _currentTask.value = null
        _timerState.value = PomodoroState.IDLE
        _timeRemainingMs.value = 0L
        _authState.value = AuthState.Idle
    }
}

// --- INTERFACES SELLADAS PARA LOS ESTADOS DE UI ---

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface PasswordResetState {
    object Idle : PasswordResetState
    object Loading : PasswordResetState
    object Success : PasswordResetState
    data class Error(val message: String) : PasswordResetState
}

sealed interface FeedbackState {
    object Idle : FeedbackState
    object Loading : FeedbackState
    object Success : FeedbackState
    data class Error(val message: String) : FeedbackState
}