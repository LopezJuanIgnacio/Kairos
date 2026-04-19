package com.juanignaciolopez.kairos.data.repository

import com.juanignaciolopez.kairos.data.local.TaskDao
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskStatus
import com.juanignaciolopez.kairos.data.remote.FirebaseTaskService
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Implementación de repositorio de tareas (offline-first básico).
 */
class TaskRepositoryImpl(
    private val daoTarea: TaskDao,
    private val servicioFirebaseTareas: FirebaseTaskService
) : TaskRepository {

    private val backgroundSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getAllTasks(userId: String): Flow<List<Task>> = daoTarea.getAllTasks(userId)

    override fun getTasksByStatus(
        userId: String,
        status: TaskStatus
    ): Flow<List<Task>> = daoTarea.getTasksByStatus(userId, status)

    override suspend fun getTaskById(taskId: String): Result<Task> = try {
        val tarea = daoTarea.getTaskById(taskId)
        if (tarea != null) {
            Result.Success(tarea)
        } else {
            Result.Error("Tarea no encontrada")
        }
    } catch (e: Exception) {
        Result.Error("Error al obtener tarea: ${e.message}", e)
    }

    override suspend fun createTask(task: Task): Result<Task> = try {
        val tareaLocal = if (task.userId.isNotEmpty()) {
            task.copy(isSyncPending = true, lastSyncedAt = null)
        } else {
            task
        }

        daoTarea.insertTask(tareaLocal)

        if (tareaLocal.userId.isNotEmpty()) {
            backgroundSyncScope.launch {
                val remoto = servicioFirebaseTareas.createTask(tareaLocal.userId, tareaLocal)
                if (remoto is Result.Success) {
                    daoTarea.updateTask(
                        tareaLocal.copy(
                            isSyncPending = false,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        Result.Success(tareaLocal)
    } catch (e: Exception) {
        Result.Error("Error al crear tarea: ${e.message}", e)
    }

    override suspend fun updateTask(task: Task): Result<Task> = try {
        val tareaLocal = if (task.userId.isNotEmpty()) {
            task.copy(isSyncPending = true, lastSyncedAt = null)
        } else {
            task
        }

        daoTarea.updateTask(tareaLocal)

        if (tareaLocal.userId.isNotEmpty()) {
            backgroundSyncScope.launch {
                val remoto = servicioFirebaseTareas.updateTask(tareaLocal.userId, tareaLocal)
                if (remoto is Result.Success) {
                    daoTarea.updateTask(
                        tareaLocal.copy(
                            isSyncPending = false,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        Result.Success(tareaLocal)
    } catch (e: Exception) {
        Result.Error("Error al actualizar tarea: ${e.message}", e)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = try {
        daoTarea.deleteTaskById(taskId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al eliminar tarea: ${e.message}", e)
    }

    override suspend fun completeTask(taskId: String): Result<Unit> = try {
        val tarea = daoTarea.getTaskById(taskId)
        tarea?.let {
            val tareaCompletada = it.copy(
                status = TaskStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
            daoTarea.updateTask(tareaCompletada)
            if (it.userId.isNotEmpty()) {
                servicioFirebaseTareas.completeTask(it.userId, taskId)
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al completar tarea: ${e.message}", e)
    }

    override fun getNextActions(userId: String): Flow<List<Task>> = daoTarea.getNextActions(userId)

    override fun searchTasks(userId: String, query: String): Flow<List<Task>> =
        daoTarea.searchTasks(userId, query)

    override suspend fun syncTasks(userId: String): Result<Unit> = try {
        // En modo offline-first, esta cola representa cambios locales aún no subidos.
        val tareasPendientes = daoTarea.getPendingSyncTasks(userId)
        tareasPendientes.forEach { tarea ->
            servicioFirebaseTareas.updateTask(userId, tarea)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Error al sincronizar tareas: ${e.message}", e)
    }
}
