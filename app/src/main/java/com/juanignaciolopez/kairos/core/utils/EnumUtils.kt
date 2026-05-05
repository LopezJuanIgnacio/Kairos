package com.juanignaciolopez.kairos.core.utils

import android.content.Context
import androidx.annotation.StringRes
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.data.models.TaskCategory

/**
 * Utilidades para convertir enums a strings legibles
 */
object EnumUtils {
    @StringRes
    fun categoryToStringRes(category: TaskCategory): Int = when (category) {
        TaskCategory.RECURRENT -> R.string.task_category_recurrent
        TaskCategory.ACTIONABLE -> R.string.task_category_actionable
        TaskCategory.SHORT_TERM -> R.string.task_category_short_term
        TaskCategory.LONG_TERM -> R.string.task_category_long_term
        TaskCategory.INCUBATOR -> R.string.task_category_incubator
    }

    fun categoryToString(context: Context, category: TaskCategory): String {
        return context.getString(categoryToStringRes(category))
    }
}
