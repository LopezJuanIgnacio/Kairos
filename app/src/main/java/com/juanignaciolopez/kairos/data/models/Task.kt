package com.juanignaciolopez.kairos.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Categoría funcional de la tarea
 */
@Serializable
enum class TaskCategory {
	RECURRENT,
	ACTIONABLE,
	SHORT_TERM,
	LONG_TERM,
	INCUBATOR
}

/**
 * Entidad principal de tarea serializable.
 */
@Serializable
data class Task(
	val id: String = UUID.randomUUID().toString(),
	val userId: String = "",
	val title: String,
	val description: String = "",
	val category: TaskCategory = TaskCategory.ACTIONABLE,
	val createdAt: Long = System.currentTimeMillis(),
	val updatedAt: Long = System.currentTimeMillis(),
	val scheduledDate: Long? = null,
	val dueDate: Long? = null,
	val estimatedMinutes: Int = 0,
	val isNextAction: Boolean = false,
	val isExported: Boolean = false,
	
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
			userId = userId
		)
	}
}

