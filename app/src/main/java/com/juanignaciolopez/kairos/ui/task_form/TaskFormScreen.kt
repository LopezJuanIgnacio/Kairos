package com.juanignaciolopez.kairos.ui.task_form

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanignaciolopez.kairos.core.components.CustomTextField
import com.juanignaciolopez.kairos.core.utils.DateUtils
import com.juanignaciolopez.kairos.core.utils.EnumUtils
import com.juanignaciolopez.kairos.data.models.TaskCategory
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TaskFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: "Error inesperado")
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
            viewModel.consumeSavedEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) {
                            "Editar tarea"
                        } else {
                            "Crear tarea"
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = "Título",
                placeholder = "Ej. Preparar presentación",
                isError = uiState.titleError != null,
                errorMessage = uiState.titleError
            )

            CustomTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = "Descripción",
                placeholder = "Detalles de la tarea",
                maxLines = 4,
                isError = uiState.descriptionError != null,
                errorMessage = uiState.descriptionError
            )

            CategoryDropdown(
                selected = uiState.category,
                onSelected = viewModel::onCategoryChanged
            )

            DueDateField(
                dueDate = uiState.dueDate,
                dueDateError = uiState.dueDateError,
                onSelectDateTime = viewModel::onDueDateChanged
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::saveTask,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Guardar")
            }

            TextButton(
                onClick = onCancel,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: TaskCategory,
    onSelected: (TaskCategory) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = EnumUtils.categoryToString(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskCategory.entries.forEach { category ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(EnumUtils.categoryToString(category)) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DueDateField(
    dueDate: Long?,
    dueDateError: String?,
    onSelectDateTime: (Long?) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    if (dueDate != null) {
        calendar.timeInMillis = dueDate
    }

    val openDateTimePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val dateCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Si existe una fecha previa, conservamos la hora ya elegida.
                    if (dueDate != null) {
                        set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                    }
                }

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        dateCalendar.apply {
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onSelectDateTime(dateCalendar.timeInMillis)
                    },
                    dateCalendar.get(Calendar.HOUR_OF_DAY),
                    dateCalendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = dueDate?.let(DateUtils::formatDateTime) ?: "Seleccionar fecha y hora límite",
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha y hora límite") },
            isError = dueDateError != null,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = openDateTimePicker, modifier = Modifier.align(Alignment.Start)) {
            Text("Elegir fecha y hora")
        }

        if (dueDateError != null) {
            Text(
                text = dueDateError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
