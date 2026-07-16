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

- Structure: quiet top app bar, editor/preview body, optional temporary surface, and a footer containing only the contextual formatting entry/actions while source editing is available.
- States: empty, editing, selected text, formatting expanded, preview, focus, find/replace, autocomplete, loading existing note.
- Spacing: Writing and Comfortable tokens.
- Accessibility: the editor has a content description; app-bar and footer actions have localized labels, tooltips where ambiguous, and state semantics.
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

### Compact formatting entry

- Purpose: expose formatting without reserving a permanent row of editor chrome.
- Structure: one 48dp `Aa` button in the editor footer, leading-aligned opposite the quiet statistics. It uses an icon plus the localized accessible label `Formatting`; `Aa` is a visual affordance, not the accessible name.
- Visibility: shown only while the Markdown source is editable. It is hidden in preview and focus modes and while Quick Insert, autocomplete, find/replace, or an external picker owns the current interaction.
- States: collapsed, pressed, keyboard/accessibility focused, expanded, disabled during save/load transitions where source edits are unavailable.
- Styling: collapsed uses `onSurfaceVariant` on the canvas. Expanded uses `secondaryContainer` and `onSecondaryContainer`. State is never communicated by color alone.
- Adaptive behavior: the entry stays attached to the editor column, not the window edge. Compact and expanded layouts use the same entry and action order; expanded width does not reintroduce a persistent toolbar.

### Selection-context actions

- Trigger: a non-collapsed source selection replaces the lone compact entry with a short footer action group. It supplements, and never replaces, Android's Cut/Copy/Paste selection menu.
- Primary order: Bold, Italic, Link, More. `More` opens the expanded style panel with the same selection intact.
- Structure: four 48dp icon buttons in a fixed, non-scrolling row. Each action has a localized content description and long-press/hover tooltip.
- Behavior: applying Bold, Italic, or Link uses the existing Markdown transformation, keeps the resulting text selected when possible, and returns input focus to the editor. Collapsing the selection restores the compact `Aa` entry.
- Fallback: when available width or font scaling cannot present the group without clipping, show Bold, Italic, and More; Link remains the first inline action in the expanded panel. No action becomes horizontal-scroll-only.

### Expanded style panel

- Purpose: provide the complete existing formatting inventory within two deliberate actions while keeping the writing canvas visually primary.
- Structure: a non-modal tonal surface anchored above the active footer trigger (`Aa` or `More`). On compact widths it spans the editor content width above the IME; on expanded widths it is a bounded popover anchored to the editor column. It never dims the canvas or becomes full-screen.
- Groups and stable order:
  - Inline: Bold, Italic, Strikethrough, Inline code, Link.
  - Structure: Heading, Bulleted list, Numbered list, Checklist, Quote.
  - Block and media: Code block, Divider, Image.
- Labels: every action combines an icon and localized text in the panel. Existing insertion/transformation semantics remain unchanged; the Heading action continues to use the existing heading behavior rather than introducing a new document model.
- States: closed, opening, open, action pressed, action keyboard/accessibility focused, disabled, and internally scrolled. The first and last groups remain discoverable at Android font scaling without clipping critical labels.
- Coexistence: Quick Insert remains the slash-command path and keeps its v2.22 command order. Opening Quick Insert, autocomplete, find/replace, preview, or focus mode closes the style panel. Opening the style panel dismisses a transient suggestion surface without changing source text.

### Formatting state and focus contract

| Editor context | Formatting surface | Input and accessibility focus | Dismissal/result |
|---|---|---|---|
| Editable, collapsed caret | Compact `Aa` entry | Editor keeps text input focus | Entry opens the expanded panel |
| Editable, selected text | Bold / Italic / Link / More group | Text selection remains authoritative | Direct action returns focus to editor; More opens panel |
| Expanded panel opened by touch | Active trigger plus anchored panel | Editor retains input focus and IME; panel receives touch semantics | Action closes panel and restores the resulting caret/selection |
| Expanded panel opened by keyboard or assistive technology | Active trigger plus anchored panel | First enabled action receives navigation/accessibility focus; source selection is retained | Back/Escape closes first and returns focus to the originating trigger/editor |
| Quick Insert, autocomplete, or find/replace active | Formatting surfaces hidden | Active temporary surface owns navigation focus | Dismiss temporary surface before formatting can open |
| Preview or focus mode | Formatting surfaces hidden | Current mode keeps focus | Return to editable mode before formatting |
| SAF image picker active | Formatting surfaces hidden | Platform picker owns focus | Cancel/result returns to editor at the retained insertion point |

