package com.classsync.app

import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.validation.ScheduleField
import com.classsync.app.domain.validation.ScheduleValidator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleValidatorTest {
    @Test
    fun blankRequiredFieldsAndInvalidTimesAreRejected() {
        val draft = draft(
            programme = "",
            semester = "",
            subjectName = "",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(9, 0),
        )

        val result = ScheduleValidator.validate(draft, emptyList())

        assertFalse(result.isValid)
        assertTrue(result.errors.containsAll(setOf(
            ScheduleField.PROGRAMME,
            ScheduleField.SEMESTER,
            ScheduleField.SUBJECT,
            ScheduleField.START_TIME,
            ScheduleField.END_TIME,
        )))
    }

    @Test
    fun oneTimeClassWarnsWhenItOverlapsAWeeklyClass() {
        val draft = draft(
            recurrenceType = RecurrenceType.ONE_TIME,
            oneTimeDate = LocalDate.of(2026, 1, 5),
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(10, 30),
        )

        val result = ScheduleValidator.validate(draft, listOf(testEntry()))

        assertTrue(result.isValid)
        assertTrue(result.hasOverlap)
    }

    @Test
    fun exactDuplicateIsBlockedForNewEntryButAllowedForExistingEdit() {
        val existing = testEntry()
        val newResult = ScheduleValidator.validate(draft(), listOf(existing))
        val editResult = ScheduleValidator.validate(draft(id = 2), listOf(existing))

        assertTrue(newResult.isDuplicate)
        assertFalse(editResult.isDuplicate)
    }

    private fun draft(
        id: Long = 0,
        programme: String = "MSc Zoology",
        semester: String = "Semester I",
        subjectName: String = "Animal Physiology",
        startTime: LocalTime = LocalTime.of(9, 0),
        endTime: LocalTime = LocalTime.of(10, 0),
        recurrenceType: RecurrenceType = RecurrenceType.WEEKLY,
        oneTimeDate: LocalDate? = null,
    ) = ScheduleDraft(
        id = id,
        mode = UserMode.TEACHER,
        programme = programme,
        semester = semester,
        subjectName = subjectName,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = startTime,
        endTime = endTime,
        recurrenceType = recurrenceType,
        oneTimeDate = oneTimeDate,
    )
}

