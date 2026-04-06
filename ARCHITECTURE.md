# 🏗️ KAIROS - Guía de Arquitectura

## 📋 Descripción del Proyecto

**Kairos** es una aplicación de gestión de tareas basada en los principios de **GTD (Getting Things Done)**, construida con Kotlin, Jetpack Compose y arquitectura MVVM modular.

### Stack Tecnológico
- **Lenguaje**: Kotlin 2.0
- **UI Framework**: Jetpack Compose
- **Arquitectura**: MVVM + Clean Architecture (modular)
- **Backend**: Firebase (Auth + Firestore)
- **Base de Datos Local**: Room
- **Inyección de Dependencias**: Hilt
- **Navegación**: Jetpack Navigation Compose
- **Min SDK**: 27 | Target SDK: 36

---

## 📁 Estructura del Proyecto

```
com/juanignaciolopez/kairos/
├── core/                          # Funcionalidades transversales
│   ├── navigation/               
│   │   ├── NavRoute.kt          # Sealed class con todas las rutas
│   │   ├── NavHost.kt           # Composable de navegación principal
│   │   └── NavGraph.kt          # Enumeración de flujos de nav
│   │
│   ├── components/              # Componentes UI reutilizables
│   │   ├── LoadingIndicator.kt  # Indicadores de carga
│   │   ├── DialogComponents.kt  # Diálogos de error/confirmación
│   │   ├── TextFieldComponents.kt # Campos de texto reutilizables
│   │   └── EmptyState.kt        # Estado vacío
│   │
│   ├── utils/                   # Utilidades comunes
│   │   ├── DateUtils.kt         # Formateo de fechas
│   │   ├── ValidationUtils.kt   # Validaciones (email, password, etc)
│   │   └── EnumUtils.kt         # Conversión de enums a strings
│   │
│   └── theme/                   # Tema Material Design 3
│
├── data/                         # Data Layer
│   ├── models/                  # Modelos de datos
│   │   ├── User.kt              # Modelo de usuario
│   │   ├── Task.kt              # Modelo de tarea (GTD)
│   │   ├── AuthState.kt         # Estados de autenticación
│   │   └── Result.kt            # Sealed class para resultados
│   │
│   ├── local/                   # Persistencia Local (Room)
│   │   ├── TaskDao.kt           # DAO para tareas
│   │   ├── UserDao.kt           # DAO para usuarios
│   │   └── KairosDatabase.kt    # Configuración de Room
│   │
│   ├── remote/                  # Fuentes de datos remotas (Firebase)
│   │   ├── FirebaseAuthService.kt    # Autenticación
│   │   ├── FirebaseTaskService.kt    # Tareas
│   │   └── FirebaseUserService.kt    # Usuario
│   │
│   └── repository/              # Implementación de repositorios
│       ├── AuthRepositoryImpl.kt
│       ├── TaskRepositoryImpl.kt
│       └── UserRepositoryImpl.kt
│
├── domain/                       # Domain Layer (Business Logic)
│   ├── repository/              # Contratos de repositorios
│   │   ├── AuthRepository.kt
│   │   ├── TaskRepository.kt
│   │   └── UserRepository.kt
│   │
│   └── usecase/                 # Casos de uso (próxima fase)
│       └── [UseCases futuros]
│
├── ui/                          # Presentation Layer
│   ├── viewmodel/               # ViewModels base
│   │   ├── BaseViewModel.kt         # Clase base genérica
│   │   ├── BaseEventViewModel.kt    # Con soporte para eventos
│   │   └── UiState.kt              # Interface para estados
│   │
│   ├── onboarding/              # Feature: Onboarding
│   │   ├── OnboardingScreen.kt
│   │   └── OnboardingViewModel.kt
│   │
│   ├── auth/                    # Feature: Autenticación
│   │   ├── SignInScreen.kt
│   │   ├── SignInViewModel.kt
│   │   ├── SignUpScreen.kt
│   │   └── SignUpViewModel.kt
│   │
│   ├── dashboard/               # Feature: Dashboard
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   │
│   ├── task_form/               # Feature: Formulario de tareas
│   │   ├── TaskFormScreen.kt
│   │   └── TaskFormViewModel.kt
│   │
│   ├── notifications/           # Feature: Notificaciones
│   │   ├── NotificationsScreen.kt
│   │   └── NotificationsViewModel.kt
│   │
│   ├── common/                  # Componentes comunes de UI
│   │   └── CommonComponents.kt
│   │
│   └── theme/                   # Tema Material Design 3
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/                          # Dependency Injection (Hilt)
│   └── AppModule.kt            # Módulos: Database, Firebase, Repository
│
├── KairosApplication.kt         # Aplicación con @HiltAndroidApp
└── MainActivity.kt              # Activity principal
```