Focus rules:

- Opening formatting by touch must not explicitly dismiss the IME or collapse the source selection.
- Back and Escape close the expanded panel before leaving the editor. Tapping outside closes the panel without editing source text.
- Existing hardware formatting shortcuts remain authoritative and work whether the panel is open or closed. The editor keeps its current Tab behavior; the formatting entry must not steal Tab while source input has focus.
- D-pad, switch access, TalkBack, and keyboard activation reach the same actions in visual order. When the panel opens from non-touch input, focus begins at the first enabled action and does not cycle outside the panel until it is dismissed.
- Image launches the existing Storage Access Framework path. On cancel or completion, restore the editor selection/insertion point and do not reopen the panel automatically.

### Editor app bar and note information surface

- Purpose: keep the document title and mode change legible while collecting secondary document facts in one predictable place instead of scattering them across the footer, preview app bar, and document tail.
- App-bar order: Back, screen title, Preview/Edit, Note information, More. The mode action uses distinct Preview/Edit vector icons plus localized accessible names, preserving title width at large text sizes without relying on color alone. Focus mode replaces the action set with Exit focus mode. Find, Focus mode, system share, Markdown export, PDF export, and Move to trash live in More in task order; saved-note-only actions remain hidden for a new unsaved note.
- Note information trigger: one 48dp icon action is present in both edit and preview modes. It is not nested in More and uses the localized accessible name `Note information`.
- Surface: a Material modal bottom sheet with the fixed reading order Note information -> Statistics -> Table of contents -> Linked from. The sheet uses the existing canvas/alternate surface roles and spacing tokens; it adds no new color, elevation, or decorative motion.
- Statistics: the existing localized word, character, and reading-time summary is always the first section. Moving it into this sheet removes permanent statistics text from the editor footer.
- Table of contents: headings are derived only while preview or the information sheet needs them, avoiding Markdown parsing on every editing keystroke. Heading rows preserve level indentation and a 48dp target. Selecting a heading while editing enters Preview and scrolls to the same rendered heading; selecting in Preview scrolls directly.
- Linked from: backlinks are available from edit and preview for a saved note. Each row opens the source note. New notes and notes without backlinks show a localized quiet empty state instead of omitting the section.
- States: new/empty note, saved note without headings or backlinks, populated statistics only, populated outline, populated backlinks, long vertically scrolling content, dark theme, large text, and all supported locales.
- Dismissal and coexistence: Back, swipe, or outside tap dismisses the sheet without changing source text. Opening it closes formatting and overflow surfaces. Returning from the sheet does not force editor focus unless a heading navigation explicitly changes mode.
- Accessibility: the sheet title and section labels expose heading semantics; every navigable row has a localized label and at least a 48dp target. Empty states are textual, traversal order matches visual order, and hierarchy is never communicated by indentation or color alone.
- Adaptive behavior: phone uses the standard bottom sheet; expanded layouts keep Material's bounded sheet width so information stays attached to the editor task rather than spanning the whole window.

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
- The compact entry and selection-context group replace each other with Material state feedback only; no bouncing, scaling, or decorative morph is introduced.
- The expanded style panel uses the Material visibility transition appropriate to its anchored surface and respects the system animation scale.
- Preserve focus after formatting, autocomplete, and Quick Insert actions.
- Respect the system animation scale and Compose accessibility behavior.

## 7. Depth & Surface

The strategy is tonal shift with minimal Material elevation.

- Main editor: `background`.
- Navigation/list panes: `surfaceVariant`.
- Temporary suggestion and formatting surfaces: `surfaceVariant` with 1dp tonal elevation.
- Selection: `secondaryContainer` or the existing selected-note container.
- Dividers: `outlineVariant` only where grouping is otherwise ambiguous.
- Avoid new drop shadows, gradients, glass effects, and arbitrary borders.

