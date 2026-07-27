package com.classsync.app.domain.master

import java.time.DayOfWeek
import javax.inject.Inject

class TimetableGenerationEngine @Inject constructor() {
    fun generate(
        data: MasterRoutineData,
        mode: GenerationMode = GenerationMode.BALANCED,
        isCancelled: () -> Boolean = { false },
        onProgress: (Int) -> Unit = {},
    ): GenerationResult {
        val inputIssues = ConstraintValidator.validateInputs(data)
        if (inputIssues.any { it.severity == GenerationIssueSeverity.ERROR }) {
            return GenerationResult(GenerationStatus.IMPOSSIBLE, issues = inputIssues)
        }

        val locked = data.entries.filter(MasterTimetableEntry::isLocked)
        val blocks = FacultyLoadPlanner.expand(data, locked)
            ?: return GenerationResult(
                status = GenerationStatus.IMPOSSIBLE,
                entries = locked,
                issues = inputIssues + GenerationIssue(
                    code = "faculty_load_block_mismatch",
                    severity = GenerationIssueSeverity.ERROR,
                    message = "Faculty loads cannot be matched to the syllabus blocks. Review practical block sizes and each faculty member's assigned periods.",
                ),
            )
        val requestedPeriods = data.subjects.sumOf(MasterSubject::totalWeeklyPeriods)
        if (blocks.isEmpty()) return successfulResult(data, locked, inputIssues, requestedPeriods)

        val maxNodes = when (mode) {
            GenerationMode.FAST -> 25_000
            GenerationMode.BALANCED -> 150_000
            GenerationMode.THOROUGH -> 600_000
        }
        var visited = 0
        var best: List<MasterTimetableEntry>? = null
        var bestScore = -1
        val placed = locked.toMutableList()

        fun search(index: Int): Boolean {
            if (isCancelled()) return true
            if (visited++ >= maxNodes) return false
            if (visited % 500 == 0) onProgress(((index.toDouble() / blocks.size) * 100).toInt().coerceIn(1, 99))
            if (index == blocks.size) {
                val score = SoftConstraintScorer.score(data, placed).score
                if (score > bestScore) {
                    bestScore = score
                    best = placed.toList()
                }
                return mode == GenerationMode.FAST || score >= 98
            }
            val block = blocks[index]
            for (candidate in candidates(data, block)) {
                if (HardConstraintChecker.checkPlacement(data, placed, candidate) == null) {
                    placed += candidate
                    if (search(index + 1)) return true
                    placed.removeAt(placed.lastIndex)
                }
            }
            return false
        }

        search(0)
        if (isCancelled()) {
            return GenerationResult(
                status = GenerationStatus.CANCELLED,
                issues = listOf(
                    GenerationIssue("cancelled", GenerationIssueSeverity.WARNING, "Generation was cancelled. Your setup is still saved."),
                ),
                requestedPeriods = requestedPeriods,
            )
        }
        val result = best
        if (result == null) {
            val unallocated = blocks.sumOf(FacultyLoadBlock::length)
            return GenerationResult(
                status = GenerationStatus.IMPOSSIBLE,
                entries = locked,
                issues = inputIssues + GenerationIssue(
                    code = "no_valid_allocation",
                    severity = GenerationIssueSeverity.ERROR,
                    message = "No valid routine fits the current limits. $unallocated period(s) remain unallocated. Add teaching slots, relax availability, or reassign workload.",
                ),
                requestedPeriods = requestedPeriods,
                allocatedPeriods = locked.sumOf(MasterTimetableEntry::periodCount),
            )
        }
        onProgress(100)
        return successfulResult(data, result, inputIssues, requestedPeriods)
    }

    private fun successfulResult(
        data: MasterRoutineData,
        entries: List<MasterTimetableEntry>,
        inputIssues: List<GenerationIssue>,
        requestedPeriods: Int,
    ): GenerationResult {
        val validation = ConstraintValidator.validateTimetable(data.copy(entries = entries), entries)
            .filterNot { it.code in inputIssues.map(GenerationIssue::code) }
        val issues = inputIssues + validation
        val hasErrors = issues.any { it.severity == GenerationIssueSeverity.ERROR }
        return GenerationResult(
            status = when {
                hasErrors -> GenerationStatus.IMPOSSIBLE
                issues.isNotEmpty() -> GenerationStatus.SUCCESS_WITH_WARNINGS
                else -> GenerationStatus.SUCCESS
            },
            entries = entries.sortedWith(compareBy({ it.dayOfWeek.value }, { it.startPeriod }, { it.academicClassId })),
            issues = issues,
            qualityReport = SoftConstraintScorer.score(data, entries),
            workloads = calculateTeacherWorkloads(data, entries),
            requestedPeriods = requestedPeriods,
            allocatedPeriods = entries.sumOf(MasterTimetableEntry::periodCount),
        )
    }

    private fun candidates(data: MasterRoutineData, block: FacultyLoadBlock): List<MasterTimetableEntry> {
        val periods = data.periods.sortedBy(MasterPeriod::periodNumber)
        val batchId = "generation-${data.routine.updatedAt.toEpochMilli()}"
        return buildList {
            enabledDays(data).forEach { day ->
                periods.indices.forEach { index ->
                    val selected = periods.drop(index).take(block.length)
                    if (selected.size == block.length && selected.all(MasterPeriod::isSchedulable) &&
                        selected.zipWithNext().all { (a, b) -> b.periodNumber == a.periodNumber + 1 }
                    ) {
                        add(
                            MasterTimetableEntry(
                                masterRoutineId = data.routine.id,
                                academicClassId = block.assignment.academicClassId,
                                subjectId = block.subject.id,
                                teacherId = block.assignment.teacherId,
                                dayOfWeek = day,
                                startPeriod = selected.first().periodNumber,
                                endPeriod = selected.last().periodNumber,
                                type = block.type,
                                generationBatchId = batchId,
                            ),
                        )
                    }
                }
            }
        }.sortedWith(
            compareBy<MasterTimetableEntry>(
                { existingSameSubjectDayPenalty(data, block.subject.id, it.dayOfWeek) },
                { it.dayOfWeek.value },
                { it.startPeriod },
            ),
        )
    }

