package com.classsync.app.ui.master

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classsync.app.R
import com.classsync.app.data.pdf.AndroidMasterRoutinePdfWriter
import com.classsync.app.domain.master.GenerationIssue
import com.classsync.app.domain.master.GenerationIssueSeverity
import com.classsync.app.domain.master.GenerationStatus
import com.classsync.app.domain.master.MasterAcademicClass
import com.classsync.app.domain.master.MasterPeriod
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterRoutinePdfPreparer
import com.classsync.app.domain.master.MasterRoutineStep
import com.classsync.app.domain.master.MasterSubject
import com.classsync.app.domain.master.MasterTeacher
import com.classsync.app.domain.master.MasterTimetableEntry
import com.classsync.app.domain.master.enabledDays
import com.classsync.app.ui.components.ClassSyncTimePickerDialog
import com.classsync.app.ui.components.dayLabel
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val wizardSteps = MasterRoutineStep.entries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterRoutineWizardScreen(
    contentPadding: PaddingValues,
    showMessage: (String) -> Unit,
    viewModel: MasterRoutineWizardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.routine_saved)
    val finalizedMessage = stringResource(R.string.routine_finalized)
    val pdfSuccess = stringResource(R.string.pdf_exported)
    val pdfFailed = stringResource(R.string.pdf_export_failed)

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val snapshot = state.data
            val issues = state.issues
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            AndroidMasterRoutinePdfWriter.write(MasterRoutinePdfPreparer.prepare(snapshot, issues), stream)
                        } ?: error("Unable to open PDF destination")
                    }
                }.onSuccess { showMessage(pdfSuccess) }.onFailure { showMessage(pdfFailed) }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MasterRoutineEvent.Saved -> showMessage(savedMessage)
                MasterRoutineEvent.Finalized -> showMessage(finalizedMessage)
                is MasterRoutineEvent.Error -> showMessage(event.message)
            }
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val step = state.data.routine.currentStep
    Column(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp)) {
        Column(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.step_progress, wizardSteps.indexOf(step) + 1, wizardSteps.size), style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(
                progress = { (wizardSteps.indexOf(step) + 1f) / wizardSteps.size },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f)) {
            when (step) {
                MasterRoutineStep.DETAILS -> DetailsStep(state.data, viewModel)
                MasterRoutineStep.WEEK -> WeekStep(state.data, viewModel)
                MasterRoutineStep.PEOPLE -> PeopleStep(state.data, viewModel)
                MasterRoutineStep.SUBJECTS -> SubjectsStep(state.data, viewModel)
                MasterRoutineStep.GENERATE -> GenerateStep(state, viewModel) {
                    val name = state.data.routine.title.ifBlank { "master-routine" }
                        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
                    pdfLauncher.launch("$name.pdf")
                }
            }
        }
        if (state.issues.isNotEmpty() && step != MasterRoutineStep.GENERATE) {
            IssueList(state.issues.take(2), Modifier.padding(vertical = 6.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step != MasterRoutineStep.DETAILS) {
                OutlinedButton(
                    onClick = { viewModel.goToStep(wizardSteps[wizardSteps.indexOf(step) - 1]) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.back_action)) }
            }
            if (step != MasterRoutineStep.GENERATE) {
                Button(
                    onClick = { viewModel.goToStep(wizardSteps[wizardSteps.indexOf(step) + 1]) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.continue_action_simple)) }
            }
        }
    }
}

