package com.example.studybuddy.data

import com.example.studybuddy.data.enums.DifficultyLevel
import com.example.studybuddy.data.enums.TaskStatus
import java.util.UUID

/**
 * Modelo principal para almacenar la información de una tarea.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    // Usamos 'name' y 'subject' como está en la vista AddTaskDialog
    val name: String,
    val subject: String,
    val dueDate: Long, // Timestamp
    val userDifficulty: DifficultyLevel,
    val details: String = "", // Este es el campo opcional 'details'

    // Resultados del análisis de la IA
    val aiDifficulty: DifficultyLevel? = null,
    val recommendedTimeMin: Int? = null,
    val aiReasoning: String? = null,
    val aiStudyTips: String? = null,

    // Métricas de progreso
    val status: TaskStatus = TaskStatus.TODO,
    val timeSpentMs: Long = 0L,
    val pomodoroCycles: Int = 0
)
