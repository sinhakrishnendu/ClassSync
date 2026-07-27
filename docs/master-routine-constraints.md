# Master Routine Constraints

## Validation levels

`ERROR` blocks generation/finalization, `WARNING` describes an accepted limitation, and `SUGGESTION` identifies a quality improvement. Each issue has a stable code, human-readable message, and affected IDs.

## Input feasibility checks

- At least one enabled working day and schedulable period.
- At least one class, faculty member, subject, and faculty-load assignment.
- Every subject requirement is positive and has at least one faculty member.
- Faculty loads for each paper total exactly the syllabus periods for that paper.
- Assignment class matches the subject's class.
- Faculty required load does not exceed the stored weekly maximum.
- Each class's required periods fit its total weekly capacity.
- Consecutive block length fits at least one day's schedulable sequence.
- Locked entries reference valid inputs and fit enabled periods.

## Slot hard checks

For every occupied period in a candidate block:

1. The day is enabled and the period is schedulable.
2. The class is not already occupied.
3. The teacher is not already occupied.
4. The teacher is not marked unavailable.
5. Weekly and daily teacher limits remain within their stored values.
6. The placement does not produce more consecutive periods than allowed.
7. The subject's daily occurrence limit is respected.

Locked entries are placed first and checked against one another. A failure names both colliding entries. Hard checks are shared by automatic generation, validation, and manual moves.

## Soft score

The score starts at 100 and applies bounded deductions for:

- same subject concentrated on one day;
- teacher idle gaps;
- uneven teacher daily load;
- uneven class daily load;
- avoidable late-period placement.

The report lists each deduction. Score does not determine validity.

## Manual editing

A proposed move is evaluated against a copy of the timetable with the selected entry removed. The move is committed only when hard validation returns no error. Locked entries cannot be moved until explicitly unlocked.

## Failure behavior

Generation never lowers workload limits, drops requirements, moves locked entries, or returns a partial routine as success. If search limits are reached, all unallocated requirements are returned with suggestions. The saved input remains unchanged.
