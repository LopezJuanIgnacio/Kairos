package com.juanignaciolopez.kairos.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.juanignaciolopez.kairos.ui.auth.AuthViewModel
import com.juanignaciolopez.kairos.ui.auth.LoginScreen
import com.juanignaciolopez.kairos.ui.auth.RegisterScreen

/**
 * Composable para el Nav Host principal de la aplicación
 * Maneja la navegación entre los dos flujos principales: AUTH y MAIN
 */
@Composable
fun KairosNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Graph - Flujo de autenticación
        authGraph(navController)
        
        // Main Graph - Flujo principal de la app
        mainGraph(navController)
    }
}

/**
 * Define las pantallas del flujo de autenticación
 */
private fun androidx.navigation.NavGraphBuilder.authGraph(
    navController: NavHostController
) {
    composable(NavRoute.Onboarding.route) {
        LoginScreen(
            onNavigateDashboard = {
                navController.navigate(NavRoute.Dashboard.route) {
                    popUpTo(NavRoute.SignIn.route) { inclusive = true }
                }
            },
            onNavigateRegister = { navController.navigate(NavRoute.SignUp.route) }
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
    
    composable(NavRoute.ForgotPassword.route) {
        // ForgotPasswordScreen(navController)
        // TODO: Implementar ForgotPasswordScreen
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
            onSignedOut = {
                navController.navigate(NavRoute.SignIn.route) {
                    popUpTo(NavRoute.Dashboard.route) { inclusive = true }
                }
            }
        )
    }
    
    composable(NavRoute.TaskList.route) {
        // TaskListScreen(navController)
        // TODO: Implementar TaskListScreen
    }
    
    composable(NavRoute.TaskForm.route) {
        // TaskFormScreen(navController, taskId = null)
        // TODO: Implementar TaskFormScreen
    }
    
    composable(NavRoute.TaskFormWithId.route) { backStackEntry ->
        val idTarea = backStackEntry.arguments?.getString(NavRoute.TASK_ID_ARG) ?: ""
        // TaskFormScreen(navController, taskId = taskId)
        // TODO: Implementar TaskFormScreen con edición
    }
    
    composable(NavRoute.TaskDetail.route) { backStackEntry ->
        val idTarea = backStackEntry.arguments?.getString(NavRoute.TASK_ID_ARG) ?: ""
        // TaskDetailScreen(navController, taskId)
        // TODO: Implementar TaskDetailScreen
    }
    
    composable(NavRoute.Profile.route) {
        // ProfileScreen(navController)
        // TODO: Implementar ProfileScreen
    }
    
    composable(NavRoute.Settings.route) {
        // SettingsScreen(navController)
        // TODO: Implementar SettingsScreen
    }
    
    composable(NavRoute.Notifications.route) {
        // NotificationsScreen(navController)
        // TODO: Implementar NotificationsScreen
    }
}

@Composable
private fun DashboardScreen(
    onSignedOut: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedOut) {
        if (state.isSignedOut) {
            onSignedOut()
            viewModel.consumeSignOutNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )
        )

        Button(
            onClick = viewModel::signOut,
            enabled = !state.isLoading,
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Cerrar sesión")
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
