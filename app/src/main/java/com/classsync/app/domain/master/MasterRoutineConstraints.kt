package com.classsync.app.domain.master

import java.time.DayOfWeek

object ConstraintValidator {
    fun validateInputs(data: MasterRoutineData): List<GenerationIssue> = buildList {
        if (data.routine.title.isBlank()) error("missing_title", "Add a routine title.", data.routine.id)
        if (data.routine.institutionName.isBlank()) error("missing_institution", "Add the institution name.", data.routine.id)

        val days = data.workingDays.filter(MasterWorkingDay::isEnabled)
        val periods = data.periods.filter(MasterPeriod::isSchedulable)
        if (days.isEmpty()) error("missing_days", "Select at least one working day.", data.routine.id)
        if (periods.isEmpty()) error("missing_periods", "Add at least one teaching period.", data.routine.id)
        if (data.classes.isEmpty()) error("missing_classes", "Add at least one class or section.", data.routine.id)
        if (data.teachers.isEmpty()) error("missing_teachers", "Add at least one teacher.", data.routine.id)
        if (data.subjects.isEmpty()) error("missing_subjects", "Add at least one subject.", data.routine.id)

        val classIds = data.classes.mapTo(mutableSetOf(), MasterAcademicClass::id)
        val teacherIds = data.teachers.mapTo(mutableSetOf(), MasterTeacher::id)
        data.subjects.forEach { subject ->
            if (subject.academicClassId !in classIds) {
                error("invalid_subject_class", "${subject.name} is not linked to a saved class.", subject.id)
            }
            if (subject.totalWeeklyPeriods <= 0) {
                error("missing_subject_periods", "${subject.name} needs at least one weekly period.", subject.id)
            }
            val assignments = data.assignments.filter { it.subjectId == subject.id && !it.isAlternateTeacher }
            if (assignments.isEmpty()) {
                error("unassigned_subject", "Allocate ${subject.name}'s syllabus periods to at least one faculty member.", subject.id)
            }
            val duplicateTeachers = assignments.groupingBy(MasterTeacherAssignment::teacherId).eachCount().filterValues { it > 1 }.keys
            if (duplicateTeachers.isNotEmpty()) {
                error("duplicate_faculty_load", "${subject.name} has more than one load entry for the same faculty member.", subject.id)
            }
            assignments.forEach { assignment ->
                if (assignment.teacherId !in teacherIds) {
                    error("invalid_assignment_teacher", "${subject.name} refers to a teacher that no longer exists.", assignment.id)
                }
                if (assignment.academicClassId != subject.academicClassId) {
                    error("assignment_class_mismatch", "${subject.name}'s teacher assignment uses a different class.", assignment.id)
                }
                if (assignment.requiredWeeklyPeriods <= 0) {
                    error("invalid_faculty_load", "Every faculty load for ${subject.name} must be at least one period.", assignment.id)
                }
                val lockedLoad = data.entries.filter {
                    it.isLocked && it.subjectId == subject.id && it.teacherId == assignment.teacherId
                }.sumOf(MasterTimetableEntry::periodCount)
                if (lockedLoad > assignment.requiredWeeklyPeriods) {
                    error(
                        "locked_faculty_overload",
                        "${subject.name} has $lockedLoad locked periods for this faculty member, above the assigned load of ${assignment.requiredWeeklyPeriods}.",
                        assignment.id,
                    )
                }
            }
            val allocatedLoad = assignments.sumOf(MasterTeacherAssignment::requiredWeeklyPeriods)
            if (assignments.isNotEmpty() && allocatedLoad != subject.totalWeeklyPeriods) {
                error(
                    "syllabus_load_mismatch",
                    "${subject.name}'s syllabus requires ${subject.totalWeeklyPeriods} periods, but faculty loads total $allocatedLoad. Make both totals equal.",
                    subject.id,
                )
            }
            if (subject.weeklyPracticalPeriods > 0 && subject.consecutivePeriodRequirement > periods.size) {
                error(
                    "practical_block_too_long",
                    "${subject.name} needs ${subject.consecutivePeriodRequirement} consecutive periods, but only ${periods.size} teaching periods exist per day.",
                    subject.id,
                )
            }
            if (assignments.isNotEmpty() && allocatedLoad == subject.totalWeeklyPeriods &&
                FacultyLoadPlanner.expandSubject(data, subject, data.entries.filter(MasterTimetableEntry::isLocked)) == null
            ) {
                error(
                    "faculty_load_block_mismatch",
                    "${subject.name}'s faculty loads cannot fit its practical block size. Give one practical-capable faculty member enough consecutive load.",
                    subject.id,
                )
            }
        }

        data.teachers.forEach { teacher ->
            val required = data.assignments
                .filter { it.teacherId == teacher.id && !it.isAlternateTeacher }
                .sumOf(MasterTeacherAssignment::requiredWeeklyPeriods)
            if (required > teacher.maxWeeklyPeriods) {
                error(
                    "teacher_weekly_overload",
                    "${teacher.fullName} requires $required periods, but the weekly limit is ${teacher.maxWeeklyPeriods}. Increase the limit or reassign a subject.",
                    teacher.id,
                )
            }
        }

        val weeklyCapacity = days.size * periods.size
        data.classes.forEach { academicClass ->
            val required = data.subjects.filter { it.academicClassId == academicClass.id }.sumOf(MasterSubject::totalWeeklyPeriods)
            if (required > weeklyCapacity) {
                error(
                    "class_capacity",
                    "${academicClass.displayName} needs $required periods, but the week has only $weeklyCapacity teaching slots.",
                    academicClass.id,
                )
            }
        }
        validateLockedEntries(data).forEach(::add)
    }

