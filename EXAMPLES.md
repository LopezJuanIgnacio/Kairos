# 🎓 KAIROS - Ejemplos de Implementación

## Ejemplo Completo #1: Sign In Screen

### 1️⃣ Crear el Use Case (domain/usecase/)

```kotlin
// SignInUseCase.kt
package com.juanignaciolopez.kairos.domain.usecase

import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import com.juanignaciolopez.kairos.domain.repository.AuthRepository

class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<User> {
        // Validar input
        if (!isValidEmail(email)) {
            return Result.Error("Email inválido")
        }
        if (!isValidPassword(password)) {
            return Result.Error("Contraseña inválida")
        }
        
        // Ejecutar repositorio
        return authRepository.signIn(email, password)
    }
    
    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }
    
    private fun isValidPassword(password: String): Boolean {
        return password.isNotEmpty()
    }
}
```

### 2️⃣ Crear el ViewModel (ui/auth/)

```kotlin
// SignInViewModel.kt
package com.juanignaciolopez.kairos.ui.auth

import androidx.lifecycle.viewModelScope
import com.juanignaciolopez.kairos.data.models.Result
import com.juanignaciolopez.kairos.data.models.User
import com.juanignaciolopez.kairos.domain.usecase.SignInUseCase
import com.juanignaciolopez.kairos.ui.viewmodel.BaseEventViewModel
import com.juanignaciolopez.kairos.ui.viewmodel.UiEvent
import com.juanignaciolopez.kairos.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// State
data class SignInState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : UiState

// Events
sealed class SignInEvent : UiEvent {
    data object NavigateToDashboard : SignInEvent()
    data class NavigateToSignUp(val email: String? = null) : SignInEvent()
    data object NavigateToForgotPassword : SignInEvent()
    data class ShowMessage(val message: String) : SignInEvent()
}

// ViewModel
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : BaseEventViewModel<SignInState, SignInEvent>() {
    
    override fun getInitialState() = SignInState()
    
    // Actualizar email
    fun onEmailChanged(email: String) {
        updateState { it.copy(email = email, emailError = null) }
    }
    
    // Actualizar password
    fun onPasswordChanged(password: String) {
        updateState { it.copy(password = password, passwordError = null) }
    }
    
    // Sign In
    fun signIn() {
        val state = getCurrentState()
        
        // Validación local
        var hasError = false
        var newEmailError: String? = null
        var newPasswordError: String? = null
        
        if (state.email.isBlank()) {
            newEmailError = "Email requerido"
            hasError = true
        }
        if (state.password.isBlank()) {
            newPasswordError = "Contraseña requerida"
            hasError = true
        }
        
        if (hasError) {
            updateState {
                it.copy(
                    emailError = newEmailError,
                    passwordError = newPasswordError
                )
            }
            return
        }
        
        // Llamar al use case
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            val result = signInUseCase(state.email, state.password)
            
            when (result) {
                is Result.Success -> {
                    updateState { it.copy(isLoading = false) }
                    sendEvent(SignInEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    updateState { it.copy(isLoading = true) }
                }
            }
        }
    }
    
    fun goToSignUp(email: String? = null) {
        viewModelScope.launch {
            sendEvent(SignInEvent.NavigateToSignUp(email))
        }
    }
    
    fun goToForgotPassword() {
        viewModelScope.launch {
            sendEvent(SignInEvent.NavigateToForgotPassword)
        }
    }
}
```

### 3️⃣ Crear la Screen (ui/auth/)

