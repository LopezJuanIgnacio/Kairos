package com.juanignaciolopez.kairos.core.utils

import com.juanignaciolopez.kairos.data.models.TaskPriority
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
}
