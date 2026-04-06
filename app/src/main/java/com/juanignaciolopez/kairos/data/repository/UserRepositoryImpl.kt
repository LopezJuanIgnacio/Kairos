package com.juanignaciolopez.kairos.data.repository

import com.juanignaciolopez.kairos.data.local.UserDao
import com.juanignaciolopez.kairos.data.remote.FirebaseUserService
import com.juanignaciolopez.kairos.data.models.User
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementación del repositorio de usuario
 */
class UserRepositoryImpl(
    private val daoUsuario: UserDao,
    private val servicioFirebaseUsuario: FirebaseUserService
) : UserRepository {
    
    override fun getCurrentUserProfile(): Flow<User?> {
        return daoUsuario.getCurrentUser()
    }
    
    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val usuario = daoUsuario.getUserById(userId)
            if (usuario != null) {
                Result.Success(usuario)
            } else {
                // Intentar obtener de Firebase
                servicioFirebaseUsuario.getUserById(userId)
            }
        } catch (e: Exception) {
            Result.Error("Error al obtener usuario: ${e.message}", e)
        }
    }
    
    override suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            // Guardamos local primero para respuesta inmediata en UI.
            daoUsuario.updateUser(user)
            servicioFirebaseUsuario.saveUserProfile(user)
        } catch (e: Exception) {
            Result.Error("Error al actualizar perfil: ${e.message}", e)
        }
    }
    
    override suspend fun updateProfilePicture(userId: String, imageUrl: String): Result<Unit> {
        return try {
            daoUsuario.getUserById(userId)?.let { usuario ->
                val usuarioActualizado = usuario.copy(photoUrl = imageUrl)
                daoUsuario.updateUser(usuarioActualizado)
            }
            servicioFirebaseUsuario.updateProfilePicture(userId, imageUrl)
        } catch (e: Exception) {
            Result.Error("Error al actualizar foto: ${e.message}", e)
        }
    }
    
    override suspend fun deleteUserAccount(userId: String): Result<Unit> {
        return try {
            daoUsuario.deleteUserById(userId)
            servicioFirebaseUsuario.deleteUserAccount(userId)
        } catch (e: Exception) {
            Result.Error("Error al eliminar cuenta: ${e.message}", e)
        }
    }
    
    override suspend fun updateThemePreference(userId: String, theme: String): Result<Unit> {
        return try {
            daoUsuario.getUserById(userId)?.let { usuario ->
                val usuarioActualizado = usuario.copy(themePreference = theme)
                daoUsuario.updateUser(usuarioActualizado)
                servicioFirebaseUsuario.saveUserProfile(usuarioActualizado)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Error al actualizar tema: ${e.message}", e)
        }
    }
    
    override suspend fun updateLanguagePreference(userId: String, language: String): Result<Unit> {
        return try {
            daoUsuario.getUserById(userId)?.let { usuario ->
                val usuarioActualizado = usuario.copy(language = language)
                daoUsuario.updateUser(usuarioActualizado)
                servicioFirebaseUsuario.saveUserProfile(usuarioActualizado)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Error al actualizar idioma: ${e.message}", e)
        }
    }
}
