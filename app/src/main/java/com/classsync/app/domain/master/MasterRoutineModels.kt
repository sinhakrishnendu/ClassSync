package com.classsync.app.domain.master

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class MasterRoutineStatus { DRAFT, READY, FINALIZED, ARCHIVED }

enum class MasterRoutineStep { DETAILS, WEEK, PEOPLE, SUBJECTS, GENERATE }

enum class MasterPeriodType { TEACHING, PRACTICAL, TUTORIAL, BREAK, LUNCH, RESERVED }

enum class MasterEntryType { THEORY, PRACTICAL, TUTORIAL, FIXED }

enum class AvailabilityStatus { AVAILABLE, PREFERRED, UNAVAILABLE }

enum class GenerationMode { FAST, BALANCED, THOROUGH }

enum class GenerationStatus { SUCCESS, SUCCESS_WITH_WARNINGS, IMPOSSIBLE, CANCELLED }

enum class GenerationIssueSeverity { ERROR, WARNING, SUGGESTION }

data class MasterRoutine(
    val id: String = masterId(),
    val title: String = "",
    val institutionName: String = "",
    val departmentName: String = "",
    val academicYear: String = "",
    val academicSession: String = "",
    val effectiveFrom: LocalDate = LocalDate.now(),
    val effectiveTo: LocalDate? = null,
    val versionLabel: String = "1.0",
    val status: MasterRoutineStatus = MasterRoutineStatus.DRAFT,
    val currentStep: MasterRoutineStep = MasterRoutineStep.DETAILS,
    val preparedBy: String = "",
    val approvedBy: String = "",
    val notes: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class MasterWorkingDay(
    val id: String = masterId(),
    val masterRoutineId: String,
    val dayOfWeek: DayOfWeek,
    val isEnabled: Boolean = true,
    val displayOrder: Int = dayOfWeek.value,
)

data class MasterPeriod(
    val id: String = masterId(),
    val masterRoutineId: String,
    val periodNumber: Int,
    val label: String = "Period $periodNumber",
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: MasterPeriodType = MasterPeriodType.TEACHING,
    val isSchedulable: Boolean = type != MasterPeriodType.BREAK && type != MasterPeriodType.LUNCH,
)

data class MasterAcademicClass(
    val id: String = masterId(),
    val masterRoutineId: String,
    val displayName: String,
    val programme: String = displayName,
    val semester: String = "",
    val section: String = "",
    val batch: String = "",
    val enrolledCount: Int? = null,
    val notes: String = "",
)

data class MasterTeacher(
    val id: String = masterId(),
    val masterRoutineId: String,
    val fullName: String,
    val shortName: String = fullName.initials(),
    val designation: String = "",
    val department: String = "",
    val maxWeeklyPeriods: Int = 18,
    val minWeeklyPeriods: Int = 0,
    val maxDailyPeriods: Int = 5,
    val maxConsecutivePeriods: Int = 3,
    val canTeachPracticals: Boolean = true,
    val notes: String = "",
)

data class MasterSubject(
    val id: String = masterId(),
    val masterRoutineId: String,
    val academicClassId: String,
    val name: String,
    val code: String = "",
    val weeklyTheoryPeriods: Int = 1,
    val weeklyPracticalPeriods: Int = 0,
    val weeklyTutorialPeriods: Int = 0,
    val consecutivePeriodRequirement: Int = 1,
    val maxOccurrencesPerDay: Int = 1,
    val distributeAcrossDays: Boolean = true,
    val notes: String = "",
) {
    val totalWeeklyPeriods: Int
        get() = weeklyTheoryPeriods + weeklyPracticalPeriods + weeklyTutorialPeriods
}

data class MasterTeacherAssignment(
    val id: String = masterId(),
    val masterRoutineId: String,
    val teacherId: String,
    val subjectId: String,
    val academicClassId: String,
    val requiredWeeklyPeriods: Int,
    val isMandatory: Boolean = true,
    val isAlternateTeacher: Boolean = false,
)

data class MasterTeacherAvailability(
    val id: String = masterId(),
    val masterRoutineId: String,
    val teacherId: String,
    val dayOfWeek: DayOfWeek,
    val periodNumber: Int,
    val status: AvailabilityStatus = AvailabilityStatus.UNAVAILABLE,
    val preferenceWeight: Int = 1,
)

data class MasterTimetableEntry(
    val id: String = masterId(),
    val masterRoutineId: String,
    val academicClassId: String,
    val subjectId: String,
    val teacherId: String,
    val dayOfWeek: DayOfWeek,
    val startPeriod: Int,
    val endPeriod: Int = startPeriod,
    val type: MasterEntryType = MasterEntryType.THEORY,
    val isLocked: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val generationBatchId: String = "",
    val notes: String = "",
) {
    val periodCount: Int get() = endPeriod - startPeriod + 1
    fun occupiedPeriods(): IntRange = startPeriod..endPeriod
}

data class MasterGenerationRun(
    val id: String = masterId(),
    val masterRoutineId: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val status: GenerationStatus,
    val qualityScore: Int,
    val totalEntriesRequested: Int,
    val totalEntriesAllocated: Int,
    val issueSummary: String,
)

data class MasterRoutineData(
    val routine: MasterRoutine,
    val workingDays: List<MasterWorkingDay> = defaultWorkingDays(routine.id),
    val periods: List<MasterPeriod> = defaultPeriods(routine.id),
    val classes: List<MasterAcademicClass> = emptyList(),
    val teachers: List<MasterTeacher> = emptyList(),
    val subjects: List<MasterSubject> = emptyList(),
    val assignments: List<MasterTeacherAssignment> = emptyList(),
    val teacherAvailability: List<MasterTeacherAvailability> = emptyList(),
    val entries: List<MasterTimetableEntry> = emptyList(),
    val generationRuns: List<MasterGenerationRun> = emptyList(),
)

data class MasterRoutineSummary(
    val id: String,
    val title: String,
    val institutionName: String,
    val status: MasterRoutineStatus,
    val currentStep: MasterRoutineStep,
    val classCount: Int,
    val teacherCount: Int,
    val subjectCount: Int,
    val unresolvedConflicts: Int,
    val updatedAt: Instant,
)

data class GenerationIssue(
    val code: String,
    val severity: GenerationIssueSeverity,
    val message: String,
    val affectedIds: Set<String> = emptySet(),
)

data class TimetableQualityReport(
    val score: Int,
    val deductions: List<String> = emptyList(),
)

enum class WorkloadStatus { UNDERALLOCATED, BALANCED, AT_MAXIMUM, OVERALLOCATED }

data class TeacherWorkload(
    val teacherId: String,
    val teacherName: String,
    val requiredWeeklyPeriods: Int,
    val totalWeeklyPeriods: Int,
    val maximumWeeklyPeriods: Int,
    val periodsByDay: Map<DayOfWeek, Int>,
    val maximumConsecutivePeriods: Int,
    val freePeriods: Int,
    val status: WorkloadStatus,
)

data class GenerationResult(
    val status: GenerationStatus,
    val entries: List<MasterTimetableEntry> = emptyList(),
    val issues: List<GenerationIssue> = emptyList(),
    val qualityReport: TimetableQualityReport = TimetableQualityReport(0),
    val workloads: List<TeacherWorkload> = emptyList(),
    val requestedPeriods: Int = 0,
    val allocatedPeriods: Int = 0,
)

fun newMasterRoutine(): MasterRoutineData {
    val routine = MasterRoutine()
    return MasterRoutineData(routine = routine)
}

fun defaultWorkingDays(routineId: String): List<MasterWorkingDay> = DayOfWeek.values().map { day ->
    MasterWorkingDay(
        masterRoutineId = routineId,
        dayOfWeek = day,
        isEnabled = day.value <= DayOfWeek.FRIDAY.value,
    )
}

fun defaultPeriods(routineId: String): List<MasterPeriod> = (1..5).map { number ->
    val start = LocalTime.of(9, 0).plusHours((number - 1).toLong())
    MasterPeriod(
        masterRoutineId = routineId,
        periodNumber = number,
        startTime = start,
        endTime = start.plusHours(1),
    )
}

fun masterId(): String = UUID.randomUUID().toString()

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(3)
    .joinToString("") { it.first().uppercase() }
