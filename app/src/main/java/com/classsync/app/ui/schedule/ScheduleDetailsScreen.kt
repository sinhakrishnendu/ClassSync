package com.classsync.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.ui.components.ClassSyncTimePickerDialog
import com.classsync.app.ui.components.dayLabel
import com.classsync.app.ui.components.formattedDate
import com.classsync.app.ui.components.rememberTimeFormatter
import java.time.LocalTime

@Composable
fun ScheduleDetailsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    showMessage: (String) -> Unit,
    viewModel: ScheduleDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    var showReschedule by remember { mutableStateOf(false) }
    val deleted = stringResource(R.string.class_deleted)
    val duplicated = stringResource(R.string.class_duplicated)
    val updated = stringResource(R.string.occurrence_updated)
    val noOccurrence = stringResource(R.string.no_upcoming_occurrence)
    val failed = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScheduleDetailsEvent.Deleted -> {
                    showMessage(deleted)
                    onBack()
                }
                is ScheduleDetailsEvent.EditDuplicate -> {
                    showMessage(duplicated)
                    onEdit(event.id)
                }
                ScheduleDetailsEvent.Updated -> showMessage(updated)
                ScheduleDetailsEvent.NoOccurrence -> showMessage(noOccurrence)
                ScheduleDetailsEvent.Failed -> showMessage(failed)
            }
        }
    }

    val entry = state.entry
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (entry == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.error_generic)) }
        return
    }
    val timeFormatter = rememberTimeFormatter(state.preferences.timeFormat)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.subject.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(entry.group.displayName, style = MaterialTheme.typography.titleMedium)
            entry.group.institution?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(stringResource(R.string.day), dayLabel(entry.schedule.dayOfWeek))
                DetailRow(
                    stringResource(R.string.start_time),
                    "${entry.schedule.startTime.format(timeFormatter)} - ${entry.schedule.endTime.format(timeFormatter)}",
                )
                DetailRow(
                    stringResource(R.string.recurrence),
                    stringResource(if (entry.schedule.recurrenceType == RecurrenceType.WEEKLY) R.string.weekly else R.string.one_time),
                )
                entry.schedule.oneTimeDate?.let { DetailRow(stringResource(R.string.date), it.formattedDate()) }
                entry.schedule.classroom?.let { DetailRow(stringResource(R.string.classroom), it) }
                entry.schedule.teacherName?.let { DetailRow(stringResource(R.string.teacher_name), it) }
                entry.schedule.topic?.let { DetailRow(stringResource(R.string.class_topic), it) }
                entry.schedule.notes?.let { DetailRow(stringResource(R.string.notes), it) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.reminder_before), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.minutes_value, entry.schedule.reminderMinutes),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = entry.schedule.reminderEnabled,
                onCheckedChange = viewModel::setReminderEnabled,
            )
        }
        HorizontalDivider()
        state.nextOccurrence?.let { next ->
            Text(stringResource(R.string.next_class), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${next.date.formattedDate()} | ${next.start.toLocalTime().format(timeFormatter)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.updateNextOccurrence(ExceptionStatus.CANCELLED) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.EventBusy, contentDescription = null)
                    Text(stringResource(R.string.cancelled), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = { viewModel.updateNextOccurrence(ExceptionStatus.COMPLETED) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.completed))
                }
            }
            OutlinedButton(onClick = { showReschedule = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Text(stringResource(R.string.reschedule_next_class), Modifier.padding(start = 8.dp))
            }
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEdit(entry.schedule.id) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text(stringResource(R.string.edit), Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = viewModel::duplicate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Text(stringResource(R.string.duplicate), Modifier.padding(start = 8.dp))
            }
        }
        TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Text(stringResource(R.string.delete), Modifier.padding(start = 8.dp))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_class_title)) },
            text = { Text(stringResource(R.string.delete_class_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showReschedule) {
        RescheduleDialog(
            initialStart = state.nextOccurrence?.start?.toLocalTime() ?: entry.schedule.startTime,
            initialEnd = state.nextOccurrence?.end?.toLocalTime() ?: entry.schedule.endTime,
            onDismiss = { showReschedule = false },
            onConfirm = { start, end ->
                viewModel.updateNextOccurrence(ExceptionStatus.RESCHEDULED, start, end)
                showReschedule = false
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.weight(0.38f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(0.62f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RescheduleDialog(
    initialStart: LocalTime,
    initialEnd: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime) -> Unit,
) {
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var editStart by remember { mutableStateOf(false) }
    var editEnd by remember { mutableStateOf(false) }
    val valid = end.isAfter(start)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reschedule_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editStart = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.start_time)}: $start")
                }
                OutlinedButton(onClick = { editEnd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.end_time)}: $end")
                }
                if (!valid) Text(stringResource(R.string.invalid_time), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(start, end) }, enabled = valid) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (editStart) ClassSyncTimePickerDialog(start, { editStart = false }) {
        start = it
        editStart = false
    }
    if (editEnd) ClassSyncTimePickerDialog(end, { editEnd = false }) {
        end = it
        editEnd = false
    }
}
