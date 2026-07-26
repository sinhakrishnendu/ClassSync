package com.classsync.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class UserMode { TEACHER, STUDENT }

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class TimeFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }

enum class RecurrenceType { WEEKLY, ONE_TIME }

enum class ExceptionStatus { CANCELLED, RESCHEDULED, COMPLETED }

data class UserPreferences(
    val selectedMode: UserMode = UserMode.TEACHER,
    val onboardingComplete: Boolean = false,
    val defaultReminderMinutes: Int = 30,
    val remindersEnabled: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val weekStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
)

data class AcademicGroup(
    val id: Long,
    val programme: String,
    val semester: String,
    val batchSection: String?,
    val institution: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val displayName: String
        get() = listOf(programme, semester, batchSection)
            .filterNot { it.isNullOrBlank() }
            .joinToString(" - ")
}

data class Subject(
    val id: Long,
    val academicGroupId: Long,
    val name: String,
    val code: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ClassSchedule(
    val id: Long,
    val mode: UserMode,
    val academicGroupId: Long,
    val subjectId: Long,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val classroom: String?,
    val topic: String?,
    val teacherName: String?,
    val notes: String?,
    val recurrenceType: RecurrenceType,
    val oneTimeDate: LocalDate?,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ScheduleException(
    val id: Long,
    val classScheduleId: Long,
    val relevantDate: LocalDate,
    val status: ExceptionStatus,
    val changedStartTime: LocalTime?,
    val changedEndTime: LocalTime?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ClassEntry(
    val schedule: ClassSchedule,
    val group: AcademicGroup,
    val subject: Subject,
    val exceptions: List<ScheduleException>,
)

data class ScheduleDraft(
    val id: Long = 0,
    val mode: UserMode,
    val programme: String,
    val semester: String,
    val batchSection: String? = null,
    val institution: String? = null,
    val subjectName: String,
    val subjectCode: String? = null,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val classroom: String? = null,
    val topic: String? = null,
    val teacherName: String? = null,
    val notes: String? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.WEEKLY,
    val oneTimeDate: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    val reminderMinutes: Int = 30,
)

data class ScheduleOccurrence(
    val entry: ClassEntry,
    val date: LocalDate,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val status: ExceptionStatus?,
) {
    val isActive: Boolean
        get() = status == null || status == ExceptionStatus.RESCHEDULED
}

