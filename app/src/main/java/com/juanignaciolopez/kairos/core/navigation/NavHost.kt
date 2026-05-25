package com.juanignaciolopez.kairos.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.juanignaciolopez.kairos.ui.auth.LoginScreen
import com.juanignaciolopez.kairos.ui.auth.RegisterScreen
import com.juanignaciolopez.kairos.ui.dashboard.DashboardScreen
import com.juanignaciolopez.kairos.ui.onboarding.OnboardingScreen
import com.juanignaciolopez.kairos.ui.task_form.TaskFormScreen

/**
 * Composable para el Nav Host principal de la aplicación
 * Maneja la navegación entre los dos flujos principales: AUTH y MAIN
 */
@Composable
fun KairosNavHost(
    navController: NavHostController,
    startDestination: String,
    onOnboardingCompleted: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Graph - Flujo de autenticación
        authGraph(
            navController = navController,
            onOnboardingCompleted = onOnboardingCompleted
        )
        
        // Main Graph - Flujo principal de la app
        mainGraph(navController)
    }
}

/**
 * Define las pantallas del flujo de autenticación
 */
private fun androidx.navigation.NavGraphBuilder.authGraph(
    navController: NavHostController,
    onOnboardingCompleted: () -> Unit
) {
    composable(NavRoute.Onboarding.route) {
        OnboardingScreen(
            onFinish = {
                onOnboardingCompleted()
                navController.navigate(NavRoute.SignIn.route) {
                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                }
            },
            onSkip = {
                onOnboardingCompleted()
                navController.navigate(NavRoute.SignIn.route) {
                    popUpTo(NavRoute.Onboarding.route) { inclusive = true }
                }
            }
        )
    }
    
    composable(NavRoute.SignIn.route) {
        LoginScreen(
            onNavigateDashboard = {
                navController.navigate(NavRoute.Dashboard.route) {
                    popUpTo(NavRoute.SignIn.route) { inclusive = true }
                }
            },
            onNavigateRegister = { navController.navigate(NavRoute.SignUp.route) }
        )
    }
    
    composable(NavRoute.SignUp.route) {
        RegisterScreen(
            onNavigateDashboard = {
                navController.navigate(NavRoute.Dashboard.route) {
                    popUpTo(NavRoute.SignIn.route) { inclusive = true }
                }
            },
            onGoBack = { navController.popBackStack() }
        )
    }

}

/**
 * Define las pantallas del flujo principal
 */
private fun androidx.navigation.NavGraphBuilder.mainGraph(
    navController: NavHostController
) {
    composable(NavRoute.Dashboard.route) {
        DashboardScreen(
            onOpenHelp = { navController.navigate(NavRoute.Onboarding.route) },
            onCreateTask = { navController.navigate(NavRoute.TaskForm.route) },
            onEditTask = { taskId ->
                navController.navigate(NavRoute.TaskFormWithId.createRoute(taskId))
            },
            onSignedOut = {
                navController.navigate(NavRoute.SignIn.route) {
                    popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                }
            }
        )
    }
    
    composable(NavRoute.TaskForm.route) {
        TaskFormScreen(
            onCancel = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }
    
    composable(NavRoute.TaskFormWithId.route) { backStackEntry ->
        val idTarea = backStackEntry.arguments?.getString(NavRoute.TASK_ID_ARG)
        if (idTarea.isNullOrBlank()) {
            navController.popBackStack()
        } else {
            TaskFormScreen(
                onCancel = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }

}
