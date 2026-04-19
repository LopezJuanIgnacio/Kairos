package com.juanignaciolopez.kairos.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.toObject
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.data.models.TaskPriority
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
                    val tareas = snapshot?.documents?.mapNotNull { doc ->
                        taskFromDocument(doc)
                    } ?: emptyList()
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
                    val tareas = snapshot?.documents?.mapNotNull { doc ->
                        taskFromDocument(doc)
                    } ?: emptyList()
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

        taskFromDocument(documento)?.let {
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

    private fun taskFromDocument(documento: com.google.firebase.firestore.DocumentSnapshot): Task? {
        val directo = runCatching { documento.toObject<Task>() }.getOrNull()
        if (directo != null) return directo

        val data = documento.data ?: return null
        return buildTaskFromMap(data, documento.id)
    }

    private fun buildTaskFromMap(data: Map<String, Any?>, docId: String): Task {
        val id = data["id"]?.toString()?.takeIf { it.isNotBlank() } ?: docId

        return Task(
            id = id,
            userId = data["userId"]?.toString().orEmpty(),
            title = data["title"]?.toString().orEmpty(),
            description = data["description"]?.toString().orEmpty(),
            status = parseStatus(data["status"]),
            priority = parsePriority(data["priority"]),
            category = parseCategory(data["category"]),
            createdAt = parseLong(data["createdAt"]) ?: System.currentTimeMillis(),
            updatedAt = parseLong(data["updatedAt"]) ?: System.currentTimeMillis(),
            scheduledDate = parseLong(data["scheduledDate"]),
            dueDate = parseLong(data["dueDate"]),
            completedAt = parseLong(data["completedAt"]),
            isRecurring = parseBoolean(data["isRecurring"]),
            recurrencePattern = data["recurrencePattern"]?.toString(),
            parentTaskId = data["parentTaskId"]?.toString(),
            estimatedMinutes = parseInt(data["estimatedMinutes"]),
            context = data["context"]?.toString().orEmpty(),
            project = data["project"]?.toString(),
            isNextAction = parseBoolean(data["isNextAction"]),
            isSyncPending = parseBoolean(data["isSyncPending"]),
            lastSyncedAt = parseLong(data["lastSyncedAt"]),
            tags = parseTags(data["tags"])
        )
    }

    private fun parseStatus(raw: Any?): TaskStatus {
        val value = raw?.toString()?.trim().orEmpty().uppercase()
        return when (value) {
            "INBOX" -> TaskStatus.INBOX
            "PROCESSING" -> TaskStatus.PROCESSING
            "TODO" -> TaskStatus.TODO
            "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
            "COMPLETED" -> TaskStatus.COMPLETED
            "ARCHIVED" -> TaskStatus.ARCHIVED
            "DELETED" -> TaskStatus.DELETED
            else -> TaskStatus.INBOX
        }
    }

    private fun parsePriority(raw: Any?): TaskPriority {
        val value = raw?.toString()?.trim().orEmpty().uppercase()
        return when (value) {
            "VERY_HIGH", "CRITICA", "CRÍTICA" -> TaskPriority.VERY_HIGH
            "HIGH", "ALTA" -> TaskPriority.HIGH
            "NORMAL" -> TaskPriority.NORMAL
            "LOW", "BAJA" -> TaskPriority.LOW
            "VERY_LOW", "MUY_BAJA" -> TaskPriority.VERY_LOW
            else -> TaskPriority.NORMAL
        }
    }

    private fun parseCategory(raw: Any?): TaskCategory {
        val value = raw?.toString()?.trim().orEmpty().uppercase()
        return when (value) {
            "RECURRENT", "RECURRENTE" -> TaskCategory.RECURRENT
            "ACTIONABLE", "ACCIONABLE" -> TaskCategory.ACTIONABLE
            "SHORT_TERM", "CORTO_PLAZO", "CORTO PLAZO" -> TaskCategory.SHORT_TERM
            "LONG_TERM", "LARGO_PLAZO", "LARGO PLAZO" -> TaskCategory.LONG_TERM
            "INCUBATOR", "INCUBADORA" -> TaskCategory.INCUBATOR
            "WORK", "PERSONAL", "HEALTH", "FINANCE", "LEARNING", "FAMILY", "HOME", "SOMEDAY", "OTHER" -> TaskCategory.ACTIONABLE
            else -> TaskCategory.ACTIONABLE
        }
    }

    private fun parseLong(raw: Any?): Long? = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }

    private fun parseInt(raw: Any?): Int = when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: 0
        else -> 0
    }

    private fun parseBoolean(raw: Any?): Boolean = when (raw) {
        is Boolean -> raw
        is String -> raw.equals("true", ignoreCase = true)
        else -> false
    }

    private fun parseTags(raw: Any?): List<String> = when (raw) {
        is List<*> -> raw.mapNotNull { it?.toString() }
        else -> emptyList()
    }
}
