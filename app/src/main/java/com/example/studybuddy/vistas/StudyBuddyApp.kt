package com.example.studybuddy.vistas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.DifficultyLevel
import com.example.studybuddy.data.enums.PomodoroState
import com.example.studybuddy.data.enums.TaskStatus
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.material.icons.filled.CalendarToday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBuddyApp(viewModel: StudyBuddyViewModel = viewModel()) {
    val sortedTasks by viewModel.sortedTasks.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timeRemainingMs by viewModel.timeRemainingMs.collectAsState()

    // Observamos el StateFlow que contiene los consejos de IA.
    val aiContent by viewModel.aiContentResult.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Buddy AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Tarea")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PomodoroTimer(
                currentTask = currentTask,
                timerState = timerState,
                timeRemainingMs = timeRemainingMs,
                aiContent = aiContent,
                onStart = viewModel::startPomodoro,
                onPause = viewModel::pausePomodoro,
                onReset = viewModel::resetPomodoro,
                onGetTips = viewModel::getAITips,
                onContentDismiss = viewModel::clearAIContentResult // Llama a la función pública del VM
            )
            Divider(Modifier.padding(vertical = 8.dp))
            TaskList(
                sortedTasks = sortedTasks,
                onTaskAction = { task, action ->
                    when (action) {
                        "start" -> viewModel.startPomodoro(task)
                        "complete" -> viewModel.updateTaskStatus(task, TaskStatus.COMPLETED)
                        "delete" -> viewModel.deleteTask(task)
                    }
                }
            )
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { task ->
                viewModel.addTask(task)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun PomodoroTimer(
    currentTask: Task?,
    timerState: PomodoroState,
    timeRemainingMs: Long,
    aiContent: String?,
    onStart: (Task) -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onGetTips: (Task) -> Unit,
    onContentDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }
    val timeDisplay = formatter.format(Date(timeRemainingMs))

    // El diálogo se muestra si hay contenido en el StateFlow
    val showTipsDialog = aiContent != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = when(timerState) {
            PomodoroState.WORK -> Color(0xFFF7F0E8) // Beige claro
            PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> Color(0xFFE8F7F0) // Verde claro
            else -> MaterialTheme.colorScheme.surfaceVariant
        })
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTask?.name ?: "Selecciona una Tarea",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (timerState) {
                    PomodoroState.WORK -> "¡ENFOQUE!"
                    PomodoroState.SHORT_BREAK -> "Descanso Corto"
                    PomodoroState.LONG_BREAK -> "Descanso Largo"
                    PomodoroState.PAUSED -> "PAUSADO"
                    PomodoroState.IDLE -> "Listo"
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = timeDisplay,
                fontSize = 60.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentTask != null) {
                    Button(onClick = {
                        when (timerState) {
                            PomodoroState.WORK, PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> onPause()
                            else -> onStart(currentTask!!)
                        }
                    }) {
                        Icon(
                            imageVector = if (timerState == PomodoroState.PAUSED || timerState == PomodoroState.IDLE) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = "Control Pomodoro"
                        )
                    }
                    Button(onClick = onReset, enabled = timerState != PomodoroState.IDLE) {
                        Icon(Icons.Filled.Stop, contentDescription = "Reiniciar")
                    }
                    // Botón para Consejos IA
                    Button(onClick = {
                        onGetTips(currentTask!!)
                    }) {
                        Text("Consejos IA")
                    }
                }
            }
        }
    }

    // Muestra el diálogo si showTipsDialog es true
    if (showTipsDialog && aiContent != null) {
        AlertDialog(
            onDismissRequest = onContentDismiss,
            title = { Text("Consejos de Estudio") },
            text = {
                Text(aiContent)
            },
            confirmButton = {
                TextButton(onClick = onContentDismiss) { Text("Cerrar") }
            }
        )
    }
}

@Composable
fun TaskList(
    sortedTasks: Map<TaskStatus, List<Task>>,
    onTaskAction: (Task, String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        TaskStatus.entries.filter { it != TaskStatus.OVERDUE }.forEach { status ->
            val tasks = sortedTasks[status] ?: emptyList()
            if (tasks.isNotEmpty()) {
                item {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp, 8.dp)
                    )
                }
                items(tasks) { task ->
                    TaskItem(task, onTaskAction)
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onTaskAction: (Task, String) -> Unit) {
    val aiDetails = if (task.aiDifficulty != null) {
        "Dificultad IA: ${task.aiDifficulty.label} | Tiempo Rec.: ${task.recommendedTimeMin} min"
    } else {
        "Pendiente de Análisis IA..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onTaskAction(task, "start") },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Bold)
                Text("Materia: ${task.subject}", fontSize = 12.sp, color = Color.Gray)
                Text(aiDetails, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Dedicado: ${task.pomodoroCycles} ciclos | ${task.timeSpentMs / 60000} min",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
            if (task.status == TaskStatus.TODO || task.status == TaskStatus.IN_PROGRESS) {
                IconButton(onClick = { onTaskAction(task, "complete") }) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Marcar como completada",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { onTaskAction(task, "delete") }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskAdded: (Task) -> Unit) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(DifficultyLevel.EASY) }

    val oneDayInMs = 86400000L
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + oneDayInMs) }

    // Controla la visibilidad del selector de fecha
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nueva Tarea") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Tarea") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Materia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detalles Opcionales") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))
                // Selector de Dificultad
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dificultad Personal:", modifier = Modifier.weight(1f))
                    DifficultyLevel.entries.forEach { level ->
                        Row(modifier = Modifier.clickable { difficulty = level }) {
                            RadioButton(selected = difficulty == level, onClick = { difficulty = level })
                            Text(level.label, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Campo de entrada para la Fecha de Entrega con Selector
                OutlinedTextField(
                    value = dateFormatter.format(Date(dueDate)),
                    onValueChange = { /* Deshabilitado */ },
                    label = { Text("Fecha de Entrega") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }, // Muestra el selector al hacer clic en el campo
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Seleccionar Fecha")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        val newTask = Task(
                            id = UUID.randomUUID().toString(), // Generación de ID
                            name = name,
                            subject = subject,
                            dueDate = dueDate, // Usamos el valor seleccionado
                            userDifficulty = difficulty,
                            details = details
                        )
                        onTaskAdded(newTask)
                    }
                },
                enabled = name.isNotBlank() && subject.isNotBlank()
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // El Composable del Selector de Fecha (DatePickerDialog)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate,
            initialDisplayedMonthMillis = dueDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Actualiza la fecha con el valor seleccionado
                        datePickerState.selectedDateMillis?.let { newDateMillis ->
                            dueDate = newDateMillis
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}