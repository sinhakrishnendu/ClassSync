package com.classsync.app.data.backup

import com.classsync.app.data.local.AcademicGroupEntity
import com.classsync.app.data.local.ClassScheduleEntity
import com.classsync.app.data.local.ScheduleExceptionEntity
import com.classsync.app.data.local.SubjectEntity
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object BackupCodec {
    const val SchemaVersion = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        explicitNulls = true
        encodeDefaults = true
    }

    fun encode(document: BackupDocument): String = json.encodeToString(document)

    fun decodeAndValidate(source: String): BackupDocument {
        val document = try {
            json.decodeFromString<BackupDocument>(source)
        } catch (error: SerializationException) {
            throw BackupValidationException("The selected file is not a valid ClassSync backup.", error)
        }
        validate(document)
        return document
    }

    fun validate(document: BackupDocument) {
        requireBackup(document.schemaVersion == SchemaVersion) {
            "Unsupported backup version ${document.schemaVersion}."
        }
        parseInstant(document.exportedAt)
        requireUniquePositiveIds(document.groups.map { it.id }, "group")
        requireUniquePositiveIds(document.subjects.map { it.id }, "subject")
        requireUniquePositiveIds(document.schedules.map { it.id }, "schedule")
        requireUniquePositiveIds(document.exceptions.map { it.id }, "exception")

        val groupIds = document.groups.mapTo(mutableSetOf()) { group ->
            requireBackup(group.programme.isNotBlank() && group.semester.isNotBlank()) {
                "Every group requires a programme and semester."
            }
            parseInstant(group.createdAt)
            parseInstant(group.updatedAt)
            group.id
        }
        val subjectGroups = document.subjects.associate { it.id to it.academicGroupId }
        val subjectIds = document.subjects.mapTo(mutableSetOf()) { subject ->
            requireBackup(subject.academicGroupId in groupIds) { "Subject ${subject.id} references a missing group." }
            requireBackup(subject.name.isNotBlank()) { "Every subject requires a name." }
            parseInstant(subject.createdAt)
            parseInstant(subject.updatedAt)
            subject.id
        }
        val scheduleIds = document.schedules.mapTo(mutableSetOf()) { schedule ->
            requireBackup(schedule.academicGroupId in groupIds) { "Schedule ${schedule.id} references a missing group." }
            requireBackup(schedule.subjectId in subjectIds) { "Schedule ${schedule.id} references a missing subject." }
            requireBackup(subjectGroups[schedule.subjectId] == schedule.academicGroupId) {
                "Schedule ${schedule.id} links a subject to the wrong group."
            }
            parseUserMode(schedule.mode)
            val recurrence = parseEnum<RecurrenceType>(schedule.recurrenceType, "recurrence")
            runCatching { DayOfWeek.of(schedule.dayOfWeek) }
                .getOrElse { throw BackupValidationException("Schedule ${schedule.id} has an invalid day.") }
            val start = parseTime(schedule.startTime)
            val end = parseTime(schedule.endTime)
            requireBackup(end.isAfter(start)) { "Schedule ${schedule.id} has an invalid time range." }
            requireBackup(schedule.reminderMinutes > 0) { "Schedule ${schedule.id} has an invalid reminder interval." }
            if (recurrence == RecurrenceType.ONE_TIME) {
                requireBackup(schedule.oneTimeDate != null) { "One-time schedule ${schedule.id} requires a date." }
            }
            schedule.oneTimeDate?.let(::parseDate)
            parseInstant(schedule.createdAt)
            parseInstant(schedule.updatedAt)
            schedule.id
        }
        requireBackup(document.exceptions.map { it.classScheduleId to it.relevantDate }.distinct().size == document.exceptions.size) {
            "Backup contains more than one exception for the same class date."
        }
        document.exceptions.forEach { exception ->
            requireBackup(exception.classScheduleId in scheduleIds) {
                "Exception ${exception.id} references a missing schedule."
            }
            parseDate(exception.relevantDate)
            val status = parseEnum<ExceptionStatus>(exception.status, "exception status")
            if (status == ExceptionStatus.RESCHEDULED) {
                val start = exception.changedStartTime?.let(::parseTime)
                val end = exception.changedEndTime?.let(::parseTime)
                requireBackup(start != null && end != null && end.isAfter(start)) {
                    "Rescheduled exception ${exception.id} requires a valid time range."
                }
            }
            parseInstant(exception.createdAt)
            parseInstant(exception.updatedAt)
        }
        parseUserMode(document.preferences.selectedMode)
        parseEnum<ThemePreference>(document.preferences.themePreference, "theme")
        parseEnum<TimeFormat>(document.preferences.timeFormat, "time format")
        requireBackup(document.preferences.defaultReminderMinutes > 0) { "The default reminder interval is invalid." }
        runCatching { DayOfWeek.of(document.preferences.weekStartDay) }
            .getOrElse { throw BackupValidationException("The week-start setting is invalid.") }
    }

    fun BackupDocument.toGroups(): List<AcademicGroupEntity> = groups.map {
        AcademicGroupEntity(
            id = it.id,
            programme = it.programme.trim(),
            semester = it.semester.trim(),
            batchSection = it.batchSection.normalizedOrNull(),
            institution = it.institution.normalizedOrNull(),
            createdAt = parseInstant(it.createdAt),
            updatedAt = parseInstant(it.updatedAt),
        )
    }

    fun BackupDocument.toSubjects(): List<SubjectEntity> = subjects.map {
        SubjectEntity(
            id = it.id,
            academicGroupId = it.academicGroupId,
            name = it.name.trim(),
            code = it.code.normalizedOrNull(),
            createdAt = parseInstant(it.createdAt),
            updatedAt = parseInstant(it.updatedAt),
        )
    }

    fun BackupDocument.toSchedules(): List<ClassScheduleEntity> = schedules.map {
        ClassScheduleEntity(
            id = it.id,
            mode = parseUserMode(it.mode),
            academicGroupId = it.academicGroupId,
            subjectId = it.subjectId,
            dayOfWeek = DayOfWeek.of(it.dayOfWeek),
            startTime = parseTime(it.startTime),
            endTime = parseTime(it.endTime),
            classroom = it.classroom.normalizedOrNull(),
            topic = it.topic.normalizedOrNull(),
            teacherName = it.teacherName.normalizedOrNull(),
            notes = it.notes.normalizedOrNull(),
            recurrenceType = enumValueOf(it.recurrenceType),
            oneTimeDate = it.oneTimeDate?.let(::parseDate),
            reminderEnabled = it.reminderEnabled,
            reminderMinutes = it.reminderMinutes,
            createdAt = parseInstant(it.createdAt),
            updatedAt = parseInstant(it.updatedAt),
        )
    }

    fun BackupDocument.toExceptions(): List<ScheduleExceptionEntity> = exceptions.map {
        ScheduleExceptionEntity(
            id = it.id,
            classScheduleId = it.classScheduleId,
            relevantDate = parseDate(it.relevantDate),
            status = enumValueOf(it.status),
            changedStartTime = it.changedStartTime?.let(::parseTime),
            changedEndTime = it.changedEndTime?.let(::parseTime),
            notes = it.notes.normalizedOrNull(),
            createdAt = parseInstant(it.createdAt),
            updatedAt = parseInstant(it.updatedAt),
        )
    }

    fun BackupDocument.toPreferences(): UserPreferences = UserPreferences(
        selectedMode = parseUserMode(preferences.selectedMode),
        onboardingComplete = true,
        defaultReminderMinutes = preferences.defaultReminderMinutes,
        remindersEnabled = preferences.remindersEnabled,
        themePreference = enumValueOf(preferences.themePreference),
        weekStartDay = DayOfWeek.of(preferences.weekStartDay),
        timeFormat = enumValueOf(preferences.timeFormat),
    )

    private fun requireUniquePositiveIds(ids: List<Long>, label: String) {
        requireBackup(ids.all { it > 0 } && ids.size == ids.toSet().size) {
            "Backup contains invalid or duplicate $label IDs."
        }
    }

    private fun parseInstant(value: String): Instant = runCatching { Instant.parse(value) }
        .getOrElse { throw BackupValidationException("Invalid timestamp in backup.") }

    private fun parseDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }
        .getOrElse { throw BackupValidationException("Invalid date in backup.") }

    private fun parseTime(value: String): LocalTime = runCatching { LocalTime.parse(value) }
        .getOrElse { throw BackupValidationException("Invalid time in backup.") }

    private inline fun <reified T : Enum<T>> parseEnum(value: String, label: String): T =
        runCatching { enumValueOf<T>(value) }
            .getOrElse { throw BackupValidationException("Invalid $label in backup.") }

    private fun parseUserMode(value: String): UserMode = when (value) {
        UserMode.TEACHER.name, "STUDENT" -> UserMode.TEACHER
        UserMode.ADMINISTRATION.name -> UserMode.ADMINISTRATION
        else -> throw BackupValidationException("Invalid mode in backup.")
    }

    private fun requireBackup(condition: Boolean, message: () -> String) {
        if (!condition) throw BackupValidationException(message())
    }

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
