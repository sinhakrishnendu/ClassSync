package com.classsync.app.domain.repository

import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeEntries(mode: UserMode): Flow<List<ClassEntry>>
    fun observeAllEntries(): Flow<List<ClassEntry>>
    fun observeEntry(id: Long): Flow<ClassEntry?>
    fun observeGroups(): Flow<List<AcademicGroup>>
    suspend fun getEntry(id: Long): ClassEntry?
    suspend fun getAllEntries(): List<ClassEntry>
    suspend fun saveSchedule(draft: ScheduleDraft): Long
    suspend fun duplicateSchedule(id: Long): Long
    suspend fun deleteSchedule(id: Long)
    suspend fun setReminderEnabled(id: Long, enabled: Boolean)
    suspend fun saveGroup(group: AcademicGroup): Long
    suspend fun deleteGroup(id: Long)
    suspend fun setException(
        scheduleId: Long,
        date: LocalDate,
        status: ExceptionStatus,
        changedStartTime: LocalTime? = null,
        changedEndTime: LocalTime? = null,
        notes: String? = null,
    )
    suspend fun deleteAll()
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun completeOnboarding(mode: UserMode)
    suspend fun setMode(mode: UserMode)
    suspend fun setDefaultReminder(minutes: Int)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setWeekStartDay(day: DayOfWeek)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun replace(preferences: UserPreferences)
}

