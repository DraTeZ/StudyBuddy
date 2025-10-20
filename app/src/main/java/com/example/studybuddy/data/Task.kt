package com.example.studybuddy.data

import com.example.studybuddy.data.enums.DifficultyLevel
import com.example.studybuddy.data.enums.TaskStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

/**
 * Modelo principal para almacenar la información de una tarea.
 */
data class Task(
    @DocumentId
    var id: String = "",
    // Usamos 'name' y 'subject' como está en la vista AddTaskDialog
    val name: String,
    val subject: String,
    val dueDate: Date? = null, // Timestamp
    @ServerTimestamp
    val creationDate: Date? = null, // Timestamp
    val userDifficulty: DifficultyLevel,
    val details: String = "", // Este es el campo opcional 'details'

    // Resultados del análisis de la IA
    val aiDifficulty: DifficultyLevel? = null,
    val recommendedTimeMin: Int? = null,
    val aiReasoning: String? = null,
    val aiStudyTips: String? = null,

    // Métricas de progreso
    var status: TaskStatus = TaskStatus.TODO,
    val totalTimeSpentMs: Long = 0L,
    val totalPomodoroCycles: Int = 0
){
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", null, null, DifficultyLevel.EASY, "", null, null, null, null, TaskStatus.TODO, 0L, 0)
}
