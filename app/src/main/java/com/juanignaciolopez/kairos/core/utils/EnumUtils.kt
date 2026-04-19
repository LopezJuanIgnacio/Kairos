package com.juanignaciolopez.kairos.core.utils

import com.juanignaciolopez.kairos.data.models.TaskPriority
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.data.models.TaskStatus

/**
 * Utilidades para convertir enums a strings legibles
 */
object EnumUtils {
    
    fun priorityToString(priority: TaskPriority): String = when (priority) {
        TaskPriority.VERY_HIGH -> "Crítica"
        TaskPriority.HIGH -> "Alta"
        TaskPriority.NORMAL -> "Normal"
        TaskPriority.LOW -> "Baja"
        TaskPriority.VERY_LOW -> "Muy baja"
    }
    
    fun statusToString(status: TaskStatus): String = when (status) {
        TaskStatus.INBOX -> "Entrada"
        TaskStatus.PROCESSING -> "En procesamiento"
        TaskStatus.TODO -> "Por hacer"
        TaskStatus.IN_PROGRESS -> "En progreso"
        TaskStatus.COMPLETED -> "Completada"
        TaskStatus.ARCHIVED -> "Archivada"
        TaskStatus.DELETED -> "Eliminada"
    }

    fun categoryToString(category: TaskCategory): String = when (category) {
        TaskCategory.RECURRENT -> "Recurrente"
        TaskCategory.ACTIONABLE -> "Accionable"
        TaskCategory.SHORT_TERM -> "Corto Plazo"
        TaskCategory.LONG_TERM -> "Largo Plazo"
        TaskCategory.INCUBATOR -> "Incubadora"
    }
    
    fun stringToPriority(string: String): TaskPriority = when (string) {
        "Crítica" -> TaskPriority.VERY_HIGH
        "Alta" -> TaskPriority.HIGH
        "Normal" -> TaskPriority.NORMAL
        "Baja" -> TaskPriority.LOW
        "Muy baja" -> TaskPriority.VERY_LOW
        else -> TaskPriority.NORMAL
    }
    
    fun stringToStatus(string: String): TaskStatus = when (string) {
        "Entrada" -> TaskStatus.INBOX
        "En procesamiento" -> TaskStatus.PROCESSING
        "Por hacer" -> TaskStatus.TODO
        "En progreso" -> TaskStatus.IN_PROGRESS
        "Completada" -> TaskStatus.COMPLETED
        "Archivada" -> TaskStatus.ARCHIVED
        "Eliminada" -> TaskStatus.DELETED
        else -> TaskStatus.TODO
    }

    fun stringToCategory(string: String): TaskCategory = when (string) {
        "Recurrente" -> TaskCategory.RECURRENT
        "Accionable" -> TaskCategory.ACTIONABLE
        "Corto Plazo" -> TaskCategory.SHORT_TERM
        "Largo Plazo" -> TaskCategory.LONG_TERM
        "Incubadora" -> TaskCategory.INCUBATOR
        else -> TaskCategory.ACTIONABLE
    }
}
