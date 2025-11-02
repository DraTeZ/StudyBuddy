package com.example.studybuddy.vistas

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.studybuddy.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.StudyBuddyViewModel
import com.example.studybuddy.presentacion.AuthState
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: StudyBuddyViewModel,
    onLoginClick: () -> Unit,
    onHelpClick: () -> Unit,
    onReportProblemClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
            viewModel.resetAuthState()
        }
    }

    if (authState is AuthState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logo_studybuddy),
                    contentDescription = "Logo de StudyBuddy",
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it }
                    )

                    ClickableText(
                        text = buildAnnotatedString {
                            append("Acepto los ")
                            withStyle(style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )) {
                                append("Términos y Condiciones")
                            }
                        },
                        onClick = { showTermsDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.register(name, email, password) },
                    modifier = Modifier.fillMaxWidth(),

                    enabled = authState !is AuthState.Loading &&
                            name.isNotBlank() &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            password == confirmPassword &&
                            termsAccepted
                ) {
                    Text("Crear cuenta")
                }

                TextButton(onClick = onLoginClick) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onHelpClick) {
                    Text("Ayuda")
                }
                TextButton(onClick = onReportProblemClick) {
                    Text("Reportar un problema")
                }
            }
        }
    }
}
@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Términos y Condiciones") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(
                    text = """
Bienvenido a StudyBuddy.
Cuando uses esta aplicación, aceptas los siguientes términos y condiciones descritos a continuación:

1. Uso de la cuenta
Eres responsable de mantener la confidencialidad de tu cuenta y contraseña. Eres responsable de todas las actividades que ocurran dentro de la misma.

2. Contenido del usuario
Nos reservamos el derecho de eliminar cualquier contenido que consideremos inapropiado, ofensivo o que viole estos términos.

3. Propiedad intelectual
El servicio y su contenido original, características y funcionalidad son propiedad de StudyBuddy y están protegidos por leyes de derechos de autor.

4. Limitación de responsabilidad
La aplicación se proporciona tal como se muestra. No garantizamos que la aplicación esté libre de errores o que el acceso sea continuo e ininterrumpido.

5. Modificaciones de los términos y condiciones
Nos reservamos el derecho de modificar estos términos en cualquier momento. Te notificaremos acerca de cualquier cambio una vez sean publicados los nuevos términos en esta aplicación.

6. Contacto
Si tienes alguna duda sobre estos términos y condiciones, por favor contáctanos.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}