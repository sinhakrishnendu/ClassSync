package com.classsync.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterRoutineDao {
    @Transaction
    @Query("SELECT * FROM master_routines ORDER BY updatedAt DESC")
    fun observeAllRecords(): Flow<List<MasterRoutineRecord>>

    @Transaction
    @Query("SELECT * FROM master_routines WHERE id = :id")
    fun observeRecord(id: String): Flow<MasterRoutineRecord?>

    @Transaction
    @Query("SELECT * FROM master_routines WHERE id = :id")
    suspend fun getRecord(id: String): MasterRoutineRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRoutine(entity: MasterRoutineEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkingDays(entities: List<MasterWorkingDayEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPeriods(entities: List<MasterPeriodEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClasses(entities: List<MasterAcademicClassEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTeachers(entities: List<MasterTeacherEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSubjects(entities: List<MasterSubjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAssignments(entities: List<MasterTeacherAssignmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTeacherAvailability(entities: List<MasterTeacherAvailabilityEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntries(entities: List<MasterTimetableEntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGenerationRuns(entities: List<MasterGenerationRunEntity>)

    @Query("DELETE FROM master_timetable_entries WHERE masterRoutineId = :id") suspend fun deleteEntries(id: String)
    @Query("DELETE FROM master_generation_runs WHERE masterRoutineId = :id") suspend fun deleteGenerationRuns(id: String)
    @Query("DELETE FROM master_teacher_availability WHERE masterRoutineId = :id") suspend fun deleteAvailability(id: String)
    @Query("DELETE FROM master_assignments WHERE masterRoutineId = :id") suspend fun deleteAssignments(id: String)
    @Query("DELETE FROM master_subjects WHERE masterRoutineId = :id") suspend fun deleteSubjects(id: String)
    @Query("DELETE FROM master_teachers WHERE masterRoutineId = :id") suspend fun deleteTeachers(id: String)
    @Query("DELETE FROM master_classes WHERE masterRoutineId = :id") suspend fun deleteClasses(id: String)
    @Query("DELETE FROM master_periods WHERE masterRoutineId = :id") suspend fun deletePeriods(id: String)
    @Query("DELETE FROM master_working_days WHERE masterRoutineId = :id") suspend fun deleteWorkingDays(id: String)
    @Query("DELETE FROM master_routines WHERE id = :id") suspend fun deleteRoutine(id: String)
    @Query("DELETE FROM master_routines") suspend fun deleteAllRoutines()
}
