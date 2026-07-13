# Markleaf Design System

## 1. Atmosphere & Identity

Markleaf is a quiet, trustworthy writing surface: calm enough to disappear while writing, explicit enough to make local ownership visible. Its signature is leaf green used sparingly over warm green-neutral surfaces, with hierarchy created by typography, whitespace, and tonal pane shifts instead of decorative chrome.

## 2. Color

All product colors come from `ui/theme/Theme.kt`. Dynamic color may replace the palette only when the user enables Material You.

| Role | Compose token | Light | Dark | Usage |
|---|---|---:|---:|---|
| Accent | `primary` | `#2E7D32` | `#81C784` | Primary actions, selected text, Markdown headings |
| Accent content | `onPrimary` | `#FFFFFF` | `#00390A` | Content on accent surfaces |
| Accent container | `primaryContainer` | `#C8E6C9` | `#1B5E20` | Selected and emphasized containers |
| Accent container content | `onPrimaryContainer` | `#003300` | `#C8E6C9` | Content on accent containers |
| Secondary | `secondary` | `#52634F` | `#B9CCB4` | Supporting Markdown and controls |
| Secondary container | `secondaryContainer` | `#D5E8CF` | `#3B4B38` | Selected navigation rows and quiet highlights |
| Secondary container content | `onSecondaryContainer` | `#111F0F` | `#D5E8CF` | Content on selected secondary containers |
| Tertiary | `tertiary` | `#38656A` | `#A0CFD4` | Code and secondary syntax emphasis |
| Canvas | `background` / `surface` | `#F9FBF9` | `#191C19` | Main writing and navigation canvas |
| Canvas content | `onBackground` / `onSurface` | `#191C19` | `#E1E3DF` | Primary text and icons |
| Raised/alternate surface | `surfaceVariant` | `#DEE5D9` | `#424940` | List panes, autocomplete panels, grouped controls |
| Alternate content | `onSurfaceVariant` | `#424940` | `#C2C9BD` | Secondary copy, labels, metadata |
| Outline | `outline` | `#72796F` | `#8C9388` | Strong boundaries when required |
| Subtle outline | `outlineVariant` | `#C2C9BD` | `#424940` | Dividers and low-emphasis separation |

Rules:

- Use `MaterialTheme.colorScheme` roles, never raw colors in components.
- Green communicates selection, action, or Markdown structure. It is not decorative fill for large surfaces.
- Disabled and secondary states derive from the matching content role with reduced alpha.
- Destructive and warning states use Material semantic roles supplied by the active color scheme.

## 3. Typography

The default family is Android's system sans. Users may switch the entire writing surface to the system serif family; code remains monospace.

| Role | Compose style | Size | Weight | Line height | Usage |
|---|---|---:|---|---:|---|
| Page headline | `headlineLarge` | 32sp | SemiBold | 40sp | Large top-level titles |
| Screen headline | `headlineMedium` | 28sp | SemiBold | 36sp | Primary screen titles |
| Dialog/title | `titleLarge` | 22sp | SemiBold | 28sp | Major component titles |
| Row/title | `titleMedium` | 16sp | SemiBold | 24sp | Note and panel titles |
| Small title | `titleSmall` | 14sp | SemiBold | 20sp | Compact grouped headings |
| Writing body | `bodyLarge` | 16sp | Normal | 26sp | Editor and long-form reading |
| UI body | `bodyMedium` | 14sp | Normal | 20sp | Rows, command labels, supporting copy |
| Secondary body | `bodySmall` | 12sp | Normal | 16sp | Descriptions and metadata |
| Action label | `labelLarge` | 14sp | Medium | 20sp | Buttons and prominent controls |
| Panel label | `labelMedium` | 12sp | Medium | Section labels and selected metadata |
| Caption | `labelSmall` | 11sp | Medium | Statistics and quiet hints |

Rules:

- Long-form body copy uses `bodyLarge` and its 26sp line height.
- Command syntax previews use monospace with a theme text size, never a new scale.
- Do not use text smaller than `labelSmall`.

## 4. Spacing & Layout

The base unit is 4dp. Existing tokens are expressed as multiples of that unit.

| Token | Value | Usage |
|---|---:|---|
| Tight | 4dp | Panel insets and compact separation |
| Compact | 8dp | Icon/label gaps and row vertical padding |
| Standard | 12dp | Panel row horizontal padding |
| Canvas | 16dp | Lists, cards, and screen gutters |
| Writing | 20dp | Editor and preview horizontal rhythm |
| Comfortable | 24dp | Empty-state and grouped content spacing |
| Section | 32dp | Major section separation |

