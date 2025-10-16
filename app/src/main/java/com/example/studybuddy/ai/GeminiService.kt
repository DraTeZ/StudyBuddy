package com.example.studybuddy.ai

import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.DifficultyLevel
import java.util.UUID



// Definición de resultado de análisis
data class DifficultyAnalysisResult(
    val difficulty: DifficultyLevel,
    val recommendedTimeMin: Int,
    val reasoning: String
)

// Interfaz del servicio de asistencia de Gemini
interface GeminiAssistantService {

    /** Analiza la dificultad de una tarea usando IA. */
    suspend fun analyzeDifficulty(task: Task): DifficultyAnalysisResult

    /** Genera consejos de estudio para la tarea. */
    suspend fun generateStudyTips(task: Task): String

    /** * Genera contenido educativo (resumen, flashcards) para la tarea.
     * @param contentType Un identificador del tipo de contenido (ej: "Summary", "Flashcards").
     */
    suspend fun generateContent(task: Task, contentType: String): String
}