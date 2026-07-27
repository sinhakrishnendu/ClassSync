package com.classsync.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.classsync.app.domain.master.AvailabilityStatus
import com.classsync.app.domain.master.GenerationStatus
import com.classsync.app.domain.master.MasterEntryType
import com.classsync.app.domain.master.MasterPeriodType
import com.classsync.app.domain.master.MasterRoutineStatus
import com.classsync.app.domain.master.MasterRoutineStep
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "master_routines", indices = [Index("status"), Index("updatedAt")])
data class MasterRoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    val institutionName: String,
    val departmentName: String,
    val academicYear: String,
    val academicSession: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val versionLabel: String,
    val status: MasterRoutineStatus,
    val currentStep: MasterRoutineStep,
    val preparedBy: String,
    val approvedBy: String,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "master_working_days",
    foreignKeys = [ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("masterRoutineId"), Index(value = ["masterRoutineId", "dayOfWeek"], unique = true)],
)
data class MasterWorkingDayEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val dayOfWeek: DayOfWeek,
    val isEnabled: Boolean,
    val displayOrder: Int,
)

@Entity(
    tableName = "master_periods",
    foreignKeys = [ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("masterRoutineId"), Index(value = ["masterRoutineId", "periodNumber"], unique = true)],
)
data class MasterPeriodEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val periodNumber: Int,
    val label: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: MasterPeriodType,
    val isSchedulable: Boolean,
)

@Entity(
    tableName = "master_classes",
    foreignKeys = [ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("masterRoutineId"), Index(value = ["masterRoutineId", "displayName"])],
)
data class MasterAcademicClassEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val displayName: String,
    val programme: String,
    val semester: String,
    val section: String,
    val batch: String,
    @ColumnInfo(name = "studentCount") val enrolledCount: Int?,
    val notes: String,
)

@Entity(
    tableName = "master_teachers",
    foreignKeys = [ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("masterRoutineId"), Index(value = ["masterRoutineId", "fullName"])],
)
data class MasterTeacherEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val fullName: String,
    val shortName: String,
    val designation: String,
    val department: String,
    val maxWeeklyPeriods: Int,
    val minWeeklyPeriods: Int,
    val maxDailyPeriods: Int,
    val maxConsecutivePeriods: Int,
    val canTeachPracticals: Boolean,
    val notes: String,
)

@Entity(
    tableName = "master_subjects",
    foreignKeys = [
        ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterAcademicClassEntity::class, ["id"], ["academicClassId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("masterRoutineId"), Index("academicClassId"), Index(value = ["masterRoutineId", "name"])],
)
data class MasterSubjectEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val academicClassId: String,
    val name: String,
    val code: String,
    val weeklyTheoryPeriods: Int,
    val weeklyPracticalPeriods: Int,
    val weeklyTutorialPeriods: Int,
    val consecutivePeriodRequirement: Int,
    val maxOccurrencesPerDay: Int,
    val distributeAcrossDays: Boolean,
    val notes: String,
)

@Entity(
    tableName = "master_assignments",
    foreignKeys = [
        ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterTeacherEntity::class, ["id"], ["teacherId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterSubjectEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterAcademicClassEntity::class, ["id"], ["academicClassId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("masterRoutineId"), Index("teacherId"), Index("subjectId"), Index("academicClassId")],
)
data class MasterTeacherAssignmentEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val teacherId: String,
    val subjectId: String,
    val academicClassId: String,
    val requiredWeeklyPeriods: Int,
    val isMandatory: Boolean,
    val isAlternateTeacher: Boolean,
)

@Entity(
    tableName = "master_teacher_availability",
    foreignKeys = [
        ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterTeacherEntity::class, ["id"], ["teacherId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("masterRoutineId"),
        Index("teacherId"),
        Index(value = ["teacherId", "dayOfWeek", "periodNumber"], unique = true),
    ],
)
data class MasterTeacherAvailabilityEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val teacherId: String,
    val dayOfWeek: DayOfWeek,
    val periodNumber: Int,
    val status: AvailabilityStatus,
    val preferenceWeight: Int,
)

@Entity(
    tableName = "master_timetable_entries",
    foreignKeys = [
        ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterAcademicClassEntity::class, ["id"], ["academicClassId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterSubjectEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MasterTeacherEntity::class, ["id"], ["teacherId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("masterRoutineId"), Index("academicClassId"), Index("subjectId"), Index("teacherId"),
        Index(value = ["masterRoutineId", "dayOfWeek", "startPeriod"]),
        Index(value = ["teacherId", "dayOfWeek", "startPeriod"]),
        Index(value = ["academicClassId", "dayOfWeek", "startPeriod"]),
    ],
)
data class MasterTimetableEntryEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val academicClassId: String,
    val subjectId: String,
    val teacherId: String,
    val dayOfWeek: DayOfWeek,
    val startPeriod: Int,
    val endPeriod: Int,
    val type: MasterEntryType,
    val isLocked: Boolean,
    val isManuallyEdited: Boolean,
    val generationBatchId: String,
    val notes: String,
)

@Entity(
    tableName = "master_generation_runs",
    foreignKeys = [ForeignKey(MasterRoutineEntity::class, ["id"], ["masterRoutineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("masterRoutineId"), Index("startedAt")],
)
data class MasterGenerationRunEntity(
    @PrimaryKey val id: String,
    val masterRoutineId: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val status: GenerationStatus,
    val qualityScore: Int,
    val totalEntriesRequested: Int,
    val totalEntriesAllocated: Int,
    val issueSummary: String,
)

data class MasterRoutineRecord(
    @Embedded val routine: MasterRoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val workingDays: List<MasterWorkingDayEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val periods: List<MasterPeriodEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val classes: List<MasterAcademicClassEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val teachers: List<MasterTeacherEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val subjects: List<MasterSubjectEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val assignments: List<MasterTeacherAssignmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val teacherAvailability: List<MasterTeacherAvailabilityEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val entries: List<MasterTimetableEntryEntity>,
    @Relation(parentColumn = "id", entityColumn = "masterRoutineId") val generationRuns: List<MasterGenerationRunEntity>,
)
