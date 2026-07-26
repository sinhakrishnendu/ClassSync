package com.classsync.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.hilt.navigation.compose.hiltViewModel
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
            selected = state.selectedMode == UserMode.STUDENT,
            title = stringResource(R.string.student_mode),
            description = stringResource(R.string.student_mode_description),
            icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
            onClick = { viewModel.selectMode(UserMode.STUDENT) },
        )

        if (state.selectedMode == UserMode.STUDENT) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.student_setup_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.student_setup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.institution,
                onValueChange = viewModel::setInstitution,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.institution)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.programme,
                onValueChange = viewModel::setProgramme,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.programme)) },
                supportingText = if (state.showValidation && state.programme.isBlank()) {
                    { Text(stringResource(R.string.required_field)) }
                } else null,
                isError = state.showValidation && state.programme.isBlank(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.semester,
                onValueChange = viewModel::setSemester,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.semester)) },
                supportingText = if (state.showValidation && state.semester.isBlank()) {
                    { Text(stringResource(R.string.required_field)) }
                } else null,
                isError = state.showValidation && state.semester.isBlank(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.batchSection,
                onValueChange = viewModel::setBatchSection,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.batch_section)) },
                singleLine = true,
            )
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
