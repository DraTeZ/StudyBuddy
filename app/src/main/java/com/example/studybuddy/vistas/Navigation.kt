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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object NewTask : Screen("new_task")
    object Pomodoro : Screen("pomodoro/{taskId}") {
        fun createRoute(taskId: String) = "pomodoro/$taskId"
    }
    object Settings : Screen("settings")
}