    fun validateTimetable(
        data: MasterRoutineData,
        entries: List<MasterTimetableEntry> = data.entries,
    ): List<GenerationIssue> = buildList {
        addAll(validateInputs(data))
        val accepted = mutableListOf<MasterTimetableEntry>()
        entries.sortedWith(compareBy({ it.dayOfWeek.value }, { it.startPeriod }, { it.id })).forEach { entry ->
            HardConstraintChecker.checkPlacement(data, accepted, entry)?.let(::add)
            accepted += entry
        }

        data.subjects.forEach { subject ->
            val allocated = entries.filter { it.subjectId == subject.id }.sumOf(MasterTimetableEntry::periodCount)
            if (allocated != subject.totalWeeklyPeriods) {
                error(
                    "subject_load_mismatch",
                    "${subject.name} needs exactly ${subject.totalWeeklyPeriods} syllabus periods, but $allocated are allocated.",
                    subject.id,
                )
            }
        }
        data.assignments.filterNot(MasterTeacherAssignment::isAlternateTeacher).forEach { assignment ->
            val teacher = data.teachers.firstOrNull { it.id == assignment.teacherId } ?: return@forEach
            val subject = data.subjects.firstOrNull { it.id == assignment.subjectId } ?: return@forEach
            val allocated = entries.filter {
                it.subjectId == assignment.subjectId && it.teacherId == assignment.teacherId
            }.sumOf(MasterTimetableEntry::periodCount)
            if (allocated != assignment.requiredWeeklyPeriods) {
                error(
                    "faculty_load_mismatch",
                    "${teacher.fullName} must receive exactly ${assignment.requiredWeeklyPeriods} periods of ${subject.name}, but $allocated are allocated.",
                    assignment.id,
                )
            }
        }
    }.distinctBy { it.code to it.message }

    private fun validateLockedEntries(data: MasterRoutineData): List<GenerationIssue> {
        val accepted = mutableListOf<MasterTimetableEntry>()
        return buildList {
            data.entries.filter(MasterTimetableEntry::isLocked).forEach { entry ->
                HardConstraintChecker.checkPlacement(data, accepted, entry)?.let(::add)
                accepted += entry
            }
        }
    }

    private fun MutableList<GenerationIssue>.error(code: String, message: String, vararg ids: String) {
        add(GenerationIssue(code, GenerationIssueSeverity.ERROR, message, ids.toSet()))
    }
}

