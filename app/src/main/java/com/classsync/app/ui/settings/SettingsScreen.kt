package com.classsync.app.ui.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.ui.components.NotificationPermissionCard
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    showMessage: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteAll by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf<String?>(null) }

    val exportSuccess = stringResource(R.string.export_success)
    val exportFailed = stringResource(R.string.export_failed)
    val importSuccess = stringResource(R.string.import_success)
    val importFailed = stringResource(R.string.import_failed)
    val deleted = stringResource(R.string.all_data_deleted)
    val genericError = stringResource(R.string.error_generic)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = exportContent
        if (uri != null && content != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { context.writeText(uri, content) } }
                    .onSuccess { showMessage(exportSuccess) }
                    .onFailure { showMessage(exportFailed) }
                exportContent = null
            }
        } else {
            exportContent = null
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { context.readText(uri) } }
                    .onSuccess(viewModel::importJson)
                    .onFailure { showMessage(importFailed) }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            showMessage(
                when (event) {
                    SettingsEvent.DataDeleted -> deleted
                    SettingsEvent.ImportSucceeded -> importSuccess
                    SettingsEvent.ImportFailed -> importFailed
                    SettingsEvent.OperationFailed -> genericError
                },
            )
        }
    }

    LazyColumn(contentPadding = contentPadding) {
        item { SettingsSection(stringResource(R.string.mode_setting)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.mode_setting_description), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.selectedMode == UserMode.TEACHER,
                        onClick = { viewModel.setMode(UserMode.TEACHER) },
                        label = { Text(stringResource(R.string.mode_teacher_short)) },
                    )
                    FilterChip(
                        selected = preferences.selectedMode == UserMode.ADMINISTRATION,
                        onClick = { viewModel.setMode(UserMode.ADMINISTRATION) },
                        label = { Text(stringResource(R.string.mode_administration_short)) },
                    )
                }
            }
        }
        item { SettingsSection(stringResource(R.string.reminder_settings)) }
        item {
            SettingsSwitchRow(
                title = stringResource(R.string.all_reminders),
                checked = preferences.remindersEnabled,
                onCheckedChange = viewModel::setRemindersEnabled,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.default_reminder), fontWeight = FontWeight.Medium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(5, 10, 15, 30, 45, 60)) { minutes ->
                        FilterChip(
                            selected = preferences.defaultReminderMinutes == minutes,
                            onClick = { viewModel.setDefaultReminder(minutes) },
                            label = { Text(stringResource(R.string.minutes_value, minutes)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = preferences.defaultReminderMinutes !in listOf(5, 10, 15, 30, 45, 60),
                            onClick = { showCustomReminder = true },
                            label = { Text(stringResource(R.string.custom_minutes)) },
                        )
                    }
                }
            }
        }
        item { NotificationPermissionCard(Modifier.padding(16.dp), showGrantedStatus = true) }
        item { SettingsSection(stringResource(R.string.appearance)) }
        item {
            ChoiceRow(
                title = stringResource(R.string.theme),
                choices = ThemePreference.entries,
                selected = preferences.themePreference,
                label = {
                    stringResource(
                        when (it) {
                            ThemePreference.SYSTEM -> R.string.theme_system
                            ThemePreference.LIGHT -> R.string.theme_light
                            ThemePreference.DARK -> R.string.theme_dark
                        },
                    )
                },
                onSelect = viewModel::setTheme,
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.week_starts_on),
                choices = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY),
                selected = preferences.weekStartDay,
                label = { if (it == DayOfWeek.MONDAY) stringResource(R.string.monday) else stringResource(R.string.sunday) },
                onSelect = viewModel::setWeekStartDay,
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.time_format),
                choices = TimeFormat.entries,
                selected = preferences.timeFormat,
                label = {
                    stringResource(
                        when (it) {
                            TimeFormat.SYSTEM -> R.string.time_system
                            TimeFormat.TWELVE_HOUR -> R.string.time_12_hour
                            TimeFormat.TWENTY_FOUR_HOUR -> R.string.time_24_hour
                        },
                    )
                },
                onSelect = viewModel::setTimeFormat,
            )
        }
        item { SettingsSection(stringResource(R.string.data_management)) }
        item {
            ActionRow(
                title = stringResource(R.string.export_data),
                description = stringResource(R.string.export_description),
                icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = {
                    scope.launch {
                        runCatching { viewModel.exportJson() }
                            .onSuccess {
                                exportContent = it
                                exportLauncher.launch("classsync-${LocalDate.now()}.json")
                            }
                            .onFailure { showMessage(exportFailed) }
                    }
                },
            )
        }
        item {
            ActionRow(
                title = stringResource(R.string.import_data),
                description = stringResource(R.string.import_description),
                icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
                onClick = { showImportConfirm = true },
            )
        }
        item {
            ActionRow(
                title = stringResource(R.string.delete_all_data),
                description = null,
                icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { showDeleteAll = true },
            )
        }
        item { SettingsSection(stringResource(R.string.about)) }
        item {
            ActionRow(
                title = stringResource(R.string.about),
                description = null,
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = onAbout,
            )
        }
        item {
            ActionRow(
                title = stringResource(R.string.privacy),
                description = null,
                icon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) },
                onClick = onPrivacy,
            )
        }
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text(stringResource(R.string.delete_all_title)) },
            text = { Text(stringResource(R.string.delete_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAll = false
                    viewModel.deleteAllData()
                }) { Text(stringResource(R.string.delete_all_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text(stringResource(R.string.choose_backup)) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showCustomReminder) {
        CustomReminderDialog(
            initial = preferences.defaultReminderMinutes,
            onDismiss = { showCustomReminder = false },
            onSave = {
                viewModel.setDefaultReminder(it)
                showCustomReminder = false
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices) { choice ->
                FilterChip(
                    selected = choice == selected,
                    onClick = { onSelect(choice) },
                    label = { Text(label(choice)) },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    description: String?,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (description != null) ({ Text(description) }) else null,
        leadingContent = icon,
        trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun CustomReminderDialog(initial: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf(initial.toString()) }
    val parsed = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_minutes)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.reminder_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = parsed == null || parsed <= 0,
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.takeIf { it > 0 }?.let(onSave) }, enabled = parsed != null && parsed > 0) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun Context.writeText(uri: android.net.Uri, value: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(value) }
        ?: error("Unable to open output file")
}

private fun Context.readText(uri: android.net.Uri): String {
    val reader = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
        ?: error("Unable to open input file")
    return reader.use {
        val output = StringBuilder()
        val buffer = CharArray(8_192)
        while (true) {
            val read = it.read(buffer)
            if (read < 0) break
            if (output.length + read > MaxBackupCharacters) error("Backup file is too large")
            output.append(buffer, 0, read)
        }
        output.toString()
    }
}

private const val MaxBackupCharacters = 10_000_000
