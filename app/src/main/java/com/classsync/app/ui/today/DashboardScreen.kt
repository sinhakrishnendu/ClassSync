package com.classsync.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.classsync.app.domain.time.ScheduleCalculator
import com.classsync.app.ui.components.ClassEntryCard
import com.classsync.app.ui.components.EmptyState
import com.classsync.app.ui.components.NotificationPermissionCard
import com.classsync.app.ui.components.formattedDate
import com.classsync.app.ui.components.rememberTimeFormatter

@Composable
fun DashboardScreen(
    onOpenSchedule: (Long) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timeFormatter = rememberTimeFormatter(state.preferences.timeFormat)

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                Text(
                    state.now.toLocalDate().formattedDate(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        if (state.mode == UserMode.TEACHER) R.string.teacher_mode else R.string.student_mode,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { NotificationPermissionCard() }
        item {
            Text(stringResource(R.string.next_class), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            val next = state.next
            if (next == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(stringResource(R.string.no_more_classes_today), Modifier.padding(16.dp))
                }
            } else {
                Surface(
                    onClick = { onOpenSchedule(next.entry.schedule.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null)
                            Text(countdownLabel(ScheduleCalculator.minutesUntil(next, state.now)), fontWeight = FontWeight.SemiBold)
                        }
                        Text(next.entry.subject.name, style = MaterialTheme.typography.titleLarge)
                        Text(next.entry.group.displayName)
                        Text("${next.date.formattedDate()} | ${next.start.toLocalTime().format(timeFormatter)}")
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.today), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.today.isEmpty()) {
            item { EmptyState(stringResource(R.string.no_classes_today), stringResource(R.string.add_first_class)) }
        } else {
            items(state.today, key = { "${it.entry.schedule.id}-${it.date}" }) { occurrence ->
                ClassEntryCard(
                    entry = occurrence.entry,
                    preferences = state.preferences,
                    status = occurrence.status,
                    onClick = { onOpenSchedule(occurrence.entry.schedule.id) },
                )
            }
        }
    }
}

@Composable
private fun countdownLabel(minutes: Long): String = when {
    minutes <= 0 -> stringResource(R.string.starts_now)
    minutes < 60 -> stringResource(R.string.starts_in_minutes, minutes)
    else -> stringResource(R.string.starts_in_hours, minutes / 60, minutes % 60)
}
