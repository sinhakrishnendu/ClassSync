# Contributing

ClassSync welcomes focused fixes and features that preserve its free, offline, privacy-first scope.

1. Open an issue for behavior changes or schema changes before a large implementation.
2. Keep timetable data local and do not add ads, analytics, tracking, accounts, or unnecessary permissions.
3. Add tests proportional to the changed behavior.
4. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` before opening a pull request.
5. Never commit keystores, signing passwords, exported user timetable files, or local SDK settings.

Database changes must include an explicit Room migration and updated exported schema. User-facing text belongs in string resources.