## 8. Accessibility Constraints & Accepted Debt

Inclusive interaction contexts:

| Context | Product need | Formatting contract response |
|---|---|---|
| Phone writer with the IME visible | Maximum writing height and one-handed recovery from a formatting action | One compact entry; anchored panel does not explicitly dismiss the IME |
| Tablet or hardware-keyboard writer | Predictable shortcuts and focus order without a phone-only layout | Existing shortcuts stay authoritative; panel is anchored to the editor column |
| TalkBack, switch-access, or limited-dexterity writer | Large targets, named state, deterministic traversal | 48dp actions, localized labels/state, fixed visual and semantic order |
| Large-text or translation-expanded UI | No clipped critical action or hidden horizontal-scroll dependency | Panel scrolls vertically; contextual group uses the documented fallback |

Constraints:

- Target WCAG 2.2 AA contrast through Material color roles.
- All icon-only controls have content descriptions; ambiguous controls also have tooltips.
- Interactive rows are at least 48dp high and work with touch and external keyboards.
- Selection is communicated through semantics as well as color.
- Text remains usable with Android font scaling and in all six supported locales.
- Formatting entry exposes expanded/collapsed state; panel groups and selected/toggled actions expose their role and state to accessibility services.
- No formatting command depends on hover, color, gesture timing, or a horizontally scrolled icon strip.
- No interaction requires network access, analytics, or account state.

Implementation evidence (2026-07-15):

- The focused Roborazzi harness covers collapsed, selected-text, expanded, keyboard-focused, disabled, dark-theme, 1.5x font-scale, Korean, and expanded-width states.
- Compose behavior tests cover touch and keyboard opening, first-action focus and activation, wrapped panel navigation, panel-focus shortcuts, Escape/context-action dismissal, direct selected-text actions, expanded actions, and disabled state. Product integration tests cover Markdown application and Quick Insert preemption.
- The permanent toolbar was removed after the showcase passed. Full unit tests, release lint, debug APK assembly, and the new snapshot suite pass; API 36 emulator boot remained `offline`, so device-level IME/TalkBack coverage stays in the Phase 29 final QA task.

Accepted pre-existing debt:

| Item | Location | Why accepted | Exit |
|---|---|---|---|
| Empty editor uses a pencil emoji instead of a vector icon | `EditorScreen.kt` empty state | Pre-existing visual behavior outside the Quick Insert scope | Replace during a dedicated editor visual consolidation cycle |
| Some large settings/tag surfaces are sparse on tablets | Settings and standalone Tags screens | Does not block writing or Quick Insert | Address in a dedicated adaptive-layout polish cycle |

## 9. Public Web Landing Contract

### Audience and page job

The GitHub Pages landing surface serves Android writers who are deciding whether
to install Markleaf, especially privacy-conscious F-Droid users and people who
need portable Markdown files. Its single job is to prove the real writing
experience and data-ownership contract, then lead visitors to the current
F-Droid release without overstating unavailable distribution channels.

Inclusive review contexts:

- A phone visitor comparing note apps one-handed before installation.
- A privacy-conscious visitor verifying the no-account and no-INTERNET claims.
- A keyboard or low-vision visitor who needs visible focus, predictable reading
  order, AA contrast, and comfortable Korean line wrapping.

### Direction and signature

The direction is **Quiet proof**: warm green-neutral paper, restrained leaf
green, large plain-spoken Korean copy, and real product evidence rather than
decorative marketing chrome. The memorable moment is a real editor screenshot
paired with a small DOM-rendered `.md` receipt that makes local ownership
visible. The existing feature graphic may remain an Open Graph asset, but it is
not the hero background because its embedded logo and copy compete with the
page heading.

No testimonial, pricing, generic three-card feature row, stock photography, or
ornamental motion is introduced. The product screenshots and the local-file
receipt carry the visual identity.

### Web tokens

The web surface maps directly to the product palette and declares the following
CSS custom properties before use:

