package com.example.studybuddy.ai



import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.DifficultyLevel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay

class GeminiAssistantReal : GeminiAssistantService { // <-- Constructor limpio

    // Firebase automáticamente usa la clave de google-services.json
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")

    // Implementación de analyzeDifficulty (Ejemplo simulado, reemplaza con tu lógica real de IA)
    override suspend fun analyzeDifficulty(task: Task): DifficultyAnalysisResult {
        // En una app real, aquí construirías un prompt y llamarías a model.generateContent()
        // Por ahora, simulamos un resultado para que el ViewModel funcione.
        delay(500) // Simula la latencia de la red
        return DifficultyAnalysisResult(
            difficulty = DifficultyLevel.MEDIUM,
            recommendedTimeMin = 45,
            reasoning = "El tema es complejo pero conocido."
        )
    }

    // Implementación de generateStudyTips (Ejemplo simulado)
    override suspend fun generateStudyTips(task: Task): String {
        delay(500) // Simula la latencia de la red
        val difficultyName = task.aiDifficulty?.name ?: DifficultyLevel.EASY.name

        val prompt = "Genera 5 consejos de estudio concisos para el tema: ${task.name}. Nivel de dificultad: $difficultyName. No debe superar los 250 caracteres"

        val response = model.generateContent(prompt)
        return response.text ?: "No se pudieron generar consejos de estudio."
    }


    override suspend fun generateContent(task: Task, contentType: String): String {
        val prompt = when (contentType) {
            "Summary" -> "Genera un resumen conciso y 5 puntos clave para el tema de estudio: ${task.name}."
            "Flashcards" -> "Genera 10 pares de preguntas y respuestas (flashcards) para el tema: ${task.name}. Enfócate en definiciones y conceptos clave."
            else -> "Genera contenido educativo general sobre: ${task.name}."
        }

        // La llamada a la función suspend dentro de otra función suspend es CORRECTA.
        val response = model.generateContent(prompt)

        return response.text ?: "No se pudo generar el contenido. Inténtalo de nuevo."
    }
}
