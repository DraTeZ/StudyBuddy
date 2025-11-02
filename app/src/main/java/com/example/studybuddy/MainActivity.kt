package com.example.studybuddy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.studybuddy.ai.GeminiAssistantReal
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.example.studybuddy.presentacion.StudyBuddyViewModelFactory
import com.example.studybuddy.vistas.StudyBuddyApp // ¡Importante! Esta es ahora tu NavHost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase


// Definición de tu tema de color (esto se queda igual)
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

    private lateinit var auth: FirebaseAuth

    // 1. Creamos la factory para el ViewModel
    private val factory by lazy { StudyBuddyViewModelFactory(GeminiAssistantReal()) }

    // 2. Creamos UNA SOLA instancia del ViewModel, que será compartida
    private val viewModel: StudyBuddyViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Opcional: Si el usuario ya estaba logueado de antes,
        // notificamos al ViewModel. Esto servirá para "recordar sesión".
        if (auth.currentUser != null) {
            Log.d("MainActivity", "User ${auth.currentUser?.uid} is already signed in.")
            viewModel.onUserAuthenticated()
            // (En un futuro, aquí podríamos hacer que el NavHost empiece en "main")
        }

        setContent {
            StudyBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. ¡Este es el gran cambio!
                    // Llamamos a StudyBuddyApp (nuestro NavHost)
                    // y le pasamos el ÚNICO viewModel que creó la Activity.
                    // Todas las pantallas (Login, Register, Main) usarán esta misma instancia.
                    StudyBuddyApp(viewModel = viewModel)
                }
            }
        }
    }
}