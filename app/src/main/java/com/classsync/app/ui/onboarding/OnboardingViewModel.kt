package com.classsync.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.repository.PreferencesRepository
import com.classsync.app.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedMode: UserMode? = null,
    val institution: String = "",
    val programme: String = "",
    val semester: String = "",
    val batchSection: String = "",
    val showValidation: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableState.asStateFlow()
    private val completedChannel = Channel<Unit>(Channel.BUFFERED)
    val completed = completedChannel.receiveAsFlow()

    fun selectMode(mode: UserMode) = mutableState.update { it.copy(selectedMode = mode, showValidation = false, saveFailed = false) }
    fun setInstitution(value: String) = mutableState.update { it.copy(institution = value) }
    fun setProgramme(value: String) = mutableState.update { it.copy(programme = value) }
    fun setSemester(value: String) = mutableState.update { it.copy(semester = value) }
    fun setBatchSection(value: String) = mutableState.update { it.copy(batchSection = value) }

    fun continueOnboarding() {
        val state = mutableState.value
        val mode = state.selectedMode ?: return mutableState.update { it.copy(showValidation = true) }
        if (mode == UserMode.STUDENT && (state.programme.isBlank() || state.semester.isBlank())) {
            mutableState.update { it.copy(showValidation = true) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, saveFailed = false) }
            runCatching {
                if (mode == UserMode.STUDENT) {
                    scheduleRepository.saveGroup(
                        AcademicGroup(
                            id = 0,
                            programme = state.programme,
                            semester = state.semester,
                            batchSection = state.batchSection,
                            institution = state.institution,
                            createdAt = Instant.EPOCH,
                            updatedAt = Instant.EPOCH,
                        ),
                    )
                }
                preferencesRepository.completeOnboarding(mode)
            }.onSuccess {
                completedChannel.send(Unit)
            }.onFailure {
                mutableState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
