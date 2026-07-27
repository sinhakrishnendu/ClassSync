# Database Schema

Room database name: `classsync.db`

Current schema version: `2`

Production builds do not use destructive migration. Version 2 adds Master Routine tables through the declared Room `1 -> 2` auto-migration and preserves every version-1 personal timetable table. Full Master Routine details are in [Master Routine Database Schema](master-routine-database-schema.md).

## Entity: AcademicGroup

Represents a course/programme/semester/batch/section combination.

Fields:

- `id: Long`
- `programme: String`
- `semester: String`
- `batchSection: String?`
- `institution: String?`
- `createdAt: Instant`
- `updatedAt: Instant`

Indexes:

- `programme`
- `semester`
- `batchSection`
- unique composite candidate for normalized programme + semester + batch/section can be added after UX confirms duplicate rules.

Notes:

- The app suggests examples like PG 1st Semester and UG 6th Semester but does not hard-code only those values.
- Teachers and administrators can create custom entries.

## Entity: Subject

Represents a subject or paper inside an academic group.

Fields:

- `id: Long`
- `academicGroupId: Long`
- `name: String`
- `code: String?`
- `createdAt: Instant`
- `updatedAt: Instant`

Foreign keys:

- `academicGroupId -> AcademicGroup.id`, cascade delete.

Indexes:

- `academicGroupId`
- `name`

## Entity: ClassSchedule

Represents a one-time or weekly recurring class entry.

Fields:

- `id: Long`
- `mode: UserMode`
- `academicGroupId: Long`
- `subjectId: Long`
- `dayOfWeek: DayOfWeek`
- `startTime: LocalTime`
- `endTime: LocalTime`
- `classroom: String?`
- `topic: String?`
- `teacherName: String?`
- `notes: String?`
- `recurrenceType: RecurrenceType`
- `oneTimeDate: LocalDate?`
- `reminderEnabled: Boolean`
- `reminderMinutes: Int`
- `createdAt: Instant`
- `updatedAt: Instant`

Foreign keys:

- `academicGroupId -> AcademicGroup.id`, restrict delete until schedules are removed or reassigned.
- `subjectId -> Subject.id`, restrict delete until schedules are removed or reassigned.

Indexes:

- `mode`
- `academicGroupId`
- `subjectId`
- `dayOfWeek`
- `startTime`
- composite `(mode, dayOfWeek, startTime)`

Validation:

- programme/course, semester, subject, day, start time, and end time are required.
- `endTime` must be later than `startTime`.
- reminder minutes must be positive.
- one-time classes require `oneTimeDate`.
- weekly classes require `dayOfWeek`.

Overlap handling:

- Overlaps are detected in the domain layer.
- Overlapping classes may be allowed after warning confirmation.

## Entity: ScheduleException

Represents a date-specific override for a recurring or one-time class.

Fields:

- `id: Long`
- `classScheduleId: Long`
- `relevantDate: LocalDate`
- `status: ExceptionStatus`
- `changedStartTime: LocalTime?`
- `changedEndTime: LocalTime?`
- `notes: String?`
- `createdAt: Instant`
- `updatedAt: Instant`

Foreign keys:

- `classScheduleId -> ClassSchedule.id`, cascade delete.

Indexes:

- `classScheduleId`
- `relevantDate`
- unique `(classScheduleId, relevantDate)`

Statuses:

- `CANCELLED`
- `RESCHEDULED`
- `COMPLETED`

Validation:

- rescheduled exceptions require both changed start and end time.
- changed end time must be later than changed start time.

## Type Converters

Room converters:

- `Instant <-> Long` epoch millis.
- `LocalDate <-> String` ISO-8601 date.
- `LocalTime <-> Int` minute-of-day.
- `DayOfWeek <-> Int` ISO day value.
- enums `<-> String`.

## DataStore Preferences

Preferences are not stored in Room.

Keys:

- `selected_mode: String`
- `onboarding_complete: Boolean`
- `default_reminder_minutes: Int`
- `reminders_enabled: Boolean`
- `theme_preference: String`
- `week_start_day: Int`
- `time_format: String`

## Import/Export JSON Versioning

Export root:

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-07-26T00:00:00Z",
  "groups": [],
  "subjects": [],
  "schedules": [],
  "exceptions": [],
  "preferences": {}
}
```

Import must validate:

- supported schema version.
- required fields.
- enum values.
- valid dates and times.
- foreign-key relationships.
- duplicate IDs within import payload.
- no partial writes on failure.
