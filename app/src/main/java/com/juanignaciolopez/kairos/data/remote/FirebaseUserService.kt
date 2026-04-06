package com.juanignaciolopez.kairos.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Servicio remoto para operaciones de usuario en Firestore.
 */
class FirebaseUserService(
    private val firestore: FirebaseFirestore?
) {
    companion object {
        private const val USERS_COLLECTION = "users"
    }

    fun getUserProfile(idUsuario: String): Flow<User?> = callbackFlow {
        val base = firestore
        if (base == null) {
            close(IllegalStateException("Firestore no está configurado"))
            return@callbackFlow
        }
        val suscripcion = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val usuario = snapshot?.toObject<User>()
                    trySend(usuario)
                }
            }
        awaitClose { suscripcion.remove() }
    }

    suspend fun getUserById(idUsuario: String): Result<User> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        val documento = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .get()
            .await()

        documento.toObject<User>()?.let {
            Result.Success(it)
        } ?: Result.Error("Usuario no encontrado")
    } catch (e: Exception) {
        Result.Error("Error al obtener usuario: ${e.message}", e)
    }

    suspend fun saveUserProfile(usuario: User): Result<User> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        val usuarioConMarcaTiempo = usuario.copy(updatedAt = System.currentTimeMillis())
        base
            .collection(USERS_COLLECTION)
            .document(usuario.id)
            .set(usuarioConMarcaTiempo)
            .await()
        Result.Success(usuarioConMarcaTiempo)
    } catch (e: Exception) {
        Result.Error("Error al guardar perfil: ${e.message}", e)
    }

    suspend fun updateProfilePicture(idUsuario: String, urlFoto: String): Result<Unit> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .update("photoUrl", urlFoto)
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al actualizar foto: ${e.message}", e)
    }

    suspend fun deleteUserAccount(idUsuario: String): Result<Unit> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .delete()
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al eliminar cuenta: ${e.message}", e)
    }
}
