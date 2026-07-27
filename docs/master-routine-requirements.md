# Master Routine Requirements

## Purpose

Master Routine is an offline Teacher and Administration workspace for producing an institution-wide timetable. It is separate from personal teaching schedules and must never alter personal classes or reminders.

## MVP outcome

A user can create several named routines, save a draft, configure the teaching week, add classes and faculty, enter each paper's syllabus periods, split that exact load among faculty, generate a conflict-free timetable, review class-wise and faculty-wise views, move an unlocked entry with validation, save the result, and export a readable PDF.

## Required inputs

- Routine: title, institution, optional department, academic year/session, effective date, optional notes.
- Week: enabled working days and ordered periods with start/end time and schedulable/break status.
- Classes: custom display name, programme, semester/year, optional section and batch.
- Teachers: name, short name, weekly/daily/consecutive limits, practical capability, unavailable slots.
- Subjects: owning class, code/name, weekly theory/practical/tutorial counts, block length, daily occurrence limit.
- Faculty loads: one or more faculty members per paper; their required weekly periods must total the syllabus periods exactly.

Defaults reduce setup work: Monday-Friday, five one-hour periods, weekly load 18, daily load 5, consecutive limit 3, and one-period theory blocks. These remain explicit stored values and are never silently changed by generation.

## Hard constraints

- No teacher or class clash.
- No use of teacher unavailable slots.
- No teaching during breaks or disabled periods.
- Required subject periods must be allocated or clearly reported as unallocated.
- Every faculty member receives exactly their assigned share of each paper's syllabus periods.
- Teacher weekly, daily, and consecutive limits cannot be exceeded.
- Consecutive blocks must fit wholly within schedulable periods.
- Locked entries remain unchanged.
- Missing teacher assignments, insufficient capacity, and contradictory locked entries stop generation.

## Soft preferences

- Spread a subject across days.
- Balance teacher and class load across the week.
- Reduce avoidable teacher gaps.
- Avoid repeated same-subject periods on one day when possible.

Soft preferences affect the quality score only; they never relax a hard constraint.

## Results and safety

Generation returns success, success with warnings, cancelled, or impossible. Every failure contains actionable issues and the original draft remains saved. Generated entries are not hidden. A manual move is rejected if validation finds a hard conflict. Finalization is allowed only when validation has no errors.

## Minimal interface policy

The main journey exposes five stages: Details, Week, People, Subjects, Generate. Optional metadata and advanced limits use sensible defaults or a secondary disclosure. Each screen has one primary action. Terminology is short and concrete.

## Deferred after the first stable version

Alternative routine variants, advanced physical-room optimization, spreadsheets, rotating weeks, shared editing, approvals, substitution scheduling, and cloud sharing are deliberately excluded from this MVP architecture.

## Non-functional requirements

- Fully offline and deterministic for the same input.
- Generator remains Android-independent and testable on the JVM.
- Work runs away from the main thread and supports cancellation/progress.
- Room writes are transactional.
- PDF preparation is separate from generation.
- No internet permission, paid API, analytics, advertising, or account.
