package com.juanignaciolopez.kairos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val userId: String = authRepository.getCurrentUserId().orEmpty()

    private val _uiState = MutableStateFlow(DashboardUiState(userId = userId))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val tasks: StateFlow<List<Task>> = if (userId.isBlank()) {
        MutableStateFlow(emptyList())
    } else {
        taskRepository.getAllTasks(userId)
            .catch { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "Error al cargar tareas") }
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    fun deleteTask(taskId: String) {
        if (taskId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            when (val result = taskRepository.deleteTask(taskId)) {
                is Result.Success -> _uiState.update { it.copy(isDeleting = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isDeleting = false, errorMessage = result.message)
                }
                is Result.Loading -> _uiState.update { it.copy(isDeleting = true) }
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