@Composable
private fun DetailsStep(data: MasterRoutineData, viewModel: MasterRoutineWizardViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepHeading(stringResource(R.string.routine_details), stringResource(R.string.details_intro)) }
        item { SimpleField(data.routine.title, viewModel::setTitle, stringResource(R.string.routine_title), stringResource(R.string.routine_title_hint)) }
        item { SimpleField(data.routine.institutionName, viewModel::setInstitution, stringResource(R.string.institution)) }
        item { SimpleField(data.routine.departmentName, viewModel::setDepartment, stringResource(R.string.department)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleField(data.routine.academicYear, viewModel::setAcademicYear, stringResource(R.string.academic_year), modifier = Modifier.weight(1f))
                SimpleField(data.routine.academicSession, viewModel::setAcademicSession, stringResource(R.string.academic_session), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeekStep(data: MasterRoutineData, viewModel: MasterRoutineWizardViewModel) {
    var editingPeriod by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepHeading(stringResource(R.string.routine_week), stringResource(R.string.week_intro)) }
        item { Text(stringResource(R.string.working_days), fontWeight = FontWeight.SemiBold) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DayOfWeek.entries) { day ->
                    FilterChip(
                        selected = data.workingDays.firstOrNull { it.dayOfWeek == day }?.isEnabled == true,
                        onClick = { viewModel.toggleDay(day) },
                        label = { Text(dayLabel(day).take(3)) },
                    )
                }
            }
        }
        item {
            CounterRow(
                label = stringResource(R.string.periods_per_day),
                value = data.periods.size,
                decrease = { viewModel.setPeriodCount(data.periods.size - 1) },
                increase = { viewModel.setPeriodCount(data.periods.size + 1) },
            )
        }
        item {
            Text(
                stringResource(R.string.teaching_capacity, enabledDays(data).size * data.periods.count(MasterPeriod::isSchedulable)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(data.periods.sortedBy(MasterPeriod::periodNumber), key = { it.id }) { period ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(period.label, fontWeight = FontWeight.SemiBold)
                        FilterChip(
                            selected = !period.isSchedulable,
                            onClick = { viewModel.togglePeriodBreak(period.periodNumber) },
                            label = { Text(stringResource(if (period.isSchedulable) R.string.teaching_period else R.string.mark_as_break)) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editingPeriod = period.periodNumber to true }, modifier = Modifier.weight(1f)) { Text(period.startTime.toString()) }
                        OutlinedButton(onClick = { editingPeriod = period.periodNumber to false }, modifier = Modifier.weight(1f)) { Text(period.endTime.toString()) }
                    }
                }
            }
        }
    }
    editingPeriod?.let { (number, isStart) ->
        val period = data.periods.first { it.periodNumber == number }
        ClassSyncTimePickerDialog(
            initialTime = if (isStart) period.startTime else period.endTime,
            onDismiss = { editingPeriod = null },
        ) { time ->
            viewModel.updatePeriodTime(number, start = time.takeIf { isStart }, end = time.takeUnless { isStart })
            editingPeriod = null
        }
    }
}

