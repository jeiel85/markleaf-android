# Tablet Two-Pane Layout Evaluation (Issue #5)

## Date
2026-04-30

## Current State
- Navigation is phone-first single-pane (`NotesListScreen` -> `EditorScreen`).
- Editor and list are fully separated routes.
- No window-size or adaptive layout decision layer exists yet.

## Goal
Improve tablet writing flow by showing note list and editor together when width is large enough, while keeping phone UX unchanged.

## Recommended Direction
Adopt a responsive two-mode UI:
- Compact width: keep current single-pane navigation.
- Medium/Expanded width: use two-pane layout (list on left, editor on right).

## Breakpoint Policy
- `Compact`: current behavior.
- `Medium` and `Expanded`: enable two-pane.

Implementation option:
- Use `WindowWidthSizeClass` as top-level mode switch in `MainActivity` or root navigation container.

## Proposed UI Composition
Tablet mode (two-pane):
- Left pane: note list + create button + quick filters.
- Right pane: editor for selected note.
- Initial right pane state: empty placeholder (`Select a note`).
- Selection model: selected note id stored in state holder (ViewModel or screen-level state).

## Navigation Impact
- Keep existing route-based navigation for phone mode.
- Add tablet-only host composable (e.g., `TabletNotesEditorScreen`) without replacing phone routes.
- Avoid deep nav refactor in first iteration.

## Data/State Impact
- Reuse existing `NotesViewModel` for list data.
- Introduce editor loading/update contract that does not require route transition.
- Keep auto-save debounce, but move blocking repository access out of composables in follow-up refactor.

## Risks
- Current `EditorScreen` performs `runBlocking` in composable; can hurt tablet UX when pane is always visible.
- Shared state synchronization risk when list and editor update concurrently.

## Phase Plan
1. Add adaptive width classification and mode switch.
2. Build tablet scaffold with split panes and note selection state.
3. Refactor editor data loading/saving into ViewModel-friendly async flow.
4. Add UI tests for pane mode switch and selection persistence.

## Success Criteria
- On tablets, selecting a note opens/updates editor pane without full screen transition.
- Creating a note focuses new note in right pane.
- Phone behavior remains unchanged.
- No `android.permission.INTERNET` or API integration introduced.

## Decision
- Proceed with phased two-pane adoption for `Medium/Expanded` width classes.
- Prioritize minimal architectural disruption over full navigation rewrite.
