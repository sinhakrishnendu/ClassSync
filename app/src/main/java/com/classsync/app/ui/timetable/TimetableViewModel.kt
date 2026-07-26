package com.classsync.app.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import com.classsync.app.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class TimetableViewMode { DAY, COURSE }

data class TimetableFilters(
    val selectedDay: DayOfWeek,
    val selectedGroupId: Long? = null,
    val query: String = "",
    val viewMode: TimetableViewMode = TimetableViewMode.DAY,
)

data class TimetableUiState(
    val isLoading: Boolean = true,
    val mode: UserMode = UserMode.TEACHER,
    val preferences: UserPreferences = UserPreferences(),
    val entries: List<ClassEntry> = emptyList(),
    val groups: List<AcademicGroup> = emptyList(),
    val filters: TimetableFilters = TimetableFilters(DayOfWeek.MONDAY),
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    preferencesRepository: PreferencesRepository,
    timeProvider: TimeProvider,
) : ViewModel() {
    private val filters = MutableStateFlow(TimetableFilters(timeProvider.now().dayOfWeek))

    val uiState: StateFlow<TimetableUiState> = preferencesRepository.preferences
        .flatMapLatest { preferences ->
            combine(
                scheduleRepository.observeEntries(preferences.selectedMode),
                scheduleRepository.observeGroups(),
                filters,
            ) { entries, groups, selectedFilters ->
                val query = selectedFilters.query.trim()
                val visible = entries.filter { entry ->
                    (selectedFilters.selectedGroupId == null || entry.group.id == selectedFilters.selectedGroupId) &&
                        (query.isEmpty() || listOf(
                            entry.subject.name,
                            entry.subject.code,
                            entry.group.programme,
                            entry.group.semester,
                            entry.group.batchSection,
                            entry.schedule.classroom,
                            entry.schedule.teacherName,
                            entry.schedule.topic,
                            entry.schedule.notes,
                            entry.schedule.dayOfWeek.name,
                        ).any { it?.contains(query, ignoreCase = true) == true }) &&
                        (selectedFilters.viewMode == TimetableViewMode.COURSE ||
                            entry.schedule.dayOfWeek == selectedFilters.selectedDay)
                }.sortedWith(compareBy({ it.schedule.dayOfWeek.value }, { it.schedule.startTime }))
                TimetableUiState(false, preferences.selectedMode, preferences, visible, groups, selectedFilters)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TimetableUiState(filters = filters.value),
        )

    fun selectDay(day: DayOfWeek) = filters.update { it.copy(selectedDay = day) }
    fun selectGroup(id: Long?) = filters.update { it.copy(selectedGroupId = id) }
    fun setQuery(value: String) = filters.update { it.copy(query = value) }
    fun setViewMode(mode: TimetableViewMode) = filters.update { it.copy(viewMode = mode) }
}
