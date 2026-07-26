package com.classsync.app.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.TimeProvider
import com.classsync.app.domain.validation.ScheduleField
import com.classsync.app.domain.validation.ScheduleValidator
import com.classsync.app.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleFormUiState(
    val id: Long = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val mode: UserMode = UserMode.TEACHER,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val programme: String = "",
    val semester: String = "",
    val batchSection: String = "",
    val institution: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val classroom: String = "",
    val topic: String = "",
    val teacherName: String = "",
    val notes: String = "",
    val recurrenceType: RecurrenceType = RecurrenceType.WEEKLY,
    val oneTimeDate: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    val reminderMinutes: String = "30",
    val errors: Set<ScheduleField> = emptySet(),
    val duplicate: Boolean = false,
    val showOverlapConfirmation: Boolean = false,
)

sealed interface ScheduleFormEvent {
    data class Saved(val id: Long, val reminderScheduled: Boolean) : ScheduleFormEvent
    data object SaveFailed : ScheduleFormEvent
}

@HiltViewModel
class ScheduleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val scheduleId = savedStateHandle.get<Long>("scheduleId") ?: 0L
    private val mutableState = MutableStateFlow(defaultState())
    val uiState: StateFlow<ScheduleFormUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<ScheduleFormEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var existingEntries: List<ClassEntry> = emptyList()

    init {
        viewModelScope.launch {
            existingEntries = scheduleRepository.getAllEntries()
            val preferences = preferencesRepository.preferences.first()
            if (scheduleId > 0) {
                scheduleRepository.getEntry(scheduleId)?.let { loadEntry(it, preferences.timeFormat) }
                    ?: mutableState.update { it.copy(isLoading = false) }
            } else {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        mode = preferences.selectedMode,
                        timeFormat = preferences.timeFormat,
                        reminderMinutes = preferences.defaultReminderMinutes.toString(),
                    )
                }
            }
        }
    }

    fun setProgramme(value: String) = change { it.copy(programme = value) }
    fun setSemester(value: String) = change { it.copy(semester = value) }
    fun setBatchSection(value: String) = change { it.copy(batchSection = value) }
    fun setInstitution(value: String) = change { it.copy(institution = value) }
    fun setSubjectName(value: String) = change { it.copy(subjectName = value) }
    fun setSubjectCode(value: String) = change { it.copy(subjectCode = value) }
    fun setDay(value: DayOfWeek) = change { it.copy(dayOfWeek = value) }
    fun setStartTime(value: LocalTime) = change { it.copy(startTime = value) }
    fun setEndTime(value: LocalTime) = change { it.copy(endTime = value) }
    fun setClassroom(value: String) = change { it.copy(classroom = value) }
    fun setTopic(value: String) = change { it.copy(topic = value) }
    fun setTeacherName(value: String) = change { it.copy(teacherName = value) }
    fun setNotes(value: String) = change { it.copy(notes = value) }
    fun setRecurrence(value: RecurrenceType) = change {
        it.copy(
            recurrenceType = value,
            oneTimeDate = if (value == RecurrenceType.ONE_TIME) it.oneTimeDate ?: timeProvider.now().toLocalDate() else null,
        )
    }
    fun setOneTimeDate(value: LocalDate) = change { it.copy(oneTimeDate = value, dayOfWeek = value.dayOfWeek) }
    fun setReminderEnabled(value: Boolean) = change { it.copy(reminderEnabled = value) }
    fun setReminderMinutes(value: String) = change { it.copy(reminderMinutes = value.filter(Char::isDigit).take(4)) }
    fun dismissOverlap() = mutableState.update { it.copy(showOverlapConfirmation = false) }

    fun save(confirmOverlap: Boolean = false) {
        val draft = mutableState.value.toDraft()
        val validation = ScheduleValidator.validate(draft, existingEntries)
        if (!validation.isValid || validation.isDuplicate) {
            mutableState.update { it.copy(errors = validation.errors, duplicate = validation.isDuplicate) }
            return
        }
        if (validation.hasOverlap && !confirmOverlap) {
            mutableState.update { it.copy(showOverlapConfirmation = true) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, showOverlapConfirmation = false) }
            runCatching {
                scheduleRepository.saveSchedule(draft)
            }.onSuccess { id ->
                val reminderScheduled = runCatching { reminderScheduler.schedule(id) }.isSuccess
                eventChannel.send(ScheduleFormEvent.Saved(id, reminderScheduled))
            }
                .onFailure {
                    mutableState.update { it.copy(isSaving = false) }
                    eventChannel.send(ScheduleFormEvent.SaveFailed)
                }
        }
    }

    private fun defaultState(): ScheduleFormUiState {
        val now = timeProvider.now()
        val candidate = now.withMinute(0).withSecond(0).withNano(0).plusHours(1)
        val nextSlot = when {
            candidate.hour in 7..21 -> candidate
            candidate.hour > 21 -> candidate.toLocalDate().plusDays(1).atTime(9, 0).atZone(now.zone)
            else -> candidate.toLocalDate().atTime(9, 0).atZone(now.zone)
        }
        val nextHour = nextSlot.toLocalTime()
        return ScheduleFormUiState(
            id = scheduleId,
            isLoading = true,
            dayOfWeek = nextSlot.dayOfWeek,
            startTime = nextHour,
            endTime = nextHour.plusHours(1),
        )
    }

    private fun loadEntry(entry: ClassEntry, timeFormat: TimeFormat) {
        mutableState.value = ScheduleFormUiState(
            id = entry.schedule.id,
            isLoading = false,
            mode = entry.schedule.mode,
            timeFormat = timeFormat,
            programme = entry.group.programme,
            semester = entry.group.semester,
            batchSection = entry.group.batchSection.orEmpty(),
            institution = entry.group.institution.orEmpty(),
            subjectName = entry.subject.name,
            subjectCode = entry.subject.code.orEmpty(),
            dayOfWeek = entry.schedule.dayOfWeek,
            startTime = entry.schedule.startTime,
            endTime = entry.schedule.endTime,
            classroom = entry.schedule.classroom.orEmpty(),
            topic = entry.schedule.topic.orEmpty(),
            teacherName = entry.schedule.teacherName.orEmpty(),
            notes = entry.schedule.notes.orEmpty(),
            recurrenceType = entry.schedule.recurrenceType,
            oneTimeDate = entry.schedule.oneTimeDate,
            reminderEnabled = entry.schedule.reminderEnabled,
            reminderMinutes = entry.schedule.reminderMinutes.toString(),
        )
    }

    private fun change(block: (ScheduleFormUiState) -> ScheduleFormUiState) {
        mutableState.update { block(it).copy(errors = emptySet(), duplicate = false) }
    }

    private fun ScheduleFormUiState.toDraft() = ScheduleDraft(
        id = id,
        mode = mode,
        programme = programme,
        semester = semester,
        batchSection = batchSection,
        institution = institution,
        subjectName = subjectName,
        subjectCode = subjectCode,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        classroom = classroom,
        topic = topic,
        teacherName = teacherName,
        notes = notes,
        recurrenceType = recurrenceType,
        oneTimeDate = oneTimeDate,
        reminderEnabled = reminderEnabled,
        reminderMinutes = reminderMinutes.toIntOrNull() ?: 0,
    )
}
