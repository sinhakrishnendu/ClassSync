# Master Routine User Flow

## Entry and navigation

Teacher Home shows one prominent **Master Routine** card. Opening it shows saved Draft, Ready, Finalized, and Archived routines plus one **New routine** button. Personal timetable navigation remains available and unchanged.

The app shell is simplified to Home, Timetable, and Settings. Course management becomes a contextual action inside Timetable rather than a permanent fourth tab.

## Five-stage guided setup

### 1. Details

Enter routine title and institution. Department, academic year/session, dates, and notes are optional. **Continue** saves the draft.

### 2. Week

Choose working days. Start from a five-period preset. The user can adjust period count/times and mark a period as break. A compact summary shows total weekly teaching capacity.

### 3. People

Add classes and teachers with short inline forms. Teacher weekly limit is visible; daily/consecutive limits use defaults unless edited. Empty-state guidance says exactly what must be added before continuing.

### 4. Subjects

Choose a class and initial faculty member, enter the paper name and its syllabus periods per week, and optionally select a consecutive practical block. The initial faculty member receives the full load by default. **Edit faculty load** exposes simple +/- controls to split those periods; the allocated total must equal the syllabus total.

### 5. Generate

Review counts and validation issues. **Generate routine** is the single primary action. The screen shows progress and supports cancellation. On success it shows quality, allocations, warnings, and Class/Teacher view tabs.

## Save and resume

Every Continue action persists the complete aggregate transactionally and records the last completed stage. Saved drafts reopen at that stage. A failed or cancelled generation never discards input or replaces a previously accepted result.

## Manual correction

Selecting an unlocked generated entry opens a compact move dialog. The user chooses another day and start period. The validator previews conflicts. A valid move is stored as manually edited; an invalid move remains unchanged and the exact reason is shown.

## Validation and finalization

**Validate** reports Errors, Warnings, and Suggestions. A routine with errors cannot be finalized. Finalization records status and update time. Users can keep a valid routine as a draft.

## PDF export

**Export PDF** opens Android's document picker. The default report includes cover details, class-wise tables, teacher-wise tables, workload summary, notes, and page numbers. Wide schedules use landscape pages. The app writes directly to the chosen URI without network access.

## Error language

Errors name the affected teacher, class, or subject and suggest the smallest corrective action. Examples: increase Dr A's weekly limit, add a teacher for Molecular Evolution, enable another teaching period, or unlock/move a conflicting fixed entry.
