# 🚀 KAIROS - Guía de Inicio Rápido

## ⚙️ Configuración Inicial

### 1. **Crear archivo `google-services.json`**

Descarga tu configuración de Firebase desde Firebase Console y colócala en:
```
app/google-services.json
```

Este archivo contiene:
- Project ID
- Mobile App Credentials
- API Keys

### 2. **Compilar el Proyecto**

```bash
./gradlew clean build
```

Si hay errores de plugins, asegúrate que el `build.gradle.kts` raíz tiene correctamente configurados:
- `kotlin-serialization`
- `hilt-android`
- `ksp`
- `google-services`

### 3. **Sincronizar con Gradle**

```bash
./gradlew sync
```

---

## 📱 Primera Ejecución

```bash
./gradlew installDebug
```

O simplemente en Android Studio: `Run > Run 'app'`

---

## 🏗️ Estructura de Features

Cada feature sigue este patrón:

```
ui/
├── auth/
│   ├── SignInScreen.kt
│   ├── SignInViewModel.kt
│   ├── SignUpScreen.kt
│   └── SignUpViewModel.kt
```

### Crear un Feature (Ejemplo: Profile)

1. **Crear carpeta**: `ui/profile/`

2. **Crear ViewModel**:
```kotlin
// ProfileViewModel.kt
package com.juanignaciolopez.kairos.ui.profile

import com.juanignaciolopez.kairos.ui.viewmodel.BaseEventViewModel
import com.juanignaciolopez.kairos.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProfileState(
    val userName: String = "",
    override val isLoading: Boolean = false,
    override val error: String? = null
) : UiState

sealed class ProfileEvent : UiEvent {
    data class ShowMessage(val message: String) : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseEventViewModel<ProfileState, ProfileEvent>() {
    
    override fun getInitialState() = ProfileState()
    
    // Implementar métodos para manejar lógica
}
```

3. **Crear Screen**:
```kotlin
// ProfileScreen.kt
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // UI Implementation
}
```

4. **Agregar a Navigation**:
```kotlin
// NavHost.kt
composable(NavRoute.Profile.route) {
    ProfileScreen(navController)
}
```

---

## 🔄 MVVM Pattern Usage

### State Management
```kotlin
// En ViewModel
private val _state = MutableStateFlow(InitialState)
val state: StateFlow<State> = _state.asStateFlow()

// En Screen
val state by viewModel.state.collectAsState()
```

### Events Handling
```kotlin
// En ViewModel
suspend fun sendEvent(event: MyEvent) {
    _event.emit(event)
}

// En Screen
LaunchedEffect(Unit) {
    viewModel.event.collect { event ->
        when (event) {
            is MyEvent.Success -> { /* Handle */ }
        }
    }
}
```

---

## 🗂️ Almacenamiento de Datos

### Local (Room)
Automático: cambios se guardan en Room
```kotlin
// Por ejemplo en TaskFormViewModel
suspend fun saveTask(task: Task) {
    val result = taskRepository.createTask(task)
    // Automáticamente se guarda en Room
}
```

### Remote (Firebase)
Automático: sincronización en segundo plano
- Firestore almacena la fuente de verdad
- Room es caché local
- Soporte offline automático

---

## 🔐 Autenticación

### Flow de Sign In
```kotlin
// SignInViewModel
fun signIn(email: String, password: String) {
    viewModelScope.launch {
        val result = authRepository.signIn(email, password)
        when (result) {
            is Result.Success -> {
                updateState { it.copy(isAuthenticated = true) }
                sendEvent(SignInEvent.NavigateToDashboard)
            }
            is Result.Error -> {
                updateState { it.copy(error = result.message) }
            }
        }
    }
}
```

### Verificar Autenticación
```kotlin
// En MainActivity
@Composable
fun KairosApp() {
    val startDestination = if (isUserAuthenticated()) {
        NavRoute.Dashboard.route
    } else {
        NavRoute.Onboarding.route
    }
}
```

---

## 🔗 Inyección de Dependencias (Hilt)

### Usar en ViewModel
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository,
    private val dispatcher: CoroutineDispatcher
) : BaseViewModel<MyState>() {
    // ...
}
```

### Usar en Composable
```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    // ...
}
```

---

## 🧪 Testing (Próxima Fase)

### Unit Test de ViewModel
```kotlin
@HiltAndroidTest
class SignInViewModelTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject lateinit var authRepository: AuthRepository
    
    @Test
    fun testSuccessfulSignIn() = runTest {
        // Test implementation
    }
}
```

---

## 📊 Debug y Logs

### Habilitar verbose logging en Firebase
```kotlin
FirebaseAuth.getInstance().useAppLanguage()
```

### Check Database Query Results
```kotlin
// Usar Database Inspector de Android Studio
// View > Tool Windows > Database Inspector
```

---

## 🚨 Troubleshooting Común

### Error: "Plugin 'com.google.dagger.hilt.android' not configured"
- Asegurate que `google-services.json` existe
- Rebuild project: `./gradlew clean build`

### Error: Firebase not initialized
- Verifica que `KairosApplication` tiene `@HiltAndroidApp`
- Verifica que `AndroidManifest.xml` apunta a `KairosApplication`

### Error de Compose Preview
- Algunos Composables con Hilt no pueden previewarse
- Crea funciones de preview separadas sin inyección

### Tareas no sincronizan
- Verifica conectividad de red
- Check Firestore rules de seguridad
- Verifica logs en logcat

---

## 📝 Convenciones de Commit

```
git commit -m "[FEATURE] Add sign-in screen"
git commit -m "[FIX] Fix task state not updating"
git commit -m "[REFACTOR] Simplify auth repository"
git commit -m "[DOCS] Update architecture guide"
git commit -m "[TEST] Add unit tests for ViewModel"
```

---

## 🎯 Próximos Pasos Típicos

1. **Crear Onboarding Screen** → `ui/onboarding/`
2. **Crear Sign In Screen** → `ui/auth/`
3. **Crear Dashboard Screen** → `ui/dashboard/`
4. **Crear Task Form Screen** → `ui/task_form/`
5. **Agregar Notificaciones** → `ui/notifications/`

---

**Happy Coding! 🎉**
