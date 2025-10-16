package com.example.studybuddy.data.enums

/**
 * Define los posibles estados del temporizador Pomodoro.
 */
enum class PomodoroState {
    IDLE,        // Inactivo, listo para comenzar
    WORK,        // Sesión de trabajo
    SHORT_BREAK, // Descanso corto
    LONG_BREAK,  // Descanso largo
    PAUSED       // En pausa
}
