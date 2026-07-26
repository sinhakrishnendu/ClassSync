package com.classsync.app.data.local

import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ClassSchedule
import com.classsync.app.domain.model.ScheduleException
import com.classsync.app.domain.model.Subject

fun AcademicGroupEntity.toDomain() = AcademicGroup(
    id, programme, semester, batchSection, institution, createdAt, updatedAt,
)

fun SubjectEntity.toDomain() = Subject(
    id, academicGroupId, name, code, createdAt, updatedAt,
)

fun ClassScheduleEntity.toDomain() = ClassSchedule(
    id = id,
    mode = mode,
    academicGroupId = academicGroupId,
    subjectId = subjectId,
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
    reminderMinutes = reminderMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ScheduleExceptionEntity.toDomain() = ScheduleException(
    id, classScheduleId, relevantDate, status, changedStartTime, changedEndTime, notes, createdAt, updatedAt,
)

fun ClassEntryRecord.toDomain() = ClassEntry(
    schedule = schedule.toDomain(),
    group = group.toDomain(),
    subject = subject.toDomain(),
    exceptions = exceptions.map(ScheduleExceptionEntity::toDomain),
)

