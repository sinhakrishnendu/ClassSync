package com.classsync.app.data.repository

import androidx.room.withTransaction
import com.classsync.app.data.local.ClassSyncDatabase
import com.classsync.app.data.local.MasterRoutineDao
import com.classsync.app.data.local.toDomain
import com.classsync.app.data.local.toEntity
import com.classsync.app.domain.master.ConstraintValidator
import com.classsync.app.domain.master.GenerationIssueSeverity
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterRoutineSummary
import com.classsync.app.domain.repository.MasterRoutineRepository
import com.classsync.app.domain.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomMasterRoutineRepository @Inject constructor(
    private val database: ClassSyncDatabase,
    private val dao: MasterRoutineDao,
    private val timeProvider: TimeProvider,
) : MasterRoutineRepository {
    override fun observeSummaries(): Flow<List<MasterRoutineSummary>> = dao.observeAllRecords().map { records ->
        records.map { record ->
            val data = record.toDomain()
            MasterRoutineSummary(
                id = data.routine.id,
                title = data.routine.title,
                institutionName = data.routine.institutionName,
                status = data.routine.status,
                currentStep = data.routine.currentStep,
                classCount = data.classes.size,
                teacherCount = data.teachers.size,
                subjectCount = data.subjects.size,
                unresolvedConflicts = ConstraintValidator.validateTimetable(data).count {
                    it.severity == GenerationIssueSeverity.ERROR
                },
                updatedAt = data.routine.updatedAt,
            )
        }
    }

    override fun observeRoutine(id: String): Flow<MasterRoutineData?> =
        dao.observeRecord(id).map { it?.toDomain() }

    override suspend fun getRoutine(id: String): MasterRoutineData? = dao.getRecord(id)?.toDomain()

    override suspend fun save(data: MasterRoutineData) = database.withTransaction {
        val normalized = data.copy(routine = data.routine.copy(updatedAt = timeProvider.now().toInstant()))
        dao.insertRoutine(normalized.routine.toEntity())
        dao.deleteEntries(normalized.routine.id)
        dao.deleteGenerationRuns(normalized.routine.id)
        dao.deleteAvailability(normalized.routine.id)
        dao.deleteAssignments(normalized.routine.id)
        dao.deleteSubjects(normalized.routine.id)
        dao.deleteTeachers(normalized.routine.id)
        dao.deleteClasses(normalized.routine.id)
        dao.deletePeriods(normalized.routine.id)
        dao.deleteWorkingDays(normalized.routine.id)
        dao.insertWorkingDays(normalized.workingDays.map { it.toEntity() })
        dao.insertPeriods(normalized.periods.map { it.toEntity() })
        dao.insertClasses(normalized.classes.map { it.toEntity() })
        dao.insertTeachers(normalized.teachers.map { it.toEntity() })
        dao.insertSubjects(normalized.subjects.map { it.toEntity() })
        dao.insertAssignments(normalized.assignments.map { it.toEntity() })
        dao.insertTeacherAvailability(normalized.teacherAvailability.map { it.toEntity() })
        dao.insertEntries(normalized.entries.map { it.toEntity() })
        dao.insertGenerationRuns(normalized.generationRuns.map { it.toEntity() })
    }

    override suspend fun delete(id: String) = dao.deleteRoutine(id)
    override suspend fun deleteAll() = dao.deleteAllRoutines()
}
