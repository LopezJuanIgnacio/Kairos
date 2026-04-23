package com.juanignaciolopez.kairos.ui.dashboard

import android.Manifest
import android.content.res.Configuration
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.core.components.CircleIcon
import com.juanignaciolopez.kairos.core.components.CategorySection
import com.juanignaciolopez.kairos.core.utils.CALENDAR_PERMISSIONS
import com.juanignaciolopez.kairos.core.utils.RequestNotificationPermissionIfNeeded
import com.juanignaciolopez.kairos.core.utils.exportTaskToCalendar
import com.juanignaciolopez.kairos.core.utils.exportTasksDirectlyToCalendar
import com.juanignaciolopez.kairos.core.utils.handleCalendarExportRequest
import com.juanignaciolopez.kairos.core.utils.EnumUtils
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.ui.auth.AuthViewModel
import kotlinx.coroutines.launch

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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val categoryHeaderDividerWidth = if (isLandscape) 112.dp else 175.dp
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteTask by remember { mutableStateOf<Task?>(null) }
    var pendingExportConfirmationTask by remember { mutableStateOf<Task?>(null) }
    var pendingCalendarExport by remember { mutableStateOf<PendingCalendarExport?>(null) }
    var showBulkExportCountDialog by remember { mutableStateOf(false) }
    var showBulkIncludeExportedDialog by remember { mutableStateOf(false) }
    var pendingBulkExportTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var pendingBulkExportLabel by remember { mutableStateOf(context.getString(R.string.dashboard_tasks_label)) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val activeTasks = allTasks

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
                snackbarHostState.showSnackbar(context.getString(R.string.dashboard_calendar_permission_required))
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
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_calendar_app_not_found))
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
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_no_tasks_to_export))
                    }
                } else if (result.exportedCount == 0) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_calendar_save_failed))
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.dashboard_exported_count_message,
                                result.exportedCount,
                                result.total
                            )
                        )
                    }
                }
            }

            null -> Unit
        }

        hasRequestedNotificationPermission = true
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
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_calendar_app_not_found))
                    }
                }
            },
            onNeedPermission = { pendingRequest ->
                pendingCalendarExport = pendingRequest
                calendarPermissionLauncher.launch(CALENDAR_PERMISSIONS)
            }
        )
    }

    fun launchBulkTasksExport(baseTasks: List<Task>, includeAlreadyExported: Boolean) {
        val tasksToExport = if (includeAlreadyExported) {
            baseTasks
        } else {
            baseTasks.filter { !it.isExported }
        }

        if (tasksToExport.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.dashboard_no_tasks_for_filter))
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
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_no_tasks_to_export))
                    } else if (result.exportedCount == 0) {
                        snackbarHostState.showSnackbar(context.getString(R.string.dashboard_calendar_save_failed))
                    } else {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.dashboard_exported_count_message,
                                result.exportedCount,
                                result.total
                            )
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

    fun requestBulkExport(tasks: List<Task>, label: String) {
        if (tasks.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.dashboard_no_tasks_to_export))
            }
            return
        }

        pendingBulkExportTasks = tasks
        pendingBulkExportLabel = label
        showBulkExportCountDialog = true
    }

    LaunchedEffect(authState.isSignedOut) {
        if (authState.isSignedOut) {
            onSignedOut()
            authViewModel.consumeSignOutNavigation()
        }
    }

    RequestNotificationPermissionIfNeeded(
        hasRequestedNotificationPermission = hasRequestedNotificationPermission,
        onPermissionResult = { hasRequestedNotificationPermission = true }
    )

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: context.getString(R.string.common_unexpected_error))
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 32.dp),
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = authViewModel::signOut,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.dashboard_sign_out_content_description),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onOpenHelp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = stringResource(R.string.dashboard_help_content_description),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                requestBulkExport(activeTasks, context.getString(R.string.dashboard_all_tasks_label))
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.dashboard_export_all_content_description),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            CircleIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.dashboard_create_task_content_description),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                onClick = onCreateTask,
                size = 88.dp
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
                        .width(52.dp),
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = authViewModel::signOut,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.dashboard_sign_out_content_description),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onOpenHelp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = stringResource(R.string.dashboard_help_content_description),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                requestBulkExport(activeTasks, context.getString(R.string.dashboard_all_tasks_label))
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.dashboard_export_all_content_description),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    items(TaskCategory.entries) { category ->
                        CategorySection(
                            modifier = Modifier
                                .width(330.dp)
                                .fillMaxHeight(),
                            title = EnumUtils.categoryToString(category),
                            tasks = tasksByCategory[category].orEmpty(),
                            categoryHeaderDividerWidth = categoryHeaderDividerWidth,
                            onEditTask = onEditTask,
                            onDeleteTask = { pendingDeleteTask = it },
                            onExportAllTasks = {
                                requestBulkExport(
                                    tasks = tasksByCategory[category].orEmpty(),
                                    label = EnumUtils.categoryToString(category)
                                )
                            },
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TaskCategory.entries.forEach { category ->
                    item {
                        CategorySection(
                            title = EnumUtils.categoryToString(category),
                            tasks = tasksByCategory[category].orEmpty(),
                            categoryHeaderDividerWidth = categoryHeaderDividerWidth,
                            onEditTask = onEditTask,
                            onDeleteTask = { pendingDeleteTask = it },
                            onExportAllTasks = {
                                requestBulkExport(
                                    tasks = tasksByCategory[category].orEmpty(),
                                    label = EnumUtils.categoryToString(category)
                                )
                            },
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
            title = { Text(stringResource(R.string.dashboard_delete_task_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dashboard_delete_task_confirmation,
                        pendingDeleteTask?.title.orEmpty()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val taskId = pendingDeleteTask?.id.orEmpty()
                        pendingDeleteTask = null
                        viewModel.deleteTask(taskId)
                    }
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTask = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (pendingExportConfirmationTask != null) {
        AlertDialog(
            onDismissRequest = { pendingExportConfirmationTask = null },
            title = { Text(stringResource(R.string.dashboard_task_already_exported_title)) },
            text = {
                Text(stringResource(R.string.dashboard_task_already_exported_message))
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
                    Text(stringResource(R.string.dashboard_export_again))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExportConfirmationTask = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showBulkExportCountDialog) {
        AlertDialog(
            onDismissRequest = { showBulkExportCountDialog = false },
            title = { Text(stringResource(R.string.dashboard_export_tasks_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dashboard_bulk_export_count_message,
                        pendingBulkExportTasks.size,
                        pendingBulkExportLabel
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkExportCountDialog = false
                        showBulkIncludeExportedDialog = true
                    }
                ) {
                    Text(stringResource(R.string.common_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkExportCountDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showBulkIncludeExportedDialog) {
        AlertDialog(
            onDismissRequest = { showBulkIncludeExportedDialog = false },
            title = { Text(stringResource(R.string.dashboard_exported_tasks_title)) },
            text = {
                Text(stringResource(R.string.dashboard_include_exported_question))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkIncludeExportedDialog = false
                        launchBulkTasksExport(
                            baseTasks = pendingBulkExportTasks,
                            includeAlreadyExported = true
                        )
                    }
                ) {
                    Text(stringResource(R.string.dashboard_include_exported_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkIncludeExportedDialog = false
                        launchBulkTasksExport(
                            baseTasks = pendingBulkExportTasks,
                            includeAlreadyExported = false
                        )
                    }
                ) {
                    Text(stringResource(R.string.dashboard_include_exported_no))
                }
            }
        )
    }
}

private sealed interface PendingCalendarExport {
    data class SingleTask(val task: Task) : PendingCalendarExport
    data class AllTasks(val tasks: List<Task>) : PendingCalendarExport
}
