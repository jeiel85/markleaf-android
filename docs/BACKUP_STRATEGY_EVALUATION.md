# Optional Backup Strategy Evaluation (Issue #6)

## Date
2026-04-30

## Conclusion
Adopt user-triggered local backup with restore preview. No automatic remote sync.

## Recommended Strategy
- Backup type: explicit user action from Settings.
- Package format: zip bundle with
  - notes markdown exports
  - metadata json (version, timestamps, ids, tags)
  - optional assets directory (future image support)

## Restore Model
- Import from user-selected backup file.
- Dry-run preview:
  - notes to create
  - notes to update
  - conflicts count
- Conflict policy:
  - keep both
  - overwrite existing
  - skip

## Why
- Preserves local-first and data ownership principles.
- Avoids mandatory account, server cost, and privacy risks.
- Keeps F-Droid-friendly architecture.

## Risks
- User may forget manual backups.
- Restore conflict UX complexity.
- Schema version drift if format evolves without migration plan.

## Mitigation
- Add optional reminder banner (local-only, no telemetry).
- Versioned backup manifest.
- Validation before restore execution.

## Decision
- Proceed with optional manual backup/restore workflow.
- Defer automated scheduling until reliability and UX are proven.
