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
import com.example.studybuddy.data.Task
import com.example.studybuddy.ui.theme.VerdeSuave
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studybuddy.presentacion.AuthState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: StudyBuddyViewModel) {
    val currentTask by viewModel.currentTask.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timeRemainingMs by viewModel.timeRemainingMs.collectAsState()
    val aiContent by viewModel.aiContentResult.collectAsState()
    val sortedTasks by viewModel.sortedTasks.collectAsState()
    val isPremium by viewModel.isUserPremium.collectAsState()
    var showRestartDialog by remember { mutableStateOf(false) }
    var taskToRestart by remember { mutableStateOf<Task?>(null) }

    // Contexto para el Toast
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (!isPremium) {
            AdMobBanner()
        }
        PomodoroTimer(
            currentTask = currentTask,
            timerState = timerState,
            timeRemainingMs = timeRemainingMs,
            aiContent = aiContent,
            onStart = viewModel::startPomodoro,
            onPause = viewModel::pausePomodoro,
            onReset = viewModel::resetPomodoro,
            onGetTips = viewModel::getAITips,
            onContentDismiss = viewModel::clearAIContentResult
        )
        Divider(Modifier.padding(vertical = 8.dp))
        TaskList(
            sortedTasks = sortedTasks,
            onTaskAction = { task, action ->
                when (action) {
                    "start" -> {
                        if (task.status == TaskStatus.COMPLETED) {
                            taskToRestart = task
                            showRestartDialog = true
                        } else {
                            viewModel.startPomodoro(task)
                        }
                    }
                    "complete" -> viewModel.updateTaskStatus(task.id, TaskStatus.COMPLETED)

                    "delete" -> {
                        // Lógica de protección contra borrado durante Pomodoro activo
                        if (currentTask?.id == task.id && timerState != PomodoroState.IDLE) {
                            Toast.makeText(context, "Detén el temporizador antes de borrar esta tarea", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.deleteTask(task)
                        }
                    }
                }
            }
        )
    }

    if (showRestartDialog && taskToRestart != null) {
        AlertDialog(
            onDismissRequest = {
                showRestartDialog = false
                taskToRestart = null
            },
            title = { Text("¿Reabrir tarea?") },
            text = {
                Text("Esta tarea ya está marcada como completada. ¿Deseas volver a trabajar en ella? Pasará a estado 'En Progreso'.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToRestart?.let { viewModel.startPomodoro(it) }
                        showRestartDialog = false
                        taskToRestart = null
                    }
                ) {
                    Text("Sí, reabrir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        taskToRestart = null
                    }
                ) {
                    Text("Cancelar")
                }
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
    val showTipsDialog = aiContent != null

    // Colores según estado
    val stateColor = when (timerState) {
        PomodoroState.WORK -> MaterialTheme.colorScheme.primary
        PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val stateText = when (timerState) {
        PomodoroState.WORK -> "🧠 MODO ENFOQUE"
        PomodoroState.SHORT_BREAK -> "☕ DESCANSO CORTO"
        PomodoroState.LONG_BREAK -> "🎉 DESCANSO LARGO"
        PomodoroState.PAUSED -> "⏸️ PAUSADO"
        PomodoroState.IDLE -> "💤 LISTO PARA EMPEZAR"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nombre de tarea
            Text(
                text = currentTask?.name ?: "Selecciona una Tarea abajo 👇",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Píldora de Estado
            Surface(
                color = stateColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stateText,
                    color = stateColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tiempo Grande
            Text(
                text = timeDisplay,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = stateColor,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(24.dp))

            // Botones de Control
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentTask != null) {
                    // Botón Play/Pause Principal
                    FilledIconButton(
                        onClick = {
                            when (timerState) {
                                PomodoroState.WORK, PomodoroState.SHORT_BREAK, PomodoroState.LONG_BREAK -> onPause()
                                else -> onStart(currentTask)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = stateColor)
                    ) {
                        Icon(
                            imageVector = if (timerState == PomodoroState.PAUSED || timerState == PomodoroState.IDLE) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = "Control",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Botón Stop
                    OutlinedIconButton(
                        onClick = onReset,
                        enabled = timerState != PomodoroState.IDLE,
                        modifier = Modifier.size(56.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "Reiniciar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Botón de IA (solo si hay tarea)
            if (currentTask != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { onGetTips(currentTask) }) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Consejos de IA")
                }
            }
        }
    }

    if (showTipsDialog && aiContent != null) {
        AlertDialog(
            onDismissRequest = onContentDismiss,
            title = { Text("Consejos de Estudio") },
            text = { Text(aiContent) },
            confirmButton = { TextButton(onClick = onContentDismiss) { Text("Cerrar") } }
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Text(
                    aiDetails,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Dedicado: ${task.totalPomodoroCycles} ciclos | ${task.totalTimeSpentMs / 60000} min",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.status == TaskStatus.TODO || task.status == TaskStatus.IN_PROGRESS) {
                IconButton(onClick = { onTaskAction(task, "complete") }) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Marcar como completada",
                        tint = VerdeSuave
                    )
                }
            }
            IconButton(onClick = { onTaskAction(task, "delete") }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
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

    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Nueva Tarea") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Tarea") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Materia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detalles Opcionales") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Dificultad Personal:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DifficultyLevel.entries.forEach { level ->
                            Row(modifier = Modifier.clickable { difficulty = level }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = difficulty == level, onClick = { difficulty = level })
                                Text(level.label)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dateFormatter.format(Date(dueDate)),
                    onValueChange = { /* Deshabilitado */ },
                    label = { Text("Fecha de Entrega") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
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
                            id = UUID.randomUUID().toString(),
                            name = name,
                            subject = subject,
                            dueDate = Date(dueDate),
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


@Composable
fun StudyBuddyApp(
    viewModel: StudyBuddyViewModel,
    onLogout: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    // Este LaunchedEffect maneja la navegación automática
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        } else if (authState is AuthState.Idle) {
            if (navController.currentDestination?.route != "login") {
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // No es necesario, el LaunchedEffect lo maneja
                },
                onCreateAccountClick = {
                    navController.navigate("register")
                },
                viewModel = viewModel,
                onForgotPasswordClick = {
                    navController.navigate("forgot_password")
                },
                onHelpClick = {
                    navController.navigate("help")
                },
                onReportProblemClick = {
                    navController.navigate("report_problem")
                },
                onGoogleSignInClicked = onGoogleSignIn
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    // No es necesario, el LaunchedEffect lo maneja
                },
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel,
                onLoginClick = {
                    navController.popBackStack()
                },
                onHelpClick = {
                    navController.navigate("help")
                },
                onReportProblemClick = {
                    navController.navigate("report_problem")
                }
            )
        }

        composable("main") {
            AppScreen(
                viewModel = viewModel,
                onLogout = onLogout, // Pasa el callback de MainActivity
                // --- NUEVO: Pasamos la navegación a Premium Stats ---
                onStatsClick = {
                    navController.navigate("premium_stats")
                }
            )
        }

        // --- NUEVO COMPOSABLE PARA ESTADÍSTICAS ---
        composable("premium_stats") {
            PremiumStatsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("help") {
            HelpScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("report_problem") {
            ReportProblemScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}