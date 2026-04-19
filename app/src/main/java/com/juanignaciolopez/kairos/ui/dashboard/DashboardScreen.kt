package com.juanignaciolopez.kairos.ui.dashboard

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanignaciolopez.kairos.core.utils.DateUtils
import com.juanignaciolopez.kairos.core.utils.EnumUtils
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.data.models.TaskStatus
import com.juanignaciolopez.kairos.ui.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenHelp: () -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteTask by remember { mutableStateOf<Task?>(null) }
    var pendingExportConfirmationTask by remember { mutableStateOf<Task?>(null) }
    var pendingCalendarExport by remember { mutableStateOf<PendingCalendarExport?>(null) }
    var showBulkExportCountDialog by remember { mutableStateOf(false) }
    var showBulkIncludeExportedDialog by remember { mutableStateOf(false) }

    val activeTasks = allTasks.filter {
        it.status != TaskStatus.COMPLETED &&
            it.status != TaskStatus.ARCHIVED &&
            it.status != TaskStatus.DELETED
    }

    val tasksByCategory = TaskCategory.entries.associateWith { category ->
        activeTasks.filter { it.category == category }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true

        if (!allGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Se requieren permisos de calendario para exportar tareas")
            }
            pendingCalendarExport = null
            return@rememberLauncherForActivityResult
        }

        when (val request = pendingCalendarExport) {
            is PendingCalendarExport.SingleTask -> {
                if (exportTaskToCalendar(context, request.task)) {
                    viewModel.markTaskExported(request.task.id)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("No se pudo abrir una app de calendario")
                    }
                }
            }

            is PendingCalendarExport.AllTasks -> {
                val result = exportTasksDirectlyToCalendar(
                    context = context,
                    tasks = request.tasks,
                    onTaskExported = { viewModel.markTaskExported(it.id) }
                )
                if (result.total == 0) {
                    scope.launch {
                        snackbarHostState.showSnackbar("No hay tareas para exportar")
                    }
                } else if (result.exportedCount == 0) {
                    scope.launch {
                        snackbarHostState.showSnackbar("No se pudo guardar eventos en el calendario")
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Se exportaron ${result.exportedCount} de ${result.total} tareas"
                        )
                    }
                }
            }

            null -> Unit
        }

        pendingCalendarExport = null
    }

    fun launchSingleTaskExport(task: Task) {
        val request = PendingCalendarExport.SingleTask(task)
        handleCalendarExportRequest(
            context = context,
            request = request,
            onReady = { readyRequest ->
                if (exportTaskToCalendar(context, readyRequest.task)) {
                    viewModel.markTaskExported(readyRequest.task.id)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("No se pudo abrir una app de calendario")
                    }
                }
            },
            onNeedPermission = { pendingRequest ->
                pendingCalendarExport = pendingRequest
                calendarPermissionLauncher.launch(CALENDAR_PERMISSIONS)
            }
        )
    }

    fun launchBulkTasksExport(includeAlreadyExported: Boolean) {
        val tasksToExport = if (includeAlreadyExported) {
            activeTasks
        } else {
            activeTasks.filter { !it.isExported }
        }

        if (tasksToExport.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar("No hay tareas para exportar con ese filtro")
            }
            return
        }

        val request = PendingCalendarExport.AllTasks(tasksToExport)
        handleCalendarExportRequest(
            context = context,
            request = request,
            onReady = { readyRequest ->
                val result = exportTasksDirectlyToCalendar(
                    context = context,
                    tasks = readyRequest.tasks,
                    onTaskExported = { viewModel.markTaskExported(it.id) }
                )
                scope.launch {
                    if (result.total == 0) {
                        snackbarHostState.showSnackbar("No hay tareas para exportar")
                    } else if (result.exportedCount == 0) {
                        snackbarHostState.showSnackbar("No se pudo guardar eventos en el calendario")
                    } else {
                        snackbarHostState.showSnackbar(
                            "Se exportaron ${result.exportedCount} de ${result.total} tareas"
                        )
                    }
                }
            },
            onNeedPermission = { pendingRequest ->
                pendingCalendarExport = pendingRequest
                calendarPermissionLauncher.launch(CALENDAR_PERMISSIONS)
            }
        )
    }

    LaunchedEffect(authState.isSignedOut) {
        if (authState.isSignedOut) {
            onSignedOut()
            authViewModel.consumeSignOutNavigation()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: "Error inesperado")
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard GTD", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            if (activeTasks.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No hay tareas para exportar")
                                }
                            } else {
                                showBulkExportCountDialog = true
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.IosShare, contentDescription = "Exportar todo")
                    }
                    IconButton(onClick = onOpenHelp) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Ayuda")
                    }
                    IconButton(onClick = authViewModel::signOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Crear nueva tarea")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TaskCategory.entries.forEach { category ->
                item {
                    CategorySection(
                        title = EnumUtils.categoryToString(category),
                        tasks = tasksByCategory[category].orEmpty(),
                        onEditTask = onEditTask,
                        onDeleteTask = { pendingDeleteTask = it },
                        onExportTask = { task ->
                            if (task.isExported) {
                                pendingExportConfirmationTask = task
                            } else {
                                launchSingleTaskExport(task)
                            }
                        }
                    )
                }
            }
        }
    }

    if (authState.isLoading || uiState.isDeleting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    if (pendingDeleteTask != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteTask = null },
            title = { Text("Eliminar tarea") },
            text = {
                Text("¿Seguro que quieres eliminar '${pendingDeleteTask?.title.orEmpty()}'?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val taskId = pendingDeleteTask?.id.orEmpty()
                        pendingDeleteTask = null
                        viewModel.deleteTask(taskId)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTask = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingExportConfirmationTask != null) {
        AlertDialog(
            onDismissRequest = { pendingExportConfirmationTask = null },
            title = { Text("Tarea ya exportada") },
            text = {
                Text("Esta tarea ya fue exportada al calendario. ¿Deseas exportarla nuevamente?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val taskToExport = pendingExportConfirmationTask
                        pendingExportConfirmationTask = null
                        if (taskToExport != null) {
                            launchSingleTaskExport(taskToExport)
                        }
                    }
                ) {
                    Text("Exportar nuevamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExportConfirmationTask = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBulkExportCountDialog) {
        AlertDialog(
            onDismissRequest = { showBulkExportCountDialog = false },
            title = { Text("Exportar tareas") },
            text = {
                Text("Se exportarán ${activeTasks.size} tareas como eventos separados. ¿Deseas continuar?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkExportCountDialog = false
                        showBulkIncludeExportedDialog = true
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkExportCountDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBulkIncludeExportedDialog) {
        AlertDialog(
            onDismissRequest = { showBulkIncludeExportedDialog = false },
            title = { Text("Tareas ya exportadas") },
            text = {
                Text("¿Quieres incluir también las tareas que ya fueron exportadas anteriormente?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkIncludeExportedDialog = false
                        launchBulkTasksExport(includeAlreadyExported = true)
                    }
                ) {
                    Text("Sí, incluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkIncludeExportedDialog = false
                        launchBulkTasksExport(includeAlreadyExported = false)
                    }
                ) {
                    Text("No, solo nuevas")
                }
            }
        )
    }
}

private sealed interface PendingCalendarExport {
    data class SingleTask(val task: Task) : PendingCalendarExport
    data class AllTasks(val tasks: List<Task>) : PendingCalendarExport
}

private data class BulkExportResult(
    val exportedCount: Int,
    val total: Int
)

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR
)

private fun <T : PendingCalendarExport> handleCalendarExportRequest(
    context: Context,
    request: T,
    onReady: (T) -> Unit,
    onNeedPermission: (T) -> Unit
) {
    if (hasCalendarPermissions(context)) {
        onReady(request)
    } else {
        onNeedPermission(request)
    }
}

private fun hasCalendarPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun CategorySection(
    title: String,
    tasks: List<Task>,
    onEditTask: (String) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onExportTask: (Task) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Sin tareas en esta categoría",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onEdit = { onEditTask(task.id) },
                        onDelete = { onDeleteTask(task) },
                        onExport = { onExportTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val dateText = task.dueDate?.let { "Fecha: ${DateUtils.formatDateTime(it)}" }
                ?: task.scheduledDate?.let { "Recordar: ${DateUtils.formatDateTime(it)}" }
                ?: "Sin fecha"

            Text(
                text = dateText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onExport) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Exportar al calendario",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun exportTaskToCalendar(context: Context, task: Task): Boolean {
    val startMillis = task.dueDate ?: task.scheduledDate ?: System.currentTimeMillis() + 60 * 60 * 1000
    val endMillis = startMillis + maxOf(task.estimatedMinutes, 30) * 60 * 1000L

    val intent = buildCalendarIntentForTask(task, startMillis, endMillis)

    return runCatching {
        context.startActivity(intent)
        true
    }.onFailure {
        if (it is ActivityNotFoundException) {
            // No calendar app available.
        }
    }.getOrDefault(false)
}

private fun exportTasksDirectlyToCalendar(
    context: Context,
    tasks: List<Task>,
    onTaskExported: (Task) -> Unit
): BulkExportResult {
    if (tasks.isEmpty()) return BulkExportResult(exportedCount = 0, total = 0)

    val calendarId = getWritableCalendarId(context)
        ?: return BulkExportResult(exportedCount = 0, total = tasks.size)

    var exportedCount = 0
    tasks.forEach { task ->
        if (insertTaskEventIntoCalendar(context, calendarId, task)) {
            onTaskExported(task)
            exportedCount += 1
        }
    }

    return BulkExportResult(
        exportedCount = exportedCount,
        total = tasks.size
    )
}

private fun getWritableCalendarId(context: Context): Long? {
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.IS_PRIMARY,
        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.SYNC_EVENTS
    )

    val selection = (
        "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_EDITOR}"
        )

    val sort = "${CalendarContract.Calendars.IS_PRIMARY} DESC"

    return runCatching {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            sort
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun insertTaskEventIntoCalendar(
    context: Context,
    calendarId: Long,
    task: Task
): Boolean {
    val startMillis = task.dueDate ?: task.scheduledDate ?: System.currentTimeMillis() + 60 * 60 * 1000
    val endMillis = startMillis + maxOf(task.estimatedMinutes, 30) * 60 * 1000L
    val categoryLabel = EnumUtils.categoryToString(task.category)

    val eventDescription = buildString {
        if (task.description.isNotBlank()) {
            append(task.description)
        }
        if (isNotEmpty()) {
            append("\n\n")
        }
        append("Categoría: ")
        append(categoryLabel)
    }

    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, task.title)
        put(CalendarContract.Events.DESCRIPTION, eventDescription)
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, endMillis)
        put(CalendarContract.Events.ALL_DAY, 0)
        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }

    return runCatching {
        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return@runCatching false

        // Solo contamos como exportado cuando el evento existe realmente en el provider.
        context.contentResolver.query(
            eventUri,
            arrayOf(CalendarContract.Events._ID),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst()
        } == true
    }.getOrDefault(false)
}

private fun buildCalendarIntentForTask(task: Task, startMillis: Long, endMillis: Long): Intent {
    val categoryLabel = EnumUtils.categoryToString(task.category)
    val eventDescription = buildString {
        if (task.description.isNotBlank()) {
            append(task.description)
        }
        if (isNotEmpty()) {
            append("\n\n")
        }
        append("Categoría: ")
        append(categoryLabel)
    }

    return Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, task.title)
        putExtra(CalendarContract.Events.DESCRIPTION, eventDescription)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    }
}