Layout rules:

- Compact widths use one navigation/editor surface at a time.
- Expanded widths use the existing 220dp tag rail, note-list pane, and flexible editor pane.
- Writing content stays visually centered and uses the existing editor width preference.
- Interactive rows retain at least a 48dp touch target.
- Temporary suggestion panels are height-bounded so the writing canvas and keyboard remain usable.

## 5. Components

### Writing canvas

- Structure: top app bar, editor/preview body, optional contextual surface, statistics, formatting toolbar.
- States: empty, editing, preview, focus, find/replace, autocomplete, loading existing note.
- Spacing: Writing and Comfortable tokens.
- Accessibility: editor has a content description; toolbar actions have tooltips and semantics.
- Motion: editor/preview uses the existing standard crossfade only.

### Suggestion surface

- Structure: tonal `Surface`, short label, full-width selectable rows.
- Variants: wikilink, tag, Quick Insert.
- States: default, selected, filtered, empty/hidden, touch pressed, keyboard focused.
- Spacing: Tight outer inset, Standard horizontal row padding, Compact vertical row padding.
- Accessibility: each row exposes a descriptive label and selected state; keyboard and touch produce the same action.
- Motion: no decorative motion; visibility follows the query state.

### Quick Insert panel

- Structure: suggestion surface containing an icon, localized command label, and monospace Markdown preview.
- Variants: block insertion, inline navigation insertion, external SAF image action, date insertion.
- States: default list, filtered list, keyboard-selected row, no-match hidden state.
- Spacing: matches Suggestion surface; maximum height preserves the editor viewport.
- Accessibility: 48dp rows, selected semantics, deterministic Up/Down/Enter navigation, screen-reader labels.
- Motion: none beyond normal Compose pressed-state feedback.

### Formatting toolbar

- Structure: horizontally scrollable icon-button groups separated by subtle dividers.
- States: default, pressed, focused, disabled.
- Accessibility: Material touch targets, content descriptions, long-press/hover tooltips.
- Quick Insert does not remove or reorder toolbar actions in v2.22.0.

### Note list row and adaptive panes

- Structure: tonal list surface, grouped section labels, note rows, selected row, optional tag rail.
- States: default, selected, pinned, archived/trashed in their respective screens, collapsed/expanded panes.
- Depth: tonal separation; avoid borders and shadows unless Material elevation communicates a temporary surface.

## 6. Motion & Interaction

| Type | Duration | Usage |
|---|---:|---|
| Micro | Material default | Press and selection feedback |
| Context switch | 220ms | Tablet note crossfade |
| Navigation | 280ms | Shared-axis phone navigation and predictive back |

Rules:

- Motion communicates navigation, content replacement, or selection only.
- Quick Insert appears and disappears with query state and adds no ornamental animation.
- Preserve focus after formatting, autocomplete, and Quick Insert actions.
- Respect the system animation scale and Compose accessibility behavior.

## 7. Depth & Surface

The strategy is tonal shift with minimal Material elevation.

- Main editor: `background`.
- Navigation/list panes: `surfaceVariant`.
- Temporary suggestion surfaces: `surfaceVariant` with 1dp tonal elevation.
- Selection: `secondaryContainer` or the existing selected-note container.
- Dividers: `outlineVariant` only where grouping is otherwise ambiguous.
- Avoid new drop shadows, gradients, glass effects, and arbitrary borders.

## 8. Accessibility Constraints & Accepted Debt

Constraints:

- Target WCAG 2.2 AA contrast through Material color roles.
- All icon-only controls have content descriptions; ambiguous controls also have tooltips.
- Interactive rows are at least 48dp high and work with touch and external keyboards.
- Selection is communicated through semantics as well as color.
- Text remains usable with Android font scaling and in all six supported locales.
- No interaction requires network access, analytics, or account state.

Accepted pre-existing debt:

| Item | Location | Why accepted | Exit |
|---|---|---|---|
| Empty editor uses a pencil emoji instead of a vector icon | `EditorScreen.kt` empty state | Pre-existing visual behavior outside the Quick Insert scope | Replace during a dedicated editor visual consolidation cycle |
| Some large settings/tag surfaces are sparse on tablets | Settings and standalone Tags screens | Does not block writing or Quick Insert | Address in a dedicated adaptive-layout polish cycle |
