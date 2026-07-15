# Markleaf Frontend Design State

Updated: 2026-07-15
Scope: Phase 29 / Quiet Editor formatting implementation

## Current objective

The permanent toolbar is replaced. The remaining Phase 29 work consolidates the editor's top app bar and information surfaces, then closes device-level accessibility and IME QA.

## Locked product decisions

- Markleaf remains a quiet, local-first Android Markdown editor. Bear is an interaction-quality benchmark, not a visual or structural template.
- The database, export, and folder mirror continue to store plain Markdown source.
- Existing formatting transformations, hardware shortcuts, Quick Insert commands, and the Storage Access Framework image flow must remain behaviorally available.
- The formatting system has three reusable primitives: compact entry, selection-context actions, and expanded style panel. Their authoritative contract is `DESIGN.md` section 5.
- The expanded panel is non-modal, anchored to the editor column, keeps the writing canvas visible, and does not explicitly dismiss the IME.
- Android's Cut/Copy/Paste selection menu remains platform-owned. Markleaf's selected-text group only adds Markdown actions.
- Preview, focus mode, Quick Insert, autocomplete, find/replace, and an external picker preempt formatting surfaces.
- The product screen now uses the three formatting primitives; later Phase 29 tasks must not recreate a permanent formatting strip.

## Source inputs

- `DESIGN.md`
- `docs/ROADMAP.md` Phase 29
- `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt`
- Existing Quick Insert, autocomplete, editor focus, and formatting behavior/tests

## Inclusive personas and stress contexts

| Persona/context | Primary risk | Required response |
|---|---|---|
| Phone writer composing with IME | Permanent chrome consumes the writing viewport | One 48dp entry and an IME-preserving anchored panel |
| Hardware-keyboard tablet writer | Touch-only affordances and focus traps | Preserve shortcuts; deterministic D-pad/keyboard entry and Escape dismissal |
| TalkBack or switch-access writer | Icon ambiguity and unstable traversal | Localized names/state, 48dp targets, stable semantic order |
| Large-text or long-translation user | Clipped labels and hidden horizontal actions | Vertical panel scrolling and contextual fallback; no horizontal-scroll-only command |
| Writer with limited dexterity or attention | Dense persistent controls and timing-sensitive gestures | Deliberate entry, large targets, no hover/timing dependency |

## Adaptive preference

- Compact width: entry lives in the editor footer; panel spans the editor content width above the IME.
- Expanded width: the same entry remains attached to the editor column; panel becomes a bounded anchored popover.
- Expanded width must not recreate the old permanent toolbar or move actions to a remote window edge.
- Dark theme, Android font scaling, and all six supported locales are required states, not optional variants.

## Verification matrix

| Stage | Evidence required | Status |
|---|---|---|
| Contract | All existing actions mapped; state, focus, dismissal, adaptive, and accessibility behavior specified | Complete (2026-07-15) |
| Primitive showcase | Collapsed, selected, expanded, keyboard-focused, disabled, dark, large-text, locale, and expanded-width states | Complete: 9 Roborazzi states (2026-07-15) |
| Product integration | Editor Markdown action, focus/dismissal, and Quick Insert coexistence | Complete in Compose/instrumentation coverage; device IME/TalkBack remains in final QA |
| Regression | Relevant unit/instrumentation tests, Roborazzi, test/lint/build | New suite, full unit tests, lintRelease, and assembleDebug pass |

## Design debt register

| ID | Severity | Debt | Affected users | Exit |
|---|---|---|---|---|
| FD-002 | Minor | Empty editor uses a pencil emoji rather than a vector asset | Users with inconsistent emoji rendering | Replace in the Phase 29 visual consolidation task |
| FD-003 | Minor | Large Settings and standalone Tags surfaces remain sparse | Tablet users | Address in the Phase 29 adaptive-layout polish task |

## Next handoff

Proceed to the next unchecked Phase 29 task: review the top app bar and consolidate statistics, table of contents, and backlinks into a calmer information structure. Keep the completed formatting contract unchanged and leave device IME/TalkBack checks for the final QA task.

## Evidence index

- `DESIGN.md` section 5: component, state, focus, and action inventory contract
- `DESIGN.md` section 8: personas, accessibility constraints, implementation gate, and accepted debt
- `.agent/decisions.md` D059: architectural interaction decision
- `.agent/tasks.md` Phase 29: execution order
- `EditorFormattingControlsSnapshotTest`: nine-state visual evidence
- `EditorFormattingControlsTest` and `EditorScreenTest`: behavior/integration evidence
