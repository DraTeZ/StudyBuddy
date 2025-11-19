package com.example.studybuddy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.studybuddy.ai.GeminiAssistantReal
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.example.studybuddy.presentacion.StudyBuddyViewModelFactory
import com.example.studybuddy.vistas.StudyBuddyApp
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


private val StudyBuddyLightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),       // Green for focus
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF2196F3),     // Blue for information
    tertiary = Color(0xFFFF9800),      // Orange for alerts/emphasis
    background = Color(0xFFF7F7F7),
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = Color.Black
)

private val StudyBuddyDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Un verde más claro para modo oscuro
    primaryContainer = Color(0xFF388E3C),
    secondary = Color(0xFF64B5F6), // Un azul más claro
    tertiary = Color(0xFFFFB74D), // Un naranja más claro
    background = Color(0xFF121212), // Fondo oscuro estándar
    surface = Color(0xFF1E1E1E), // Superficie oscura
    onPrimary = Color.Black,
    onSurface = Color.White
)

@Composable
fun StudyBuddyTheme(
    isDark: Boolean, // Parámetro para controlar el modo
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) {
        StudyBuddyDarkColorScheme
    } else {
        StudyBuddyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, // Aplicar el esquema seleccionado
        content = content
    )
}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    // Creamos la factory para el ViewModel
    private val factory by lazy { StudyBuddyViewModelFactory(GeminiAssistantReal()) }

    // Creamos UNA SOLA instancia del ViewModel, que será compartida
    private val viewModel: StudyBuddyViewModel by viewModels { factory }

    // Inicializamos el Credential Manager
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase Auth
        auth = Firebase.auth
        if (auth.currentUser != null) {
            Log.d("MainActivity", "User ${auth.currentUser?.uid} is already signed in.")
            viewModel.onUserAuthenticated()
            viewModel.forceAuthStateSuccess()
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            StudyBuddyTheme(isDark = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyBuddyApp(
                        viewModel = viewModel,
                        onLogout = ::completeSignOut,
                        onGoogleSignIn = ::handleGoogleSignInRequest
                    )
                }
            }
        }
    }


    /**
     * Inicia el flujo de Google Sign-In usando Credential Manager.
     */
    private fun handleGoogleSignInRequest() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                // Muestra el selector de cuentas de Google
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity
                )
                // Procesa el resultado
                handleSignInResult(result.credential)
            } catch (e: Exception) {

                Log.e("MainActivity", "Google Sign-In failed: ${e.localizedMessage}")
                viewModel.handleAuthError("Inicio de sesión cancelado o fallido.")
            }
        }
    }

    /**
     * Procesa el resultado de Credential Manager y lo envía al ViewModel.
     */
    private fun handleSignInResult(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Llama al ViewModel para que Firebase valide el token
            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)

        } else {
            Log.w("MainActivity", "Credential is not of type Google ID!")
            viewModel.handleAuthError("Credencial de Google no válida.")
        }
    }

    /**
     * Cierra la sesión en Firebase (ViewModel) Y limpia el estado de Credential Manager.
     */
    private fun completeSignOut() {
        // Cierra sesión en Firebase y limpia el estado del ViewModel
        viewModel.logout()

        // Limpia el estado de credenciales de Google
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                Log.d("MainActivity", "Credenciales de Google limpiadas.")
            } catch (e: ClearCredentialException) {
                Log.e("MainActivity", "Error al limpiar credenciales: ${e.localizedMessage}")
            }
        }
    }
}