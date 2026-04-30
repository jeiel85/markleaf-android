# Note Links Evaluation (Issue #4)

## Date
2026-04-30

## Conclusion
Proceed with ID-backed wiki links in phased steps.

## Link Model
- User-facing syntax: `[[Note Title]]`
- Internal identity: stable note id
- Resolution strategy:
  - Try exact title match
  - If ambiguous, show disambiguation picker
  - Persist resolved target id in link index table

## Why ID-backed
- Title changes are common.
- ID-backed mapping preserves link integrity after rename.
- Enables future backlink graph and reference navigation.

## Data Impact
- Add `note_links` table:
  - source_note_id
  - target_note_id
  - raw_label
  - created/updated timestamps
- Reindex links on note save.

## UX Phases
1. Parse and detect `[[...]]` tokens.
2. Tap to navigate to resolved note.
3. Add unresolved state UI and quick resolve flow.
4. Add backlink section in note detail.

## Risks
- Ambiguous titles and unresolved links.
- Parsing cost on large notes without incremental strategy.
- Potential conflicts with markdown edge cases.

## Decision
- Proceed with phased rollout.
- Keep markdown compatibility by preserving raw `[[...]]` text in note body.
