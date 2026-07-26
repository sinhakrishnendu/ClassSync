package com.classsync.app.domain.time

import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleOccurrence
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object ScheduleCalculator {
    fun occurrenceOn(
        entry: ClassEntry,
        date: LocalDate,
        zoneId: ZoneId,
    ): ScheduleOccurrence? {
        val schedule = entry.schedule
        val occurs = when (schedule.recurrenceType) {
            RecurrenceType.WEEKLY -> schedule.dayOfWeek == date.dayOfWeek
            RecurrenceType.ONE_TIME -> schedule.oneTimeDate == date
        }
        if (!occurs) return null

        val exception = entry.exceptions.firstOrNull { it.relevantDate == date }
        val startTime = if (exception?.status == ExceptionStatus.RESCHEDULED) {
            exception.changedStartTime ?: schedule.startTime
        } else {
            schedule.startTime
        }
        val endTime = if (exception?.status == ExceptionStatus.RESCHEDULED) {
            exception.changedEndTime ?: schedule.endTime
        } else {
            schedule.endTime
        }
        return ScheduleOccurrence(
            entry = entry,
            date = date,
            start = date.atTime(startTime).atZone(zoneId),
            end = date.atTime(endTime).atZone(zoneId),
            status = exception?.status,
        )
    }

    fun occurrencesForDate(
        entries: List<ClassEntry>,
        date: LocalDate,
        zoneId: ZoneId,
        includeInactive: Boolean = false,
    ): List<ScheduleOccurrence> = entries.mapNotNull { occurrenceOn(it, date, zoneId) }
        .filter { includeInactive || it.isActive }
        .sortedBy { it.start.toInstant() }

    fun nextOccurrence(
        entries: List<ClassEntry>,
        now: ZonedDateTime,
        searchDays: Long = 366,
    ): ScheduleOccurrence? = entries.asSequence()
        .flatMap { entry -> candidateDates(entry, now.toLocalDate(), searchDays).mapNotNull { occurrenceOn(entry, it, now.zone) } }
        .filter { it.isActive }
        .filter { it.start.isAfter(now) }
        .minByOrNull { it.start.toInstant() }

    fun nextReminderAt(
        entry: ClassEntry,
        now: ZonedDateTime,
        searchDays: Long = 366,
    ): ZonedDateTime? {
        if (!entry.schedule.reminderEnabled) return null
        return candidateDates(entry, now.toLocalDate(), searchDays)
            .mapNotNull { occurrenceOn(entry, it, now.zone) }
            .filter { it.isActive }
            .map { it.start.minusMinutes(entry.schedule.reminderMinutes.toLong()) }
            .firstOrNull { it.isAfter(now) }
    }

    fun minutesUntil(occurrence: ScheduleOccurrence, now: ZonedDateTime): Long =
        Duration.between(now, occurrence.start).toMinutes().coerceAtLeast(0)

    private fun candidateDates(
        entry: ClassEntry,
        fromDate: LocalDate,
        searchDays: Long,
    ): Sequence<LocalDate> = when (entry.schedule.recurrenceType) {
        RecurrenceType.ONE_TIME -> entry.schedule.oneTimeDate
            ?.let(::sequenceOf)
            ?.filter { !it.isBefore(fromDate) }
            ?: emptySequence()
        RecurrenceType.WEEKLY -> (0..searchDays).asSequence().map(fromDate::plusDays)
    }
}
