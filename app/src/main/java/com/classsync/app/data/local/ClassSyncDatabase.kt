package com.classsync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AcademicGroupEntity::class,
        SubjectEntity::class,
        ClassScheduleEntity::class,
        ScheduleExceptionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ClassSyncConverters::class)
abstract class ClassSyncDatabase : RoomDatabase() {
    abstract fun academicGroupDao(): AcademicGroupDao
    abstract fun subjectDao(): SubjectDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun scheduleExceptionDao(): ScheduleExceptionDao
}

