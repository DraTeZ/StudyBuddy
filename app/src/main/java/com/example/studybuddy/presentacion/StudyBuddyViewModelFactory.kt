package com.example.studybuddy.presentacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studybuddy.ai.GeminiAssistantService

// Este Factory es necesario porque StudyBuddyViewModel tiene un argumento en su constructor.
class StudyBuddyViewModelFactory(
    private val geminiService: GeminiAssistantService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica que la clase que se está solicitando es StudyBuddyViewModel
        if (modelClass.isAssignableFrom(StudyBuddyViewModel::class.java)) {
            // Crea y retorna la instancia del ViewModel, inyectando el servicio.
            return StudyBuddyViewModel(geminiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}