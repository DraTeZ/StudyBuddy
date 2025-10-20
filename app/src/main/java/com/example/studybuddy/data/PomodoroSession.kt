package com.example.studybuddy.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una sesión de estudio completa para una tarea, que puede contener
 * múltiples ciclos de trabajo/descanso. Se persiste cuando el usuario detiene
 * el temporizador definitivamente.
 */
data class PomodoroSession(
    @DocumentId
    var id: String = "",

    @ServerTimestamp // Firestore asignará la fecha del servidor cuando se crea el documento.
    var startDatetime: Date? = null,

    var finishDatetime: Date? = null,
    var completedCycles: Int = 0,
    var timeUsed: Long = 0L // Duración total en milisegundos de la sesión
) {
    // Constructor vacío requerido por Firestore para la deserialización
    constructor() : this("", null, null, 0, 0L)
}