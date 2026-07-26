package com.classsync.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupDocument(
    val schemaVersion: Int,
    val exportedAt: String,
    val groups: List<GroupBackup>,
    val subjects: List<SubjectBackup>,
    val schedules: List<ScheduleBackup>,
    val exceptions: List<ExceptionBackup>,
    val preferences: PreferencesBackup,
)

@Serializable
data class GroupBackup(
    val id: Long,
    val programme: String,
    val semester: String,
    val batchSection: String? = null,
    val institution: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SubjectBackup(
    val id: Long,
    val academicGroupId: Long,
    val name: String,
    val code: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ScheduleBackup(
    val id: Long,
    val mode: String,
    val academicGroupId: Long,
    val subjectId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val classroom: String? = null,
    val topic: String? = null,
    val teacherName: String? = null,
    val notes: String? = null,
    val recurrenceType: String,
    val oneTimeDate: String? = null,
    val reminderEnabled: Boolean,
    val reminderMinutes: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ExceptionBackup(
    val id: Long,
    val classScheduleId: Long,
    val relevantDate: String,
    val status: String,
    val changedStartTime: String? = null,
    val changedEndTime: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class PreferencesBackup(
    val selectedMode: String,
    val defaultReminderMinutes: Int,
    val remindersEnabled: Boolean,
    val themePreference: String,
    val weekStartDay: Int,
    val timeFormat: String,
)

class BackupValidationException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

