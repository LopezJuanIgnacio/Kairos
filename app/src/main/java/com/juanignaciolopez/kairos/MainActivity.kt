package com.juanignaciolopez.kairos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.juanignaciolopez.kairos.core.navigation.KairosNavHost
import com.juanignaciolopez.kairos.core.navigation.NavRoute
import com.juanignaciolopez.kairos.core.preferences.OnboardingPreferences
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.ui.theme.KairosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_Kairos)
        super.onCreate(savedInstanceState)

        setContent {
            KairosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    KairosApp(
                        authRepository = authRepository,
                        onboardingPreferences = OnboardingPreferences(applicationContext)
                    )
                }
            }
        }
    }
}

/**
 * Composable principal de la aplicación
 * Maneja la lógica de navegación y estados globales
 */
@Composable
fun KairosApp(
    authRepository: AuthRepository,
    onboardingPreferences: OnboardingPreferences
) {
    val isUserAuthenticated by produceState<Boolean?>(initialValue = null, authRepository) {
        value = authRepository.isUserAuthenticated()
    }

    if (isUserAuthenticated == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (isUserAuthenticated == true) {
        NavRoute.Dashboard.route
    } else if (!onboardingPreferences.isCompleted()) {
        NavRoute.Onboarding.route
    } else {
        NavRoute.SignIn.route
    }
    
    KairosNavHost(
        navController = navController,
        startDestination = startDestination,
        onOnboardingCompleted = onboardingPreferences::markCompleted
    )
}