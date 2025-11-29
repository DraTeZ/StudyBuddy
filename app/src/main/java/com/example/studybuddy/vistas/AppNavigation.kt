package com.example.studybuddy.vistas

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.studybuddy.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studybuddy.presentacion.StudyBuddyViewModel

sealed class AppScreenRoute(val route: String, val label: String, val icon: ImageVector) {
    object Home : AppScreenRoute("home", "Inicio", Icons.Default.Home)
    object Calendar : AppScreenRoute("calendar", "Calendario", Icons.Default.CalendarMonth)
    object Profile : AppScreenRoute("profile", "Perfil", Icons.Default.Person)
}

val bottomNavItems = listOf(
    AppScreenRoute.Home,
    AppScreenRoute.Calendar,
    AppScreenRoute.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    viewModel: StudyBuddyViewModel,
    onLogout: () -> Unit,
    onStatsClick: () -> Unit // <--- 1. NUEVO PARÁMETRO
) {
    // Controlador de navegación para las pestañas (Home, Calendar, Profile)
    val appNavController = rememberNavController()
    val navBackStackEntry by appNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showAddTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_studybuddy),
                            contentDescription = "Logo",
                            modifier = Modifier.size(65.dp) // Logo pequeño
                        )
                        Spacer(modifier = Modifier.width(12.dp)) // Espacio
                        val title = when (currentRoute) {
                            AppScreenRoute.Calendar.route -> "Calendario"
                            AppScreenRoute.Profile.route -> "Perfil y Ajustes"
                            else -> "Study Buddy"
                        }
                        Text(title, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            AppBottomNavigation(navController = appNavController, currentRoute = currentRoute)
        },
        floatingActionButton = {

            if (currentRoute == AppScreenRoute.Home.route) {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir Tarea")
                }
            }
        }
    ) { innerPadding ->
        // NavHost interno para las pantallas de la app
        NavHost(
            navController = appNavController,
            startDestination = AppScreenRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreenRoute.Home.route) {
                HomeScreen(viewModel = viewModel)
            }
            composable(AppScreenRoute.Calendar.route) {
                CalendarScreen(viewModel = viewModel)
            }
            composable(AppScreenRoute.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onLogout = onLogout,
                    onStatsClick = onStatsClick // <--- 2. PASAMOS EL CALLBACK A PROFILE
                )
            }
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
fun AppBottomNavigation(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {

                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}