package com.juanignaciolopez.kairos.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Prioridad de tareas en GTD
 */
@Serializable
enum class TaskPriority {
    VERY_HIGH,
    HIGH,
    NORMAL,
    LOW,
    VERY_LOW
}

/**
 * Estado de ciclo de vida de una tarea
 */
@Serializable
enum class TaskStatus {
    INBOX,
    PROCESSING,
    TODO,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED,
    DELETED
}

/**
 * Categoría funcional de la tarea
 */
@Serializable
enum class TaskCategory {
    WORK,
    PERSONAL,
    HEALTH,
    FINANCE,
    LEARNING,
    FAMILY,
    HOME,
    SOMEDAY,
    OTHER
}

/**
 * Entidad principal de tarea (Room + serializable)
 */
@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.INBOX,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val scheduledDate: Long? = null,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null,
    val parentTaskId: String? = null,
    val estimatedMinutes: Int = 0,
    val context: String = "",
    val project: String? = null,
    val isNextAction: Boolean = false,
    val isSyncPending: Boolean = false,
    val lastSyncedAt: Long? = null,
    val tags: List<String> = emptyList()
) {
    companion object {
        fun empty() = Task(title = "")

        fun createFromInbox(
            title: String,
            description: String = "",
            userId: String = ""
        ) = Task(
            title = title,
            description = description,
            userId = userId,
            status = TaskStatus.INBOX
        )
    }
}

// Alias de transición para nombres en español.
typealias Tarea = Task
typealias PrioridadTarea = TaskPriority
typealias EstadoTarea = TaskStatus
typealias CategoriaTarea = TaskCategory
