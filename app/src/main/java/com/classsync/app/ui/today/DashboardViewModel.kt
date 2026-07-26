package com.classsync.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.ScheduleOccurrence
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.ScheduleCalculator
import com.classsync.app.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val isLoading: Boolean = true,
    val mode: UserMode = UserMode.TEACHER,
    val preferences: UserPreferences = UserPreferences(),
    val now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(),
    val today: List<ScheduleOccurrence> = emptyList(),
    val next: ScheduleOccurrence? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    preferencesRepository: PreferencesRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val ticks: Flow<java.time.ZonedDateTime> = flow {
        while (true) {
            emit(timeProvider.now())
            delay(30_000)
        }
    }

    val uiState: StateFlow<DashboardUiState> = preferencesRepository.preferences
        .flatMapLatest { preferences ->
            combine(scheduleRepository.observeEntries(preferences.selectedMode), ticks) { entries, now ->
                DashboardUiState(
                    isLoading = false,
                    mode = preferences.selectedMode,
                    preferences = preferences,
                    now = now,
                    today = ScheduleCalculator.occurrencesForDate(
                        entries,
                        now.toLocalDate(),
                        now.zone,
                        includeInactive = true,
                    ),
                    next = ScheduleCalculator.nextOccurrence(entries, now),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}

