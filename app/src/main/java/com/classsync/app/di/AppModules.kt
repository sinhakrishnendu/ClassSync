package com.classsync.app.di

import android.content.Context
import androidx.room.Room
import com.classsync.app.data.local.AcademicGroupDao
import com.classsync.app.data.local.ClassScheduleDao
import com.classsync.app.data.local.ClassSyncDatabase
import com.classsync.app.data.local.ScheduleExceptionDao
import com.classsync.app.data.local.SubjectDao
import com.classsync.app.data.local.MasterRoutineDao
import com.classsync.app.data.preferences.DataStorePreferencesRepository
import com.classsync.app.data.repository.RoomScheduleRepository
import com.classsync.app.data.repository.RoomMasterRoutineRepository
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.repository.MasterRoutineRepository
import com.classsync.app.domain.time.SystemTimeProvider
import com.classsync.app.domain.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClassSyncDatabase =
        Room.databaseBuilder(context, ClassSyncDatabase::class.java, "classsync.db").build()

    @Provides fun provideGroupDao(database: ClassSyncDatabase): AcademicGroupDao = database.academicGroupDao()
    @Provides fun provideSubjectDao(database: ClassSyncDatabase): SubjectDao = database.subjectDao()
    @Provides fun provideScheduleDao(database: ClassSyncDatabase): ClassScheduleDao = database.classScheduleDao()
    @Provides fun provideExceptionDao(database: ClassSyncDatabase): ScheduleExceptionDao = database.scheduleExceptionDao()
    @Provides fun provideMasterRoutineDao(database: ClassSyncDatabase): MasterRoutineDao = database.masterRoutineDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds abstract fun bindScheduleRepository(implementation: RoomScheduleRepository): ScheduleRepository
    @Binds abstract fun bindPreferencesRepository(implementation: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun bindMasterRoutineRepository(implementation: RoomMasterRoutineRepository): MasterRoutineRepository
    @Binds abstract fun bindTimeProvider(implementation: SystemTimeProvider): TimeProvider
}
