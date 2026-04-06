package com.juanignaciolopez.kairos.core.navigation

/**
 * Sealed class que contiene todas las rutas de navegación de la app
 */
sealed class NavRoute(val route: String) {
    
    // Rutas de autenticación
    data object Onboarding : NavRoute("onboarding")
    data object SignIn : NavRoute("signin")
    data object SignUp : NavRoute("signup")
    data object ForgotPassword : NavRoute("forgot_password")
    
    // Rutas principales (después de login)
    data object Dashboard : NavRoute("dashboard")
    data object TaskList : NavRoute("task_list")
    
    // Rutas de tareas
    data object TaskForm : NavRoute("task_form")
    data object TaskFormWithId : NavRoute("task_form/{taskId}") {
        fun createRoute(idTarea: String) = "task_form/$idTarea"
    }
    data object TaskDetail : NavRoute("task_detail/{taskId}") {
        fun createRoute(idTarea: String) = "task_detail/$idTarea"
    }
    
    // Rutas de usuario
    data object Profile : NavRoute("profile")
    data object Settings : NavRoute("settings")
    data object Notifications : NavRoute("notifications")
    
    companion object {
        const val TASK_ID_ARG = "taskId"
    }
}

/**
 * Enumeración de los flujos de navegación principales
 */
enum class NavGraph {
    AUTH,      // Onboarding, SignIn, SignUp
    MAIN       // Dashboard, Tasks, Profile
}
