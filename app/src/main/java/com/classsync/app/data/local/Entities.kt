package com.classsync.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.UserMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "academic_groups",
    indices = [Index("programme"), Index("semester"), Index("batchSection")],
)
data class AcademicGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programme: String,
    val semester: String,
    val batchSection: String?,
    val institution: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = AcademicGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("academicGroupId"), Index("name")],
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val academicGroupId: Long,
    val name: String,
    val code: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "class_schedules",
    foreignKeys = [
        ForeignKey(
            entity = AcademicGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicGroupId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("mode"),
        Index("academicGroupId"),
        Index("subjectId"),
        Index("dayOfWeek"),
        Index("startTime"),
        Index(value = ["mode", "dayOfWeek", "startTime"]),
    ],
)
data class ClassScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: UserMode,
    val academicGroupId: Long,
    val subjectId: Long,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val classroom: String?,
    val topic: String?,
    val teacherName: String?,
    val notes: String?,
    val recurrenceType: RecurrenceType,
    val oneTimeDate: LocalDate?,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "schedule_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = ClassScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["classScheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("classScheduleId"),
        Index("relevantDate"),
        Index(value = ["classScheduleId", "relevantDate"], unique = true),
    ],
)
data class ScheduleExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classScheduleId: Long,
    val relevantDate: LocalDate,
    val status: ExceptionStatus,
    val changedStartTime: LocalTime?,
    val changedEndTime: LocalTime?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ClassEntryRecord(
    @Embedded val schedule: ClassScheduleEntity,
    @Relation(parentColumn = "academicGroupId", entityColumn = "id")
    val group: AcademicGroupEntity,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectEntity,
    @Relation(parentColumn = "id", entityColumn = "classScheduleId")
    val exceptions: List<ScheduleExceptionEntity>,
)

