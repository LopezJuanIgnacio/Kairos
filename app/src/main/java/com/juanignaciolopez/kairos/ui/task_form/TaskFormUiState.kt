package com.juanignaciolopez.kairos.ui.task_form

import com.juanignaciolopez.kairos.data.models.TaskCategory

data class TaskFormUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.ACTIONABLE,
    val dueDate: Long? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val dueDateError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    val isEditMode: Boolean
        get() = !taskId.isNullOrBlank()
}
