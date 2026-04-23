package com.juanignaciolopez.kairos.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Servicio remoto para autenticación con Firebase.
 */
class FirebaseAuthService(
    private val firebaseAuth: FirebaseAuth?
) {
    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { state ->
            val user = state.currentUser?.let {
                User(
                    id = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName ?: "Usuario",
                    photoUrl = it.photoUrl?.toString(),
                    emailVerified = it.isEmailVerified
                )
            }
            trySend(user)
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(
        correo: String,
        contraseña: String,
        nombreMostrado: String
    ): Result<User> = try {
        val auth = firebaseAuth ?: return Result.Error("Firebase Auth no está configurado")
        val resultadoAutenticacion = auth.createUserWithEmailAndPassword(correo, contraseña).await()
        val usuarioFirebase = resultadoAutenticacion.user

        usuarioFirebase?.let {
            val actualizacionesPerfil = UserProfileChangeRequest.Builder()
                .setDisplayName(nombreMostrado)
                .build()
            it.updateProfile(actualizacionesPerfil).await()

            val usuario = User(
                id = it.uid,
                email = it.email ?: correo,
                displayName = nombreMostrado,
                photoUrl = it.photoUrl?.toString(),
                emailVerified = it.isEmailVerified
            )
            Result.Success(usuario)
        } ?: Result.Error("Usuario no creado correctamente")
    } catch (e: Exception) {
        Result.Error("Error en registro: ${e.message}", e)
    }

    suspend fun signIn(
        correo: String,
        contraseña: String
    ): Result<User> = try {
        val auth = firebaseAuth ?: return Result.Error("Firebase Auth no está configurado")
        val resultadoAutenticacion = auth.signInWithEmailAndPassword(correo, contraseña).await()
        val usuarioFirebase = resultadoAutenticacion.user

        usuarioFirebase?.let {
            val usuario = User(
                id = it.uid,
                email = it.email ?: correo,
                displayName = it.displayName ?: "Usuario",
                photoUrl = it.photoUrl?.toString(),
                emailVerified = it.isEmailVerified
            )
            Result.Success(usuario)
        } ?: Result.Error("Error en autenticación")
    } catch (e: Exception) {
        Result.Error("Error en login: ${e.message}", e)
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> = try {
        val auth = firebaseAuth ?: return Result.Error("Firebase Auth no está configurado")
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        val resultadoAutenticacion = auth.signInWithCredential(credencial).await()
        val usuarioFirebase = resultadoAutenticacion.user

        usuarioFirebase?.let {
            val usuario = User(
                id = it.uid,
                email = it.email ?: "",
                displayName = it.displayName ?: "Usuario",
                photoUrl = it.photoUrl?.toString(),
                emailVerified = it.isEmailVerified
            )
            Result.Success(usuario)
        } ?: Result.Error("Error en autenticación con Google")
    } catch (e: Exception) {
        Result.Error("Error en login con Google: ${e.message}", e)
    }

    fun signOut(): Result<Unit> = try {
        val auth = firebaseAuth ?: return Result.Error("Firebase Auth no está configurado")
        auth.signOut()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al cerrar sesión: ${e.message}", e)
    }

    suspend fun sendPasswordReset(correo: String): Result<Unit> = try {
        val auth = firebaseAuth ?: return Result.Error("Firebase Auth no está configurado")
        auth.sendPasswordResetEmail(correo).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al enviar email de reset: ${e.message}", e)
    }

    fun getCurrentUserId(): String? = firebaseAuth?.currentUser?.uid

    fun isUserAuthenticated(): Boolean = firebaseAuth?.currentUser != null
}
