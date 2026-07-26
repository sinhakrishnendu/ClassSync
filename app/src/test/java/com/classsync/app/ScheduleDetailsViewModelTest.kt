package com.classsync.app

import androidx.lifecycle.SavedStateHandle
import com.classsync.app.ui.schedule.ScheduleDetailsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleDetailsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun deletingScheduleAlsoCancelsItsReminder() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeScheduleRepository(listOf(testEntry()))
        val scheduler = FakeReminderScheduler()
        val viewModel = createViewModel(repository, scheduler)

        viewModel.delete()
        advanceUntilIdle()

        assertNull(repository.getEntry(1))
        assertEquals(listOf(1L), scheduler.cancelled)
    }

    @Test
    fun disablingReminderUpdatesDataAndReschedulesUniqueWork() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeScheduleRepository(listOf(testEntry()))
        val scheduler = FakeReminderScheduler()
        val viewModel = createViewModel(repository, scheduler)

        viewModel.setReminderEnabled(false)
        advanceUntilIdle()

        assertEquals(false, repository.getEntry(1)?.schedule?.reminderEnabled)
        assertEquals(listOf(1L), scheduler.scheduled)
    }

    private fun createViewModel(
        repository: FakeScheduleRepository,
        scheduler: FakeReminderScheduler,
    ) = ScheduleDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("scheduleId" to 1L)),
        scheduleRepository = repository,
        preferencesRepository = FakePreferencesRepository(),
        reminderScheduler = scheduler,
        timeProvider = FixedTimeProvider,
    )
}
