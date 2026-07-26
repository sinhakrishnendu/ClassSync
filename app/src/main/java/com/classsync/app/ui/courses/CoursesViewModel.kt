package com.classsync.app.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CourseEvent {
    data object Saved : CourseEvent
    data object Deleted : CourseEvent
    data object DeleteFailed : CourseEvent
    data object SaveFailed : CourseEvent
}

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    val groups: StateFlow<List<AcademicGroup>> = scheduleRepository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val eventChannel = Channel<CourseEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun save(
        existing: AcademicGroup?,
        programme: String,
        semester: String,
        batchSection: String,
        institution: String,
    ) {
        if (programme.isBlank() || semester.isBlank()) return
        viewModelScope.launch {
            runCatching {
                scheduleRepository.saveGroup(
                    AcademicGroup(
                        id = existing?.id ?: 0,
                        programme = programme,
                        semester = semester,
                        batchSection = batchSection,
                        institution = institution,
                        createdAt = existing?.createdAt ?: Instant.EPOCH,
                        updatedAt = existing?.updatedAt ?: Instant.EPOCH,
                    ),
                )
            }.onSuccess { eventChannel.send(CourseEvent.Saved) }
                .onFailure { eventChannel.send(CourseEvent.SaveFailed) }
        }
    }

    fun delete(group: AcademicGroup) {
        viewModelScope.launch {
            runCatching { scheduleRepository.deleteGroup(group.id) }
                .onSuccess { eventChannel.send(CourseEvent.Deleted) }
                .onFailure { eventChannel.send(CourseEvent.DeleteFailed) }
        }
    }
}