    private fun existingSameSubjectDayPenalty(data: MasterRoutineData, subjectId: String, day: DayOfWeek): Int =
        data.entries.count { it.subjectId == subjectId && it.dayOfWeek == day }

}

internal data class FacultyLoadBlock(
    val subject: MasterSubject,
    val assignment: MasterTeacherAssignment,
    val type: MasterEntryType,
    val length: Int,
    val index: Int,
)

/** Converts syllabus periods into exact per-faculty work before slot placement begins. */
internal object FacultyLoadPlanner {
    fun expand(data: MasterRoutineData, locked: List<MasterTimetableEntry>): List<FacultyLoadBlock>? {
        val blocks = mutableListOf<FacultyLoadBlock>()
        data.subjects.forEach { subject ->
            blocks += expandSubject(data, subject, locked) ?: return null
        }
        return blocks.sortedWith(
            compareByDescending<FacultyLoadBlock> { it.length }
                .thenBy { block -> availabilityCount(data, block.assignment.teacherId) }
                .thenByDescending { it.subject.totalWeeklyPeriods }
                .thenBy { it.subject.id }
                .thenBy { it.index },
        )
    }

    fun expandSubject(
        data: MasterRoutineData,
        subject: MasterSubject,
        locked: List<MasterTimetableEntry>,
    ): List<FacultyLoadBlock>? {
        val assignments = data.assignments.filter { it.subjectId == subject.id && !it.isAlternateTeacher }
        if (assignments.isEmpty()) return null
        val lockedForSubject = locked.filter { it.subjectId == subject.id }
        val remainingByAssignment = assignments.associateWith { assignment ->
            assignment.requiredWeeklyPeriods - lockedForSubject.filter { it.teacherId == assignment.teacherId }
                .sumOf(MasterTimetableEntry::periodCount)
        }.toMutableMap()
        if (remainingByAssignment.values.any { it < 0 }) return null

        var remainingTheory = subject.weeklyTheoryPeriods
        var remainingPractical = subject.weeklyPracticalPeriods
        var remainingTutorial = subject.weeklyTutorialPeriods
        lockedForSubject.forEach { entry ->
            when (entry.type) {
                MasterEntryType.THEORY -> remainingTheory -= entry.periodCount
                MasterEntryType.PRACTICAL -> remainingPractical -= entry.periodCount
                MasterEntryType.TUTORIAL -> remainingTutorial -= entry.periodCount
                MasterEntryType.FIXED -> {
                    var fixed = entry.periodCount
                    val theoryPart = minOf(fixed, remainingTheory.coerceAtLeast(0))
                    remainingTheory -= theoryPart
                    fixed -= theoryPart
                    val tutorialPart = minOf(fixed, remainingTutorial.coerceAtLeast(0))
                    remainingTutorial -= tutorialPart
                    fixed -= tutorialPart
                    remainingPractical -= fixed
                }
            }
        }
        if (remainingTheory < 0 || remainingPractical < 0 || remainingTutorial < 0) return null

        val templates = buildList {
            repeat(remainingTheory) { add(Template(MasterEntryType.THEORY, 1, it)) }
            var practical = remainingPractical
            var practicalIndex = 0
            while (practical > 0) {
                val length = minOf(subject.consecutivePeriodRequirement.coerceAtLeast(1), practical)
                add(Template(MasterEntryType.PRACTICAL, length, practicalIndex++))
                practical -= length
            }
            repeat(remainingTutorial) { add(Template(MasterEntryType.TUTORIAL, 1, it)) }
        }.sortedByDescending(Template::length)
        if (templates.sumOf(Template::length) != remainingByAssignment.values.sum()) return null

        val planned = mutableListOf<FacultyLoadBlock>()
        fun assign(index: Int): Boolean {
            if (index == templates.size) return remainingByAssignment.values.all { it == 0 }
            val template = templates[index]
            val candidates = assignments
                .filter { assignment ->
                    remainingByAssignment.getValue(assignment) >= template.length &&
                        (template.type != MasterEntryType.PRACTICAL || data.teachers.firstOrNull { it.id == assignment.teacherId }?.canTeachPracticals == true)
                }
                .sortedByDescending { remainingByAssignment.getValue(it) }
            candidates.forEach { assignment ->
                remainingByAssignment[assignment] = remainingByAssignment.getValue(assignment) - template.length
                planned += FacultyLoadBlock(subject, assignment, template.type, template.length, template.index)
                if (assign(index + 1)) return true
                planned.removeAt(planned.lastIndex)
                remainingByAssignment[assignment] = remainingByAssignment.getValue(assignment) + template.length
            }
            return false
        }
        return planned.takeIf { assign(0) }?.toList()
    }

    private fun availabilityCount(data: MasterRoutineData, teacherId: String): Int {
        val unavailable = data.teacherAvailability.count {
            it.teacherId == teacherId && it.status == AvailabilityStatus.UNAVAILABLE
        }
        return enabledDays(data).size * data.periods.count(MasterPeriod::isSchedulable) - unavailable
    }

    private data class Template(val type: MasterEntryType, val length: Int, val index: Int)
}
