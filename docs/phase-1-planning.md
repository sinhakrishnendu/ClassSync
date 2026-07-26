# Phase 1: Planning And Architecture

## Requirement Summary

ClassSync is a free, ad-free Android timetable app for teachers and students. It must work fully offline, preserve data across restarts, and require no account. In the MVP, "digital roster" means a digital routine or timetable. Attendance and roll-list management are deferred.

The app has two local modes:

- Teacher Mode: create and manage class schedules, academic groups, weekly routines, reminders, and class exceptions.
- Student Mode: manually maintain a personal weekly class routine, reminders, and basic profile selections.

The selected mode is stored locally and can be changed later from Settings without deleting existing data.

## MVP Feature List

- First-launch onboarding with Teacher Mode and Student Mode.
- Mode-aware dashboard showing date, next class, countdown, and today's classes.
- Add/edit/delete/duplicate timetable entries.
- Weekly recurring class support and one-time class support.
- Day-wise timetable sorted by start time.
- Grouped timetable by programme, course, semester, batch, or section.
- Course and semester management with custom entries.
- Search and filter by subject, group, day, classroom, and notes.
- Reminder enabled/disabled per class.
- Default reminder of 30 minutes before class.
- Reminder choices: 5, 10, 15, 30, 45, 60, and custom minutes.
- Notification channel and Android 13+ notification permission flow.
- Reminder restoration after device restart and timezone/time changes.
- Schedule exceptions for cancelled, rescheduled, and completed occurrences.
- Settings for mode, default reminder, all reminders, theme, week start, time format, export/import, delete all data, About, and Privacy.
- Local JSON export/import with validation.
- Light, dark, and system theme.

## Explicit Non-MVP Items

- Cloud synchronization.
- Teacher-to-student timetable publishing.
- Attendance register.
- Student roll list.
- Assignment tracking.
- Exam timetable.
- Home-screen widgets.
- Calendar integration.
- Institutional administration.
- Login, registration, subscription, ads, analytics, or tracking.

## Screen Map

1. Launch
2. Onboarding / mode selection
3. Main shell with bottom navigation
4. Today
5. Weekly timetable
6. Courses / groups
7. Add class
8. Edit class
9. Class details
10. Search and filters
11. Settings
12. About
13. Privacy
14. Export/import

Primary bottom tabs:

- Today
- Timetable
- Courses
- Settings

Labels remain simple and mode-neutral where possible. Mode-specific copy is loaded from resources.

## User Flows

### First Launch

1. App opens.
2. Preferences are read from DataStore.
3. If onboarding is incomplete, show mode selection.
4. User selects Teacher or Student.
5. Store selected mode and onboarding completion locally.
6. Navigate to the mode-aware Today dashboard.

### Add Class

1. User taps Add Class.
2. Form asks for academic group, subject, day, start time, end time, optional location/topic/notes/teacher, recurrence, and reminder settings.
3. Form validates required fields.
4. End time must be later than start time.
5. Overlap is detected and shown as a warning.
6. User confirms save.
7. Room transaction creates or reuses group/subject and stores schedule.
8. Reminder scheduler cancels stale work for the class ID and schedules the next reminder if enabled.
9. Snackbar confirms save.

### Edit Class

1. User opens class details.
2. User taps Edit.
3. Existing fields load into the same validated form.
4. Save updates Room and updated timestamp.
5. Reminder scheduler reschedules or cancels as needed.

### Delete Class

1. User opens details or class menu.
2. Confirmation dialog explains deletion.
3. User confirms.
4. Reminder work is cancelled.
5. Schedule and exceptions are deleted by cascade.
6. Snackbar confirms deletion.

### Skip Or Cancel Occurrence

1. User opens a class occurrence for a specific date.
2. User marks it Cancelled, Rescheduled, or Completed.
3. ScheduleException is stored for that date.
4. Dashboard and reminders exclude cancelled/completed occurrences and use changed times for rescheduled occurrences.

### Mode Switch

1. User opens Settings.
2. User changes mode.
3. DataStore updates selected mode.
4. Navigation refreshes dashboard and labels.
5. Existing data remains in Room.

### Export

1. User opens Settings > Export.
2. App serializes groups, subjects, schedules, exceptions, and reminder settings to JSON.
3. User saves/shares/copies JSON locally.
4. No network or account is required.

### Import

1. User chooses a JSON file or pastes JSON.
2. App validates version, required fields, enum values, times, and relationships.
3. Import runs in a Room transaction.
4. Invalid input returns a clear error and does not partially mutate the database.
5. Reminder scheduler refreshes all active reminders.

## Database Schema Plan