@Composable
private fun PeopleStep(data: MasterRoutineData, viewModel: MasterRoutineWizardViewModel) {
    var className by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var teacherShort by remember { mutableStateOf("") }
    var weeklyLimit by remember { mutableStateOf("18") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepHeading(stringResource(R.string.routine_people), stringResource(R.string.people_intro)) }
        item { Text(stringResource(R.string.classes_count, data.classes.size), fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SimpleField(className, { className = it }, stringResource(R.string.class_name), stringResource(R.string.class_name_hint), Modifier.weight(1f))
                IconButton(onClick = { viewModel.addClass(className); className = "" }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_class_short))
                }
            }
        }
        items(data.classes, key = { it.id }) { academicClass ->
            CompactRemoveCard(academicClass.displayName) { viewModel.removeClass(academicClass.id) }
        }
        item { Text(stringResource(R.string.teachers_count, data.teachers.size), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleField(teacherName, { teacherName = it }, stringResource(R.string.teacher_full_name))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SimpleField(teacherShort, { teacherShort = it }, stringResource(R.string.teacher_short_name), modifier = Modifier.weight(1f))
                    SimpleField(
                        weeklyLimit,
                        { weeklyLimit = it.filter(Char::isDigit).take(2) },
                        stringResource(R.string.weekly_limit),
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    )
                    IconButton(onClick = {
                        viewModel.addTeacher(teacherName, teacherShort, weeklyLimit.toIntOrNull() ?: 18)
                        teacherName = ""; teacherShort = ""; weeklyLimit = "18"
                    }) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_teacher)) }
                }
            }
        }
        items(data.teachers, key = { it.id }) { teacher ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(teacher.fullName, fontWeight = FontWeight.SemiBold)
                            Text("${teacher.shortName} · ${teacher.maxWeeklyPeriods} periods/week", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.removeTeacher(teacher.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.remove))
                        }
                    }
                    Text(stringResource(R.string.unavailable_days), style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DayOfWeek.entries) { day ->
                            val unavailable = data.teacherAvailability.any { it.teacherId == teacher.id && it.dayOfWeek == day }
                            FilterChip(
                                selected = unavailable,
                                onClick = { viewModel.toggleTeacherUnavailableDay(teacher.id, day) },
                                label = { Text(dayLabel(day).take(3)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectsStep(data: MasterRoutineData, viewModel: MasterRoutineWizardViewModel) {
    var name by remember { mutableStateOf("") }
    var selectedClassId by remember(data.classes) { mutableStateOf(data.classes.firstOrNull()?.id.orEmpty()) }
    var selectedTeacherId by remember(data.teachers) { mutableStateOf(data.teachers.firstOrNull()?.id.orEmpty()) }
    var periods by remember { mutableIntStateOf(3) }
    var practical by remember { mutableStateOf(false) }
    var classMenu by remember { mutableStateOf(false) }
    var teacherMenu by remember { mutableStateOf(false) }
    var editLoadSubjectId by remember { mutableStateOf<String?>(null) }
    val selectedClass = data.classes.firstOrNull { it.id == selectedClassId }
    val selectedTeacher = data.teachers.firstOrNull { it.id == selectedTeacherId }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepHeading(stringResource(R.string.routine_subjects), stringResource(R.string.subject_intro)) }
        item { SimpleField(name, { name = it }, stringResource(R.string.subject)) }
        item {
            ExposedDropdownMenuBox(expanded = classMenu, onExpandedChange = { classMenu = it }) {
                OutlinedTextField(
                    value = selectedClass?.displayName.orEmpty(), onValueChange = {}, readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), label = { Text(stringResource(R.string.choose_class)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(classMenu) },
                )
                ExposedDropdownMenu(classMenu, { classMenu = false }) {
                    data.classes.forEach { item -> DropdownMenuItem({ Text(item.displayName) }, { selectedClassId = item.id; classMenu = false }) }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(expanded = teacherMenu, onExpandedChange = { teacherMenu = it }) {
                OutlinedTextField(
                    value = selectedTeacher?.fullName.orEmpty(), onValueChange = {}, readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), label = { Text(stringResource(R.string.choose_teacher)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(teacherMenu) },
                )
                ExposedDropdownMenu(teacherMenu, { teacherMenu = false }) {
                    data.teachers.forEach { item -> DropdownMenuItem({ Text(item.fullName) }, { selectedTeacherId = item.id; teacherMenu = false }) }
                }
            }
        }
        item { CounterRow(stringResource(R.string.periods_week), periods, { periods = (periods - 1).coerceAtLeast(1) }, { periods = (periods + 1).coerceAtMost(20) }) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.practical_block), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.practical_block_description), style = MaterialTheme.typography.bodySmall)
                }
                Switch(practical, { practical = it })
            }
        }
        item {
            Button(
                onClick = { viewModel.addSubject(name, selectedClassId, selectedTeacherId, periods, practical); name = "" },
                enabled = name.isNotBlank() && selectedClass != null && selectedTeacher != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_subject)) }
        }
        item { Text(stringResource(R.string.subjects_count, data.subjects.size), fontWeight = FontWeight.SemiBold) }
        items(data.subjects, key = { it.id }) { subject ->
            val academicClass = data.classes.firstOrNull { it.id == subject.academicClassId }
            val assignments = data.assignments.filter { it.subjectId == subject.id && !it.isAlternateTeacher }
            val allocated = assignments.sumOf { it.requiredWeeklyPeriods }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(subject.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${academicClass?.displayName.orEmpty()} · ${subject.totalWeeklyPeriods} syllabus periods/week",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { viewModel.removeSubject(subject.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.remove))
                        }
                    }
                    assignments.forEach { assignment ->
                        val faculty = data.teachers.firstOrNull { it.id == assignment.teacherId }
                        Text(
                            "${faculty?.shortName.orEmpty()}: ${assignment.requiredWeeklyPeriods} periods",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        stringResource(R.string.faculty_load_total, allocated, subject.totalWeeklyPeriods),
                        color = if (allocated == subject.totalWeeklyPeriods) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { editLoadSubjectId = subject.id }) {
                        Text(stringResource(R.string.edit_faculty_load))
                    }
                }
            }
        }
    }
    editLoadSubjectId?.let { subjectId ->
        data.subjects.firstOrNull { it.id == subjectId }?.let { subject ->
            FacultyLoadDialog(
                data = data,
                subject = subject,
                onChange = { teacherId, delta -> viewModel.changeFacultyLoad(subject.id, teacherId, delta) },
                onDismiss = { editLoadSubjectId = null },
            )
        } ?: run { editLoadSubjectId = null }
    }
}

