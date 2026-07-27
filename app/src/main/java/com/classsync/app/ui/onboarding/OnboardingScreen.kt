package com.classsync.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.domain.model.UserMode

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.completed.collect { onCompleted() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Outlined.Today,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.onboarding_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ModeCard(
            selected = state.selectedMode == UserMode.TEACHER,
            title = stringResource(R.string.teacher_mode),
            description = stringResource(R.string.teacher_mode_description),
            icon = { Icon(Icons.Outlined.School, contentDescription = null) },
            onClick = { viewModel.selectMode(UserMode.TEACHER) },
        )
        ModeCard(
            selected = state.selectedMode == UserMode.ADMINISTRATION,
            title = stringResource(R.string.administration_mode),
            description = stringResource(R.string.administration_mode_description),
            icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
            onClick = { viewModel.selectMode(UserMode.ADMINISTRATION) },
        )
        if (state.showValidation && state.selectedMode == null) {
            Text(stringResource(R.string.choose_workspace), color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = viewModel::continueOnboarding,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.isSaving) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.continue_action))
        }
        if (state.saveFailed) {
            Text(stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ModeCard(
    selected: Boolean,
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}
