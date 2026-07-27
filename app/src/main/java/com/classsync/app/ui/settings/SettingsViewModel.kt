package com.classsync.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.data.backup.BackupManager
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.repository.MasterRoutineRepository
import com.classsync.app.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object DataDeleted : SettingsEvent
    data object ImportSucceeded : SettingsEvent
    data object ImportFailed : SettingsEvent
    data object OperationFailed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val masterRoutineRepository: MasterRoutineRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())
    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun setMode(mode: UserMode) = viewModelScope.launch {
        runCatching {
            preferencesRepository.setMode(mode)
            reminderScheduler.rescheduleAll()
        }.onFailure { eventChannel.send(SettingsEvent.OperationFailed) }
    }
    fun setDefaultReminder(minutes: Int) = launchPreference { preferencesRepository.setDefaultReminder(minutes) }
    fun setTheme(theme: ThemePreference) = launchPreference { preferencesRepository.setTheme(theme) }
    fun setWeekStartDay(day: DayOfWeek) = launchPreference { preferencesRepository.setWeekStartDay(day) }
    fun setTimeFormat(format: TimeFormat) = launchPreference { preferencesRepository.setTimeFormat(format) }

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            preferencesRepository.setRemindersEnabled(enabled)
            if (enabled) reminderScheduler.rescheduleAll() else reminderScheduler.cancelAll()
        }.onFailure { eventChannel.send(SettingsEvent.OperationFailed) }
    }

    suspend fun exportJson(): String = backupManager.exportJson()

    fun importJson(source: String) = viewModelScope.launch {
        runCatching {
            backupManager.importJson(source)
            reminderScheduler.cancelAll()
            reminderScheduler.rescheduleAll()
        }.onSuccess { eventChannel.send(SettingsEvent.ImportSucceeded) }
            .onFailure { eventChannel.send(SettingsEvent.ImportFailed) }
    }

    fun deleteAllData() = viewModelScope.launch {
        runCatching {
            scheduleRepository.deleteAll()
            masterRoutineRepository.deleteAll()
            reminderScheduler.cancelAll()
        }.onSuccess { eventChannel.send(SettingsEvent.DataDeleted) }
            .onFailure { eventChannel.send(SettingsEvent.OperationFailed) }
    }

    private fun launchPreference(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { eventChannel.send(SettingsEvent.OperationFailed) }
    }
}
