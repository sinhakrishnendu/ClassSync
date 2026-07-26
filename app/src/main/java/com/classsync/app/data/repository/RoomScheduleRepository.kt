package com.classsync.app.data.repository

import androidx.room.withTransaction
import com.classsync.app.data.local.AcademicGroupDao
import com.classsync.app.data.local.AcademicGroupEntity
import com.classsync.app.data.local.ClassScheduleDao
import com.classsync.app.data.local.ClassScheduleEntity
import com.classsync.app.data.local.ClassSyncDatabase
import com.classsync.app.data.local.ScheduleExceptionDao
import com.classsync.app.data.local.ScheduleExceptionEntity
import com.classsync.app.data.local.SubjectDao
import com.classsync.app.data.local.SubjectEntity
import com.classsync.app.data.local.toDomain
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.TimeProvider
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomScheduleRepository @Inject constructor(
    private val database: ClassSyncDatabase,
    private val groupDao: AcademicGroupDao,
    private val subjectDao: SubjectDao,
    private val scheduleDao: ClassScheduleDao,
    private val exceptionDao: ScheduleExceptionDao,
    private val timeProvider: TimeProvider,
) : ScheduleRepository {
    override fun observeEntries(mode: com.classsync.app.domain.model.UserMode): Flow<List<ClassEntry>> =
        scheduleDao.observeByMode(mode).map { records -> records.map { it.toDomain() } }

    override fun observeAllEntries(): Flow<List<ClassEntry>> =
        scheduleDao.observeAllRecords().map { records -> records.map { it.toDomain() } }

    override fun observeEntry(id: Long): Flow<ClassEntry?> =
        scheduleDao.observeRecord(id).map { it?.toDomain() }

    override fun observeGroups(): Flow<List<AcademicGroup>> =
        groupDao.observeAll().map { groups -> groups.map { it.toDomain() } }

    override suspend fun getEntry(id: Long): ClassEntry? = scheduleDao.getRecord(id)?.toDomain()

    override suspend fun getAllEntries(): List<ClassEntry> = scheduleDao.getAllRecords().map { it.toDomain() }

    override suspend fun saveSchedule(draft: ScheduleDraft): Long = database.withTransaction {
        val now = timeProvider.now().toInstant()
        val programme = draft.programme.trim()
        val semester = draft.semester.trim()
        val batch = draft.batchSection.normalizedOrNull()
        val institution = draft.institution.normalizedOrNull()
        val group = groupDao.findMatching(programme, semester, batch, institution)
        val groupId = group?.id ?: groupDao.insert(
            AcademicGroupEntity(
                programme = programme,
                semester = semester,
                batchSection = batch,
                institution = institution,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val subjectName = draft.subjectName.trim()
        val subject = subjectDao.findMatching(groupId, subjectName)
        val subjectId = subject?.id ?: subjectDao.insert(
            SubjectEntity(
                academicGroupId = groupId,
                name = subjectName,
                code = draft.subjectCode.normalizedOrNull(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        val existing = draft.id.takeIf { it > 0 }?.let { scheduleDao.getRecord(it)?.schedule }
        val entity = ClassScheduleEntity(
            id = draft.id,
            mode = draft.mode,
            academicGroupId = groupId,
            subjectId = subjectId,
            dayOfWeek = draft.oneTimeDate?.dayOfWeek ?: draft.dayOfWeek,
            startTime = draft.startTime,
            endTime = draft.endTime,
            classroom = draft.classroom.normalizedOrNull(),
            topic = draft.topic.normalizedOrNull(),
            teacherName = draft.teacherName.normalizedOrNull(),
            notes = draft.notes.normalizedOrNull(),
            recurrenceType = draft.recurrenceType,
            oneTimeDate = draft.oneTimeDate,
            reminderEnabled = draft.reminderEnabled,
            reminderMinutes = draft.reminderMinutes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (draft.id > 0) {
            check(existing != null) { "Schedule ${draft.id} does not exist" }
            scheduleDao.update(entity)
            draft.id
        } else {
            scheduleDao.insert(entity)
        }
    }

    override suspend fun duplicateSchedule(id: Long): Long {
        val entry = requireNotNull(getEntry(id)) { "Schedule $id does not exist" }
        return saveSchedule(
            ScheduleDraft(
                mode = entry.schedule.mode,
                programme = entry.group.programme,
                semester = entry.group.semester,
                batchSection = entry.group.batchSection,
                institution = entry.group.institution,
                subjectName = entry.subject.name,
                subjectCode = entry.subject.code,
                dayOfWeek = entry.schedule.dayOfWeek,
                startTime = entry.schedule.startTime,
                endTime = entry.schedule.endTime,
                classroom = entry.schedule.classroom,
                topic = entry.schedule.topic,
                teacherName = entry.schedule.teacherName,
                notes = entry.schedule.notes,
                recurrenceType = entry.schedule.recurrenceType,
                oneTimeDate = entry.schedule.oneTimeDate,
                reminderEnabled = entry.schedule.reminderEnabled,
                reminderMinutes = entry.schedule.reminderMinutes,
            ),
        )
    }

    override suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)

    override suspend fun setReminderEnabled(id: Long, enabled: Boolean) =
        scheduleDao.setReminderEnabled(id, enabled, timeProvider.now().toInstant())

    override suspend fun saveGroup(group: AcademicGroup): Long {
        val now = timeProvider.now().toInstant()
        val entity = AcademicGroupEntity(
            id = group.id,
            programme = group.programme.trim(),
            semester = group.semester.trim(),
            batchSection = group.batchSection.normalizedOrNull(),
            institution = group.institution.normalizedOrNull(),
            createdAt = if (group.id == 0L) now else group.createdAt,
            updatedAt = now,
        )
        return if (group.id == 0L) groupDao.insert(entity) else {
            groupDao.update(entity)
            group.id
        }
    }

    override suspend fun deleteGroup(id: Long) {
        groupDao.getById(id)?.let { groupDao.delete(it) }
    }

    override suspend fun setException(
        scheduleId: Long,
        date: LocalDate,
        status: ExceptionStatus,
        changedStartTime: LocalTime?,
        changedEndTime: LocalTime?,
        notes: String?,
    ) {
        require(status != ExceptionStatus.RESCHEDULED ||
            (changedStartTime != null && changedEndTime != null && changedEndTime.isAfter(changedStartTime))) {
            "Rescheduled classes require a valid start and end time"
        }
        val now = timeProvider.now().toInstant()
        val existing = exceptionDao.getForDate(scheduleId, date)
        exceptionDao.insert(
            ScheduleExceptionEntity(
                id = existing?.id ?: 0,
                classScheduleId = scheduleId,
                relevantDate = date,
                status = status,
                changedStartTime = changedStartTime,
                changedEndTime = changedEndTime,
                notes = notes.normalizedOrNull(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteAll() = database.withTransaction {
        exceptionDao.deleteAll()
        scheduleDao.deleteAll()
        subjectDao.deleteAll()
        groupDao.deleteAll()
    }

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
