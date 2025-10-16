package com.example.studybuddy.ai



import com.example.studybuddy.data.Task
import com.example.studybuddy.data.enums.DifficultyLevel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay
import java.util.Locale

class GeminiAssistantReal : GeminiAssistantService { // <-- Constructor limpio

    // Firebase automáticamente usa la clave de google-services.json
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")
    private fun getDifficultyPrompt(task: Task) =
        "Teniendo en cuenta los detalles ${task.details} y el nombre ${task.name}, selecciona solo 1 de estas dificultades: Fácil, Medio, o Difícil. Tu respuesta debe ser SOLO UNA de esas 3 palabras, NADA MAS."

    private fun getTimePrompt(task: Task) =
        "Teniendo en cuenta los detalles ${task.details} y el nombre ${task.name}, dame un número ENTERO en MINUTOS (De manera muy conservadora) para la tarea. Tu respuesta deberá ser SOLO EL NÚMERO, nada más."

    private fun getReasoningPrompt(task: Task, difficulty: String, time: String) =
        "Teniendo en cuenta los detalles ${task.details}, el nombre ${task.name}, la dificultad '$difficulty' y el tiempo '$time' recomendado, en 80 caracteres (máximo) dame tu razonamiento del porqué de esta dificultad y tiempo estimado."


    override suspend fun analyzeDifficulty(task: Task): DifficultyAnalysisResult {
        // Simula la latencia de la red
        delay(300)

        // OBTENER DIFICULTAD REAL DE LA IA
        val difficultyResponse = model.generateContent(getDifficultyPrompt(task)).text?.trim() ?: "Fácil"

        // OBTENER TIEMPO REAL DE LA IA
        val timeResponse = model.generateContent(getTimePrompt(task)).text?.trim() ?: "0"

        // OBTENER RAZONAMIENTO REAL DE LA IA
        val reasoningResponse = model.generateContent(getReasoningPrompt(task, difficultyResponse, timeResponse)).text ?: "Análisis automático de tarea completado."


        // MAPEO CORRECTO
        val mappedDifficulty = when (difficultyResponse.lowercase(Locale.ROOT)) {
            "fácil" -> DifficultyLevel.EASY
            "medio" -> DifficultyLevel.MEDIUM
            "difícil" -> DifficultyLevel.HARD
            else -> DifficultyLevel.EASY
        }

        // CASTEADO SEGURO
        val mappedTime = timeResponse.toIntOrNull() ?: 0

        return DifficultyAnalysisResult(
            difficulty = mappedDifficulty,
            recommendedTimeMin = mappedTime,
            reasoning = reasoningResponse
        )
    }

    // Implementación de generateStudyTips (Ejemplo simulado)
    override suspend fun generateStudyTips(task: Task): String {
        delay(500) // Simula la latencia de la red
        val difficultyName = task.aiDifficulty?.name ?: DifficultyLevel.EASY.name

        val prompt = "Genera 5 consejos de estudio concisos para el tema: ${task.name}, tenieno en cuenta estos detalles ${task.details}. Nivel de dificultad: $difficultyName. No debe superar los 250 caracteres"

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