---

## 🎯 Arquitectura Modular (Clean Architecture)

### Capas de Arquitectura

```
┌─────────────────────────────────────┐
│  PRESENTATION (ui/)                 │
│  ├── Screens (Compose)              │
│  ├── ViewModels                     │
│  └── UI States & Events             │
├─────────────────────────────────────┤
│  DOMAIN (domain/)                   │
│  ├── Repository Interfaces          │
│  ├── Use Cases (futuro)             │
│  └── Business Rules                 │
├─────────────────────────────────────┤
│  DATA (data/)                       │
│  ├── Repository Implementations     │
│  ├── Local Data Source (Room)       │
│  ├── Remote Data Source (Firebase)  │
│  └── Data Models                    │
└─────────────────────────────────────┘
```

### Flujo de Datos

```
┌──────────────┐
│ UI (Compose) │
└──────────────┘
       ↓
┌──────────────────────┐
│ ViewModel            │
│ - State (StateFlow)  │
│ - Events (Channel)   │
└──────────────────────┘
       ↓
┌──────────────────────┐
│ Repository          │
│ (Abstract interface) │
└──────────────────────┘
       ↓
     ┌────────────────────────┐
     │   Data Sources         │
     ├────────────────────────┤
     │ Local: Room Database   │
     │ Remote: Firebase       │
     └────────────────────────┘
```

---

## 🏗️ Patrones Implementados

### 1. **MVVM (Model-View-ViewModel)**
```kotlin
// ViewModel Base
BaseViewModel<STATE> : ViewModel()
    - Maneja states
    - Emite actualizaciones via StateFlow
    - Expone: state: StateFlow<STATE>

// Con Events
BaseEventViewModel<STATE, EVENT> : BaseViewModel<STATE>()
    - Maneja state + events
    - Useful para notificaciones, navegación
```

### 2. **Repository Pattern**
- **Interfaces** en `domain/repository/` definen el contrato
- **Implementaciones** en `data/repository/` usan Local + Remote
- Abstrae la lógica de obtención de datos

### 3. **Sealed Classes para Type Safety**
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(...) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

sealed class NavRoute(val route: String) {
    data object SignIn : NavRoute("signin")
    data object Dashboard : NavRoute("dashboard")
}
```

### 4. **Inyección de Dependencias con Hilt**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context): KairosDatabase = ...
}
```

### 5. **Flow para Reactividad**
```kotlin
// Repositories exponen Flows
fun getAllTasks(userId: String): Flow<List<Task>>
fun getCurrentUser(): Flow<User?>

// ViewModels colectan Flow → StateFlow
StateFlow<List<Task>>
```

---

## 📱 Metodología GTD Implementada

### Estados de Tarea (TaskStatus)
```kotlin
enum class TaskStatus {
    INBOX,          // 📥 Entrada inicial
    PROCESSING,     // ⚙️ En procesamiento
    TODO,           // ☐ Por hacer
    IN_PROGRESS,    // 🔄 En progreso
    COMPLETED,      // ✅ Completada
    ARCHIVED,       // 📦 Archivada
    DELETED         // 🗑️ Eliminada
}
```

