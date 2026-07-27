package com.classsync.app.ui.master

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classsync.app.domain.master.AvailabilityStatus
import com.classsync.app.domain.master.ConstraintValidator
import com.classsync.app.domain.master.GenerationIssue
import com.classsync.app.domain.master.GenerationIssueSeverity
import com.classsync.app.domain.master.GenerationMode
import com.classsync.app.domain.master.GenerationResult
import com.classsync.app.domain.master.GenerationStatus
import com.classsync.app.domain.master.HardConstraintChecker
import com.classsync.app.domain.master.MasterAcademicClass
import com.classsync.app.domain.master.MasterEntryType
import com.classsync.app.domain.master.MasterGenerationRun
import com.classsync.app.domain.master.MasterPeriod
import com.classsync.app.domain.master.MasterPeriodType
import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterRoutineStatus
import com.classsync.app.domain.master.MasterRoutineStep
import com.classsync.app.domain.master.MasterRoutineSummary
import com.classsync.app.domain.master.MasterSubject
import com.classsync.app.domain.master.MasterTeacher
import com.classsync.app.domain.master.MasterTeacherAssignment
import com.classsync.app.domain.master.MasterTeacherAvailability
import com.classsync.app.domain.master.TimetableGenerationEngine
import com.classsync.app.domain.master.masterId
import com.classsync.app.domain.master.newMasterRoutine
import com.classsync.app.domain.repository.MasterRoutineRepository
import com.classsync.app.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MasterRoutineDashboardViewModel @Inject constructor(
    repository: MasterRoutineRepository,
) : ViewModel() {
    val routines: StateFlow<List<MasterRoutineSummary>> = repository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class MasterRoutineWizardUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isGenerating: Boolean = false,
    val generationProgress: Int = 0,
    val data: MasterRoutineData = newMasterRoutine(),
    val issues: List<GenerationIssue> = emptyList(),
    val generationResult: GenerationResult? = null,
)

sealed interface MasterRoutineEvent {
    data object Saved : MasterRoutineEvent
    data object Finalized : MasterRoutineEvent
    data class Error(val message: String) : MasterRoutineEvent
}

