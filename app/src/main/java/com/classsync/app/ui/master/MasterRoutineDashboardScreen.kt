package com.classsync.app.ui.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.ui.components.EmptyState

@Composable
fun MasterRoutineDashboardScreen(
    contentPadding: PaddingValues,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: MasterRoutineDashboardViewModel = hiltViewModel(),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.master_routines), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.master_routine_home_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(stringResource(R.string.new_master_routine), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        if (routines.isEmpty()) {
            item { EmptyState(stringResource(R.string.no_master_routines), stringResource(R.string.no_master_routines_body)) }
        } else {
            items(routines, key = { it.id }) { routine ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(routine.title.ifBlank { stringResource(R.string.master_routine) }, fontWeight = FontWeight.SemiBold)
                        if (routine.institutionName.isNotBlank()) {
                            Text(routine.institutionName, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            stringResource(
                                R.string.routine_status,
                                routine.status.name.lowercase().replaceFirstChar(Char::uppercase),
                                routine.classCount,
                                routine.teacherCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { onOpen(routine.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.open_routine))
                        }
                    }
                }
            }
        }
    }
}

