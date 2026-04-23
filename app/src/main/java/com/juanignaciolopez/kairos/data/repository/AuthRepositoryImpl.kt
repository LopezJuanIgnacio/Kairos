package com.juanignaciolopez.kairos.data.repository

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import com.juanignaciolopez.kairos.data.remote.FirebaseAuthService
import com.juanignaciolopez.kairos.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val servicioFirebaseAutenticacion: FirebaseAuthService
) : AuthRepository {

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return servicioFirebaseAutenticacion.signUp(email, password, displayName)
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        return servicioFirebaseAutenticacion.signIn(email, password)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return servicioFirebaseAutenticacion.signInWithGoogle(idToken)
    }

    override suspend fun signOut(): Result<Unit> {
        return servicioFirebaseAutenticacion.signOut()
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
