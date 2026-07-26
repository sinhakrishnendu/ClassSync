# Feature Checklist

## Phases 1-3: Planning, Foundation, Data And Domain

- [x] Requirements, screen map, user flows, architecture, schema, notifications, folders, and milestones
- [x] Gradle Kotlin DSL project and reproducible wrapper
- [x] Compose, Material 3, Hilt, Room, DataStore, Navigation, Coroutines, and WorkManager
- [x] Light, dark, system, and dynamic-color theme support
- [x] Shared schedule, empty-state, permission, date, and time components
- [x] Room entities, indexed foreign keys, relations, DAOs, converters, and schema export configuration
- [x] Domain models and repository contracts separate from UI and Room records
- [x] Transactional repository save, edit, delete, duplicate, group, and exception operations
- [x] Required-field, time, reminder, duplicate, and overlap validation
- [x] Day sorting, next class, weekly/one-time recurrence, exception, and reminder calculations
- [x] Versioned JSON codec with relationship and malformed-input validation
- [x] Explicit migration policy without destructive fallback

## Phases 4-6: Onboarding, Teacher And Student Modes

- [x] First-launch Teacher/Student selection stored in DataStore
- [x] Student course/semester setup and later course-group editing
- [x] Non-destructive mode switching
- [x] Mode-aware Today dashboard with date, next class, countdown, and today's entries
- [x] Custom course, semester, batch/section, institution, and subject handling
- [x] Add/edit class form with weekly and one-time recurrence
- [x] Class detail, reminder toggle, delete confirmation, and duplicate operation
- [x] Date-specific cancelled, completed, and rescheduled occurrences
- [x] Day-wise timetable sorted by start time
- [x] Course/group timetable across the week
- [x] Search across subject, course, semester, batch, day, room, teacher, topic, and notes
- [x] Course/group filter
- [x] Shared teacher/student timetable workflows with mode-separated schedules

## Phases 7-8: Notifications, Settings And Data

- [x] Notification channel and Android 13+ permission explanation/request
- [x] Permission status in Settings
- [x] Unique one-time WorkManager reminder per schedule
- [x] Weekly and one-time reminder calculation using the device timezone
- [x] Reschedule after save/edit and cancel after deletion/disable
- [x] Restoration after boot, app update, timezone change, and clock change
- [x] Notification deep link to class details
- [x] Global and per-class reminder controls
- [x] Default and custom reminder intervals
- [x] Theme, week start, and 12/24/system time-format settings
- [x] JSON export through Android's document picker
- [x] Confirmed, transactional JSON import
- [x] Confirmed delete-all operation and reminder cancellation
- [x] About and Privacy screens

## Phase 9: Tests And Quality

- [x] Form and time validation tests
- [x] Duplicate and overlap tests
- [x] Schedule sorting and next-class tests
- [x] Weekly, one-time, cancelled-occurrence, and far-future reminder tests
- [x] ViewModel save, overlap-confirmation, and scheduler-invocation tests
- [x] Room/repository save, relation, update, and delete device test
- [x] Import round-trip, malformed JSON, and foreign-key validation tests
- [x] Essential Compose schedule-card semantics/click test
- [ ] Physical-device notification timing, permission-denial, and reboot restoration verification
- [ ] Full tablet, TalkBack, font-scaling, and light/dark screenshot review

## Phase 10: Release Preparation

- [x] Debug and minified/resource-shrunk release build configuration
- [x] Version name `0.1.0` and version code `1`
- [x] Adaptive launcher and notification icons
- [x] Optional untracked release-signing configuration
- [x] APK and App Bundle instructions
- [x] GitHub Actions build/test/lint workflow
- [x] Dependabot Gradle and Actions updates
- [x] Play Store listing draft and screenshot checklist
- [x] Final README, privacy policy, and MIT license
- [ ] Successful JDK 17/Android SDK 36 build in Android Studio or CI
- [ ] Signed release artifact and physical-device release acceptance test

## Known Limitations

- No teacher-to-student sync, accounts, cloud backup, attendance, roll lists, assignments, exams, widgets, PDF, or CSV export.
- JSON import intentionally replaces local timetable records after confirmation.
- WorkManager is persistent but Android may delay reminders under battery optimisation.
- No exact-alarm permission is requested.

## Future Roadmap

- Attendance and student roll lists.
- Assignments, exams, holidays, and academic calendars.
- Timetable file/QR sharing and optional teacher publishing.
- Optional cross-device sync and calendar integration.
- Widgets, PDF/CSV export, substitutions, and syllabus progress.

