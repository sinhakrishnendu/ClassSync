package com.classsync.app

import com.classsync.app.domain.master.MasterAcademicClass
import com.classsync.app.domain.master.MasterPeriod
import com.classsync.app.domain.master.MasterRoutine
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterRoutinePdfPreparer
import com.classsync.app.domain.master.MasterSubject
import com.classsync.app.domain.master.MasterTeacher
import com.classsync.app.domain.master.MasterTimetableEntry
import com.classsync.app.domain.master.MasterWorkingDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterRoutinePdfPreparerTest {
    @Test
    fun preparedDocumentContainsCoverClassTeacherAndWorkloadSections() {
        val data = MasterRoutineData(
            routine = MasterRoutine(id = "r", title = "Zoology 2026", institutionName = "North College", effectiveFrom = LocalDate.of(2026, 8, 1)),
            workingDays = listOf(MasterWorkingDay("d", "r", DayOfWeek.MONDAY)),
            periods = listOf(MasterPeriod("p", "r", 1, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0))),
            classes = listOf(MasterAcademicClass("c", "r", "MSc I")),
            teachers = listOf(MasterTeacher("t", "r", "Dr Ada", "ADA")),
            subjects = listOf(MasterSubject("s", "r", "c", "Physiology")),
            entries = listOf(MasterTimetableEntry("e", "r", "c", "s", "t", DayOfWeek.MONDAY, 1)),
        )

        val document = MasterRoutinePdfPreparer.prepare(data)

        assertTrue(document.sections.first().title.contains("Zoology"))
        assertTrue(document.sections.any { it.subtitle == "Class-wise routine" && it.rows.flatten().any { cell -> "Physiology" in cell } })
        assertTrue(document.sections.any { it.subtitle == "Teacher-wise routine" })
        assertTrue(document.sections.any { it.title == "Faculty workload" })
    }
}