@HiltViewModel
class MasterRoutineWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MasterRoutineRepository,
    private val engine: TimetableGenerationEngine,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val requestedId = savedStateHandle.get<String>("routineId").orEmpty()
    private val mutableState = MutableStateFlow(MasterRoutineWizardUiState())
    val uiState: StateFlow<MasterRoutineWizardUiState> = mutableState.asStateFlow()
    private val eventsChannel = Channel<MasterRoutineEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private var cancellation = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            val existing = if (requestedId.isNotBlank() && requestedId != "new") {
                repository.getRoutine(requestedId)
            } else {
                null
            }
            val data = existing ?: newMasterRoutine()
            mutableState.value = MasterRoutineWizardUiState(
                isLoading = false,
                data = data,
                generationResult = data.entries.takeIf(List<*>::isNotEmpty)?.let {
                    val issues = ConstraintValidator.validateTimetable(data)
                    GenerationResult(
                        status = if (issues.any { it.severity == GenerationIssueSeverity.ERROR }) {
                            GenerationStatus.IMPOSSIBLE
                        } else {
                            GenerationStatus.SUCCESS_WITH_WARNINGS
                        },
                        entries = data.entries,
                        issues = issues,
                    )
                },
            )
        }
    }

    fun setTitle(value: String) = changeRoutine { it.copy(title = value) }
    fun setInstitution(value: String) = changeRoutine { it.copy(institutionName = value) }
    fun setDepartment(value: String) = changeRoutine { it.copy(departmentName = value) }
    fun setAcademicYear(value: String) = changeRoutine { it.copy(academicYear = value) }
    fun setAcademicSession(value: String) = changeRoutine { it.copy(academicSession = value) }

    fun toggleDay(day: DayOfWeek) = changeData { data ->
        data.copy(workingDays = data.workingDays.map {
            if (it.dayOfWeek == day) it.copy(isEnabled = !it.isEnabled) else it
        })
    }

    fun setPeriodCount(count: Int) = changeData { data ->
        val target = count.coerceIn(1, 10)
        val existing = data.periods.sortedBy(MasterPeriod::periodNumber).take(target)
        val periods = if (existing.size == target) existing else {
            val additions = (existing.size + 1..target).map { number ->
                val start = existing.lastOrNull()?.endTime ?: LocalTime.of(9, 0).plusHours((number - 1).toLong())
                MasterPeriod(
                    masterRoutineId = data.routine.id,
                    periodNumber = number,
                    startTime = start,
                    endTime = start.plusHours(1),
                )
            }
            existing + additions
        }
        data.copy(periods = periods)
    }

    fun updatePeriodTime(periodNumber: Int, start: LocalTime? = null, end: LocalTime? = null) = changeData { data ->
        data.copy(periods = data.periods.map { period ->
            if (period.periodNumber == periodNumber) period.copy(
                startTime = start ?: period.startTime,
                endTime = end ?: period.endTime,
            ) else period
        })
    }

    fun togglePeriodBreak(periodNumber: Int) = changeData { data ->
        data.copy(periods = data.periods.map { period ->
            if (period.periodNumber == periodNumber) {
                val becomesBreak = period.isSchedulable
                period.copy(
                    type = if (becomesBreak) MasterPeriodType.BREAK else MasterPeriodType.TEACHING,
                    isSchedulable = !becomesBreak,
                )
            } else period
        })
    }

    fun addClass(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        changeData { data ->
            if (data.classes.any { it.displayName.equals(clean, ignoreCase = true) }) data else data.copy(
                classes = data.classes + MasterAcademicClass(masterRoutineId = data.routine.id, displayName = clean),
            )
        }
    }

    fun removeClass(id: String) = changeData { data ->
        val subjectIds = data.subjects.filter { it.academicClassId == id }.mapTo(mutableSetOf(), MasterSubject::id)
        data.copy(
            classes = data.classes.filterNot { it.id == id },
            subjects = data.subjects.filterNot { it.id in subjectIds },
            assignments = data.assignments.filterNot { it.academicClassId == id || it.subjectId in subjectIds },
            entries = data.entries.filterNot { it.academicClassId == id || it.subjectId in subjectIds },
        )
    }

    fun addTeacher(name: String, shortName: String, weeklyLimit: Int) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        changeData { data ->
            if (data.teachers.any { it.fullName.equals(clean, ignoreCase = true) }) data else data.copy(
                teachers = data.teachers + MasterTeacher(
                    masterRoutineId = data.routine.id,
                    fullName = clean,
                    shortName = shortName.trim().ifBlank { clean.initials() },
                    maxWeeklyPeriods = weeklyLimit.coerceIn(1, 60),
                ),
            )
        }
    }

    fun removeTeacher(id: String) = changeData { data ->
        data.copy(
            teachers = data.teachers.filterNot { it.id == id },
            teacherAvailability = data.teacherAvailability.filterNot { it.teacherId == id },
            assignments = data.assignments.filterNot { it.teacherId == id },
            entries = data.entries.filterNot { it.teacherId == id },
        )
    }

    fun toggleTeacherUnavailableDay(teacherId: String, day: DayOfWeek) = changeData { data ->
        val current = data.teacherAvailability.any { it.teacherId == teacherId && it.dayOfWeek == day }
        val retained = data.teacherAvailability.filterNot { it.teacherId == teacherId && it.dayOfWeek == day }
        val added = if (current) emptyList() else data.periods.map { period ->
            MasterTeacherAvailability(
                masterRoutineId = data.routine.id,
                teacherId = teacherId,
                dayOfWeek = day,
                periodNumber = period.periodNumber,
                status = AvailabilityStatus.UNAVAILABLE,
            )
        }
        data.copy(teacherAvailability = retained + added)
    }

    fun addSubject(name: String, classId: String, teacherId: String, weeklyPeriods: Int, practical: Boolean) {
        val clean = name.trim()
        if (clean.isEmpty() || classId.isBlank() || teacherId.isBlank()) return
        changeData { data ->
            val periods = weeklyPeriods.coerceIn(1, 20)
            val subject = MasterSubject(
                masterRoutineId = data.routine.id,
                academicClassId = classId,
                name = clean,
                weeklyTheoryPeriods = if (practical) 0 else periods,
                weeklyPracticalPeriods = if (practical) periods else 0,
                consecutivePeriodRequirement = if (practical) minOf(2, periods) else 1,
            )
            val assignment = MasterTeacherAssignment(
                masterRoutineId = data.routine.id,
                teacherId = teacherId,
                subjectId = subject.id,
                academicClassId = classId,
                requiredWeeklyPeriods = periods,
            )
            data.copy(subjects = data.subjects + subject, assignments = data.assignments + assignment)
        }
    }

    fun removeSubject(id: String) = changeData { data ->
        data.copy(
            subjects = data.subjects.filterNot { it.id == id },
            assignments = data.assignments.filterNot { it.subjectId == id },
            entries = data.entries.filterNot { it.subjectId == id },
        )
    }

    fun changeFacultyLoad(subjectId: String, teacherId: String, delta: Int) = changeData { data ->
        val subject = data.subjects.firstOrNull { it.id == subjectId } ?: return@changeData data
        if (data.teachers.none { it.id == teacherId }) return@changeData data
        val assignments = data.assignments.filter { it.subjectId == subjectId && !it.isAlternateTeacher }
        val current = assignments.firstOrNull { it.teacherId == teacherId }
        val currentLoad = current?.requiredWeeklyPeriods ?: 0
        val allocated = assignments.sumOf(MasterTeacherAssignment::requiredWeeklyPeriods)
        val nextLoad = (currentLoad + delta).coerceAtLeast(0)
        if (delta > 0 && allocated >= subject.totalWeeklyPeriods) return@changeData data
        val adjustedLoad = if (delta > 0) {
            minOf(nextLoad, currentLoad + subject.totalWeeklyPeriods - allocated)
        } else {
            nextLoad
        }
        val retained = data.assignments.filterNot { it.subjectId == subjectId && it.teacherId == teacherId && !it.isAlternateTeacher }
        val replacement = when {
            adjustedLoad <= 0 -> emptyList()
            current != null -> listOf(current.copy(requiredWeeklyPeriods = adjustedLoad))
            else -> listOf(
                MasterTeacherAssignment(
                    masterRoutineId = data.routine.id,
                    teacherId = teacherId,
                    subjectId = subjectId,
                    academicClassId = subject.academicClassId,
                    requiredWeeklyPeriods = adjustedLoad,
                ),
            )
        }
        data.copy(
            assignments = retained + replacement,
            entries = data.entries.filterNot { it.subjectId == subjectId && !it.isLocked },
        )
    }

    fun goToStep(step: MasterRoutineStep) {
        val errors = when (mutableState.value.data.routine.currentStep) {
            MasterRoutineStep.DETAILS -> validateDetails()
            else -> emptyList()
        }
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(issues = errors) }
            return
        }
        changeRoutine { it.copy(currentStep = step) }
        save(showEvent = false)
    }

    fun validate() {
        val issues = ConstraintValidator.validateTimetable(mutableState.value.data)
        mutableState.update { it.copy(issues = issues) }
    }

    fun save(showEvent: Boolean = true) {
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            runCatching { repository.save(mutableState.value.data) }
                .onSuccess { if (showEvent) eventsChannel.send(MasterRoutineEvent.Saved) }
                .onFailure { eventsChannel.send(MasterRoutineEvent.Error(it.message ?: "Unable to save the routine.")) }
            mutableState.update { it.copy(isSaving = false) }
        }
    }

    fun generate(mode: GenerationMode = GenerationMode.BALANCED) {
        if (mutableState.value.isGenerating) return
        cancellation = AtomicBoolean(false)
        mutableState.update { it.copy(isGenerating = true, generationProgress = 0, issues = emptyList()) }
        viewModelScope.launch {
            val source = mutableState.value.data
            val started = timeProvider.now().toInstant()
            val result = withContext(Dispatchers.Default) {
                engine.generate(
                    data = source,
                    mode = mode,
                    isCancelled = cancellation::get,
                    onProgress = { progress -> mutableState.update { it.copy(generationProgress = progress) } },
                )
            }
            val ended = timeProvider.now().toInstant()
            val run = MasterGenerationRun(
                masterRoutineId = source.routine.id,
                startedAt = started,
                endedAt = ended,
                status = result.status,
                qualityScore = result.qualityReport.score,
                totalEntriesRequested = result.requestedPeriods,
                totalEntriesAllocated = result.allocatedPeriods,
                issueSummary = result.issues.joinToString("\n", transform = GenerationIssue::message),
            )
            val accepted = result.status == GenerationStatus.SUCCESS || result.status == GenerationStatus.SUCCESS_WITH_WARNINGS
            val updatedData = if (accepted) {
                source.copy(
                    routine = source.routine.copy(status = MasterRoutineStatus.READY, currentStep = MasterRoutineStep.GENERATE),
                    entries = result.entries,
                    generationRuns = source.generationRuns + run,
                )
            } else {
                source.copy(generationRuns = source.generationRuns + run)
            }
            mutableState.update {
                it.copy(
                    isGenerating = false,
                    generationProgress = if (accepted) 100 else it.generationProgress,
                    data = updatedData,
                    issues = result.issues,
                    generationResult = result,
                )
            }
            repository.save(updatedData)
        }
    }

    fun cancelGeneration() = cancellation.set(true)

    fun toggleEntryLock(id: String) = changeData { data ->
        data.copy(entries = data.entries.map { if (it.id == id) it.copy(isLocked = !it.isLocked) else it })
    }.also { save(showEvent = false) }

    fun moveEntry(id: String, day: DayOfWeek, startPeriod: Int) {
        val data = mutableState.value.data
        val entry = data.entries.firstOrNull { it.id == id } ?: return
        if (entry.isLocked) {
            mutableState.update { it.copy(issues = listOf(errorIssue("locked_entry", "Unlock this entry before moving it."))) }
            return
        }
        val candidate = entry.copy(
            dayOfWeek = day,
            startPeriod = startPeriod,
            endPeriod = startPeriod + entry.periodCount - 1,
            isManuallyEdited = true,
        )
        val conflict = HardConstraintChecker.checkPlacement(data, data.entries.filterNot { it.id == id }, candidate)
        if (conflict != null) {
            mutableState.update { it.copy(issues = listOf(conflict)) }
            return
        }
        changeData { it.copy(entries = it.entries.map { current -> if (current.id == id) candidate else current }) }
        save(showEvent = false)
    }

    fun finalizeRoutine() {
        val issues = ConstraintValidator.validateTimetable(mutableState.value.data)
        if (issues.any { it.severity == GenerationIssueSeverity.ERROR }) {
            mutableState.update { it.copy(issues = issues) }
            return
        }
        changeRoutine { it.copy(status = MasterRoutineStatus.FINALIZED) }
        viewModelScope.launch {
            repository.save(mutableState.value.data)
            eventsChannel.send(MasterRoutineEvent.Finalized)
        }
    }

    private fun validateDetails(): List<GenerationIssue> = buildList {
        val routine = mutableState.value.data.routine
        if (routine.title.isBlank()) add(errorIssue("missing_title", "Add a routine title."))
        if (routine.institutionName.isBlank()) add(errorIssue("missing_institution", "Add the institution name."))
    }

    private fun errorIssue(code: String, message: String) =
        GenerationIssue(code, GenerationIssueSeverity.ERROR, message)

    private fun changeRoutine(block: (com.classsync.app.domain.master.MasterRoutine) -> com.classsync.app.domain.master.MasterRoutine) {
        changeData { it.copy(routine = block(it.routine), entries = it.entries) }
    }

    private fun changeData(block: (MasterRoutineData) -> MasterRoutineData) {
        mutableState.update { state ->
            state.copy(data = block(state.data), issues = emptyList(), generationResult = null)
        }
    }

    private fun String.initials(): String = trim().split(Regex("\\s+")).filter(String::isNotBlank)
        .take(3).joinToString("") { it.first().uppercase() }
}
