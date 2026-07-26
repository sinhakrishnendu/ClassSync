package com.classsync.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.validation.ScheduleField
import com.classsync.app.ui.components.ClassSyncDatePickerDialog
import com.classsync.app.ui.components.ClassSyncTimePickerDialog
import com.classsync.app.ui.components.dayLabel
import com.classsync.app.ui.components.rememberTimeFormatter
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFormScreen(
    contentPadding: PaddingValues,
    onSaved: (Long) -> Unit,
    showMessage: (String) -> Unit,
    viewModel: ScheduleFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(R.string.class_saved)
    val failedMessage = stringResource(R.string.save_failed)
    val reminderFailedMessage = stringResource(R.string.class_saved_reminder_failed)
    val timeFormatter = rememberTimeFormatter(state.timeFormat)
    var dayExpanded by remember { mutableStateOf(false) }
    var selectingStart by remember { mutableStateOf(false) }
    var selectingEnd by remember { mutableStateOf(false) }
    var selectingDate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScheduleFormEvent.Saved -> {
                    showMessage(if (event.reminderScheduled) savedMessage else reminderFailedMessage)
                    onSaved(event.id)
                }
                ScheduleFormEvent.SaveFailed -> showMessage(failedMessage)
            }
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle(stringResource(R.string.courses)) }
        item {
            FormTextField(
                value = state.programme,
                onValueChange = viewModel::setProgramme,
                label = stringResource(R.string.programme),
                error = state.errors.contains(ScheduleField.PROGRAMME),
            )
        }
        item {
            FormTextField(
                value = state.semester,
                onValueChange = viewModel::setSemester,
                label = stringResource(R.string.semester),
                error = state.errors.contains(ScheduleField.SEMESTER),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    value = state.batchSection,
                    onValueChange = viewModel::setBatchSection,
                    label = stringResource(R.string.batch_section),
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = state.institution,
                    onValueChange = viewModel::setInstitution,
                    label = stringResource(R.string.institution),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { SectionTitle(stringResource(R.string.class_details)) }
        item {
            FormTextField(
                value = state.subjectName,
                onValueChange = viewModel::setSubjectName,
                label = stringResource(R.string.subject),
                error = state.errors.contains(ScheduleField.SUBJECT),
            )
        }
        item {
            FormTextField(
                value = state.subjectCode,
                onValueChange = viewModel::setSubjectCode,
                label = stringResource(R.string.subject_code),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(
                    value = state.classroom,
                    onValueChange = viewModel::setClassroom,
                    label = stringResource(R.string.classroom),
                    modifier = Modifier.weight(1f),
                )
                FormTextField(
                    value = if (state.mode == UserMode.STUDENT) state.teacherName else state.topic,
                    onValueChange = if (state.mode == UserMode.STUDENT) viewModel::setTeacherName else viewModel::setTopic,
                    label = stringResource(if (state.mode == UserMode.STUDENT) R.string.teacher_name else R.string.class_topic),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            FormTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = stringResource(R.string.notes),
                singleLine = false,
                minLines = 3,
            )
        }
        item { SectionTitle(stringResource(R.string.recurrence)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.recurrenceType == RecurrenceType.WEEKLY,
                    onClick = { viewModel.setRecurrence(RecurrenceType.WEEKLY) },
                    label = { Text(stringResource(R.string.weekly)) },
                )
                FilterChip(
                    selected = state.recurrenceType == RecurrenceType.ONE_TIME,
                    onClick = { viewModel.setRecurrence(RecurrenceType.ONE_TIME) },
                    label = { Text(stringResource(R.string.one_time)) },
                )
            }
        }
        if (state.recurrenceType == RecurrenceType.WEEKLY) {
            item {
                ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }) {
                    OutlinedTextField(
                        value = dayLabel(state.dayOfWeek),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text(stringResource(R.string.day)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                    )
                    ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        DayOfWeek.values().forEach { day ->
                            DropdownMenuItem(
                                text = { Text(dayLabel(day)) },
                                onClick = {
                                    viewModel.setDay(day)
                                    dayExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        } else {
            item {
                OutlinedButton(onClick = { selectingDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Text(
                        state.oneTimeDate?.format(DateTimeFormatter.ofPattern("dd MMM uuuu"))
                            ?: stringResource(R.string.select_date),
                        Modifier.padding(start = 8.dp),
                    )
                }
                if (state.errors.contains(ScheduleField.ONE_TIME_DATE)) {
                    Text(stringResource(R.string.invalid_date), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeButton(
                    label = stringResource(R.string.start_time),
                    time = state.startTime.format(timeFormatter),
                    isError = state.errors.contains(ScheduleField.START_TIME),
                    modifier = Modifier.weight(1f),
                    onClick = { selectingStart = true },
                )
                TimeButton(
                    label = stringResource(R.string.end_time),
                    time = state.endTime.format(timeFormatter),
                    isError = state.errors.contains(ScheduleField.END_TIME),
                    modifier = Modifier.weight(1f),
                    onClick = { selectingEnd = true },
                )
            }
            if (state.errors.contains(ScheduleField.START_TIME) || state.errors.contains(ScheduleField.END_TIME)) {
                Text(stringResource(R.string.invalid_time), color = MaterialTheme.colorScheme.error)
            }
        }
        item { SectionTitle(stringResource(R.string.reminder)) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.reminder_before))
                Switch(checked = state.reminderEnabled, onCheckedChange = viewModel::setReminderEnabled)
            }
        }
        if (state.reminderEnabled) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(5, 10, 15, 30, 45, 60)) { minutes ->
                        FilterChip(
                            selected = state.reminderMinutes == minutes.toString(),
                            onClick = { viewModel.setReminderMinutes(minutes.toString()) },
                            label = { Text(stringResource(R.string.minutes_value, minutes)) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.reminderMinutes,
                    onValueChange = viewModel::setReminderMinutes,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.errors.contains(ScheduleField.REMINDER_MINUTES),
                    supportingText = if (state.errors.contains(ScheduleField.REMINDER_MINUTES)) {
                        { Text(stringResource(R.string.invalid_reminder)) }
                    } else null,
                    singleLine = true,
                )
            }
        }
        if (state.duplicate) {
            item { Text(stringResource(R.string.duplicate_warning), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            ) {
                if (state.isSaving) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.save))
            }
        }
    }

    if (selectingStart) {
        ClassSyncTimePickerDialog(state.startTime, { selectingStart = false }) {
            viewModel.setStartTime(it)
            selectingStart = false
        }
    }
    if (selectingEnd) {
        ClassSyncTimePickerDialog(state.endTime, { selectingEnd = false }) {
            viewModel.setEndTime(it)
            selectingEnd = false
        }
    }
    if (selectingDate) {
        ClassSyncDatePickerDialog(state.oneTimeDate ?: java.time.LocalDate.now(), { selectingDate = false }) {
            viewModel.setOneTimeDate(it)
            selectingDate = false
        }
    }
    if (state.showOverlapConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::dismissOverlap,
            title = { Text(stringResource(R.string.overlap_title)) },
            text = { Text(stringResource(R.string.overlap_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.save(confirmOverlap = true) }) {
                    Text(stringResource(R.string.save_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverlap) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = error,
        supportingText = if (error) ({ Text(stringResource(R.string.required_field)) }) else null,
        singleLine = singleLine,
        minLines = minLines,
    )
}

@Composable
private fun TimeButton(
    label: String,
    time: String,
    isError: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Schedule, contentDescription = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(time, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}
