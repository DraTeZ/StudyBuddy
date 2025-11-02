package com.example.studybuddy.vistas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.PasswordResetState
import com.example.studybuddy.presentacion.StudyBuddyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: StudyBuddyViewModel,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val resetState by viewModel.resetState.collectAsState()


    LaunchedEffect(resetState) {
        if (resetState is PasswordResetState.Success || resetState is PasswordResetState.Error) {

        }
    }

    if (resetState is PasswordResetState.Success) {
        AlertDialog(
            onDismissRequest = {
                viewModel.resetPasswordResetState()
                onBackClick()
            },
            title = { Text("Cambio de contraseña exitoso. Puedes volver a iniciar sesión") },
            text = { Text("Se ha enviado un enlace de recuperación a tu correo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetPasswordResetState()
                    onBackClick()
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (resetState is PasswordResetState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetPasswordResetState() },
            title = { Text("Error") },
            text = { Text((resetState as PasswordResetState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetPasswordResetState() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
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

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ingresa tu correo",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Te enviaremos un enlace para recuperar tu cuenta.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = resetState !is PasswordResetState.Loading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.sendPasswordReset(email) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = resetState !is PasswordResetState.Loading
                ) {
                    Text("Enviar enlace")
                }
            }

            if (resetState is PasswordResetState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}