```kotlin
// SignInScreen.kt
package com.juanignaciolopez.kairos.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.juanignaciolopez.kairos.core.components.CustomTextField
import com.juanignaciolopez.kairos.core.components.LoadingIndicator
import com.juanignaciolopez.kairos.core.navigation.NavRoute

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Manejar eventos
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is SignInEvent.NavigateToDashboard -> {
                    navController.navigate(NavRoute.Dashboard.route) {
                        popUpTo(NavRoute.SignIn.route) { inclusive = true }
                    }
                }
                is SignInEvent.NavigateToSignUp -> {
                    navController.navigate(NavRoute.SignUp.route)
                }
                is SignInEvent.NavigateToForgotPassword -> {
                    navController.navigate(NavRoute.ForgotPassword.route)
                }
                is SignInEvent.ShowMessage -> {
                    // Mostrar Snackbar
                }
            }
        }
    }
    
    if (state.isLoading) {
        LoadingIndicator()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineLarge
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Email Field
                CustomTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email",
                    placeholder = "tu@email.com",
                    isError = state.emailError != null,
                    errorMessage = state.emailError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
                CustomTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChanged,
                    label = "Contraseña",
                    placeholder = "Ingresa tu contraseña",
                    isError = state.passwordError != null,
                    errorMessage = state.passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Error Message
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sign In Button
                Button(
                    onClick = viewModel::signIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Iniciar Sesión")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Forgot Password Link
                Text(
                    "¿Olvidaste tu contraseña?",
                    modifier = Modifier.clickable {
                        viewModel.goToForgotPassword()
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sign Up Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿No tienes cuenta? ")
                    Text(
                        "Regístrate",
                        modifier = Modifier.clickable {
                            viewModel.goToSignUp(state.email.takeIf { it.isNotBlank() })
                        },
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

### 4️⃣ Registrar en Hilt (di/AppModule.kt)

```kotlin
// Agregar a RepositoryModule o crear nuevo módulo
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    @Provides
    @Singleton
    fun provideSignInUseCase(
        authRepository: AuthRepository
    ): SignInUseCase = SignInUseCase(authRepository)
}
```

### 5️⃣ Agregar a Navegación (core/navigation/NavHost.kt)

```kotlin
composable(NavRoute.SignIn.route) {
    SignInScreen(navController)
}
```

---

## Ejemplo Completo #2: Task List Screen (Simple)

### State & ViewModel

```kotlin
data class TaskListState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskStatus = TaskStatus.TODO,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : UiState

sealed class TaskListEvent : UiEvent {
    data class NavigateToTaskForm(val taskId: String? = null) : TaskListEvent()
    data class NavigateToTaskDetail(val taskId: String) : TaskListEvent()
    data class ShowMessage(val message: String) : TaskListEvent()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository
) : BaseEventViewModel<TaskListState, TaskListEvent>() {
    
    override fun getInitialState() = TaskListState()
    
    init {
        loadTasks()
    }
    
    private fun loadTasks() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            taskRepository
                .getTasksByStatus(userId, TaskStatus.TODO)
                .collect { tasks ->
                    updateState { it.copy(tasks = tasks, isLoading = false) }
                }
        }
    }
    
    fun changeFilter(status: TaskStatus) {
        updateState { it.copy(filter = status) }
        loadTasks()
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = taskRepository.deleteTask(taskId)
            if (result is Result.Success) {
                sendEvent(TaskListEvent.ShowMessage("Tarea eliminada"))
            }
        }
    }
    
    fun navigateToForm(taskId: String? = null) {
        viewModelScope.launch {
            sendEvent(TaskListEvent.NavigateToTaskForm(taskId))
        }
    }
}
```

### Screen

```kotlin
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is TaskListEvent.NavigateToTaskForm -> {
                    val route = event.taskId?.let {
                        NavRoute.TaskFormWithId.createRoute(it)
                    } ?: NavRoute.TaskForm.route
                    navController.navigate(route)
                }
                // ... otros eventos
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TopAppBar(title = { Text("Mis Tareas") })
        
        if (state.isLoading) {
            LoadingIndicator()
        } else if (state.tasks.isEmpty()) {
            EmptyState(
                title = "Sin tareas",
                message = "Crea una tarea para empezar"
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.tasks) { task ->
                    TaskItem(
                        task = task,
                        onDelete = { viewModel.deleteTask(task.id) },
                        onEdit = { viewModel.navigateToForm(task.id) }
                    )
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = { viewModel.navigateToForm() }
        ) {
            Icon(Icons.Default.Add, "Nueva tarea")
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                if (task.description.isNotEmpty()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Eliminar")
            }
        }
    }
}
```

---

## 📚 Patrones Clave

### Pattern 1: Estado + Eventos para Navegación
```kotlin
// ViewModel
sealed class MyEvent : UiEvent {
    data object NavigateToDetail : MyEvent()
}

// Screen
LaunchedEffect(Unit) {
    viewModel.event.collect { event ->
        when (event) {
            is MyEvent.NavigateToDetail -> {
                navController.navigate(...)
            }
        }
    }
}
```

### Pattern 2: Validación Antes de Acción
```kotlin
fun submitForm() {
    val errors = validateForm()
    if (errors.isNotEmpty()) {
        updateState { it.copy(errors = errors) }
        return
    }
    
    // Ejecutar acción
    performAction()
}
```

### Pattern 3: Manejo de Resultado Asincrónico
```kotlin
viewModelScope.launch {
    val result = repository.doSomething()
    when (result) {
        is Result.Success -> handleSuccess(result.data)
        is Result.Error -> handleError(result.message)
        is Result.Loading -> updateState { it.copy(isLoading = true) }
    }
}
```

---

**¡Usa estos ejemplos como guía para implementar nuevas features!**
