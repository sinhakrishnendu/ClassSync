package com.classsync.app

import com.classsync.app.data.backup.BackupCodec
import com.classsync.app.data.backup.BackupDocument
import com.classsync.app.data.backup.BackupValidationException
import com.classsync.app.data.backup.GroupBackup
import com.classsync.app.data.backup.PreferencesBackup
import com.classsync.app.data.backup.ScheduleBackup
import com.classsync.app.data.backup.SubjectBackup
import com.classsync.app.domain.model.UserMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    @Test
    fun validBackupRoundTrips() {
        val source = validDocument()

        val decoded = BackupCodec.decodeAndValidate(BackupCodec.encode(source))

        assertEquals(source.groups, decoded.groups)
        assertEquals(source.schedules, decoded.schedules)
    }

    @Test
    fun missingForeignKeyIsRejectedBeforeImport() {
        val invalid = validDocument().copy(
            subjects = validDocument().subjects.map { it.copy(academicGroupId = 999) },
        )

        assertThrows(BackupValidationException::class.java) { BackupCodec.validate(invalid) }
    }

    @Test
    fun malformedJsonIsRejected() {
        assertThrows(BackupValidationException::class.java) {
            BackupCodec.decodeAndValidate("{not-json}")
        }
    }

    @Test
    fun legacyStudentWorkspaceImportsAsTeacherWithoutLosingSchedules() {
        val legacy = validDocument().copy(
            schedules = validDocument().schedules.map { it.copy(mode = "STUDENT") },
            preferences = validDocument().preferences.copy(selectedMode = "STUDENT"),
        )

        BackupCodec.validate(legacy)
        val schedules = with(BackupCodec) { legacy.toSchedules() }
        val preferences = with(BackupCodec) { legacy.toPreferences() }

        assertEquals(UserMode.TEACHER, schedules.single().mode)
        assertEquals(UserMode.TEACHER, preferences.selectedMode)
    }

    private fun validDocument(): BackupDocument {
        val timestamp = "2026-01-01T00:00:00Z"
        return BackupDocument(
            schemaVersion = 1,
            exportedAt = timestamp,
            groups = listOf(GroupBackup(1, "MSc Zoology", "Semester I", null, null, timestamp, timestamp)),
            subjects = listOf(SubjectBackup(1, 1, "Animal Physiology", null, timestamp, timestamp)),
            schedules = listOf(
                ScheduleBackup(
                    id = 1,
                    mode = "TEACHER",
                    academicGroupId = 1,
                    subjectId = 1,
                    dayOfWeek = 1,
                    startTime = "09:00",
                    endTime = "10:00",
                    recurrenceType = "WEEKLY",
                    reminderEnabled = true,
                    reminderMinutes = 30,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            ),
            exceptions = emptyList(),
            preferences = PreferencesBackup("TEACHER", 30, true, "SYSTEM", 1, "SYSTEM"),
        )
    }
}