@Composable
private fun FacultyLoadDialog(
    data: MasterRoutineData,
    subject: MasterSubject,
    onChange: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val assignments = data.assignments.filter { it.subjectId == subject.id && !it.isAlternateTeacher }
    val allocated = assignments.sumOf { it.requiredWeeklyPeriods }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.faculty_load_title)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(subject.name, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.syllabus_load_summary, subject.totalWeeklyPeriods))
                Text(
                    stringResource(R.string.faculty_load_total, allocated, subject.totalWeeklyPeriods),
                    color = if (allocated == subject.totalWeeklyPeriods) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                data.teachers.forEach { teacher ->
                    val load = assignments.firstOrNull { it.teacherId == teacher.id }?.requiredWeeklyPeriods ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(teacher.fullName)
                            Text(teacher.shortName, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onChange(teacher.id, -1) }, enabled = load > 0) {
                            Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.decrease_load))
                        }
                        Text(load.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onChange(teacher.id, 1) }, enabled = allocated < subject.totalWeeklyPeriods) {
                            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.increase_load))
                        }
                    }
                }
                Text(stringResource(R.string.faculty_load_help), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun GenerateStep(
    state: MasterRoutineWizardUiState,
    viewModel: MasterRoutineWizardViewModel,
    exportPdf: () -> Unit,
) {
    val data = state.data
    var viewByTeacher by remember { mutableStateOf(false) }
    var moveEntry by remember { mutableStateOf<MasterTimetableEntry?>(null) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StepHeading(stringResource(R.string.routine_generate), stringResource(R.string.review_ready)) }
        item { Text(stringResource(R.string.review_summary, data.classes.size, data.teachers.size, data.subjects.size)) }
        if (data.teachers.isNotEmpty()) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.faculty_load_review), fontWeight = FontWeight.SemiBold)
                        data.teachers.forEach { teacher ->
                            val required = data.assignments.filter {
                                it.teacherId == teacher.id && !it.isAlternateTeacher
                            }.sumOf { it.requiredWeeklyPeriods }
                            Text(
                                stringResource(R.string.faculty_load_line, teacher.shortName, required, teacher.maxWeeklyPeriods),
                                color = if (required <= teacher.maxWeeklyPeriods) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        if (state.isGenerating) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.generating_routine, state.generationProgress), fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(progress = { state.generationProgress / 100f }, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = viewModel::cancelGeneration) { Text(stringResource(R.string.cancel_generation)) }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::validate, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.validate_routine)) }
                    Button(onClick = { viewModel.generate() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.generate_routine)) }
                }
            }
        }
        if (state.issues.isNotEmpty()) item { IssueList(state.issues) }
        state.generationResult?.let { result ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            stringResource(if (result.status == GenerationStatus.IMPOSSIBLE) R.string.routine_impossible else R.string.routine_generated),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (result.status != GenerationStatus.IMPOSSIBLE) {
                            Text(stringResource(R.string.quality_score, result.qualityReport.score))
                            Text(stringResource(R.string.allocation_summary, result.allocatedPeriods, result.requestedPeriods))
                        }
                    }
                }
            }
        }
        if (data.entries.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!viewByTeacher, { viewByTeacher = false }, label = { Text(stringResource(R.string.class_wise)) })
                    FilterChip(viewByTeacher, { viewByTeacher = true }, label = { Text(stringResource(R.string.teacher_wise)) })
                }
            }
            val groups: List<Pair<String, List<MasterTimetableEntry>>> = if (viewByTeacher) {
                data.teachers.map { it.fullName to data.entries.filter { entry -> entry.teacherId == it.id } }
            } else {
                data.classes.map { it.displayName to data.entries.filter { entry -> entry.academicClassId == it.id } }
            }
            groups.filter { it.second.isNotEmpty() }.forEach { (title, entries) ->
                item(key = "heading-$title") { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                items(entries.sortedWith(compareBy({ it.dayOfWeek.value }, { it.startPeriod })), key = { it.id }) { entry ->
                    RoutineEntryCard(data, entry, { moveEntry = entry }, { viewModel.toggleEntryLock(entry.id) })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = exportPdf, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Text(stringResource(R.string.export_pdf), Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = viewModel::finalizeRoutine, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.finalize_routine)) }
                }
            }
            item { OutlinedButton(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_draft)) } }
        } else if (!state.isGenerating) {
            item { Text(stringResource(R.string.no_entries), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    moveEntry?.let { entry ->
        MoveEntryDialog(data, entry, { moveEntry = null }) { day, period ->
            viewModel.moveEntry(entry.id, day, period)
            moveEntry = null
        }
    }
}

@Composable
private fun RoutineEntryCard(data: MasterRoutineData, entry: MasterTimetableEntry, move: () -> Unit, toggleLock: () -> Unit) {
    val subject = data.subjects.firstOrNull { it.id == entry.subjectId }
    val teacher = data.teachers.firstOrNull { it.id == entry.teacherId }
    val academicClass = data.classes.firstOrNull { it.id == entry.academicClassId }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(subject?.name.orEmpty(), fontWeight = FontWeight.SemiBold)
            Text("${dayLabel(entry.dayOfWeek)} · Period ${entry.startPeriod}${if (entry.endPeriod > entry.startPeriod) "-${entry.endPeriod}" else ""}")
            Text("${academicClass?.displayName.orEmpty()} · ${teacher?.shortName.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = move, enabled = !entry.isLocked) { Text(stringResource(R.string.move_entry)) }
                TextButton(onClick = toggleLock) {
                    Icon(if (entry.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null)
                    Text(stringResource(if (entry.isLocked) R.string.unlock_entry else R.string.lock_entry), Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MoveEntryDialog(data: MasterRoutineData, entry: MasterTimetableEntry, dismiss: () -> Unit, confirm: (DayOfWeek, Int) -> Unit) {
    var day by remember { mutableStateOf(entry.dayOfWeek) }
    var period by remember { mutableIntStateOf(entry.startPeriod) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.move_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.choose_day_period))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(enabledDays(data)) { item -> FilterChip(day == item, { day = item }, label = { Text(dayLabel(item).take(3)) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(data.periods.filter(MasterPeriod::isSchedulable)) { item ->
                        FilterChip(period == item.periodNumber, { period = item.periodNumber }, label = { Text(item.periodNumber.toString()) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { confirm(day, period) }) { Text(stringResource(R.string.save_move)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun IssueList(issues: List<GenerationIssue>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        issues.forEach { issue ->
            val color = when (issue.severity) {
                GenerationIssueSeverity.ERROR -> MaterialTheme.colorScheme.error
                GenerationIssueSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                GenerationIssueSeverity.SUGGESTION -> MaterialTheme.colorScheme.primary
            }
            Text("${issue.severity.name.lowercase().replaceFirstChar(Char::uppercase)}: ${issue.message}", color = color, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StepHeading(title: String, body: String) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SimpleField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder.takeIf(String::isNotBlank)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun CounterRow(label: String, value: Int, decrease: () -> Unit, increase: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = decrease) { Icon(Icons.Outlined.Remove, contentDescription = null) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = increase) { Icon(Icons.Outlined.Add, contentDescription = null) }
        }
    }
}

@Composable
private fun CompactRemoveCard(title: String, subtitle: String = "", remove: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = remove) { Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.remove)) }
        }
    }
}
