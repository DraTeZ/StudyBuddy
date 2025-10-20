package com.example.studybuddy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studybuddy.ai.GeminiAssistantReal
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.example.studybuddy.presentacion.StudyBuddyViewModelFactory
import com.example.studybuddy.vistas.StudyBuddyApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase


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

    private lateinit var auth: FirebaseAuth
    private val viewModel: StudyBuddyViewModel by viewModels {
        StudyBuddyViewModelFactory(GeminiAssistantReal())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Crear la dependencia del servicio de IA.
        // El constructor de GeminiAssistantReal()
        val geminiService = GeminiAssistantReal()

        // Crear la Factoría (Factory), pasándole la dependencia.
        val factory = StudyBuddyViewModelFactory(geminiService)

        setContent {
            StudyBuddyTheme {
                var isLoading by remember { mutableStateOf(true) }
                // Surface es el contenedor que aplica el color de fondo del tema a la pantalla.
                Surface(
                    modifier = Modifier.fillMaxSize(), // Ocupa toda la pantalla
                    color = MaterialTheme.colorScheme.background
                ) {

                    if (isLoading) {
                        // Muestra una pantalla de carga en el centro.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        StudyBuddyApp(viewModel(factory = factory))
                    }
                }
                LaunchedEffect(Unit) {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        auth.signInAnonymously()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("MainActivity", "signInAnonymously:success")
                                    // Autenticación exitosa: notificamos al ViewModel y ocultamos la carga.
                                    viewModel.onUserAuthenticated()
                                    isLoading = false // <-- Oculta la pantalla de carga
                                } else {
                                    Log.w("MainActivity", "signInAnonymously:failure", task.exception)
                                    // Opcional: Manejar el error de autenticación.
                                    // Podrías mostrar un mensaje de error permanente.
                                }
                            }
                    }else{
                        Log.d("MainActivity", "User is already signed in with UID: ${currentUser.uid}")
                        // El usuario ya estaba autenticado: notificamos al ViewModel y ocultamos la carga.
                        viewModel.onUserAuthenticated()
                        isLoading = false // <-- Oculta la pantalla de carga
                    }

                }
            }
        }
    }

//    public override fun onStart() {
//        super.onStart()
//        // Check if user is signed in (non-null) and update UI accordingly.
//        val currentUser = auth.currentUser
//        if (currentUser == null) {
//            auth.signInAnonymously()
//                .addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
//                        // Sign in success, update UI with the signed-in user's information
//                        Log.d("MainActivity", "signInAnonymously:success")
//                        val user = auth.currentUser
//                        viewModel.onUserAuthenticated()
//                        // You can use the user object here
//                    } else {
//                        // If sign in fails, display a message to the user.
//                        Log.w("MainActivity", "signInAnonymously:failure", task.exception)
//                    }
//                }
//        } else {
//            Log.d("MainActivity", "User already signed in")
//            onUserAutheticated()
//
//        }
//    }


}