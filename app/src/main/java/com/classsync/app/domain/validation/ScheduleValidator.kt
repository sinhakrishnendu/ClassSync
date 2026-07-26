package com.classsync.app.domain.validation

import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleDraft

enum class ScheduleField {
    PROGRAMME,
    SEMESTER,
    SUBJECT,
    START_TIME,
    END_TIME,
    ONE_TIME_DATE,
    REMINDER_MINUTES,
}

data class ScheduleValidationResult(
    val errors: Set<ScheduleField>,
    val hasOverlap: Boolean,
    val isDuplicate: Boolean,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

object ScheduleValidator {
    fun validate(draft: ScheduleDraft, existing: List<ClassEntry>): ScheduleValidationResult {
        val errors = buildSet {
            if (draft.programme.isBlank()) add(ScheduleField.PROGRAMME)
            if (draft.semester.isBlank()) add(ScheduleField.SEMESTER)
            if (draft.subjectName.isBlank()) add(ScheduleField.SUBJECT)
            if (!draft.endTime.isAfter(draft.startTime)) {
                add(ScheduleField.START_TIME)
                add(ScheduleField.END_TIME)
            }
            if (draft.recurrenceType == RecurrenceType.ONE_TIME && draft.oneTimeDate == null) {
                add(ScheduleField.ONE_TIME_DATE)
            }
            if (draft.reminderEnabled && draft.reminderMinutes <= 0) {
                add(ScheduleField.REMINDER_MINUTES)
            }
        }
        val comparable = existing.filter { entry ->
            entry.schedule.id != draft.id &&
                entry.schedule.mode == draft.mode &&
                sameOccurrencePattern(draft, entry)
        }
        val overlaps = comparable.filter { entry ->
            draft.startTime < entry.schedule.endTime && draft.endTime > entry.schedule.startTime
        }
        val duplicate = draft.id == 0L && overlaps.any { entry ->
            entry.subject.name.equals(draft.subjectName.trim(), ignoreCase = true) &&
                entry.group.programme.equals(draft.programme.trim(), ignoreCase = true) &&
                entry.group.semester.equals(draft.semester.trim(), ignoreCase = true) &&
                entry.schedule.startTime == draft.startTime &&
                entry.schedule.endTime == draft.endTime
        }
        return ScheduleValidationResult(errors, overlaps.isNotEmpty(), duplicate)
    }

    private fun sameOccurrencePattern(draft: ScheduleDraft, entry: ClassEntry): Boolean {
        val schedule = entry.schedule
        return when {
            draft.recurrenceType == RecurrenceType.ONE_TIME && schedule.recurrenceType == RecurrenceType.ONE_TIME ->
                draft.oneTimeDate == schedule.oneTimeDate
            draft.recurrenceType == RecurrenceType.WEEKLY && schedule.recurrenceType == RecurrenceType.WEEKLY ->
                draft.dayOfWeek == schedule.dayOfWeek
            draft.recurrenceType == RecurrenceType.ONE_TIME && schedule.recurrenceType == RecurrenceType.WEEKLY ->
                draft.oneTimeDate?.dayOfWeek == schedule.dayOfWeek
            draft.recurrenceType == RecurrenceType.WEEKLY && schedule.recurrenceType == RecurrenceType.ONE_TIME ->
                schedule.oneTimeDate?.dayOfWeek == draft.dayOfWeek
            else -> false
        }
    }
}
