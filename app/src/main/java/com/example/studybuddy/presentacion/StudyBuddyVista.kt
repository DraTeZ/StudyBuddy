package com.example.studybuddy.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studybuddy.ai.GeminiAssistantService
import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.PomodoroState
import com.example.studybuddy.data.enums.TaskStatus
import com.example.studybuddy.data.enums.DifficultyLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

class StudyBuddyViewModel(
    private val geminiService: GeminiAssistantService
) : ViewModel() {

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

    private fun updateSortedTasks() {
        (sortedTasks as MutableStateFlow).value = _tasks.value
            .groupBy { it.status }
            .mapValues { (_, taskList) ->
                taskList.sortedBy { it.dueDate }
            }
    }




    /** Limpia el resultado del contenido de IA para cerrar el diálogo en la UI. */
    fun clearAIContentResult() {
        _aiContentResult.value = null
    }

    // --- TASK MANAGEMENT ---

    /** Añade una nueva tarea y ejecuta el análisis de IA. */
    fun addTask(task: Task) {
        viewModelScope.launch {
            // 1. Añadir la tarea inicial (sin datos de IA) a la lista
            _tasks.update { it + task }
            updateSortedTasks()

            // 2. Ejecutar el análisis de dificultad de la IA
            try {
                val result = geminiService.analyzeDifficulty(task)

                // 3. Actualizar el StateFlow de tareas con los datos de la IA
                _tasks.update { list ->
                    list.map {
                        // Buscamos la tarea recién añadida por ID y creamos una copia con los datos de la IA
                        if (it.id == task.id) {
                            it.copy(
                                aiDifficulty = result.difficulty,
                                // Casteo seguro de String (desde la IA) a Int
                                recommendedTimeMin = result.recommendedTimeMin,
                                aiReasoning = result.reasoning
                            )
                        } else {
                            it
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error al analizar la dificultad con Gemini: ${e.message}")
            }
            // 4. Aseguramos que la lista se reordena/refresca
            updateSortedTasks()
        }
    }

    /** Cambia el estado de una tarea. */
    fun updateTaskStatus(task: Task, newStatus: TaskStatus) {
        _tasks.update { list ->
            list.map { if (it.id == task.id) it.copy(status = newStatus) else it }
        }
        updateSortedTasks()
    }

    /** Elimina una tarea de la lista. */
    fun deleteTask(task: Task) {
        _tasks.update { it - task }
        updateSortedTasks()
    }

    /** Actualiza el tiempo dedicado de una tarea (llamado por el temporizador). */
    private fun updateTaskTimeSpent(task: Task, timeToAddMs: Long, cyclesToAdd: Int) {
        _tasks.update { list ->
            list.map {
                if (it.id == task.id) {
                    it.copy(
                        timeSpentMs = it.timeSpentMs + timeToAddMs,
                        pomodoroCycles = it.pomodoroCycles + cyclesToAdd
                    )
                } else {
                    it
                }
            }
        }
        // También actualiza la tarea actual del temporizador si es la misma
        if (_currentTask.value?.id == task.id) {
            _currentTask.value = _tasks.value.find { it.id == task.id }
        }
        updateSortedTasks()
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


    // --- POMODORO TIMER ---

    /** Inicia o reanuda el temporizador para una tarea. */
    fun startPomodoro(task: Task) {
        timerJob?.cancel()

        // Si es una nueva tarea o estaba inactiva
        if (_currentTask.value?.id != task.id || _timerState.value == PomodoroState.IDLE) {
            _currentTask.value = task.copy(status = TaskStatus.IN_PROGRESS)
            updateTaskStatus(task, TaskStatus.IN_PROGRESS)
            _timerState.value = PomodoroState.WORK
            _timeRemainingMs.value = workDurationMs
        } else if (_timerState.value == PomodoroState.PAUSED) {
            // Si estaba pausado, reanuda el estado actual (re-usa el tiempo restante)
            _timerState.value = when (_currentTask.value!!.pomodoroCycles % cyclesBeforeLongBreak == 0 && _timeRemainingMs.value <= longBreakMs) {
                true -> PomodoroState.LONG_BREAK
                false -> if (_timeRemainingMs.value > 0) PomodoroState.WORK else PomodoroState.SHORT_BREAK
            }
        } else {
            return // Ya está corriendo
        }

        runTimer()
    }

    /** Pausa el temporizador y registra el tiempo de trabajo parcial. */
    fun pausePomodoro() {
        timerJob?.cancel()

        val task = _currentTask.value
        // Solo calculamos y registramos si estábamos en el estado WORK (o PAUSED si viene de WORK)
        if ((_timerState.value == PomodoroState.WORK || _timerState.value == PomodoroState.PAUSED) && task != null) {

            // Calculamos el tiempo transcurrido en el ciclo actual.
            val timeElapsedMs = workDurationMs - _timeRemainingMs.value

            // Registramos el tiempo parcial de TRABAJO (0 ciclos completados).
            // Solo registra si ha pasado al menos un segundo de trabajo.
            if (timeElapsedMs > 1000L) {
                updateTaskTimeSpent(task, timeElapsedMs, 0)
            }
        }

        _timerState.value = PomodoroState.PAUSED
    }

    /** Lógica principal del temporizador. */
    private fun runTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timeRemainingMs.update { it - 1000L }

                if (_timeRemainingMs.value <= 0) {
                    handleTimerEnd()
                    break
                }
            }
        }
    }

    /** Maneja el cambio de estado cuando el tiempo se agota. */
    private fun handleTimerEnd() {
        timerJob?.cancel()
        val currentTask = _currentTask.value ?: return
        val currentCycles = currentTask.pomodoroCycles

        when (_timerState.value) {
            PomodoroState.WORK -> {
                // Actualiza el tiempo total dedicado a la tarea con el ciclo COMPLETO (1 ciclo).
                updateTaskTimeSpent(currentTask, workDurationMs, 1)

                val nextCycles = currentCycles + 1

                if (nextCycles % cyclesBeforeLongBreak == 0) {
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

    /** Reinicia el temporizador, registra el tiempo parcial si existe y mueve la tarea a TODO. */
    fun resetPomodoro() {
        timerJob?.cancel()

        val task = _currentTask.value

        // Registrar el tiempo parcial antes de resetear
        // Solo si estaba en WORK o PAUSED, y no en un estado de descanso
        if (task != null && (_timerState.value == PomodoroState.WORK || _timerState.value == PomodoroState.PAUSED)) {

            // Calcular el tiempo transcurrido (ya sea en WORK o PAUSED)
            val timeElapsedMs = workDurationMs - _timeRemainingMs.value

            if (timeElapsedMs > 1000L) {
                updateTaskTimeSpent(task, timeElapsedMs, 0)
            }

            // Mover la tarea de vuelta a TODO/PENDIENTE
            updateTaskStatus(task, TaskStatus.TODO)
        }

        _timerState.value = PomodoroState.IDLE
        _timeRemainingMs.value = 0L
        _currentTask.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}