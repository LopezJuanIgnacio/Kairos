package com.juanignaciolopez.kairos.data.repository

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.remote.FirebaseAuthService
import com.juanignaciolopez.kairos.data.remote.FirebaseTaskService
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val servicioFirebaseTareas: FirebaseTaskService,
    private val servicioFirebaseAutenticacion: FirebaseAuthService
) : TaskRepository {

    override fun getAllTasks(userId: String): Flow<List<Task>> = servicioFirebaseTareas.getAllTasks(userId)

    override suspend fun getTaskById(taskId: String): Result<Task> {
        val userId = currentUserId() ?: return Result.Error("Usuario no autenticado")
        return servicioFirebaseTareas.getTaskById(userId, taskId)
    }

    override suspend fun createTask(task: Task): Result<Task> {
        val userId = task.userId.ifBlank { currentUserId().orEmpty() }
        if (userId.isBlank()) return Result.Error("Usuario no autenticado")

        val now = System.currentTimeMillis()
        val taskToCreate = task.copy(
            userId = userId,
            updatedAt = now
        )
        return servicioFirebaseTareas.createTask(userId, taskToCreate)
    }

    override suspend fun updateTask(task: Task): Result<Task> {
        val userId = task.userId.ifBlank { currentUserId().orEmpty() }
        if (userId.isBlank()) return Result.Error("Usuario no autenticado")

        val now = System.currentTimeMillis()
        val taskToUpdate = task.copy(
            userId = userId,
            updatedAt = now
        )
        return servicioFirebaseTareas.updateTask(userId, taskToUpdate)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val userId = currentUserId() ?: return Result.Error("Usuario no autenticado")
        return servicioFirebaseTareas.deleteTask(userId, taskId)
    }

    override suspend fun markTaskExported(taskId: String): Result<Unit> {
        val userId = currentUserId() ?: return Result.Error("Usuario no autenticado")
        return when (val current = servicioFirebaseTareas.getTaskById(userId, taskId)) {
            is Result.Success -> {
                val updated = current.data.copy(
                    isExported = true,
                    updatedAt = System.currentTimeMillis()
                )
                when (servicioFirebaseTareas.updateTask(userId, updated)) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error("Error al marcar tarea exportada")
                    is Result.Loading -> Result.Loading
                }
            }

            is Result.Error -> Result.Error(current.message, current.exception)
            is Result.Loading -> Result.Loading
        }
    }

    private fun currentUserId(): String? {
        return servicioFirebaseAutenticacion.getCurrentUserId()
    }
}
