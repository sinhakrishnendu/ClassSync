package com.classsync.app

import com.classsync.app.domain.master.AvailabilityStatus
import com.classsync.app.domain.master.ConstraintValidator
import com.classsync.app.domain.master.GenerationIssueSeverity
import com.classsync.app.domain.master.GenerationStatus
import com.classsync.app.domain.master.HardConstraintChecker
import com.classsync.app.domain.master.MasterAcademicClass
import com.classsync.app.domain.master.MasterEntryType
import com.classsync.app.domain.master.MasterPeriod
import com.classsync.app.domain.master.MasterRoutine
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterSubject
import com.classsync.app.domain.master.MasterTeacher
import com.classsync.app.domain.master.MasterTeacherAssignment
import com.classsync.app.domain.master.MasterTeacherAvailability
import com.classsync.app.domain.master.MasterTimetableEntry
import com.classsync.app.domain.master.MasterWorkingDay
import com.classsync.app.domain.master.SoftConstraintScorer
import com.classsync.app.domain.master.TimetableGenerationEngine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterRoutineGenerationEngineTest {
    private val engine = TimetableGenerationEngine()

    @Test
    fun validDatasetAllocatesEveryPeriodWithoutHardConflicts() {
        val data = dataset()
        val result = engine.generate(data)

        assertTrue(result.status == GenerationStatus.SUCCESS || result.status == GenerationStatus.SUCCESS_WITH_WARNINGS)
        assertEquals(4, result.requestedPeriods)
        assertEquals(4, result.allocatedPeriods)
        assertEquals(0, ConstraintValidator.validateTimetable(data.copy(entries = result.entries)).count {
            it.severity == GenerationIssueSeverity.ERROR
        })
    }

    @Test
    fun teacherWeeklyLimitMakesInputImpossibleAndNamesTeacher() {
        val data = dataset().let { source ->
            source.copy(teachers = source.teachers.map { if (it.id == "t1") it.copy(maxWeeklyPeriods = 1) else it })
        }
        val result = engine.generate(data)

        assertEquals(GenerationStatus.IMPOSSIBLE, result.status)
        assertTrue(result.issues.any { it.code == "teacher_weekly_overload" && "Dr Ada" in it.message })
    }

    @Test
    fun sharedPaperAllocatesEachFacultyMemberTheirExactSyllabusLoad() {
        val source = dataset()
        val sharedSubject = source.subjects.first().copy(weeklyTheoryPeriods = 4)
        val fourDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
        ).mapIndexed { index, day -> MasterWorkingDay("shared-day-$index", "r", day, true, index) }
        val data = source.copy(
            workingDays = fourDays,
            subjects = listOf(sharedSubject),
            assignments = listOf(
                MasterTeacherAssignment("shared-a1", "r", "t1", "s1", "c1", 1),
                MasterTeacherAssignment("shared-a2", "r", "t2", "s1", "c1", 3),
            ),
        )

        val result = engine.generate(data)

        assertTrue(result.status == GenerationStatus.SUCCESS || result.status == GenerationStatus.SUCCESS_WITH_WARNINGS)
        assertEquals(1, result.entries.filter { it.teacherId == "t1" }.sumOf { it.periodCount })
        assertEquals(3, result.entries.filter { it.teacherId == "t2" }.sumOf { it.periodCount })
        assertTrue(result.issues.none { it.code == "faculty_load_mismatch" })
    }

    @Test
    fun facultyLoadsMustEqualTheSyllabusTotal() {
        val source = dataset()
        val data = source.copy(assignments = source.assignments.map {
            if (it.subjectId == "s1") it.copy(requiredWeeklyPeriods = 1) else it
        })

        val result = engine.generate(data)

        assertEquals(GenerationStatus.IMPOSSIBLE, result.status)
        assertTrue(result.issues.any { it.code == "syllabus_load_mismatch" })
    }

    @Test
    fun teacherUnavailableDayIsNeverUsed() {
        val source = dataset()
        val unavailable = source.periods.map { period ->
            MasterTeacherAvailability(
                id = "u-${period.periodNumber}",
                masterRoutineId = "r",
                teacherId = "t1",
                dayOfWeek = DayOfWeek.MONDAY,
                periodNumber = period.periodNumber,
                status = AvailabilityStatus.UNAVAILABLE,
            )
        }
        val result = engine.generate(source.copy(teacherAvailability = unavailable))

        assertTrue(result.entries.filter { it.teacherId == "t1" }.none { it.dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test
    fun lockedTeacherClashStopsGenerationWithoutMovingEntries() {
        val source = dataset()
        val locked = listOf(
            MasterTimetableEntry("e1", "r", "c1", "s1", "t1", DayOfWeek.MONDAY, 1, isLocked = true),
            MasterTimetableEntry("e2", "r", "c2", "s2", "t1", DayOfWeek.MONDAY, 1, isLocked = true),
        )
        val result = engine.generate(source.copy(entries = locked))

        assertEquals(GenerationStatus.IMPOSSIBLE, result.status)
        assertTrue(result.issues.any { it.code == "teacher_clash" })
        assertTrue(locked.all { original -> result.entries.isEmpty() || result.entries.any { it.id == original.id && it.dayOfWeek == original.dayOfWeek } })
    }

    @Test
    fun practicalRequirementIsAllocatedAsOneConsecutiveBlock() {
        val source = dataset()
        val practical = source.subjects.first().copy(
            weeklyTheoryPeriods = 0,
            weeklyPracticalPeriods = 2,
            consecutivePeriodRequirement = 2,
        )
        val data = source.copy(
            subjects = listOf(practical),
            assignments = listOf(source.assignments.first().copy(requiredWeeklyPeriods = 2)),
        )
        val result = engine.generate(data)

        val entry = result.entries.single()
        assertEquals(MasterEntryType.PRACTICAL, entry.type)
        assertEquals(2, entry.periodCount)
    }

    @Test
    fun manualMoveCheckerRejectsClassClash() {
        val data = dataset()
        val first = MasterTimetableEntry("e1", "r", "c1", "s1", "t1", DayOfWeek.MONDAY, 1)
        val second = MasterTimetableEntry("e2", "r", "c1", "s2", "t2", DayOfWeek.MONDAY, 1)

        val issue = HardConstraintChecker.checkPlacement(data, listOf(first), second)

        assertNotNull(issue)
        assertEquals("class_clash", issue?.code)
    }

    @Test
    fun qualityScorePenalizesSameDaySubjectConcentration() {
        val data = dataset()
        val entries = listOf(
            MasterTimetableEntry("e1", "r", "c1", "s1", "t1", DayOfWeek.MONDAY, 1),
            MasterTimetableEntry("e2", "r", "c1", "s1", "t1", DayOfWeek.MONDAY, 2),
        )
        assertTrue(SoftConstraintScorer.score(data, entries).score < 100)
    }

    private fun dataset(): MasterRoutineData {
        val routine = MasterRoutine(id = "r", title = "Science Routine", institutionName = "North College", effectiveFrom = LocalDate.of(2026, 8, 1))
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY).mapIndexed { index, day ->
            MasterWorkingDay("d$index", "r", day, true, index)
        }
        val periods = (1..3).map { number ->
            MasterPeriod(
                id = "p$number",
                masterRoutineId = "r",
                periodNumber = number,
                startTime = LocalTime.of(9 + number - 1, 0),
                endTime = LocalTime.of(10 + number - 1, 0),
            )
        }
        val classes = listOf(
            MasterAcademicClass("c1", "r", "MSc I"),
            MasterAcademicClass("c2", "r", "MSc III"),
        )
        val teachers = listOf(
            MasterTeacher("t1", "r", "Dr Ada", "ADA", maxWeeklyPeriods = 4),
            MasterTeacher("t2", "r", "Dr Bose", "BOS", maxWeeklyPeriods = 4),
        )
        val subjects = listOf(
            MasterSubject("s1", "r", "c1", "Physiology", weeklyTheoryPeriods = 2),
            MasterSubject("s2", "r", "c2", "Evolution", weeklyTheoryPeriods = 2),
        )
        val assignments = listOf(
            MasterTeacherAssignment("a1", "r", "t1", "s1", "c1", 2),
            MasterTeacherAssignment("a2", "r", "t2", "s2", "c2", 2),
        )
        return MasterRoutineData(routine, days, periods, classes, teachers, subjects, assignments)
    }
}
