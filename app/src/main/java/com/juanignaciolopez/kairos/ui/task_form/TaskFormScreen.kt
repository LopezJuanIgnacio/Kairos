package com.juanignaciolopez.kairos.ui.task_form

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanignaciolopez.kairos.R
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(uiState.errorMessage ?: context.getString(R.string.common_unexpected_error))
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
                label = stringResource(R.string.task_form_title_label),
                placeholder = stringResource(R.string.task_form_title_placeholder),
                isError = uiState.titleError != null,
                errorMessage = uiState.titleError
            )

            CustomTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = stringResource(R.string.task_form_description_label),
                placeholder = stringResource(R.string.task_form_description_placeholder),
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

            Spacer(modifier = Modifier.height(150.dp))

            Button(
                onClick = viewModel::saveTask,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(stringResource(R.string.common_save), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(stringResource(R.string.common_cancel), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
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
            label = { Text(stringResource(R.string.common_category)) },
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
    val pickerInteractionSource = remember { MutableInteractionSource() }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = pickerInteractionSource,
                    indication = null,
                    onClick = openDateTimePicker
                )
        ) {
            OutlinedTextField(
                value = dueDate?.let(DateUtils::formatDateTime)
                    ?: stringResource(R.string.task_form_due_date_placeholder),
                onValueChange = {},
                enabled = false,
                label = { Text(stringResource(R.string.task_form_due_date_label)) },
                isError = dueDateError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
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
