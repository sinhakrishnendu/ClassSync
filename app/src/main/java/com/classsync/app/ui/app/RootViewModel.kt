package com.classsync.app.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RootUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
)

@HiltViewModel
class RootViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<RootUiState> = preferencesRepository.preferences
        .map { RootUiState(isLoading = false, preferences = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())
}

