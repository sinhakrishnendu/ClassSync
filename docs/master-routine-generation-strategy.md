# Master Routine Generation Strategy

## Components

- `ConstraintValidator`: validates inputs and completed routines.
- `HardConstraintChecker`: decides whether a candidate block may be placed.
- `SoftConstraintScorer`: produces a 0-100 quality report.
- `TimetableGenerationEngine`: deterministic constraint search.
- `GenerationResult` and `GenerationIssue`: stable result contract.

All components live in the domain layer and have no Android, Room, Compose, or PDF dependency.

## Algorithm

1. Validate the aggregate and stop on feasibility errors.
2. Create the enabled day/period slot matrix.
3. Validate and place locked entries.
4. Expand each subject into required blocks. Practical/consecutive blocks stay atomic.
5. Match those blocks to the exact faculty load split; the split must total the syllabus requirement.
6. Sort blocks by difficulty: longest blocks, least available faculty, highest load, then stable subject ID.
7. Build candidate placements in a deterministic preference order.
8. Use bounded depth-first backtracking. Every tentative placement passes `HardConstraintChecker`.
9. Keep the highest-scoring complete timetable found within the selected search budget.
10. Return all allocations, score details, workload summary, and warnings.

The MVP uses `FAST`, `BALANCED`, and `THOROUGH` node budgets. Budgets only affect search depth/optimization attempts; hard constraints never change.

## Responsiveness

The ViewModel runs generation on `Dispatchers.Default`. The engine accepts a cancellation callback and progress callback. Cancellation returns `CANCELLED` and does not overwrite saved entries.

## Determinism and reproducibility

There is no random placement. Stable sorting of inputs and candidates means the same data/mode yields the same first valid solution. Tests use small named datasets.

## Partial regeneration

Existing locked entries become fixed input. Unlocked entries are removed and their requirements regenerated. If fixed entries make the routine impossible, validation stops with specific collision/capacity issues.
