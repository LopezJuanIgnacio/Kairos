package com.juanignaciolopez.kairos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanignaciolopez.kairos.core.utils.ValidationUtils
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onFirstNameChanged(value: String) {
        _uiState.update { it.copy(firstName = value, errorMessage = null) }
    }

    fun onLastNameChanged(value: String) {
        _uiState.update { it.copy(lastName = value, errorMessage = null) }
    }

    fun login() {
        val estado = _uiState.value
        if (!ValidationUtils.isValidEmail(estado.email)) {
            _uiState.update { it.copy(errorMessage = "Email inválido") }
            return
        }
        if (estado.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Contraseña requerida.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val resultado = authRepository.signIn(estado.email.trim(), estado.password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = resultado.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun register() {
        val estado = _uiState.value
        val nombreCompleto = "${estado.firstName.trim()} ${estado.lastName.trim()}".trim()

        if (!ValidationUtils.isValidEmail(estado.email)) {
            _uiState.update { it.copy(errorMessage = "Email inválido") }
            return
        }
        if (!ValidationUtils.isValidPassword(estado.password)) {
            _uiState.update {
                it.copy(errorMessage = "La contraseña debe tener 8+ caracteres, una mayúscula y un número")
            }
            return
        }
        if (!ValidationUtils.isValidName(nombreCompleto)) {
            _uiState.update { it.copy(errorMessage = "Nombre y apellido requeridos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val resultado = authRepository.signUp(
                    email = estado.email.trim(),
                    password = estado.password,
                    displayName = nombreCompleto
                )
            ) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = resultado.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun signInWithGoogle(idToken: String?) {
        if (idToken.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Google Sign-In no configurado en este build") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val resultado = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = resultado.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(isAuthenticated = false) }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val resultado = authRepository.signOut()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, isSignedOut = true)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = resultado.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun consumeSignOutNavigation() {
        _uiState.update { it.copy(isSignedOut = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}
