# Release Preparation

## Current Build Configuration

- Application ID: `com.classsync.app`
- Minimum SDK: 26 (Android 8.0)
- Compile/target SDK: 36
- Version name: `0.1.0`
- Version code: `1`
- Debug build for development and CI
- Release build with R8 minification and resource shrinking
- Room schema export enabled; destructive migration fallback disabled
- Android cloud backup disabled

## Signing

Create a keystore outside version control:

```bash
keytool -genkeypair -v -keystore classsync-release.jks -alias classsync -keyalg RSA -keysize 4096 -validity 10000
```

Create untracked `keystore.properties` at the repository root with `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. The Gradle release build uses it when present. The keystore and properties file are ignored by Git.

Build artifacts:

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Expected paths:

- APK: `app/build/outputs/apk/release/`
- App Bundle: `app/build/outputs/bundle/release/app-release.aab`

## Play Store Listing Draft

Short description:

```text
Offline class timetable and reminder app for teachers and students.
```

Full description:

```text
ClassSync helps teachers and students manage weekly academic class routines completely offline. Create one-time or weekly classes, organise courses and semesters, see today's schedule and next class, and receive local reminders before class. ClassSync requires no account and includes no advertisements, analytics, subscriptions, or internet access.
```

## Screenshot Checklist

- Onboarding mode selection
- Teacher Today dashboard
- Student Today dashboard
- Add/edit class form
- Day timetable and course-group timetable
- Class details and occurrence actions
- Course management
- Settings and backup controls
- Light and dark themes
- Phone and tablet widths

## Pre-Release Acceptance

- Build, unit tests, lint, and debug APK pass in GitHub Actions.
- Instrumented Room and Compose tests pass on API 26 and current Android.
- Notification permission allowed and denied paths verified.
- Weekly reminder fires and opens the correct detail screen.
- Reminder edit, disable, delete, reboot, timezone, and clock-change restoration verified.
- Import/export round trip verified with valid and malformed files.
- TalkBack, large font, light/dark, and tablet layouts reviewed.
- Signed App Bundle installed and smoke-tested on a physical device.
- Privacy policy URL and support contact added to the store listing before publication.

