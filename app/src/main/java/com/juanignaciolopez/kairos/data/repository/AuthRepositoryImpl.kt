package com.juanignaciolopez.kairos.data.repository

import com.juanignaciolopez.kairos.data.local.UserDao
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import com.juanignaciolopez.kairos.data.remote.FirebaseAuthService
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementación del repositorio de autenticación.
 * Sincroniza login/registro remoto con caché local de usuario.
 */
class AuthRepositoryImpl(
    private val servicioFirebaseAutenticacion: FirebaseAuthService,
    private val daoUsuario: UserDao
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = daoUsuario.getCurrentUser()

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        val resultado = servicioFirebaseAutenticacion.signUp(email, password, displayName)
        if (resultado is Result.Success) {
            daoUsuario.insertUser(resultado.data)
        }
        return resultado
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        val resultado = servicioFirebaseAutenticacion.signIn(email, password)
        if (resultado is Result.Success) {
            daoUsuario.insertUser(resultado.data)
        }
        return resultado
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        val resultado = servicioFirebaseAutenticacion.signInWithGoogle(idToken)
        if (resultado is Result.Success) {
            daoUsuario.insertUser(resultado.data)
        }
        return resultado
    }

    override suspend fun signOut(): Result<Unit> {
        val resultado = servicioFirebaseAutenticacion.signOut()
        if (resultado is Result.Success) {
            // Si el cierre remoto fue exitoso, limpiamos cache local para evitar estado fantasma.
            daoUsuario.deleteAllUsers()
        }
        return resultado
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return servicioFirebaseAutenticacion.sendPasswordReset(email)
    }

    override fun getCurrentUserId(): String? {
        return servicioFirebaseAutenticacion.getCurrentUserId()
    }

    override suspend fun isUserAuthenticated(): Boolean {
        return servicioFirebaseAutenticacion.isUserAuthenticated()
    }
}
