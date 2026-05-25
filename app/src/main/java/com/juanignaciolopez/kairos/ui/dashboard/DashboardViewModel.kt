package com.juanignaciolopez.kairos.ui.dashboard

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanignaciolopez.kairos.core.notifications.TaskNotificationScheduler
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.S)
    val tasks: StateFlow<List<Task>> = authRepository.observeCurrentUser()
        .map { user -> user?.id.orEmpty() }
        .distinctUntilChanged()
        .onEach { currentUserId ->
            _uiState.update { it.copy(userId = currentUserId) }
        }
        .flatMapLatest { currentUserId ->
            if (currentUserId.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.getAllTasks(currentUserId)
            }
        }
        .onEach { allTasks ->
            TaskNotificationScheduler.syncNotifications(appContext, allTasks)
        }
        .retryWhen { cause, attempt ->
            _uiState.update {
                it.copy(
                    errorMessage = cause.message ?: "Error de conexión. Reintentando sincronización..."
                )
            }
            if (attempt >= MAX_TASKS_RETRY_ATTEMPTS) {
                false
            } else {
                delay(TASKS_RETRY_DELAY_MS)
                true
            }
        }
        .catch { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Error al cargar tareas") }
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteTask(taskId: String) {
        if (taskId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            when (val result = executeDeleteWithRetry(taskId)) {
                is Result.Success -> _uiState.update { it.copy(isDeleting = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isDeleting = false, errorMessage = result.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isDeleting = true) }
            }
        }
    }

    private suspend fun executeDeleteWithRetry(taskId: String): Result<Unit> {
        repeat(MAX_DELETE_ATTEMPTS) { attempt ->
            try {
                val result = withTimeout(DELETE_ATTEMPT_TIMEOUT_MS) {
                    taskRepository.deleteTask(taskId)
                }

                return result
            } catch (_: TimeoutCancellationException) {
                val isLastAttempt = attempt == MAX_DELETE_ATTEMPTS - 1
                if (isLastAttempt) {
                    return Result.Error(
                        "No se pudo confirmar la eliminación a tiempo. Verifica la conexión e intenta nuevamente."
                    )
                }
            }
        }

        return Result.Error("No se pudo eliminar la tarea.")
    }

    fun markTaskExported(taskId: String) {
        if (taskId.isBlank()) return

        viewModelScope.launch {
            when (val result = taskRepository.markTaskExported(taskId)) {
                is Result.Success -> Unit
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class DashboardUiState(
    val userId: String = "",
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

private const val TASKS_RETRY_DELAY_MS = 2_000L
private const val MAX_TASKS_RETRY_ATTEMPTS = 5L
private const val DELETE_ATTEMPT_TIMEOUT_MS = 10_000L
private const val MAX_DELETE_ATTEMPTS = 2
