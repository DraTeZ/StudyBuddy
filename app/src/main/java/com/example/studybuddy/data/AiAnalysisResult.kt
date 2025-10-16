package com.example.studybuddy.data

import com.example.studybuddy.data.enums.DifficultyLevel

/**
 * Modelo de datos para el resultado del análisis de la IA (similar a tu Genkit Output Schema).
 */
data class AiAnalysisResult(
    val difficulty: DifficultyLevel,
    val recommendedTimeMin: Int,
    val reasoning: String
)
