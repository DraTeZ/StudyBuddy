package com.example.studybuddy.vistas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBackClick: () -> Unit) {
    val faqs = listOf(
        "Ayuda" to "Si tienes problemas con la aplicación, a continuación te presentamos una serie de preguntas frecuentes que te pueden orientar.",
        "¿Cómo recupero mi contraseña?" to "Si olvidaste tu contraseña, puedes hacer clic en '¿Has olvidado tu contraseña?' en la pantalla de inicio de sesión. Recibirás un correo para reestablecerla.",
        "¿Cómo funciona el temporizador Pomodoro?" to "El temporizador Pomodoro te ayuda a trabajar en bloques de tiempo (generalmente 25 minutos) seguidos de un breve descanso. Esto mejora la concentración y previene la fatiga.",
        "¿Puedo editar una tarea?" to "Actualmente, puedes crear y eliminar tareas. La funcionalidad de edición se añadirá en futuras actualizaciones.",
        "¿Para qué sirve 'Reportar un problema'?" to "Si encuentras un error (bug) o tienes una sugerencia, usa 'Reportar un problema' para enviarnos tus comentarios. ¡Nos ayuda a mejorar la app!"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y preguntas frecuentes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(faqs) { (question, answer) ->
                FaqItem(question = question, answer = answer)
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}