package com.juanignaciolopez.kairos.domain.repository

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para autenticación.
 */
interface AuthRepository {
    fun getCurrentUser(): Flow<User?>

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User>

    suspend fun signIn(
        email: String,
        password: String
    ): Result<User>

    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun signOut(): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun getCurrentUserId(): String?

    suspend fun isUserAuthenticated(): Boolean
}
