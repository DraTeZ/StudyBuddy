package com.example.studybuddy.vistas

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studybuddy.presentacion.FeedbackState
import com.example.studybuddy.presentacion.StudyBuddyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    viewModel: StudyBuddyViewModel,
    onBackClick: () -> Unit
) {

    val feedbackOptions = listOf("Error (Bug)", "Sugerencia", "Otro")
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(feedbackOptions[0]) }
    var description by remember { mutableStateOf("") }
    val feedbackState by viewModel.feedbackState.collectAsState()

    if (feedbackState is FeedbackState.Success) {
        AlertDialog(
            onDismissRequest = {
                viewModel.resetFeedbackState()
                onBackClick()
            },
            title = { Text("¡Gracias!") },
            text = { Text("Hemos recibido tu reporte. Gracias por ayudarnos a mejorar.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetFeedbackState()
                    onBackClick()
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (feedbackState is FeedbackState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetFeedbackState() },
            title = { Text("Error") },
            text = { Text((feedbackState as FeedbackState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetFeedbackState() }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar un problema") },
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
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cuéntanos más acerca del problema o sugerencia que tienes.",
                    style = MaterialTheme.typography.bodyLarge
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de reporte") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        feedbackOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedType = option
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    enabled = feedbackState !is FeedbackState.Loading,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.sendFeedback(selectedType, description) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = feedbackState !is FeedbackState.Loading && description.isNotBlank()
                ) {
                    Text("Enviar reporte")
                }
            }

            if (feedbackState is FeedbackState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}