object HardConstraintChecker {
    fun checkPlacement(
        data: MasterRoutineData,
        placed: List<MasterTimetableEntry>,
        candidate: MasterTimetableEntry,
    ): GenerationIssue? {
        val academicClass = data.classes.firstOrNull { it.id == candidate.academicClassId }
            ?: return issue("missing_entry_class", "A timetable entry refers to a class that no longer exists.", candidate.id)
        val teacher = data.teachers.firstOrNull { it.id == candidate.teacherId }
            ?: return issue("missing_entry_teacher", "A timetable entry refers to a teacher that no longer exists.", candidate.id)
        val subject = data.subjects.firstOrNull { it.id == candidate.subjectId }
            ?: return issue("missing_entry_subject", "A timetable entry refers to a subject that no longer exists.", candidate.id)

        if (data.workingDays.none { it.dayOfWeek == candidate.dayOfWeek && it.isEnabled }) {
            return issue("disabled_day", "${subject.name} cannot be placed on ${candidate.dayOfWeek.displayName()}.", candidate.id)
        }
        val periodNumbers = data.periods.filter(MasterPeriod::isSchedulable).mapTo(mutableSetOf(), MasterPeriod::periodNumber)
        if (candidate.occupiedPeriods().any { it !in periodNumbers }) {
            return issue("non_teaching_period", "${subject.name} crosses a break or unavailable period.", candidate.id)
        }
        val unavailable = data.teacherAvailability.any {
            it.teacherId == teacher.id && it.dayOfWeek == candidate.dayOfWeek &&
                it.status == AvailabilityStatus.UNAVAILABLE && it.periodNumber in candidate.occupiedPeriods()
        }
        if (unavailable) {
            return issue("teacher_unavailable", "${teacher.fullName} is unavailable in this slot.", teacher.id, candidate.id)
        }

        placed.firstOrNull { other ->
            other.dayOfWeek == candidate.dayOfWeek && rangesOverlap(other, candidate) && other.teacherId == candidate.teacherId
        }?.let { other ->
            return issue("teacher_clash", "${teacher.fullName} is already teaching in this slot.", candidate.id, other.id)
        }
        placed.firstOrNull { other ->
            other.dayOfWeek == candidate.dayOfWeek && rangesOverlap(other, candidate) && other.academicClassId == candidate.academicClassId
        }?.let { other ->
            return issue("class_clash", "${academicClass.displayName} already has a class in this slot.", candidate.id, other.id)
        }

        val teacherEntries = placed.filter { it.teacherId == teacher.id }
        val weekly = teacherEntries.sumOf(MasterTimetableEntry::periodCount) + candidate.periodCount
        if (weekly > teacher.maxWeeklyPeriods) {
            return issue("teacher_weekly_limit", "${teacher.fullName}'s weekly limit is ${teacher.maxWeeklyPeriods} periods.", teacher.id)
        }
        val dailyPeriods = teacherEntries.filter { it.dayOfWeek == candidate.dayOfWeek }
            .sumOf(MasterTimetableEntry::periodCount) + candidate.periodCount
        if (dailyPeriods > teacher.maxDailyPeriods) {
            return issue("teacher_daily_limit", "${teacher.fullName}'s daily limit is ${teacher.maxDailyPeriods} periods.", teacher.id)
        }
        val occupied = teacherEntries.filter { it.dayOfWeek == candidate.dayOfWeek }
            .flatMap { it.occupiedPeriods().toList() }
            .plus(candidate.occupiedPeriods().toList())
            .distinct()
            .sorted()
        if (maximumConsecutive(occupied) > teacher.maxConsecutivePeriods) {
            return issue(
                "teacher_consecutive_limit",
                "${teacher.fullName} cannot teach more than ${teacher.maxConsecutivePeriods} consecutive periods.",
                teacher.id,
            )
        }
        val occurrences = placed.count {
            it.subjectId == candidate.subjectId && it.dayOfWeek == candidate.dayOfWeek
        } + 1
        if (occurrences > subject.maxOccurrencesPerDay) {
            return issue("subject_daily_limit", "${subject.name} is already scheduled on ${candidate.dayOfWeek.displayName()}.", subject.id)
        }
        return null
    }

    private fun rangesOverlap(first: MasterTimetableEntry, second: MasterTimetableEntry): Boolean =
        first.startPeriod <= second.endPeriod && second.startPeriod <= first.endPeriod

