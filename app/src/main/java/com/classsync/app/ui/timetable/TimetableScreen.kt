package com.classsync.app.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.ui.components.ClassEntryCard
import com.classsync.app.ui.components.EmptyState
import com.classsync.app.ui.components.dayLabel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onOpenSchedule: (Long) -> Unit,
    onManageCourses: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: TimetableViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var groupsExpanded by remember { mutableStateOf(false) }
    val selectedGroup = state.groups.firstOrNull { it.id == state.filters.selectedGroupId }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.filters.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text(stringResource(R.string.search_classes)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (state.filters.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                } else null,
                singleLine = true,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filters.viewMode == TimetableViewMode.DAY,
                        onClick = { viewModel.setViewMode(TimetableViewMode.DAY) },
                        label = { Text(stringResource(R.string.day_view)) },
                    )
                    FilterChip(
                        selected = state.filters.viewMode == TimetableViewMode.COURSE,
                        onClick = { viewModel.setViewMode(TimetableViewMode.COURSE) },
                        label = { Text(stringResource(R.string.course_view)) },
                    )
                }
                TextButton(onClick = onManageCourses) { Text(stringResource(R.string.manage_courses)) }
            }
        }
        if (state.filters.viewMode == TimetableViewMode.DAY) {
            item {
                val orderedDays = orderedDays(state.preferences.weekStartDay)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orderedDays) { day ->
                        FilterChip(
                            selected = day == state.filters.selectedDay,
                            onClick = { viewModel.selectDay(day) },
                            label = { Text(dayLabel(day)) },
                        )
                    }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(
                expanded = groupsExpanded,
                onExpandedChange = { groupsExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedGroup?.displayName ?: stringResource(R.string.all_courses),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    label = { Text(stringResource(R.string.courses)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupsExpanded) },
                )
                ExposedDropdownMenu(expanded = groupsExpanded, onDismissRequest = { groupsExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.all_courses)) },
                        onClick = {
                            viewModel.selectGroup(null)
                            groupsExpanded = false
                        },
                    )
                    state.groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.displayName) },
                            onClick = {
                                viewModel.selectGroup(group.id)
                                groupsExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (state.entries.isEmpty()) {
            item { EmptyState(stringResource(R.string.no_classes_match), stringResource(R.string.add_first_class)) }
        } else if (state.filters.viewMode == TimetableViewMode.COURSE) {
            state.entries.groupBy { it.group.id }.forEach { (_, entries) ->
                item(key = "group-${entries.first().group.id}") {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(entries.first().group.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                items(entries, key = { it.schedule.id }) { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(dayLabel(entry.schedule.dayOfWeek), style = MaterialTheme.typography.labelLarge)
                        ClassEntryCard(entry, state.preferences, onClick = { onOpenSchedule(entry.schedule.id) })
                    }
                }
            }
        } else {
            items(state.entries, key = { it.schedule.id }) { entry ->
                ClassEntryCard(entry, state.preferences, onClick = { onOpenSchedule(entry.schedule.id) })
            }
        }
    }
}

private fun orderedDays(start: DayOfWeek): List<DayOfWeek> =
    (0..6).map { DayOfWeek.of(((start.value - 1 + it) % 7) + 1) }
