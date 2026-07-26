# Architecture Overview

## Principles

- Offline-first by default.
- No account, analytics, ads, subscriptions, tracking, or unnecessary permissions.
- Room is the local timetable source of truth.
- DataStore is used only for app preferences.
- Business rules stay out of Composables.
- Date/time and recurrence logic remain testable in the domain layer.
- Repository interfaces isolate UI/domain code from Room, DataStore, WorkManager, and Android framework details.
- Migrations are explicit; destructive database migration is not used in production.

## Layers

### UI Layer

Responsibilities:

- Compose screens and reusable components.
- Navigation.
- Snackbar and dialog rendering.
- Permission request UI.
- Accessibility labels and content descriptions.

The UI consumes immutable StateFlow-backed state from ViewModels and sends typed events back.

### Presentation Layer

Responsibilities:

- ViewModels.
- Screen UI state shaping.
- Form state and validation messages.
- Calling use cases and repositories.
- Triggering snackbar/event streams.

ViewModels do not directly depend on Room DAOs, WorkManager, or Android notification APIs.

### Domain Layer

Responsibilities:

- Domain models.
- Repository interfaces.
- Validation rules.
- Next-class calculation.
- Weekly recurrence calculation.
- Reminder-time calculation.
- Overlap detection.
- Import validation contracts.

Domain logic should be testable with plain JVM unit tests.

### Data Layer

Responsibilities:

- Room entities, DAOs, type converters, database.
- DataStore preference implementation.
- Repository implementations.
- Import/export serialization and mapping.
- Transaction boundaries.

Data entities are not exposed directly to UI screens.

### Platform Layer

Responsibilities:

- Notification channel creation.
- WorkManager workers.
- Reminder scheduling/cancellation.
- Boot and time-change receivers.
- Android permission helpers.
- Deep-link intent creation.

The platform layer depends on domain contracts, not the reverse.

## State Management

StateFlow is the default observable state primitive.

Examples:

- `PreferencesRepository.preferences: Flow<UserPreferences>`
- `ScheduleRepository.observeSchedules(mode): Flow<List<ClassSchedule>>`
- `DashboardViewModel.uiState: StateFlow<DashboardUiState>`
- `ScheduleFormViewModel.formState: StateFlow<ScheduleFormState>`

One-shot events such as snackbar messages use a Channel or SharedFlow.

## Navigation

Navigation Compose is used for the MVP because it is stable, widely supported, and fits a single-activity app.

Route groups:

- `onboarding`
- `main/today`
- `main/timetable`
- `main/courses`
- `main/settings`
- `schedule/new`
- `schedule/{scheduleId}`
- `schedule/{scheduleId}/edit`
- `about`
- `privacy`
- `importExport`

Navigation actions should avoid duplicate top-level destinations by using `launchSingleTop` and `restoreState`.

## Error Handling

Expected error classes:

- Validation errors.
- Duplicate/overlap warnings.
- Import validation errors.
- Database operation failures.
- Reminder scheduling failures.
- Notification permission missing.

The UI should show recoverable errors as field messages, dialogs, or snackbars. Crashes from malformed import data must be avoided.

## Dependency Injection

Hilt provides:

- Room database singleton.
- DAOs.
- DataStore singleton.
- Repository implementations.
- Clock/time provider.
- Reminder scheduler.
- Worker dependencies via AndroidX Hilt Work integration.

## Time Handling

Use `java.time` APIs:

- `LocalDate`
- `LocalTime`
- `DayOfWeek`
- `ZonedDateTime`
- `ZoneId`
- `Instant`

A `TimeProvider` abstraction supplies current time for tests.

## Accessibility

Implementation requirements:

- Content descriptions for meaningful icon buttons.
- Text labels for form controls.
- Error text linked to fields where practical.
- Large touch targets.
- Dynamic type support by avoiding fixed text heights.
- No status conveyed by color alone.

## Privacy And Permissions

Allowed planned permissions:

- `POST_NOTIFICATIONS` for Android 13+ reminders.
- `RECEIVE_BOOT_COMPLETED` for reminder restoration.

Not allowed:

- Internet.
- Location.
- Contacts.
- Camera.
- Microphone.
- Advertising ID.
