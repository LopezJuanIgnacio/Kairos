package com.juanignaciolopez.kairos.core.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.juanignaciolopez.kairos.R
import com.juanignaciolopez.kairos.data.models.Task

@Composable
fun CategorySection(
    modifier: Modifier = Modifier,
    title: String,
    tasks: List<Task>,
    categoryHeaderDividerWidth: Dp,
    onEditTask: (String) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onExportAllTasks: () -> Unit,
    onExportTask: (Task) -> Unit
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = if (isLandscape) Modifier.fillMaxHeight() else Modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    modifier = Modifier
                        .size(width = categoryHeaderDividerWidth, height = 3.dp)
                        .padding(horizontal = 12.dp),
                    thickness = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                CircleIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.dashboard_export_category_content_description),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    onClick = onExportAllTasks,
                    hasShadow = false,
                    size = 52.dp
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_no_tasks_in_category),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                if (isLandscape) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tasks.forEach { task ->
                            TaskCard(
                                task = task,
                                onEdit = { onEditTask(task.id) },
                                onDelete = { onDeleteTask(task) },
                                onExport = { onExportTask(task) }
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tasks.forEach { task ->
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
    }
}
