package com.classsync.app.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.ui.components.EmptyState

@Composable
fun CoursesScreen(
    contentPadding: PaddingValues,
    showMessage: (String) -> Unit,
    viewModel: CoursesViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AcademicGroup?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AcademicGroup?>(null) }
    val savedMessage = stringResource(R.string.course_saved)
    val deletedMessage = stringResource(R.string.course_deleted)
    val deleteFailedMessage = stringResource(R.string.course_delete_failed)
    val failedMessage = stringResource(R.string.error_generic)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CourseEvent.Saved -> {
                    showEditor = false
                    showMessage(savedMessage)
                }
                CourseEvent.Deleted -> {
                    deleting = null
                    showMessage(deletedMessage)
                }
                CourseEvent.DeleteFailed -> {
                    deleting = null
                    showMessage(deleteFailedMessage)
                }
                CourseEvent.SaveFailed -> showMessage(failedMessage)
            }
        }
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Button(
                onClick = {
                    editing = null
                    showEditor = true
                },
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.add_course), Modifier.padding(start = 8.dp))
            }
        }
        if (groups.isEmpty()) {
            item { EmptyState(stringResource(R.string.no_courses)) }
        } else {
            items(groups, key = { it.id }) { group ->
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(group.programme, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(group.semester, group.batchSection).joinToString(" - "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            group.institution?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        IconButton(onClick = {
                            editing = group
                            showEditor = true
                        }) { Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_course)) }
                        IconButton(onClick = { deleting = group }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        CourseEditorDialog(
            group = editing,
            onDismiss = { showEditor = false },
            onSave = { programme, semester, batch, institution ->
                viewModel.save(editing, programme, semester, batch, institution)
            },
        )
    }
    deleting?.let { group ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.course_delete_title)) },
            text = { Text(stringResource(R.string.course_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(group) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CourseEditorDialog(
    group: AcademicGroup?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var programme by remember(group) { mutableStateOf(group?.programme.orEmpty()) }
    var semester by remember(group) { mutableStateOf(group?.semester.orEmpty()) }
    var batch by remember(group) { mutableStateOf(group?.batchSection.orEmpty()) }
    var institution by remember(group) { mutableStateOf(group?.institution.orEmpty()) }
    var validate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (group == null) R.string.add_course else R.string.edit_course)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    programme,
                    { programme = it },
                    label = { Text(stringResource(R.string.programme)) },
                    isError = validate && programme.isBlank(),
                    supportingText = if (validate && programme.isBlank()) ({ Text(stringResource(R.string.required_field)) }) else null,
                )
                OutlinedTextField(
                    semester,
                    { semester = it },
                    label = { Text(stringResource(R.string.semester)) },
                    isError = validate && semester.isBlank(),
                    supportingText = if (validate && semester.isBlank()) ({ Text(stringResource(R.string.required_field)) }) else null,
                )
                OutlinedTextField(batch, { batch = it }, label = { Text(stringResource(R.string.batch_section)) })
                OutlinedTextField(institution, { institution = it }, label = { Text(stringResource(R.string.institution)) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                validate = true
                if (programme.isNotBlank() && semester.isNotBlank()) onSave(programme, semester, batch, institution)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
