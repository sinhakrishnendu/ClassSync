package com.classsync.app.domain.master

import java.time.LocalDate

data class MasterPdfDocument(
    val title: String,
    val generatedOn: LocalDate,
    val sections: List<MasterPdfSection>,
)

data class MasterPdfSection(
    val title: String,
    val subtitle: String = "",
    val landscape: Boolean = false,
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val notes: List<String> = emptyList(),
)

object MasterRoutinePdfPreparer {
    fun prepare(data: MasterRoutineData, issues: List<GenerationIssue> = emptyList()): MasterPdfDocument {
        val periods = data.periods.filter(MasterPeriod::isSchedulable).sortedBy(MasterPeriod::periodNumber)
        val days = enabledDays(data)
        val sections = mutableListOf<MasterPdfSection>()
        sections += MasterPdfSection(
            title = data.routine.title,
            subtitle = listOf(data.routine.institutionName, data.routine.departmentName).filter(String::isNotBlank).joinToString(" · "),
            notes = buildList {
                add("Academic year: ${data.routine.academicYear.ifBlank { "—" }}")
                if (data.routine.academicSession.isNotBlank()) add("Session: ${data.routine.academicSession}")
                add("Effective from: ${data.routine.effectiveFrom}")
                add("Version: ${data.routine.versionLabel}")
                if (data.routine.preparedBy.isNotBlank()) add("Prepared by: ${data.routine.preparedBy}")
                if (data.routine.approvedBy.isNotBlank()) add("Approved by: ${data.routine.approvedBy}")
            },
        )
        data.classes.sortedBy(MasterAcademicClass::displayName).forEach { academicClass ->
            sections += timetableSection(
                title = academicClass.displayName,
                subtitle = "Class-wise routine",
                periods = periods,
                days = days,
            ) { day, period ->
                data.entries.firstOrNull {
                    it.academicClassId == academicClass.id && it.dayOfWeek == day && period.periodNumber in it.occupiedPeriods()
                }?.let { entry ->
                    val subject = data.subjects.firstOrNull { it.id == entry.subjectId }?.name.orEmpty()
                    val teacher = data.teachers.firstOrNull { it.id == entry.teacherId }?.shortName.orEmpty()
                    "$subject\n$teacher"
                }.orEmpty()
            }
        }
        data.teachers.sortedBy(MasterTeacher::fullName).forEach { teacher ->
            sections += timetableSection(
                title = teacher.fullName,
                subtitle = "Teacher-wise routine",
                periods = periods,
                days = days,
            ) { day, period ->
                data.entries.firstOrNull {
                    it.teacherId == teacher.id && it.dayOfWeek == day && period.periodNumber in it.occupiedPeriods()
                }?.let { entry ->
                    val subject = data.subjects.firstOrNull { it.id == entry.subjectId }?.name.orEmpty()
                    val academicClass = data.classes.firstOrNull { it.id == entry.academicClassId }?.displayName.orEmpty()
                    "$subject\n$academicClass"
                }.orEmpty()
            }
        }
        val workloads = calculateTeacherWorkloads(data, data.entries)
        sections += MasterPdfSection(
            title = "Faculty workload",
            headers = listOf("Teacher", "Syllabus load", "Allocated", "Maximum", "Status"),
            rows = workloads.map {
                listOf(
                    it.teacherName,
                    it.requiredWeeklyPeriods.toString(),
                    it.totalWeeklyPeriods.toString(),
                    it.maximumWeeklyPeriods.toString(),
                    it.status.name,
                )
            },
        )
        if (issues.isNotEmpty()) {
            sections += MasterPdfSection(
                title = "Validation report",
                headers = listOf("Level", "Issue"),
                rows = issues.map { listOf(it.severity.name, it.message) },
            )
        }
        return MasterPdfDocument(data.routine.title, LocalDate.now(), sections)
    }

    private fun timetableSection(
        title: String,
        subtitle: String,
        periods: List<MasterPeriod>,
        days: List<java.time.DayOfWeek>,
        cell: (java.time.DayOfWeek, MasterPeriod) -> String,
    ) = MasterPdfSection(
        title = title,
        subtitle = subtitle,
        landscape = true,
        headers = listOf("Day") + periods.map { "${it.label}\n${it.startTime}-${it.endTime}" },
        rows = days.map { day -> listOf(day.name.lowercase().replaceFirstChar(Char::uppercase)) + periods.map { cell(day, it) } },
    )
}
