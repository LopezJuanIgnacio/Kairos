package com.juanignaciolopez.kairos.domain.repository

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para gestión de perfil de usuario.
 */
interface UserRepository {
    fun getCurrentUserProfile(): Flow<User?>

    suspend fun getUserById(userId: String): Result<User>

    suspend fun updateUserProfile(user: User): Result<User>

    suspend fun updateProfilePicture(userId: String, imageUrl: String): Result<Unit>

    suspend fun deleteUserAccount(userId: String): Result<Unit>

    suspend fun updateThemePreference(userId: String, theme: String): Result<Unit>

    suspend fun updateLanguagePreference(userId: String, language: String): Result<Unit>
}
