package com.classsync.app.data.local

import com.classsync.app.domain.master.MasterAcademicClass
import com.classsync.app.domain.master.MasterGenerationRun
import com.classsync.app.domain.master.MasterPeriod
import com.classsync.app.domain.master.MasterRoutine
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterSubject
import com.classsync.app.domain.master.MasterTeacher
import com.classsync.app.domain.master.MasterTeacherAssignment
import com.classsync.app.domain.master.MasterTeacherAvailability
import com.classsync.app.domain.master.MasterTimetableEntry
import com.classsync.app.domain.master.MasterWorkingDay

fun MasterRoutineRecord.toDomain() = MasterRoutineData(
    routine = routine.toDomain(),
    workingDays = workingDays.map(MasterWorkingDayEntity::toDomain).sortedBy(MasterWorkingDay::displayOrder),
    periods = periods.map(MasterPeriodEntity::toDomain).sortedBy(MasterPeriod::periodNumber),
    classes = classes.map(MasterAcademicClassEntity::toDomain).sortedBy(MasterAcademicClass::displayName),
    teachers = teachers.map(MasterTeacherEntity::toDomain).sortedBy(MasterTeacher::fullName),
    subjects = subjects.map(MasterSubjectEntity::toDomain).sortedBy(MasterSubject::name),
    assignments = assignments.map(MasterTeacherAssignmentEntity::toDomain),
    teacherAvailability = teacherAvailability.map(MasterTeacherAvailabilityEntity::toDomain),
    entries = entries.map(MasterTimetableEntryEntity::toDomain),
    generationRuns = generationRuns.map(MasterGenerationRunEntity::toDomain).sortedByDescending(MasterGenerationRun::startedAt),
)

fun MasterRoutine.toEntity() = MasterRoutineEntity(
    id, title, institutionName, departmentName, academicYear, academicSession, effectiveFrom, effectiveTo,
    versionLabel, status, currentStep, preparedBy, approvedBy, notes, createdAt, updatedAt,
)
fun MasterRoutineEntity.toDomain() = MasterRoutine(
    id, title, institutionName, departmentName, academicYear, academicSession, effectiveFrom, effectiveTo,
    versionLabel, status, currentStep, preparedBy, approvedBy, notes, createdAt, updatedAt,
)

fun MasterWorkingDay.toEntity() = MasterWorkingDayEntity(id, masterRoutineId, dayOfWeek, isEnabled, displayOrder)
fun MasterWorkingDayEntity.toDomain() = MasterWorkingDay(id, masterRoutineId, dayOfWeek, isEnabled, displayOrder)

fun MasterPeriod.toEntity() = MasterPeriodEntity(id, masterRoutineId, periodNumber, label, startTime, endTime, type, isSchedulable)
fun MasterPeriodEntity.toDomain() = MasterPeriod(id, masterRoutineId, periodNumber, label, startTime, endTime, type, isSchedulable)

fun MasterAcademicClass.toEntity() = MasterAcademicClassEntity(
    id, masterRoutineId, displayName, programme, semester, section, batch, enrolledCount, notes,
)
fun MasterAcademicClassEntity.toDomain() = MasterAcademicClass(
    id, masterRoutineId, displayName, programme, semester, section, batch, enrolledCount, notes,
)

fun MasterTeacher.toEntity() = MasterTeacherEntity(
    id, masterRoutineId, fullName, shortName, designation, department, maxWeeklyPeriods, minWeeklyPeriods,
    maxDailyPeriods, maxConsecutivePeriods, canTeachPracticals, notes,
)
fun MasterTeacherEntity.toDomain() = MasterTeacher(
    id, masterRoutineId, fullName, shortName, designation, department, maxWeeklyPeriods, minWeeklyPeriods,
    maxDailyPeriods, maxConsecutivePeriods, canTeachPracticals, notes,
)

fun MasterSubject.toEntity() = MasterSubjectEntity(
    id, masterRoutineId, academicClassId, name, code, weeklyTheoryPeriods, weeklyPracticalPeriods,
    weeklyTutorialPeriods, consecutivePeriodRequirement, maxOccurrencesPerDay, distributeAcrossDays, notes,
)
fun MasterSubjectEntity.toDomain() = MasterSubject(
    id, masterRoutineId, academicClassId, name, code, weeklyTheoryPeriods, weeklyPracticalPeriods,
    weeklyTutorialPeriods, consecutivePeriodRequirement, maxOccurrencesPerDay, distributeAcrossDays, notes,
)

fun MasterTeacherAssignment.toEntity() = MasterTeacherAssignmentEntity(
    id, masterRoutineId, teacherId, subjectId, academicClassId, requiredWeeklyPeriods, isMandatory, isAlternateTeacher,
)
fun MasterTeacherAssignmentEntity.toDomain() = MasterTeacherAssignment(
    id, masterRoutineId, teacherId, subjectId, academicClassId, requiredWeeklyPeriods, isMandatory, isAlternateTeacher,
)

fun MasterTeacherAvailability.toEntity() = MasterTeacherAvailabilityEntity(
    id, masterRoutineId, teacherId, dayOfWeek, periodNumber, status, preferenceWeight,
)
fun MasterTeacherAvailabilityEntity.toDomain() = MasterTeacherAvailability(
    id, masterRoutineId, teacherId, dayOfWeek, periodNumber, status, preferenceWeight,
)

fun MasterTimetableEntry.toEntity() = MasterTimetableEntryEntity(
    id, masterRoutineId, academicClassId, subjectId, teacherId, dayOfWeek, startPeriod, endPeriod, type,
    isLocked, isManuallyEdited, generationBatchId, notes,
)
fun MasterTimetableEntryEntity.toDomain() = MasterTimetableEntry(
    id, masterRoutineId, academicClassId, subjectId, teacherId, dayOfWeek, startPeriod, endPeriod, type,
    isLocked, isManuallyEdited, generationBatchId, notes,
)

fun MasterGenerationRun.toEntity() = MasterGenerationRunEntity(
    id, masterRoutineId, startedAt, endedAt, status, qualityScore, totalEntriesRequested, totalEntriesAllocated, issueSummary,
)
fun MasterGenerationRunEntity.toDomain() = MasterGenerationRun(
    id, masterRoutineId, startedAt, endedAt, status, qualityScore, totalEntriesRequested, totalEntriesAllocated, issueSummary,
)
