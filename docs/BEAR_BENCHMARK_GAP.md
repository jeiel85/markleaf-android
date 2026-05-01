# Bear Benchmark Gap Review

Date: 2026-05-01

## Position

Markleaf should aim for a Bear-class local Markdown writing experience on Android, while remaining an independent product.

Functional similarity is acceptable when it serves common note-app expectations:

- fast Markdown writing
- tag-based organization
- wiki links and backlinks
- search
- export and backup
- polished phone and tablet layouts

Markleaf must not copy Bear's name, icon, brand identity, color system, exact layout, marketing copy, or distinctive visual expression.

## Current Assessment

Markleaf is a stable local-first Markdown MVP, not yet a Bear-class product.

Strong areas:

- local-first storage
- no account, API, analytics, ads, tracking, or INTERNET permission
- Markdown notes
- tag parsing
- search
- trash and restore
- Markdown export
- ZIP backup and restore
- Markdown preview
- image attachments
- wiki links and backlinks
- tablet two-pane baseline
- signed GitHub release automation

Main gaps:

- first-run experience was empty and did not teach the product
- editor lacks a high-polish formatting surface
- Markdown preview is basic and does not cover richer writing cases
- tag navigation is functional but not yet delightful
- search lacks quick-open style speed and refinement
- backup/export UX needs more confidence signals
- tablet layout exists but needs visual and interaction polish
- large-library performance is not yet proven

## Product Target

Markleaf should feel like:

- a writing app first
- local and private by default
- fast enough to trust for daily capture
- visually quiet but not bare
- powerful through Markdown, tags, links, and export

## Phase 9 - Bear-Class Product Polish

P0:

- Add first-run starter notes that demonstrate Markdown, tags, wiki links, backlinks, backup, and privacy.
- Improve editor toolbar for common Markdown actions.
- Improve note list and editor empty states.
- Verify first-run, create, edit, tag, search, link, backup, restore, and release install flows on a real tablet.

P1:

- Add quick-open style search for notes, tags, and links.
- Improve tag screen with counts, nested-tag display rules, and clearer filtering.
- Improve backlinks with better context snippets.
- Improve export and backup status messages.
- Add large dataset performance checks.

P2:

- Evaluate richer Markdown support such as tables and footnotes.
- Evaluate app shortcuts and share-sheet import.
- Evaluate optional user-controlled sync only after a separate privacy and F-Droid review.

## Explicit Non-Goals For MVP

- cloud sync
- login
- analytics
- ads
- proprietary crash reporting
- remote config
- AI writing APIs
- automatic upload of note content
