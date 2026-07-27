package com.classsync.app.data.local

import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AcademicGroupEntity::class,
        SubjectEntity::class,
        ClassScheduleEntity::class,
        ScheduleExceptionEntity::class,
        MasterRoutineEntity::class,
        MasterWorkingDayEntity::class,
        MasterPeriodEntity::class,
        MasterAcademicClassEntity::class,
        MasterTeacherEntity::class,
        MasterSubjectEntity::class,
        MasterTeacherAssignmentEntity::class,
        MasterTeacherAvailabilityEntity::class,
        MasterTimetableEntryEntity::class,
        MasterGenerationRunEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(ClassSyncConverters::class)
abstract class ClassSyncDatabase : RoomDatabase() {
    abstract fun academicGroupDao(): AcademicGroupDao
    abstract fun subjectDao(): SubjectDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun scheduleExceptionDao(): ScheduleExceptionDao
    abstract fun masterRoutineDao(): MasterRoutineDao
}