| Web token | Value | Product role / usage |
|---|---:|---|
| `--color-canvas` | `#F9FBF9` | `background`; page canvas |
| `--color-surface` | `#FFFFFF` | `surface`; screenshot and primary action surfaces |
| `--color-surface-soft` | `#EEF4EA` | quiet alternate section |
| `--color-primary` | `#2E7D32` | primary action and Markdown evidence |
| `--color-primary-strong` | `#1B5E20` | headings and strong action state |
| `--color-primary-ink` | `#00390A` | high-contrast brand text |
| `--color-primary-soft` | `#D5E8CF` | proof and selected surfaces |
| `--color-ink` | `#191C19` | primary copy |
| `--color-muted` | `#52634F` | secondary copy |
| `--color-line` | `#C2C9BD` | separators where tonal shift is insufficient |
| `--color-focus` | `#38656A` | keyboard focus outline |

Spacing uses the existing 4-unit rhythm through `--space-1` (4px),
`--space-2` (8px), `--space-3` (12px), `--space-4` (16px), `--space-6`
(24px), `--space-8` (32px), `--space-12` (48px), `--space-16` (64px), and
`--space-24` (96px). Corner radii are `--radius-small` (8px),
`--radius-medium` (16px), and `--radius-large` (24px); inner controls never
use a larger radius than their container.

The only pronounced elevation is `--shadow-product`, a green-tinted two-layer
shadow for real app screenshots. Other hierarchy uses whitespace, separators,
and tonal shifts rather than card shadows.

### Web typography

- Use a local system stack only; the public page does not contact a font CDN.
- Display and section headings use the system display face at SemiBold/Bold with
  tight tracking and balanced wrapping.
- Body copy uses the system UI face with a maximum readable measure of 65ch.
- Permission names, file paths, and `.md` evidence use the system monospace
  stack and never simulate code with literal backticks in prose.
- The minimum visible text size is 14px; long Korean copy uses `text-wrap:
  pretty` and avoids forced single-line layout.

### Web layout and primitives

The content order is fixed:

1. Sticky header with brand and in-page navigation.
2. Asymmetric hero: value proposition and install action beside real app proof.
3. Trust ledger: current version, no-INTERNET, and plain Markdown ownership.
4. Three sequential story rows: write, organize, own/export.
5. Privacy flow: device storage -> explicit user action -> chosen destination.
6. Download hand-off with F-Droid recommended and other channels accurately
   labeled.
7. Compact legal/source footer.

Reusable primitives and states:

- `Action link`: primary, secondary, hover, pressed, keyboard focus.
- `Proof item`: label, value, supporting copy; separated by rules rather than a
  floating card.
- `Product frame`: real screenshot, caption, loading state provided by the
  image element, and responsive crop that never replaces the screenshot with a
  mock UI.
- `Story row`: text-first and media-first variants, alternating only on wide
  screens; DOM reading order stays text then evidence.
- `Privacy step`: ordered step with connector, compact stacked mobile state,
  and no color-only meaning.
- `Channel row`: recommended, available, and paused states with textual status.

### Web responsive, motion, and accessibility rules

- Validate at 375px, 768px, and 1280px. No horizontal overflow or clipped CJK
  text is accepted.
- Images declare intrinsic width and height. Below-fold screenshots use native
  lazy loading; the hero screenshot is eagerly fetched.
- Interactive targets are at least 48px high. Every link has a visible
  `:focus-visible` outline that is stronger than a color-only change.
- A skip link precedes the sticky header. Section anchors account for the
  sticky header offset.
- Motion is limited to hover, press, and focus feedback on interactive
  elements, using only `transform`, `opacity`, and color. There is no decorative
  entrance or scroll animation. `prefers-reduced-motion: reduce` removes smooth
  scrolling and non-essential transitions.
- Semantic landmarks, heading order, descriptive screenshot alternative text,
  and native ordered lists remain authoritative. The visual arrangement never
  changes DOM task order.
- The page makes no analytics, advertising, tracking, account, or runtime API
  requests. Metadata may link to public Markleaf pages, and fonts/assets ship
  from the same GitHub Pages origin.
