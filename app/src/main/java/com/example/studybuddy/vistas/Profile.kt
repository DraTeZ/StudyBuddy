package com.example.studybuddy.vistas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

@Composable
fun ProfileScreen(
    viewModel: StudyBuddyViewModel,
    onLogout: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser
    val userName = currentUser?.displayName ?: "Sin Nombre"
    val userEmail = currentUser?.email ?: "Sin Correo"

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Acerca de StudyBuddy") },
            text = {
                Column {
                    Text(
                        "StudyBuddy App 2025",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Desarrollado por:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Fredy Alexander Gonzalez Pobre")
                    Text("Julian David Huertas Dominguez")
                    Text("David Alexander Rátiva Gutiérrez")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Versión 1.1",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        Column {

            Text(
                "Perfil de Usuario",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Usar los valores reales ---
            ProfileInfoItem(title = "Nombre", subtitle = userName)
            Divider()
            ProfileInfoItem(title = "Correo", subtitle = userEmail)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Ajustes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- Conectar el Toggle al ViewModel ---
            SettingItemToggle(
                title = "Modo Oscuro",
                icon = Icons.Default.Palette,
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) }
            )
            Divider()

            SettingItem(
                title = "Acerca de StudyBuddy",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )
        }

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión")
        }
    }
}


@Composable
fun ProfileInfoItem(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingItemToggle(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Toggle al hacer clic en la fila
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}