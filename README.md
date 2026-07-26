# ClassSync

ClassSync is a free, open-source Android timetable and class-reminder app for teachers and students. It works offline, requires no account, and contains no ads, subscriptions, analytics, tracking SDKs, or internet permission.

The project name is temporary and is isolated in `app_name` so it can be changed without restructuring the codebase.

## Features

- Teacher and Student modes with non-destructive switching.
- First-run mode selection and student class setup.
- Weekly and one-time classes with validated start/end times.
- Today dashboard, next-class countdown, day view, and course/group view.
- Add, edit, delete, duplicate, search, and filter timetable entries.
- Custom programmes, semesters, batches, sections, subjects, rooms, topics, teachers, and notes.
- Per-class and global reminders with custom lead times.
- Cancelled, completed, and rescheduled date-specific occurrences.
- Reminder restoration after reboot, app update, timezone change, and manual clock change.
- System, light, and dark themes; 12/24-hour display; configurable week start.
- Versioned, human-readable JSON export/import with full validation and transactional Room import.
- Confirmed delete-all flow, About page, and in-app privacy statement.

## Architecture

ClassSync is a single-activity Compose application using MVVM and practical Clean Architecture boundaries:

- `domain`: models, repository contracts, validation, recurrence, next-class, and reminder calculations.
- `data`: Room, DataStore, repository implementations, and versioned JSON backup handling.
- `notification`: notification channels, Hilt workers, unique WorkManager reminders, and system-change restoration.
- `ui`: Compose screens, immutable screen state, StateFlow ViewModels, and Navigation Compose.

Room is the timetable source of truth. DataStore stores preferences only. WorkManager is intentionally used instead of exact alarms, so reminders may be delayed by Android battery optimisation.

See [Architecture](docs/architecture.md), [Database Schema](docs/database-schema.md), and [Notification Architecture](docs/notifications.md).

## Requirements

- Android Studio with Android SDK 36 installed.
- JDK 17.
- No external service credentials.

The pinned toolchain is AGP 8.13.2, Gradle 8.13, Kotlin 2.3.10, and Java 17. The minimum supported Android version is Android 8.0 (API 26).

## Build

From the repository root:

```bash
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Tests

Run JVM unit and ViewModel tests:

```bash
./gradlew testDebugUnitTest
```

Run Room and Compose device tests on a connected Android device:

```bash
./gradlew connectedDebugAndroidTest
```

The repository's GitHub Actions workflow runs unit tests, Android lint, and a debug build for pushes and pull requests.

## Release Signing

Release minification and resource shrinking are enabled. A release build is signed automatically when an untracked `keystore.properties` file exists at the repository root:

```properties
storeFile=release/classsync-release.jks
storePassword=change-me
keyAlias=classsync
keyPassword=change-me
```

Generate a private keystore outside version control, then build an APK with `./gradlew assembleRelease` or an Android App Bundle with `./gradlew bundleRelease`. Never commit the keystore or `keystore.properties`.

## Privacy

Timetable data stays in the local Room database. Android cloud backup is disabled. Exported files are created only after an explicit user action through Android's document picker. See the [Privacy Policy](docs/privacy-policy.md).

## Known Limitations

- No teacher-to-student sync, accounts, cloud backup, attendance, assignments, exams, or widgets.
- JSON import replaces the current timetable after an explicit confirmation.
- WorkManager reminders are persistent but not guaranteed to fire at the exact minute on heavily optimised devices.
- Reminder and reboot behavior still requires final manual verification on physical Android devices before a public release.

## Documentation

- [Phase 1 Planning](docs/phase-1-planning.md)
- [Feature Checklist](docs/feature-checklist.md)
- [Folder Structure](docs/folder-structure.md)
- [Release Preparation](docs/release-prep.md)

## License

MIT License. See [LICENSE](LICENSE).