Room is the source of truth for timetable data. DataStore stores preferences only.

Entities:

- AcademicGroup
- Subject
- ClassSchedule
- ScheduleException

DataStore preferences:

- selected mode
- default reminder duration
- reminders globally enabled
- theme preference
- onboarding complete
- week starting day
- time format

Schema details are documented in [Database Schema](database-schema.md).

## Architecture Plan

ClassSync uses a single-activity Compose app with MVVM and pragmatic Clean Architecture boundaries:

- UI layer: Composables, screen state, navigation, UI events.
- Presentation layer: ViewModels exposing immutable StateFlow UI state.
- Domain layer: models, validation, use cases, recurrence calculations, next-class calculations.
- Data layer: Room entities/DAOs, DataStore preferences, repository implementations, import/export mappers.
- Platform layer: notification scheduler, WorkManager workers, boot/time-change receivers, permission helpers.

Repository interfaces live in the domain layer. Implementations live in the data/platform layer and are wired with Hilt.

Architecture details are documented in [Architecture Overview](architecture.md).

## Notification Strategy

The MVP uses WorkManager for reminder scheduling. It avoids exact alarms because class reminders do not require alarm-clock-level exactness and exact alarm permission would add unnecessary policy and UX friction.

Strategy:

- Create a notification channel on app start.
- On Android 13+, explain and request POST_NOTIFICATIONS before scheduling can produce visible notifications.
- Schedule one unique work item per schedule ID for the next reminder occurrence.
- Use unique work names to prevent duplicates.
- Cancel work when a class is deleted or reminders are disabled.
- Reschedule work when a class is edited.
- On worker execution, show the notification and schedule the next weekly occurrence if the class recurs.
- On BOOT_COMPLETED, TIMEZONE_CHANGED, and TIME_SET, enqueue a reschedule worker.
- Notification tap deep-links to class details.

Details are documented in [Notification Architecture](notifications.md).

## Planned Project Folder Structure

```text
app/src/main/java/com/classsync/app/
  ClassSyncApplication.kt
  MainActivity.kt
  core/
  data/
  domain/
  notification/
  ui/
```

Full structure is documented in [Folder Structure](folder-structure.md).

## Dependency List

Selected stable targets as of 2026-07-26:

- Android Gradle Plugin: 9.3.0
- Gradle: 9.5.0
- Kotlin: 2.4.10
- KSP: 2.3.10
- Jetpack Compose BOM: 2026.06.00
- Material 3: from Compose BOM
- Activity Compose: 1.13.0
- Lifecycle: 2.11.0
- Navigation Compose: 2.9.8
- Room: 2.8.4
- DataStore Preferences: 1.2.1
- WorkManager: 2.11.2
- Hilt Android: 2.60.1 if using AGP 9.x; otherwise 2.58 with AGP 8.13.x
- AndroidX Hilt: 1.4.0

Implementation will pin versions in `gradle/libs.versions.toml`. If the user's Android Studio cannot sync AGP 9.x, the fallback plan is AGP 8.13.2, Gradle 8.13, Kotlin 2.3.x, and Dagger Hilt 2.58.

## Development Milestones

### Phase 2: Project Foundation

- Create Android project files.
- Configure Gradle Kotlin DSL.
- Add Compose, Material 3, Hilt, Room, DataStore, Navigation, WorkManager.
- Add base theme, app shell, and reusable UI components.

### Phase 3: Database And Domain Layer

- Add entities, DAOs, database, type converters, repositories, use cases, validation, recurrence calculations, and targeted tests.

### Phase 4: Onboarding And Mode Selection

- Implement onboarding, selected-mode persistence, and mode switching.

### Phase 5: Teacher Mode

- Implement teacher dashboard, course/group management, schedule CRUD, day-wise timetable, grouped timetable, details, duplicate, delete confirmation, search, and filters.

### Phase 6: Student Mode

- Implement student setup, manual timetable entry, dashboard, weekly/day-wise views, edit/delete, and reminders.

### Phase 7: Notifications

- Implement notification channel, permission flow, WorkManager reminders, reboot/time-change restoration, cancellation, editing, recurrence, and deep links.

### Phase 8: Settings And Data Management

- Implement theme, reminder defaults, all-reminders toggle, time format, week start, export/import, delete all data, About, and Privacy.

### Phase 9: Testing And Quality Review

- Add focused unit, repository, ViewModel, Room, and essential Compose UI tests.
- Do not run resource-intensive tests unless explicitly requested.

### Phase 10: Release Preparation

- Add release configuration, adaptive icons, versioning, signed build instructions, Play Store draft, screenshot checklist, final README, and final privacy policy.
