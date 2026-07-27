package com.classsync.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val showValidation: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableState.asStateFlow()
    private val completedChannel = Channel<Unit>(Channel.BUFFERED)
    val completed = completedChannel.receiveAsFlow()

    fun selectMode(mode: UserMode) = mutableState.update { it.copy(selectedMode = mode, showValidation = false, saveFailed = false) }
    fun continueOnboarding() {
        val state = mutableState.value
        val mode = state.selectedMode ?: return mutableState.update { it.copy(showValidation = true) }
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, saveFailed = false) }
            runCatching {
                preferencesRepository.completeOnboarding(mode)
            }.onSuccess {
                completedChannel.send(Unit)
            }.onFailure {
                mutableState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
