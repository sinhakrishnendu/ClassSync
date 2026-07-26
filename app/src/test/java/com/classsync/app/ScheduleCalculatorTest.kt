package com.classsync.app

import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleException
import com.classsync.app.domain.time.ScheduleCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun occurrencesAreSortedByStartTime() {
        val date = LocalDate.of(2026, 1, 5)
        val entries = listOf(
            testEntry(id = 1, start = LocalTime.of(11, 0), end = LocalTime.NOON),
            testEntry(id = 2, start = LocalTime.of(8, 30), end = LocalTime.of(9, 30)),
        )

        val result = ScheduleCalculator.occurrencesForDate(entries, date, zone)

        assertEquals(listOf(2L, 1L), result.map { it.entry.schedule.id })
    }

    @Test
    fun cancelledOccurrenceMovesNextClassToFollowingWeek() {
        val date = LocalDate.of(2026, 1, 5)
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val exception = ScheduleException(
            1,
            1,
            date,
            ExceptionStatus.CANCELLED,
            null,
            null,
            null,
            instant,
            instant,
        )
        val entry = testEntry(exceptions = listOf(exception))
        val now = ZonedDateTime.of(date, LocalTime.of(8, 0), zone)

        val next = ScheduleCalculator.nextOccurrence(listOf(entry), now)

        assertEquals(date.plusWeeks(1), next?.date)
    }

    @Test
    fun reminderAfterCurrentWindowUsesNextWeeklyOccurrence() {
        val date = LocalDate.of(2026, 1, 5)
        val entry = testEntry(reminderMinutes = 30)
        val now = ZonedDateTime.of(date, LocalTime.of(8, 45), zone)

        val reminder = ScheduleCalculator.nextReminderAt(entry, now)

        assertEquals(date.plusWeeks(1), reminder?.toLocalDate())
        assertEquals(LocalTime.of(8, 30), reminder?.toLocalTime())
    }

    @Test
    fun farFutureOneTimeReminderIsStillFound() {
        val now = ZonedDateTime.of(2026, 1, 1, 8, 0, 0, 0, zone)
        val date = now.toLocalDate().plusDays(500)
        val entry = testEntry(
            recurrence = RecurrenceType.ONE_TIME,
            oneTimeDate = date,
            day = date.dayOfWeek,
        )

        val reminder = ScheduleCalculator.nextReminderAt(entry, now)

        assertEquals(date, reminder?.toLocalDate())
    }

    @Test
    fun pastOneTimeClassHasNoNextOccurrence() {
        val now = ZonedDateTime.of(2026, 1, 5, 8, 0, 0, 0, zone)
        val entry = testEntry(
            recurrence = RecurrenceType.ONE_TIME,
            oneTimeDate = LocalDate.of(2025, 12, 1),
            day = DayOfWeek.MONDAY,
        )

        assertNull(ScheduleCalculator.nextOccurrence(listOf(entry), now))
    }
}

