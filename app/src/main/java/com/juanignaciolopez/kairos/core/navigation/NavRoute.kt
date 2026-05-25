package com.juanignaciolopez.kairos.core.navigation

/**
 * Sealed class que contiene todas las rutas de navegación de la app
 */
sealed class NavRoute(val route: String) {
    
    // Rutas de autenticación
    data object Onboarding : NavRoute("onboarding")
    data object SignIn : NavRoute("signin")
    data object SignUp : NavRoute("signup")
    
    // Rutas principales (después de login)
    data object Dashboard : NavRoute("dashboard")
    
    // Rutas de tareas
    data object TaskForm : NavRoute("task_form")
    data object TaskFormWithId : NavRoute("task_form/{taskId}") {
        fun createRoute(idTarea: String) = "task_form/$idTarea"
    }
    companion object {
        const val TASK_ID_ARG = "taskId"
    }
}