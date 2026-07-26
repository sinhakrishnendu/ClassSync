package com.classsync.app.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.ScheduleOccurrence
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.time.ScheduleCalculator
import com.classsync.app.domain.time.TimeProvider
import com.classsync.app.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleDetailsUiState(
    val isLoading: Boolean = true,
    val entry: ClassEntry? = null,
    val nextOccurrence: ScheduleOccurrence? = null,
    val preferences: com.classsync.app.domain.model.UserPreferences = com.classsync.app.domain.model.UserPreferences(),
)

sealed interface ScheduleDetailsEvent {
    data object Deleted : ScheduleDetailsEvent
    data class EditDuplicate(val id: Long) : ScheduleDetailsEvent
    data object Updated : ScheduleDetailsEvent
    data object NoOccurrence : ScheduleDetailsEvent
    data object Failed : ScheduleDetailsEvent
}

@HiltViewModel
class ScheduleDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val scheduleId = checkNotNull(savedStateHandle.get<Long>("scheduleId"))
    private val eventChannel = Channel<ScheduleDetailsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ScheduleDetailsUiState> = kotlinx.coroutines.flow.combine(
        scheduleRepository.observeEntry(scheduleId),
        preferencesRepository.preferences,
    ) { entry, preferences ->
            ScheduleDetailsUiState(
                isLoading = false,
                entry = entry,
                nextOccurrence = entry?.let { ScheduleCalculator.nextOccurrence(listOf(it), timeProvider.now()) },
                preferences = preferences,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleDetailsUiState())

    fun delete() = viewModelScope.launch {
        runCatching {
            scheduleRepository.deleteSchedule(scheduleId)
            reminderScheduler.cancel(scheduleId)
        }.onSuccess { eventChannel.send(ScheduleDetailsEvent.Deleted) }
            .onFailure { eventChannel.send(ScheduleDetailsEvent.Failed) }
    }

    fun duplicate() = viewModelScope.launch {
        runCatching {
            scheduleRepository.duplicateSchedule(scheduleId).also { reminderScheduler.schedule(it) }
        }.onSuccess { eventChannel.send(ScheduleDetailsEvent.EditDuplicate(it)) }
            .onFailure { eventChannel.send(ScheduleDetailsEvent.Failed) }
    }

    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            scheduleRepository.setReminderEnabled(scheduleId, enabled)
            reminderScheduler.schedule(scheduleId)
        }.onSuccess { eventChannel.send(ScheduleDetailsEvent.Updated) }
            .onFailure { eventChannel.send(ScheduleDetailsEvent.Failed) }
    }

    fun updateNextOccurrence(
        status: ExceptionStatus,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ) = viewModelScope.launch {
        val occurrence = uiState.value.nextOccurrence
        if (occurrence == null) {
            eventChannel.send(ScheduleDetailsEvent.NoOccurrence)
            return@launch
        }
        runCatching {
            scheduleRepository.setException(
                scheduleId = scheduleId,
                date = occurrence.date,
                status = status,
                changedStartTime = startTime,
                changedEndTime = endTime,
            )
            reminderScheduler.schedule(scheduleId)
        }.onSuccess { eventChannel.send(ScheduleDetailsEvent.Updated) }
            .onFailure { eventChannel.send(ScheduleDetailsEvent.Failed) }
    }
}
