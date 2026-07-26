# Folder Structure

```text
.
  .github/                 GitHub Actions and dependency updates
  gradle/                  version catalog and Gradle wrapper
  docs/                    architecture, schema, privacy, and release notes
  app/
    schemas/               generated Room schema history
    src/main/
      java/com/classsync/app/
        data/
          backup/          versioned JSON codec and transactional import/export
          local/           Room entities, relations, DAOs, converters, database
          preferences/     DataStore implementation
          repository/      Room repository implementation
        di/                Hilt bindings and providers
        domain/
          model/           Android-independent timetable models
          repository/      domain-facing persistence contracts
          time/            recurrence, next-class, reminder, and clock logic
          validation/      form, duplicate, and overlap rules
        notification/      WorkManager scheduling, workers, channels, receiver
        ui/
          app/             app shell, navigation, root state
          components/      shared cards, permission, date, and time controls
          onboarding/      first-launch mode and student setup
          today/           dashboard and next-class state
          timetable/       day/course views, search, filters
          courses/         group management
          schedule/        class form, details, and exceptions
          settings/        preferences, backup, About, and Privacy
          theme/           Material 3 colors, type, theme behavior
      res/                  strings, styles, and icons
    src/test/               JVM domain, codec, and ViewModel tests
    src/androidTest/        Room/repository and Compose UI tests
```

## Boundaries

- `domain` contains no Android framework dependencies.
- `data` maps Room/DataStore records to domain models and owns transactions.
- `notification` owns Android scheduling and notification APIs behind a domain-facing interface.
- `ui` observes immutable StateFlow state and contains no database logic.
- User-facing text lives in `res/values/strings.xml` for future localisation.
- App branding lives in resources; the package and persistence architecture do not depend on the temporary display name.
