package com.classsync.app.data.backup

import androidx.room.withTransaction
import com.classsync.app.data.local.AcademicGroupDao
import com.classsync.app.data.local.ClassScheduleDao
import com.classsync.app.data.local.ClassSyncDatabase
import com.classsync.app.data.local.ScheduleExceptionDao
import com.classsync.app.data.local.SubjectDao
import com.classsync.app.domain.repository.PreferencesRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackupManager @Inject constructor(
    private val database: ClassSyncDatabase,
    private val groupDao: AcademicGroupDao,
    private val subjectDao: SubjectDao,
    private val scheduleDao: ClassScheduleDao,
    private val exceptionDao: ScheduleExceptionDao,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend fun exportJson(): String {
        val preferences = preferencesRepository.preferences.first()
        val document = BackupDocument(
            schemaVersion = BackupCodec.SchemaVersion,
            exportedAt = Instant.now().toString(),
            groups = groupDao.getAll().map {
                GroupBackup(it.id, it.programme, it.semester, it.batchSection, it.institution, it.createdAt.toString(), it.updatedAt.toString())
            },
            subjects = subjectDao.getAll().map {
                SubjectBackup(it.id, it.academicGroupId, it.name, it.code, it.createdAt.toString(), it.updatedAt.toString())
            },
            schedules = scheduleDao.getAll().map {
                ScheduleBackup(
                    id = it.id,
                    mode = it.mode.name,
                    academicGroupId = it.academicGroupId,
                    subjectId = it.subjectId,
                    dayOfWeek = it.dayOfWeek.value,
                    startTime = it.startTime.toString(),
                    endTime = it.endTime.toString(),
                    classroom = it.classroom,
                    topic = it.topic,
                    teacherName = it.teacherName,
                    notes = it.notes,
                    recurrenceType = it.recurrenceType.name,
                    oneTimeDate = it.oneTimeDate?.toString(),
                    reminderEnabled = it.reminderEnabled,
                    reminderMinutes = it.reminderMinutes,
                    createdAt = it.createdAt.toString(),
                    updatedAt = it.updatedAt.toString(),
                )
            },
            exceptions = exceptionDao.getAll().map {
                ExceptionBackup(
                    it.id,
                    it.classScheduleId,
                    it.relevantDate.toString(),
                    it.status.name,
                    it.changedStartTime?.toString(),
                    it.changedEndTime?.toString(),
                    it.notes,
                    it.createdAt.toString(),
                    it.updatedAt.toString(),
                )
            },
            preferences = PreferencesBackup(
                selectedMode = preferences.selectedMode.name,
                defaultReminderMinutes = preferences.defaultReminderMinutes,
                remindersEnabled = preferences.remindersEnabled,
                themePreference = preferences.themePreference.name,
                weekStartDay = preferences.weekStartDay.value,
                timeFormat = preferences.timeFormat.name,
            ),
        )
        return BackupCodec.encode(document)
    }

    suspend fun importJson(source: String) {
        val document = BackupCodec.decodeAndValidate(source)
        database.withTransaction {
            exceptionDao.deleteAll()
            scheduleDao.deleteAll()
            subjectDao.deleteAll()
            groupDao.deleteAll()
            groupDao.insertAll(with(BackupCodec) { document.toGroups() })
            subjectDao.insertAll(with(BackupCodec) { document.toSubjects() })
            scheduleDao.insertAll(with(BackupCodec) { document.toSchedules() })
            exceptionDao.insertAll(with(BackupCodec) { document.toExceptions() })
        }
        preferencesRepository.replace(with(BackupCodec) { document.toPreferences() })
    }
}

