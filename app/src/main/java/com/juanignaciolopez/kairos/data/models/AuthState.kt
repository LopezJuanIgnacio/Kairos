package com.juanignaciolopez.kairos.data.models

/**
 * Estados posibles del flujo de autenticación.
 */
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String, val exception: Exception? = null) : AuthState()
    data object Unauthenticated : AuthState()
}

// Alias de transición para nombres en español.
typealias EstadoAutenticacion = AuthState
