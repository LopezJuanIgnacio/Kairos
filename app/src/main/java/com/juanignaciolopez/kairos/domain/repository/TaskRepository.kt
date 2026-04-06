package com.juanignaciolopez.kairos.domain.repository

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para gestión de tareas.
 */
interface TaskRepository {
    fun getAllTasks(userId: String): Flow<List<Task>>

    fun getTasksByStatus(
        userId: String,
        status: TaskStatus
    ): Flow<List<Task>>

    suspend fun getTaskById(taskId: String): Result<Task>

    suspend fun createTask(task: Task): Result<Task>

    suspend fun updateTask(task: Task): Result<Task>

    suspend fun deleteTask(taskId: String): Result<Unit>

    suspend fun completeTask(taskId: String): Result<Unit>

    fun getNextActions(userId: String): Flow<List<Task>>

    fun searchTasks(
        userId: String,
        query: String
    ): Flow<List<Task>>

    suspend fun syncTasks(userId: String): Result<Unit>
}
