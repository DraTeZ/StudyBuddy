package com.example.studybuddy.ai

import com.example.studybuddy.BuildConfig
import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.DifficultyLevel
import com.google.ai.client.generativeai.GenerativeModel
import java.util.Locale

class GeminiAssistantReal : GeminiAssistantService { // <-- Constructor limpio

    // 1. OBTENER LA CLAVE API de forma segura
    private val API_KEY = BuildConfig.GEMINI_API_KEY

    // 2. INICIALIZAR EL MODELO usando el SDK de Google AI y la clave API
    private val model = GenerativeModel(
        modelName = "gemini-2.5-pro", // Puedes cambiar a "gemini-2.5-pro" si es lo que deseas usar
        apiKey = API_KEY
    )

    // Las funciones de prompt se mantienen IDÉNTICAS
    private fun getDifficultyPrompt(task: Task) =
        "Teniendo en cuenta los detalles ${task.details} y el nombre ${task.name}, selecciona solo 1 de estas dificultades: Fácil, Medio, o Difícil. Tu respuesta debe ser SOLO UNA de esas 3 palabras, NADA MAS."

    private fun getTimePrompt(task: Task) =
        "Teniendo en cuenta los detalles ${task.details} y el nombre ${task.name}, dame un número ENTERO en MINUTOS (De manera muy conservadora) para la tarea. Tu respuesta deberá ser SOLO EL NÚMERO, nada más."

    private fun getReasoningPrompt(task: Task, difficulty: String, time: String) =
        "Teniendo en cuenta los detalles ${task.details}, el nombre ${task.name}, la dificultad '$difficulty' y el tiempo '$time' recomendado, en 80 caracteres (máximo) dame tu razonamiento del porqué de esta dificultad y tiempo estimado."


    override suspend fun analyzeDifficulty(task: Task): DifficultyAnalysisResult {

        val difficultyResponse = model.generateContent(getDifficultyPrompt(task)).text?.trim() ?: "Fácil"

        val timeResponse = model.generateContent(getTimePrompt(task)).text?.trim() ?: "0"

        val reasoningResponse = model.generateContent(getReasoningPrompt(task, difficultyResponse, timeResponse)).text ?: "Análisis automático de tarea completado."

        val mappedDifficulty = when (difficultyResponse.lowercase(Locale.ROOT)) {
            "fácil" -> DifficultyLevel.EASY
            "medio" -> DifficultyLevel.MEDIUM
            "difícil" -> DifficultyLevel.HARD
            else -> DifficultyLevel.EASY
        }

        val mappedTime = timeResponse.toIntOrNull() ?: 0

        return DifficultyAnalysisResult(
            difficulty = mappedDifficulty,
            recommendedTimeMin = mappedTime,
            reasoning = reasoningResponse
        )
    }

    override suspend fun generateStudyTips(task: Task): String {

        val difficultyName = task.aiDifficulty?.name ?: DifficultyLevel.EASY.name

        val prompt = "Genera 5 consejos de estudio concisos para el tema: ${task.name}, tenieno en cuenta estos detalles ${task.details}. Nivel de dificultad: ${task.aiDifficulty}. No debe superar los 250 caracteres"

        val response = model.generateContent(prompt)
        return response.text ?: "No se pudieron generar consejos de estudio."
    }


    override suspend fun generateContent(task: Task, contentType: String): String {
        val prompt = when (contentType) {
            "Summary" -> "Genera un resumen conciso y 5 puntos clave para el tema de estudio: ${task.name}."
            "Flashcards" -> "Genera 10 pares de preguntas y respuestas (flashcards) para el tema: ${task.name}. Enfócate en definiciones y conceptos clave."
            else -> "Genera contenido educativo general sobre: ${task.name}."
        }

        val response = model.generateContent(prompt)

        return response.text ?: "No se pudo generar el contenido. Inténtalo de nuevo."
    }
}