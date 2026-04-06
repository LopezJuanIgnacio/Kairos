package com.juanignaciolopez.kairos.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Servicio remoto para operaciones CRUD de tareas en Firestore.
 */
class FirebaseTaskService(
    private val firestore: FirebaseFirestore?
) {
    companion object {
        private const val TASKS_COLLECTION = "tasks"
        private const val USERS_COLLECTION = "users"
    }

    fun getAllTasks(idUsuario: String): Flow<List<Task>> = callbackFlow {
        val base = firestore
        if (base == null) {
            close(IllegalStateException("Firestore no está configurado"))
            return@callbackFlow
        }
        // callbackFlow permite adaptar listeners de Firebase a Flow de Kotlin.
        val suscripcion = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val tareas = snapshot?.documents?.mapNotNull { doc -> doc.toObject<Task>() } ?: emptyList()
                    trySend(tareas)
                }
            }
        awaitClose { suscripcion.remove() }
    }

    fun getTasksByStatus(idUsuario: String, estado: TaskStatus): Flow<List<Task>> = callbackFlow {
        val base = firestore
        if (base == null) {
            close(IllegalStateException("Firestore no está configurado"))
            return@callbackFlow
        }
        val suscripcion = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .whereEqualTo("status", estado)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    val tareas = snapshot?.documents?.mapNotNull { doc -> doc.toObject<Task>() } ?: emptyList()
                    trySend(tareas)
                }
            }
        awaitClose { suscripcion.remove() }
    }

    suspend fun getTaskById(idUsuario: String, idTarea: String): Result<Task> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        val documento = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(idTarea)
            .get()
            .await()

        documento.toObject<Task>()?.let {
            Result.Success(it)
        } ?: Result.Error("Tarea no encontrada")
    } catch (e: Exception) {
        Result.Error("Error al obtener tarea: ${e.message}", e)
    }

    suspend fun createTask(idUsuario: String, tarea: Task): Result<Task> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        val tareaConUsuario = tarea.copy(userId = idUsuario)
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(tarea.id)
            .set(tareaConUsuario)
            .await()
        Result.Success(tareaConUsuario)
    } catch (e: Exception) {
        Result.Error("Error al crear tarea: ${e.message}", e)
    }

    suspend fun updateTask(idUsuario: String, tarea: Task): Result<Task> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(tarea.id)
            .set(tarea)
            .await()
        Result.Success(tarea)
    } catch (e: Exception) {
        Result.Error("Error al actualizar tarea: ${e.message}", e)
    }

    suspend fun deleteTask(idUsuario: String, idTarea: String): Result<Unit> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(idTarea)
            .delete()
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al eliminar tarea: ${e.message}", e)
    }

    suspend fun completeTask(idUsuario: String, idTarea: String): Result<Unit> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(idTarea)
            .update(
                "status", TaskStatus.COMPLETED,
                "completedAt", System.currentTimeMillis()
            )
            .await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al completar tarea: ${e.message}", e)
    }
}