    fun maximumConsecutive(periods: List<Int>): Int {
        var longest = 0
        var current = 0
        var previous: Int? = null
        periods.forEach { period ->
            val prior = previous
            current = if (prior != null && period == prior + 1) current + 1 else 1
            longest = maxOf(longest, current)
            previous = period
        }
        return longest
    }

    private fun issue(code: String, message: String, vararg ids: String) =
        GenerationIssue(code, GenerationIssueSeverity.ERROR, message, ids.toSet())
}

object SoftConstraintScorer {
    fun score(data: MasterRoutineData, entries: List<MasterTimetableEntry>): TimetableQualityReport {
        var score = 100
        val deductions = mutableListOf<String>()
        data.subjects.filter(MasterSubject::distributeAcrossDays).forEach { subject ->
            val subjectEntries = entries.filter { it.subjectId == subject.id }
            val repeatedDays = subjectEntries.groupingBy(MasterTimetableEntry::dayOfWeek).eachCount().values.sumOf { (it - 1).coerceAtLeast(0) }
            if (repeatedDays > 0) {
                val deduction = minOf(8, repeatedDays * 2)
                score -= deduction
                deductions += "${subject.name} repeats on the same day (-$deduction)."
            }
        }
        data.teachers.forEach { teacher ->
            val gaps = enabledDays(data).sumOf { day ->
                val periods = entries.filter { it.teacherId == teacher.id && it.dayOfWeek == day }
                    .flatMap { it.occupiedPeriods().toList() }.distinct().sorted()
                if (periods.size < 2) 0 else (periods.last() - periods.first() + 1 - periods.size).coerceAtLeast(0)
            }
            if (gaps > 0) {
                val deduction = minOf(6, gaps)
                score -= deduction
                deductions += "${teacher.shortName} has $gaps idle gap period(s) (-$deduction)."
            }
        }
        return TimetableQualityReport(score.coerceIn(0, 100), deductions)
    }
}

fun calculateTeacherWorkloads(
    data: MasterRoutineData,
    entries: List<MasterTimetableEntry>,
): List<TeacherWorkload> {
    val capacity = enabledDays(data).size * data.periods.count(MasterPeriod::isSchedulable)
    return data.teachers.map { teacher ->
        val teacherEntries = entries.filter { it.teacherId == teacher.id }
        val required = data.assignments.filter {
            it.teacherId == teacher.id && !it.isAlternateTeacher
        }.sumOf(MasterTeacherAssignment::requiredWeeklyPeriods)
        val byDay = enabledDays(data).associateWith { day ->
            teacherEntries.filter { it.dayOfWeek == day }.sumOf(MasterTimetableEntry::periodCount)
        }
        val total = byDay.values.sum()
        val status = when {
            total > teacher.maxWeeklyPeriods -> WorkloadStatus.OVERALLOCATED
            total < maxOf(required, teacher.minWeeklyPeriods) -> WorkloadStatus.UNDERALLOCATED
            total == teacher.maxWeeklyPeriods -> WorkloadStatus.AT_MAXIMUM
            else -> WorkloadStatus.BALANCED
        }
        TeacherWorkload(
            teacherId = teacher.id,
            teacherName = teacher.fullName,
            requiredWeeklyPeriods = required,
            totalWeeklyPeriods = total,
            maximumWeeklyPeriods = teacher.maxWeeklyPeriods,
            periodsByDay = byDay,
            maximumConsecutivePeriods = byDay.keys.maxOfOrNull { day ->
                HardConstraintChecker.maximumConsecutive(
                    teacherEntries.filter { it.dayOfWeek == day }.flatMap { it.occupiedPeriods().toList() }.distinct().sorted(),
                )
            } ?: 0,
            freePeriods = (capacity - total).coerceAtLeast(0),
            status = status,
        )
    }
}

fun enabledDays(data: MasterRoutineData): List<DayOfWeek> = data.workingDays
    .filter(MasterWorkingDay::isEnabled)
    .sortedBy(MasterWorkingDay::displayOrder)
    .map(MasterWorkingDay::dayOfWeek)

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::uppercase)
