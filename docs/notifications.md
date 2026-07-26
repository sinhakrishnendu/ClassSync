# Notification Architecture

## Goals

- Remind users before class.
- Default reminder: 30 minutes before start.
- Support custom reminder minutes.
- Survive app closure.
- Restore reminders after device restart.
- Reschedule after class edits.
- Cancel after class deletion.
- Respect disabled per-class and global reminders.
- Schedule reminders only for the currently selected Teacher or Student mode.
- Handle weekly recurring classes.
- Avoid duplicate notifications.
- Avoid unnecessary exact alarm permissions.

## Scheduling Choice

Use WorkManager for MVP reminders.

Reasoning:

- Class reminders tolerate small execution delays better than alarm-clock or medical events.
- WorkManager persists work across app process death.
- Unique work names prevent duplicates.
- WorkManager integrates cleanly with Hilt and app startup.
- Avoids requesting `SCHEDULE_EXACT_ALARM`, which is unnecessary for the MVP.

AlarmManager is reserved for a future option only if users explicitly need exact-to-the-minute reminders and platform policy requirements are satisfied.

## Unique Work

Work name:

```text
class_reminder_{scheduleId}
```

Scheduling a reminder replaces existing work for that class. Deleting a class cancels the unique work.

## Reminder Calculation

Inputs:

- schedule recurrence type.
- class day/date.
- start time.
- reminder minutes.
- schedule exceptions.
- current `ZonedDateTime`.
- device `ZoneId`.

Output:

- next reminder `Instant`, or `null` if no future reminder exists.

Cancelled and completed occurrences are skipped. Rescheduled occurrences use changed times.

## Worker Flow

1. Worker receives `scheduleId`.
2. Load schedule from repository.
3. Verify reminders are globally enabled and class reminder is enabled.
4. Verify notification permission status when needed.
5. Validate the planned occurrence timestamp against the current schedule and exception state.
6. Skip stale work if the occurrence changed, was cancelled/completed, or already started.
7. If still valid, build notification using the actual remaining minutes:
   - title: `Class in X minutes`
   - body line 1: subject and course/semester
   - body line 2: time and classroom when available
8. Show notification on `class_reminders` channel.
9. If schedule recurs weekly, schedule the next reminder.

## Notification Tap

The notification pending intent opens MainActivity with:

- a `classsync://schedule/{scheduleId}` deep link.

The navigation layer routes to class details.

## Channels

Channel ID:

```text
class_reminders
```

User-visible name:

```text
Class reminders
```

Importance:

- High, so Android can present time-sensitive class reminders according to the user's channel settings.

## Permissions

Required:

- `POST_NOTIFICATIONS` on Android 13+.
- `RECEIVE_BOOT_COMPLETED` for reboot restoration.

Not requested:

- exact alarm permission.
- internet.
- location.
- contacts.
- camera.
- microphone.

## Reboot And Time Changes

Receivers:

- `BOOT_COMPLETED`
- `TIME_SET`
- `TIMEZONE_CHANGED`

Receiver behavior:

1. Enqueue a unique reschedule worker.
2. Worker reads all active schedules.
3. Worker cancels stale reminder work.
4. Worker schedules next reminders.

## Failure Handling

Worker failures should be logged locally and surfaced in Settings as a non-blocking status when practical.

Cases:

- no permission: no visible notification, Settings shows permission needed.
- missing class: cancel work.
- reminders disabled: cancel work.
- invalid schedule: cancel work and keep database data untouched.
- scheduling failure: show snackbar after direct user action and leave schedule saved.
