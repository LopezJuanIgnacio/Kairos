package com.juanignaciolopez.kairos.ui.task_form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanignaciolopez.kairos.core.navigation.NavRoute
import com.juanignaciolopez.kairos.core.utils.ValidationUtils
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskIdArg: String? = savedStateHandle[NavRoute.TASK_ID_ARG]

    private val _uiState = MutableStateFlow(TaskFormUiState(taskId = taskIdArg))
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()
    private var loadedTask: Task? = null

    init {
        if (!taskIdArg.isNullOrBlank()) {
            loadTask(taskIdArg)
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                title = value,
                titleError = null,
                errorMessage = null
            )
        }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                descriptionError = null,
                errorMessage = null
            )
        }
    }

    fun onCategoryChanged(value: TaskCategory) {
        _uiState.update {
            it.copy(
                category = value,
                errorMessage = null
            )
        }
    }

    fun onDueDateChanged(value: Long?) {
        _uiState.update {
            it.copy(
                dueDate = value,
                dueDateError = null,
                errorMessage = null
            )
        }
    }

    fun saveTask() {
        val current = _uiState.value
        val title = current.title.trim()
        val description = current.description.trim()

        if (current.isEditMode && loadedTask == null) {
            _uiState.update {
                it.copy(errorMessage = "No se pudo cargar la tarea a editar. Intenta nuevamente.")
            }
            return
        }

        val titleError = if (!ValidationUtils.isValidTaskTitle(title)) {
            "El título es obligatorio y debe tener hasta 200 caracteres"
        } else {
            null
        }

        val descriptionError = if (!ValidationUtils.isValidTaskDescription(description)) {
            "La descripción no puede superar los 2000 caracteres"
        } else {
            null
        }

        val dueDateError = if (current.dueDate == null) {
            when (current.category) {
                TaskCategory.RECURRENT,
                TaskCategory.ACTIONABLE -> "La hora de notificación es obligatoria"

                else -> "La fecha límite es obligatoria"
            }
        } else {
            null
        }

        if (titleError != null || descriptionError != null || dueDateError != null) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    descriptionError = descriptionError,
                    dueDateError = dueDateError
                )
            }
            return
        }

        val now = System.currentTimeMillis()
        val existingTask = loadedTask
        val currentUserId = currentUserId()
        val task = if (current.isEditMode && existingTask != null) {
            existingTask.copy(
                title = title,
                description = description,
                category = current.category,
                dueDate = current.dueDate,
                updatedAt = now,
                userId = if (existingTask.userId.isBlank()) currentUserId else existingTask.userId
            )
        } else {
            Task(
                id = current.taskId ?: java.util.UUID.randomUUID().toString(),
                userId = currentUserId,
                title = title,
                description = description,
                category = current.category,
                dueDate = current.dueDate,
                createdAt = now,
                updatedAt = now
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val result = executeSaveWithRetry(task = task, isEditMode = current.isEditMode)

                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(isSaving = false, isSaved = true)
                    }

                    is Result.Error -> _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.message)
                    }

                    is Result.Loading -> _uiState.update { it.copy(isSaving = true) }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo guardar la tarea en este momento. Verifica tu conexión e intenta de nuevo."
                    )
                }
            }
        }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = executeLoadWithRetry(taskId)) {
                is Result.Success -> {
                    val task = result.data
                    loadedTask = task
                    _uiState.update {
                        it.copy(
                            taskId = task.id,
                            title = task.title,
                            description = task.description,
                            category = task.category,
                            dueDate = task.dueDate,
                            isLoading = false
                        )
                    }
                }

                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    private suspend fun executeLoadWithRetry(taskId: String): Result<Task> {
        repeat(MAX_LOAD_ATTEMPTS) { attempt ->
            try {
                val result = withTimeout(LOAD_ATTEMPT_TIMEOUT_MS) {
                    taskRepository.getTaskById(taskId)
                }

                return result
            } catch (_: TimeoutCancellationException) {
                val isLastAttempt = attempt == MAX_LOAD_ATTEMPTS - 1
                if (isLastAttempt) {
                    return Result.Error(
                        "No se pudo cargar la tarea a tiempo. Verifica la conexión e intenta de nuevo."
                    )
                }
            }
        }

        return Result.Error("No se pudo cargar la tarea.")
    }

    private fun currentUserId(): String {
        return authRepository.getCurrentUserId().orEmpty()
    }

    private suspend fun executeSaveWithRetry(task: Task, isEditMode: Boolean): Result<Task> {
        repeat(MAX_SAVE_ATTEMPTS) { attempt ->
            try {
                val result = withTimeout(SAVE_ATTEMPT_TIMEOUT_MS) {
                    if (isEditMode) {
                        taskRepository.updateTask(task)
                    } else {
                        taskRepository.createTask(task)
                    }
                }

                return result
            } catch (_: TimeoutCancellationException) {
                val isLastAttempt = attempt == MAX_SAVE_ATTEMPTS - 1
                if (isLastAttempt) {
                    return Result.Error(
                        "No se pudo confirmar el guardado a tiempo. Revisa tu conexión e intenta nuevamente."
                    )
                }
            }
        }

        return Result.Error("No se pudo guardar la tarea.")
    }

    companion object {
        private const val SAVE_ATTEMPT_TIMEOUT_MS = 12_000L
        private const val MAX_SAVE_ATTEMPTS = 2
        private const val LOAD_ATTEMPT_TIMEOUT_MS = 10_000L
        private const val MAX_LOAD_ATTEMPTS = 2
    }
}