### Prioridades (TaskPriority)
```kotlin
enum class TaskPriority {
    VERY_HIGH,  // 🔴 Crítica
    HIGH,       // 🟠 Alta
    NORMAL,     // 🟡 Normal
    LOW,        // 🟢 Baja
    VERY_LOW    // ⚪ Muy baja
}
```

### Características GTD en Task
```kotlin
data class Task(
    val id: String,
    val title: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val category: TaskCategory,
    
    // GTD specifics
    val isNextAction: Boolean,      // Próxima acción
    val context: String,            // @Home, @Office, @Computer
    val project: String?,           // Proyecto asociado
    val estimatedMinutes: Int,      // Estimación de tiempo
    val dueDate: Long?,             // Fecha de vencimiento
    val isRecurring: Boolean,       // ¿Recurrente?
    val recurrencePattern: String?, // Patrón de recurrencia
    
    // Sincronización
    val isSyncPending: Boolean,
    val lastSyncedAt: Long?
)
```

---

## 🔐 Seguridad y Autenticación

### Firebase Auth Flow
1. **Sign Up**: Email + Password + DisplayName → Firebase Auth
2. **Sign In**: Email + Password → Firebase Auth
3. **User Profile**: Almacenado en Firestore + Room local
4. **Offline Support**: Room cache para acceso offline

### Sincronización
- Cambios locales se guardan inmediatamente en Room
- Background sync con Firestore cuando hay conectividad
- Conflictos resueltos por last-write-wins

---

## 📦 Dependencias Principales

```toml
# Compose & UI
androidx-compose-bom = 2024.09.00
androidx-compose-material3
androidx-navigation-compose = 2.7.0

# ViewModel & Lifecycle
androidx-lifecycle-viewmodel-compose = 2.7.0

# Firebase
firebase-bom = 33.1.1
firebase-auth
firebase-firestore

# Hilt (Dependency Injection)
hilt = 2.51

# Room (Local Database)
room = 2.6.1

# Serialization
kotlinx-serialization-json = 1.6.3
kotlinx-datetime = 0.5.1
kotlinx-coroutines = 1.8.0

# Image Loading
coil = 2.6.0
```

---

## 🚀 Próximas Fases

### Phase 2: Use Cases (Domain Layer)
- `CreateTaskUseCase`
- `UpdateTaskUseCase`
- `GetTasksUseCase`
- `SignInUseCase`
- `SignUpUseCase`

### Phase 3: Screens & ViewModels
- Onboarding screen
- Auth screens (SignIn, SignUp)
- Dashboard screen
- Task form screen
- Task detail screen
- Profile/Settings screens

### Phase 4: Features
- Push notifications
- Recurring tasks
- Task reminders
- Cloud sync
- Offline mode
- Data export

### Phase 5: Testing
- Unit tests (ViewModels, Repositories)
- Integration tests (Database)
- UI tests (Compose)

---

## 🎨 Convenciones de Código

### Nomenclatura
- `Screen.kt`: Composables principales de pantalla
- `ViewModel.kt`: ViewModels asociados
- `Repository.kt`: Interfaz de repositorio
- `RepositoryImpl.kt`: Implementación
- `Dao.kt`: Data Access Objects

### Naming Patterns
```kotlin
// ViewModels
class SignInViewModel : BaseEventViewModel<SignInState, SignInEvent>()

// States
data class SignInState(...) : UiState

// Events
sealed class SignInEvent : UiEvent

// Repositories
interface AuthRepository
class AuthRepositoryImpl : AuthRepository
```

### Folder Organization
- Una carpeta por feature (auth/, dashboard/, etc)
- Mantener related screens & viewmodels en la misma carpeta
- Componentes compartidos en `common/` o `core/`

---

## 🔗 Referencias y Recursos

- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose)
- [Android Architecture Guide](https://developer.android.com/guide/architecture)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [GTD Methodology](https://gettingthingsdone.com/)

---

**Última actualización**: 2024
**Versión**: 1.0.0-base
