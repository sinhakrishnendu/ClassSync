# Master Routine Database Schema

## Isolation and migration

Room schema version 2 adds Master Routine tables beside the version-1 personal timetable tables. Migration `1 -> 2` only creates new tables and indices; it does not modify or delete personal groups, subjects, schedules, exceptions, or preferences.

String UUID primary keys let a complete draft be assembled offline before persistence and preserve stable references when the aggregate is replaced transactionally.

## Tables

### `master_routines`

- `id` primary key
- `title`, `institutionName`, optional `departmentName`
- `academicYear`, optional `academicSession`
- `effectiveFrom`, optional `effectiveTo`
- `versionLabel`, `status`, `currentStep`
- optional `preparedBy`, `approvedBy`, `notes`
- `createdAt`, `updatedAt`

### `master_working_days`

- `id` primary key; `masterRoutineId` foreign key cascade
- `dayOfWeek`, `isEnabled`, `displayOrder`
- unique index on routine/day

### `master_periods`

- `id` primary key; `masterRoutineId` foreign key cascade
- `periodNumber`, `label`, `startTime`, `endTime`, `periodType`, `isSchedulable`
- unique index on routine/period number

### `master_classes`

- `id` primary key; `masterRoutineId` foreign key cascade
- `displayName`, `programme`, `semester`, optional `section`, `batch`, enrolled count, `notes`

### `master_teachers`

- `id` primary key; `masterRoutineId` foreign key cascade
- `fullName`, `shortName`, optional `designation`, `department`
- `maxWeeklyPeriods`, `minWeeklyPeriods`, `maxDailyPeriods`, `maxConsecutivePeriods`
- `canTeachPracticals`, `notes`

### `master_subjects`

- `id` primary key; routine and class foreign keys cascade
- `name`, optional `code`
- `weeklyTheoryPeriods`, `weeklyPracticalPeriods`, `weeklyTutorialPeriods`
- `consecutivePeriodRequirement`, `maxOccurrencesPerDay`, `distributeAcrossDays`, `notes`

### `master_assignments`

- `id` primary key; routine, teacher, subject, and class foreign keys cascade
- `requiredWeeklyPeriods`, `isMandatory`, `isAlternateTeacher`

### `master_teacher_availability`

- `id` primary key; routine and teacher foreign keys cascade
- `dayOfWeek`, `periodNumber`, `status`, `preferenceWeight`
- unique index on teacher/day/period

### `master_timetable_entries`

- `id` primary key; routine, class, subject, and teacher foreign keys cascade
- `dayOfWeek`, `startPeriod`, `endPeriod`, `entryType`
- `isLocked`, `isManuallyEdited`, `generationBatchId`, optional `notes`
- indices for routine/day, teacher/day/period, and class/day/period

### `master_generation_runs`

- `id` primary key; routine foreign key cascade
- start/end timestamps, result status, quality score
- requested/allocated counts and serialized issue summary

## Aggregate persistence

`MasterRoutineRepository.save()` writes the routine and all child collections inside one Room transaction. Existing child rows for that routine are replaced in dependency order. Readers observe a `MasterRoutineRecord` with Room relations and never see a partially replaced aggregate.

## Converters

Existing converters handle `Instant`, `LocalDate`, `LocalTime`, and `DayOfWeek`. New enums use explicit string converters so stored values remain readable and migration-safe.
