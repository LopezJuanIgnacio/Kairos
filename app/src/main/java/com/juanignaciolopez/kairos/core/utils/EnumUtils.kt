package com.juanignaciolopez.kairos.core.utils

import com.juanignaciolopez.kairos.data.models.TaskCategory

/**
 * Utilidades para convertir enums a strings legibles
 */
object EnumUtils {
    fun categoryToString(category: TaskCategory): String = when (category) {
        TaskCategory.RECURRENT -> "Recurrente"
        TaskCategory.ACTIONABLE -> "Accionable"
        TaskCategory.SHORT_TERM -> "Corto Plazo"
        TaskCategory.LONG_TERM -> "Largo Plazo"
        TaskCategory.INCUBATOR -> "Incubadora"
    }
}
