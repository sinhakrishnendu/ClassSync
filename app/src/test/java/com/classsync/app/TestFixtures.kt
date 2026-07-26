package com.classsync.app

import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ClassSchedule
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleException
import com.classsync.app.domain.model.Subject
import com.classsync.app.domain.model.UserMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun testEntry(
    id: Long = 1,
    day: DayOfWeek = DayOfWeek.MONDAY,
    start: LocalTime = LocalTime.of(9, 0),
    end: LocalTime = LocalTime.of(10, 0),
    recurrence: RecurrenceType = RecurrenceType.WEEKLY,
    oneTimeDate: LocalDate? = null,
    reminderMinutes: Int = 30,
    exceptions: List<ScheduleException> = emptyList(),
): ClassEntry {
    val instant = Instant.parse("2026-01-01T00:00:00Z")
    val group = AcademicGroup(1, "MSc Zoology", "Semester I", "Section A", null, instant, instant)
    val subject = Subject(1, group.id, "Animal Physiology", null, instant, instant)
    return ClassEntry(
        schedule = ClassSchedule(
            id = id,
            mode = UserMode.TEACHER,
            academicGroupId = group.id,
            subjectId = subject.id,
            dayOfWeek = day,
            startTime = start,
            endTime = end,
            classroom = "Room 204",
            topic = null,
            teacherName = null,
            notes = null,
            recurrenceType = recurrence,
            oneTimeDate = oneTimeDate,
            reminderEnabled = true,
            reminderMinutes = reminderMinutes,
            createdAt = instant,
            updatedAt = instant,
        ),
        group = group,
        subject = subject,
        exceptions = exceptions,
    )
}

