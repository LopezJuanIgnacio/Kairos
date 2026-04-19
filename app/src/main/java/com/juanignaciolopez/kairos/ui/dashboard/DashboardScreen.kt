package com.juanignaciolopez.kairos.ui.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanignaciolopez.kairos.core.utils.DateUtils
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskStatus
import com.juanignaciolopez.kairos.ui.auth.AuthViewModel
import kotlin.math.abs

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteTask by remember { mutableStateOf<Task?>(null) }

    val activeTasks = allTasks.filter {
        it.status != TaskStatus.COMPLETED &&
            it.status != TaskStatus.ARCHIVED &&
            it.status != TaskStatus.DELETED
    }

    val now = System.currentTimeMillis()
    val weekInMillis = 7L * 24L * 60L * 60L * 1000L

    val recurrentes = activeTasks.filter { it.isRecurring }
    val accionables = activeTasks.filter {
        it.isNextAction || (it.estimatedMinutes in 1..5)
    }
    val cortoPlazo = activeTasks.filter { task ->
        task.dueDate?.let { due ->
            due >= now && due - now <= weekInMillis
        } == true
    }
    val largoPlazo = activeTasks.filter { task ->
        task.dueDate?.let { due ->
            due - now > weekInMillis
        } == true
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
                    IconButton(onClick = { exportAllTasksToCalendar(context, activeTasks) }) {
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
            item {
                CategorySection(
                    title = "Recurrentes",
                    tasks = recurrentes,
                    onEditTask = onEditTask,
                    onDeleteTask = { pendingDeleteTask = it },
                    onExportTask = { exportTaskToCalendar(context, it) }
                )
            }
            item {
                CategorySection(
                    title = "Accionables",
                    tasks = accionables,
                    onEditTask = onEditTask,
                    onDeleteTask = { pendingDeleteTask = it },
                    onExportTask = { exportTaskToCalendar(context, it) }
                )
            }
            item {
                CategorySection(
                    title = "Corto plazo",
                    tasks = cortoPlazo,
                    onEditTask = onEditTask,
                    onDeleteTask = { pendingDeleteTask = it },
                    onExportTask = { exportTaskToCalendar(context, it) }
                )
            }
            item {
                CategorySection(
                    title = "Largo plazo",
                    tasks = largoPlazo,
                    onEditTask = onEditTask,
                    onDeleteTask = { pendingDeleteTask = it },
                    onExportTask = { exportTaskToCalendar(context, it) }
                )
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

private fun exportTaskToCalendar(context: android.content.Context, task: Task) {
    val startMillis = task.dueDate ?: task.scheduledDate ?: System.currentTimeMillis() + 60 * 60 * 1000
    val endMillis = startMillis + maxOf(task.estimatedMinutes, 30) * 60 * 1000L

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, task.title)
        putExtra(CalendarContract.Events.DESCRIPTION, task.description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        if (it is ActivityNotFoundException) {
            // No calendar app available.
        }
    }
}

private fun exportAllTasksToCalendar(context: android.content.Context, tasks: List<Task>) {
    if (tasks.isEmpty()) return

    val nextDate = tasks.mapNotNull { it.dueDate ?: it.scheduledDate }
        .minByOrNull { abs(it - System.currentTimeMillis()) }
        ?: System.currentTimeMillis() + 60 * 60 * 1000

    val description = buildString {
        append("Tareas exportadas de Kairos:\n\n")
        tasks.forEachIndexed { index, task ->
            append("${index + 1}. ${task.title}")
            if (task.description.isNotBlank()) {
                append(" - ${task.description}")
            }
            task.dueDate?.let {
                append(" | ${DateUtils.formatDateTime(it)}")
            }
            append("\n")
        }
    }

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Kairos - Exportación de tareas")
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nextDate)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, nextDate + 60 * 60 * 1000)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        if (it is ActivityNotFoundException) {
            // No calendar app available.
        }
    }
}
