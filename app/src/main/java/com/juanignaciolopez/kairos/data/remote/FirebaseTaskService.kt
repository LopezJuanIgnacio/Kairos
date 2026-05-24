package com.juanignaciolopez.kairos.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.toObject
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * CRUD de tareas en Firestore.
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
        Result.Error("Error al obtener tarea: ${firestoreErrorMessage(e)}", e)
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
        Result.Error("Error al crear tarea: ${firestoreErrorMessage(e)}", e)
    }

    suspend fun updateTask(idUsuario: String, tarea: Task): Result<Task> = try {
        val base = firestore ?: return Result.Error("Firestore no está configurado")
        Log.d("KairosExport", "updateTask: writing doc=${tarea.id} isExported=${tarea.isExported}")
        val docRef = base
            .collection(USERS_COLLECTION)
            .document(idUsuario)
            .collection(TASKS_COLLECTION)
            .document(tarea.id)

        docRef
            .set(tarea)
            .addOnSuccessListener {
                Log.d("KairosExport", "updateTask: write success doc=${tarea.id}")
            }
            .addOnFailureListener { e ->
                Log.d("KairosExport", "updateTask: write failure doc=${tarea.id} error=${e.message}")
            }
            .await()

        // Read back the document to verify stored fields
        try {
            val fresh = docRef.get().await()
            Log.d("KairosExport", "updateTask: readback doc=${tarea.id} data=${fresh.data}")
        } catch (e: Exception) {
            Log.d("KairosExport", "updateTask: readback failed doc=${tarea.id} error=${e.message}")
        }

        Log.d("KairosExport", "updateTask: completed await doc=${tarea.id}")
        Result.Success(tarea)
    } catch (e: Exception) {
        Result.Error("Error al actualizar tarea: ${firestoreErrorMessage(e)}", e)
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
        Result.Error("Error al eliminar tarea: ${firestoreErrorMessage(e)}", e)
    }

    private fun firestoreErrorMessage(error: Exception): String {
        val firestoreError = error as? FirebaseFirestoreException
        val code = firestoreError?.code?.name
        val message = error.message ?: "Sin detalle"
        return if (code.isNullOrBlank()) message else "[$code] $message"
    }

    private fun taskFromDocument(documento: com.google.firebase.firestore.DocumentSnapshot): Task? {
        val directo = runCatching { documento.toObject<Task>() }.getOrNull()
        if (directo != null) return directo

        val data = documento.data ?: return null
        return buildTaskFromMap(data, documento.id)
    }

    private fun buildTaskFromMap(data: Map<String, Any?>, docId: String): Task {
        val id = data["id"]?.toString()?.takeIf { it.isNotBlank() } ?: docId
        val rawIsExported = data["isExported"] ?: data["exported"]
        val parsedIsExported = parseBoolean(rawIsExported)

        val rawIsNextAction = data["isNextAction"] ?: data["nextAction"]
        val parsedIsNextAction = parseBoolean(rawIsNextAction)

        Log.d(
            "KairosExport",
            "firestore doc=$docId id=$id rawIsExported=$rawIsExported parsedIsExported=$parsedIsExported rawIsNextAction=$rawIsNextAction parsedIsNextAction=$parsedIsNextAction"
        )

        return Task(
            id = id,
            userId = data["userId"]?.toString().orEmpty(),
            title = data["title"]?.toString().orEmpty(),
            description = data["description"]?.toString().orEmpty(),
            category = parseCategory(data["category"]),
            createdAt = parseLong(data["createdAt"]) ?: System.currentTimeMillis(),
            updatedAt = parseLong(data["updatedAt"]) ?: System.currentTimeMillis(),
            scheduledDate = parseLong(data["scheduledDate"]),
            dueDate = parseLong(data["dueDate"]),
            estimatedMinutes = parseInt(data["estimatedMinutes"]),
            isNextAction = parsedIsNextAction,
            isExported = parsedIsExported
        )
    }

    private fun parseCategory(raw: Any?): TaskCategory {
        val value = raw?.toString()?.trim().orEmpty().uppercase()
        return when (value) {
            "RECURRENT", "RECURRENTE" -> TaskCategory.RECURRENT
            "ACTIONABLE", "ACCIONABLE" -> TaskCategory.ACTIONABLE
            "SHORT_TERM", "CORTO_PLAZO", "CORTO PLAZO" -> TaskCategory.SHORT_TERM
            "LONG_TERM", "LARGO_PLAZO", "LARGO PLAZO" -> TaskCategory.LONG_TERM
            "INCUBATOR", "INCUBADORA" -> TaskCategory.INCUBATOR
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
        is Number -> raw.toInt() != 0
        is String -> {
            val normalized = raw.trim()
            normalized.equals("true", ignoreCase = true) ||
                normalized == "1" ||
                normalized.equals("yes", ignoreCase = true) ||
                normalized.equals("si", ignoreCase = true)
        }
        else -> false
    }
}
