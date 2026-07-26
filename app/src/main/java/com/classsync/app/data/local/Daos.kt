package com.classsync.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.classsync.app.domain.model.UserMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicGroupDao {
    @Query("SELECT * FROM academic_groups ORDER BY programme COLLATE NOCASE, semester COLLATE NOCASE, batchSection COLLATE NOCASE")
    fun observeAll(): Flow<List<AcademicGroupEntity>>

    @Query("SELECT * FROM academic_groups ORDER BY id")
    suspend fun getAll(): List<AcademicGroupEntity>

    @Query(
        """SELECT * FROM academic_groups
           WHERE lower(programme) = lower(:programme)
             AND lower(semester) = lower(:semester)
             AND lower(COALESCE(batchSection, '')) = lower(COALESCE(:batchSection, ''))
             AND lower(COALESCE(institution, '')) = lower(COALESCE(:institution, ''))
           LIMIT 1""",
    )
    suspend fun findMatching(
        programme: String,
        semester: String,
        batchSection: String?,
        institution: String?,
    ): AcademicGroupEntity?

    @Insert suspend fun insert(entity: AcademicGroupEntity): Long
    @Insert suspend fun insertAll(entities: List<AcademicGroupEntity>)
    @Update suspend fun update(entity: AcademicGroupEntity)
    @Delete suspend fun delete(entity: AcademicGroupEntity)
    @Query("SELECT * FROM academic_groups WHERE id = :id") suspend fun getById(id: Long): AcademicGroupEntity?
    @Query("DELETE FROM academic_groups") suspend fun deleteAll()
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id") suspend fun getAll(): List<SubjectEntity>
    @Query("SELECT * FROM subjects WHERE academicGroupId = :groupId AND lower(name) = lower(:name) LIMIT 1")
    suspend fun findMatching(groupId: Long, name: String): SubjectEntity?
    @Insert suspend fun insert(entity: SubjectEntity): Long
    @Insert suspend fun insertAll(entities: List<SubjectEntity>)
    @Query("DELETE FROM subjects") suspend fun deleteAll()
}

@Dao
interface ClassScheduleDao {
    @Transaction
    @Query("SELECT * FROM class_schedules WHERE mode = :mode ORDER BY dayOfWeek, startTime")
    fun observeByMode(mode: UserMode): Flow<List<ClassEntryRecord>>

    @Transaction
    @Query("SELECT * FROM class_schedules ORDER BY mode, dayOfWeek, startTime")
    fun observeAllRecords(): Flow<List<ClassEntryRecord>>

    @Transaction
    @Query("SELECT * FROM class_schedules WHERE id = :id")
    fun observeRecord(id: Long): Flow<ClassEntryRecord?>

    @Transaction
    @Query("SELECT * FROM class_schedules WHERE id = :id")
    suspend fun getRecord(id: Long): ClassEntryRecord?

    @Transaction
    @Query("SELECT * FROM class_schedules ORDER BY id")
    suspend fun getAllRecords(): List<ClassEntryRecord>

    @Query("SELECT * FROM class_schedules ORDER BY id")
    suspend fun getAll(): List<ClassScheduleEntity>

    @Insert suspend fun insert(entity: ClassScheduleEntity): Long
    @Insert suspend fun insertAll(entities: List<ClassScheduleEntity>)
    @Update suspend fun update(entity: ClassScheduleEntity)
    @Query("DELETE FROM class_schedules WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("UPDATE class_schedules SET reminderEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setReminderEnabled(id: Long, enabled: Boolean, updatedAt: java.time.Instant)
    @Query("DELETE FROM class_schedules") suspend fun deleteAll()
}

@Dao
interface ScheduleExceptionDao {
    @Query("SELECT * FROM schedule_exceptions ORDER BY id") suspend fun getAll(): List<ScheduleExceptionEntity>
    @Query("SELECT * FROM schedule_exceptions WHERE classScheduleId = :scheduleId AND relevantDate = :date LIMIT 1")
    suspend fun getForDate(scheduleId: Long, date: LocalDate): ScheduleExceptionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: ScheduleExceptionEntity): Long
    @Insert suspend fun insertAll(entities: List<ScheduleExceptionEntity>)
    @Query("DELETE FROM schedule_exceptions") suspend fun deleteAll()
}

