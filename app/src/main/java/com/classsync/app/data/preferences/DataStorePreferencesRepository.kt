package com.classsync.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.classsync.app.domain.model.ThemePreference
import com.classsync.app.domain.model.TimeFormat
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.classSyncDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesRepository {
    private val dataStore = context.classSyncDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::toPreferences)

    override suspend fun completeOnboarding(mode: UserMode) = update {
        it[Keys.SelectedMode] = mode.name
        it[Keys.OnboardingComplete] = true
    }

    override suspend fun setMode(mode: UserMode) = update { it[Keys.SelectedMode] = mode.name }
    override suspend fun setDefaultReminder(minutes: Int) = update { it[Keys.DefaultReminderMinutes] = minutes.coerceAtLeast(1) }
    override suspend fun setRemindersEnabled(enabled: Boolean) = update { it[Keys.RemindersEnabled] = enabled }
    override suspend fun setTheme(theme: ThemePreference) = update { it[Keys.Theme] = theme.name }
    override suspend fun setWeekStartDay(day: DayOfWeek) = update { it[Keys.WeekStartDay] = day.value }
    override suspend fun setTimeFormat(format: TimeFormat) = update { it[Keys.TimeFormat] = format.name }

    override suspend fun replace(preferences: UserPreferences) {
        dataStore.edit { values ->
            values.clear()
            values[Keys.SelectedMode] = preferences.selectedMode.name
            values[Keys.OnboardingComplete] = true
            values[Keys.DefaultReminderMinutes] = preferences.defaultReminderMinutes.coerceAtLeast(1)
            values[Keys.RemindersEnabled] = preferences.remindersEnabled
            values[Keys.Theme] = preferences.themePreference.name
            values[Keys.WeekStartDay] = preferences.weekStartDay.value
            values[Keys.TimeFormat] = preferences.timeFormat.name
        }
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private fun toPreferences(values: Preferences): UserPreferences = UserPreferences(
        selectedMode = enumValueOrDefault(values[Keys.SelectedMode], UserMode.TEACHER),
        onboardingComplete = values[Keys.OnboardingComplete] ?: false,
        defaultReminderMinutes = (values[Keys.DefaultReminderMinutes] ?: 30).coerceAtLeast(1),
        remindersEnabled = values[Keys.RemindersEnabled] ?: true,
        themePreference = enumValueOrDefault(values[Keys.Theme], ThemePreference.SYSTEM),
        weekStartDay = runCatching { DayOfWeek.of(values[Keys.WeekStartDay] ?: 1) }.getOrDefault(DayOfWeek.MONDAY),
        timeFormat = enumValueOrDefault(values[Keys.TimeFormat], TimeFormat.SYSTEM),
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private object Keys {
        val SelectedMode = stringPreferencesKey("selected_mode")
        val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
        val DefaultReminderMinutes = intPreferencesKey("default_reminder_minutes")
        val RemindersEnabled = booleanPreferencesKey("reminders_enabled")
        val Theme = stringPreferencesKey("theme_preference")
        val WeekStartDay = intPreferencesKey("week_start_day")
        val TimeFormat = stringPreferencesKey("time_format")
    }
}
