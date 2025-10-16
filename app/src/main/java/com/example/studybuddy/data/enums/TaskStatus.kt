package com.example.studybuddy.data.enums

/**
 * Define el estado de progreso de una tarea.
 */
enum class TaskStatus(val label: String) {
    TODO("Por Hacer"),
    IN_PROGRESS("En Progreso"),
    COMPLETED("Completada"),
    OVERDUE("Vencida")
}
