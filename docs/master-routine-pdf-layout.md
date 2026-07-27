# Master Routine PDF Layout

## Architecture

`MasterRoutinePdfPreparer` converts a routine and validation/workload results into Android-independent pages, tables, rows, and cells. `AndroidMasterRoutinePdfWriter` is the only component using `PdfDocument`, canvas, fonts, and a destination stream.

## Default document

1. Cover: institution, department, title, academic year/session, effective date, version, prepared/approved fields.
2. Class-wise weekly routines.
3. Teacher-wise weekly routines.
4. Faculty workload summary with syllabus load, allocated load, maximum and status.
5. Validation notes and abbreviations.

## Page rules

- A4 dimensions.
- Portrait for cover and workload pages; landscape for timetable grids.
- Fixed margins, readable 9-12 pt text, wrapped labels, and visible borders.
- Repeated title/header row on every continuation page.
- Large datasets split by class or teacher instead of shrinking below readable size.
- Footer contains routine title, version, generated date, and page number.

## Grid

Rows represent enabled days. Columns represent schedulable periods and include start/end time. Cells show subject, teacher short name, and class/room context when applicable. Consecutive blocks repeat or span logically without relying on color alone.

## Export flow

The UI launches `ACTION_CREATE_DOCUMENT` with `application/pdf`, then writes to the selected URI. A later share action may use the standard Android share sheet. Generation requires no network permission.

## Testability

JVM tests verify page ordering, headings, row/cell content, period labels, workload totals, and continuation chunking. Android-specific tests only verify that prepared pages can be rendered to a non-empty PDF stream.
