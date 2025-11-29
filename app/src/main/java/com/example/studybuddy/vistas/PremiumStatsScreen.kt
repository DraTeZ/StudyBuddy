package com.example.studybuddy.vistas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.StudyBuddyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumStatsScreen(
    viewModel: StudyBuddyViewModel,
    onBackClick: () -> Unit
) {
    val isPremium by viewModel.isUserPremium.collectAsState()
    val stats by viewModel.monthlyStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas Premium") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isPremium) {
                // --- VISTA DESBLOQUEADA (PREMIUM) ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Resumen de los últimos 30 días",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Tarjeta Principal: Tiempo
                    StatCard(
                        title = "Tiempo Total Estudiado",
                        value = "${stats.totalMinutesStudied} min",
                        icon = Icons.Default.AccessTime,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            StatCard(
                                title = "Tareas Listas",
                                value = stats.totalTasksCompleted.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            StatCard(
                                title = "Efectividad",
                                value = "${stats.completionRate}%",
                                icon = Icons.Default.ShowChart,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Materia Dominante", style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stats.mostProductiveSubject.ifBlank { "Sin datos suficientes" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Es la materia con más tareas completadas este mes.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

            } else {
                // --- VISTA BLOQUEADA (PAYWALL) ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Función Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Visualiza métricas avanzadas sobre tu rendimiento mensual, materias favoritas y eficiencia de estudio.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Simulado (Ya que es activación manual)
                    Button(
                        onClick = { },
                        enabled = false, // Deshabilitado
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Contactar Administrador")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "(Demo: Activación manual desde Firebase Console)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}