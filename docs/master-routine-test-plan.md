# Master Routine Test Plan

## Domain generator tests

- Valid small timetable allocates every requirement without clashes.
- Several valid solutions produce a deterministic valid result.
- Teacher clash, class clash, and locked-entry clash are rejected.
- Weekly/daily/consecutive teacher limits are enforced.
- Teacher unavailable slots are never used.
- Break periods are never used.
- Consecutive practical blocks remain contiguous.
- Subject daily maximum and distribution scoring work.
- Too little class capacity and too little teacher capacity produce named issues.
- Missing assignments produce named issues.
- Locked entries can make regeneration impossible without being moved.
- Cancellation stops search and returns cancelled without partial success.

## Validation/manual-edit tests

- A valid routine has zero hard errors.
- Moving an entry to a free slot succeeds.
- Moving into a teacher/class clash fails without mutating the original.
- Locked entries cannot move.
- Workload summaries classify underallocated, balanced, maximum, and overallocated teachers.
- Shared papers allocate each faculty member exactly their configured syllabus load.
- A faculty-load total that differs from the paper's syllabus total blocks generation with an actionable error.

## Persistence tests

- Migration 1 to 2 preserves all personal timetable rows.
- Saving and loading a complete Master Routine aggregate is lossless.
- Replacing a draft is transactional.
- Deleting one master routine cascades only its children.
- Multiple master routines remain independent.

## PDF preparation tests

- Cover fields and version are present.
- Class and teacher sections contain all allocated entries.
- Workload totals match generator output.
- Wide/large tables split into readable continuation pages with repeated headers.
- Validation issues appear in the report.

## UI checks

- Teacher and Administration Home both expose Master Routine.
- A new user completes the five stages using only required fields.
- Draft progress resumes at the saved stage.
- Generate shows progress/cancel and actionable failures.
- Class/Teacher views are readable at phone width.
- PDF document picker handles cancellation and write failure.
- Large font, TalkBack labels, light/dark theme, and rotation remain usable.

## Regression checks

- Teacher and Administration schedules still save, notify, import/export, and render.
- Existing database opens through migration without destructive fallback.
- Unit tests and lint pass; an emulator is used only for the later manual acceptance pass requested by the user.
