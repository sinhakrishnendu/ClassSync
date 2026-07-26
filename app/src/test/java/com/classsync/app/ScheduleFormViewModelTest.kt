package com.classsync.app

import androidx.lifecycle.SavedStateHandle
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.TimeProvider
import com.classsync.app.notification.ReminderScheduler
import com.classsync.app.ui.schedule.ScheduleFormViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleFormViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validFormSavesAndSchedulesReminder() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeScheduleRepository()
        val scheduler = FakeReminderScheduler()
        val viewModel = createViewModel(repository, scheduler)
        advanceUntilIdle()

        viewModel.setProgramme("MSc Zoology")
        viewModel.setSemester("Semester I")
        viewModel.setSubjectName("Animal Physiology")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Animal Physiology", repository.savedDraft?.subjectName)
        assertEquals(listOf(99L), scheduler.scheduled)
    }

    @Test
    fun overlapRequiresExplicitConfirmation() = runTest(mainDispatcherRule.dispatcher) {
        val overlapping = testEntry().let { it.copy(subject = it.subject.copy(name = "Botany")) }
        val repository = FakeScheduleRepository(listOf(overlapping))
        val scheduler = FakeReminderScheduler()
        val viewModel = createViewModel(repository, scheduler)
        advanceUntilIdle()

        viewModel.setProgramme("MSc Zoology")
        viewModel.setSemester("Semester I")
        viewModel.setSubjectName("Animal Physiology")
        viewModel.setStartTime(LocalTime.of(9, 30))
        viewModel.setEndTime(LocalTime.of(10, 30))
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showOverlapConfirmation)
        assertEquals(null, repository.savedDraft)

        viewModel.save(confirmOverlap = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showOverlapConfirmation)
        assertEquals(99L, scheduler.scheduled.single())
    }

    private fun createViewModel(
        repository: FakeScheduleRepository,
        scheduler: FakeReminderScheduler,
    ) = ScheduleFormViewModel(
        savedStateHandle = SavedStateHandle(mapOf("scheduleId" to 0L)),
        scheduleRepository = repository,
        preferencesRepository = FakePreferencesRepository(),
        reminderScheduler = scheduler,
        timeProvider = FixedTimeProvider,
    )
}

internal object FixedTimeProvider : TimeProvider {
    private val current = ZonedDateTime.of(2026, 1, 5, 8, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
    override fun now(): ZonedDateTime = current
    override fun zoneId(): ZoneId = current.zone
}

internal class FakePreferencesRepository : PreferencesRepository {
    private val state = MutableStateFlow(UserPreferences(onboardingComplete = true))
    override val preferences: Flow<UserPreferences> = state
    override suspend fun completeOnboarding(mode: UserMode) { state.value = state.value.copy(selectedMode = mode, onboardingComplete = true) }
    override suspend fun setMode(mode: UserMode) { state.value = state.value.copy(selectedMode = mode) }
    override suspend fun setDefaultReminder(minutes: Int) { state.value = state.value.copy(defaultReminderMinutes = minutes) }
    override suspend fun setRemindersEnabled(enabled: Boolean) { state.value = state.value.copy(remindersEnabled = enabled) }
    override suspend fun setTheme(theme: ThemePreference) { state.value = state.value.copy(themePreference = theme) }
    override suspend fun setWeekStartDay(day: DayOfWeek) { state.value = state.value.copy(weekStartDay = day) }
    override suspend fun setTimeFormat(format: TimeFormat) { state.value = state.value.copy(timeFormat = format) }
    override suspend fun replace(preferences: UserPreferences) { state.value = preferences }
}

internal class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Long>()
    val cancelled = mutableListOf<Long>()
    override suspend fun schedule(scheduleId: Long) { scheduled += scheduleId }
    override suspend fun scheduleNext(scheduleId: Long) { scheduled += scheduleId }
    override suspend fun cancel(scheduleId: Long) { cancelled += scheduleId }
    override suspend fun rescheduleAll() = Unit
    override suspend fun cancelAll() = Unit
}

internal class FakeScheduleRepository(initial: List<ClassEntry> = emptyList()) : ScheduleRepository {
    private val entries = MutableStateFlow(initial)
    private val groups = MutableStateFlow(initial.map { it.group }.distinctBy { it.id })
    var savedDraft: ScheduleDraft? = null

    override fun observeEntries(mode: UserMode): Flow<List<ClassEntry>> = entries.map { list -> list.filter { it.schedule.mode == mode } }
    override fun observeAllEntries(): Flow<List<ClassEntry>> = entries
    override fun observeEntry(id: Long): Flow<ClassEntry?> = entries.map { list -> list.firstOrNull { it.schedule.id == id } }
    override fun observeGroups(): Flow<List<AcademicGroup>> = groups
    override suspend fun getEntry(id: Long): ClassEntry? = entries.value.firstOrNull { it.schedule.id == id }
    override suspend fun getAllEntries(): List<ClassEntry> = entries.value
    override suspend fun saveSchedule(draft: ScheduleDraft): Long {
        savedDraft = draft
        return 99L
    }
    override suspend fun duplicateSchedule(id: Long): Long = 99L
    override suspend fun deleteSchedule(id: Long) { entries.value = entries.value.filterNot { it.schedule.id == id } }
    override suspend fun setReminderEnabled(id: Long, enabled: Boolean) {
        entries.value = entries.value.map { entry ->
            if (entry.schedule.id == id) entry.copy(schedule = entry.schedule.copy(reminderEnabled = enabled)) else entry
        }
    }
    override suspend fun saveGroup(group: AcademicGroup): Long = group.id.takeIf { it > 0 } ?: 1L
    override suspend fun deleteGroup(id: Long) = Unit
    override suspend fun setException(
        scheduleId: Long,
        date: LocalDate,
        status: ExceptionStatus,
        changedStartTime: LocalTime?,
        changedEndTime: LocalTime?,
        notes: String?,
    ) = Unit
    override suspend fun deleteAll() { entries.value = emptyList() }
}
