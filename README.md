# Study Buddy AI: Tu Compañero de Estudio Inteligente con Técnica Pomodoro

## Visión General del Proyecto

**Study Buddy AI** es una aplicación de gestión de tareas y productividad desarrollada en **Android Nativo** utilizando **Jetpack Compose**. Su objetivo principal es potenciar la concentración y eficiencia del usuario mediante la integración de la **Técnica Pomodoro** y capacidades de **Inteligencia Artificial** impulsadas por **Google Gemini**.

La aplicación no solo organiza y distribuye el tiempo de estudio, sino que también analiza las tareas registradas para ofrecer recomendaciones personalizadas que optimizan el enfoque y el rendimiento académico.

---

## Características Principales

### Productividad con Técnica Pomodoro

* **Temporizador Integrado:** Permite iniciar ciclos de trabajo (enfoque), descansos cortos y descansos largos con un solo toque, adaptándose a la tarea activa.
* **Seguimiento de Tiempo:** Registra automáticamente los ciclos completados y el tiempo invertido en cada tarea, generando métricas claras sobre el esfuerzo y la productividad.
* **Estados Claros:** Proporciona una interfaz intuitiva que indica el estado actual del ciclo (trabajo, pausa o inactividad).

### Inteligencia Artificial (Gemini)

* **Análisis de Dificultad:** Al agregar una tarea, el sistema analiza su nombre y descripción para recomendar una dificultad estimada (fácil, media o difícil) y un tiempo de estudio sugerido.
* **Consejos de Estudio Personalizados:** Ofrece sugerencias específicas según la materia y el tema de la tarea, ayudando al usuario a superar bloqueos y mejorar su proceso de aprendizaje.
* **Generación de Contenido (Extensible):** La arquitectura está preparada para futuras integraciones que permitan crear resúmenes, tarjetas de estudio (flashcards) o estrategias personalizadas a partir de las descripciones de las tareas.

### Gestión de Tareas

* **Creación Rápida:** Permite añadir tareas con nombre, materia, nivel de dificultad percibido y fecha de entrega mediante un selector de calendario moderno.
* **Organización Clara:** Las tareas se presentan agrupadas según su estado (pendiente, en progreso o completada).
* **Flujo de Trabajo Eficiente:** Facilita la gestión del ciclo de vida de las tareas mediante opciones para marcarlas como completadas o eliminarlas.

---

## Stack Tecnológico

* **Lenguaje:** Kotlin
* **Framework de Interfaz:** Jetpack Compose (Modern UI Toolkit)
* **Arquitectura:** MVVM (Model-View-ViewModel) con uso de StateFlow para una reactividad sólida y escalable.
* **Backend e Inteligencia Artificial:** Integración de la API de **Google Gemini** en la capa de ViewModel.


