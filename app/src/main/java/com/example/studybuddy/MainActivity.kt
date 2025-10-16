package com.example.studybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studybuddy.ai.GeminiAssistantReal
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.example.studybuddy.presentacion.StudyBuddyViewModelFactory
import com.example.studybuddy.vistas.StudyBuddyApp


// Definición de un tema de color simple para Jetpack Compose
val StudyBuddyColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),       // Green for focus
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF2196F3),     // Blue for information
    tertiary = Color(0xFFFF9800),      // Orange for alerts/emphasis
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = Color.Black
)

@Composable
fun StudyBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudyBuddyColorScheme,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear la dependencia del servicio de IA.
        // El constructor de GeminiAssistantReal()
        val geminiService = GeminiAssistantReal()

        // Crear la Factoría (Factory), pasándole la dependencia.
        val factory = StudyBuddyViewModelFactory(geminiService)

        setContent {
            StudyBuddyTheme {
                // Surface es el contenedor que aplica el color de fondo del tema a la pantalla.
                Surface(
                    modifier = Modifier.fillMaxSize(), // Ocupa toda la pantalla
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyBuddyApp(viewModel(factory = factory))
                }
            }
        }
    }
}