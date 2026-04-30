# Image Attachments Evaluation (Issue #3)

## Date
2026-04-30

## Conclusion
Adopt image attachments in a constrained local-first model, not full rich media.

## Scope Recommendation
- First scope: attach image from device and reference it from note.
- No cloud media sync, no remote hosting, no heavy editor embedding system.

## Storage Model
- Keep note text in Room as-is.
- Store attachment metadata in local DB:
  - attachment id
  - note id
  - uri/path
  - mime type
  - created at
- Render references in markdown using local URI abstraction.

## UX Model
- Add "Attach image" action in editor.
- Show inline placeholder with tap-to-open preview.
- Keep editor responsive by loading thumbnails lazily.

## Risks
- Storage growth and orphan file cleanup complexity.
- URI permission persistence differences across Android versions.
- Export behavior ambiguity for image-linked markdown.

## Export Policy
- Single note export: markdown + optional assets folder.
- Full export: per-note folders or shared assets folder with stable naming.

## Decision
- Proceed with minimal attachment scope after tablet UX stabilization.
- Defer advanced features (drag-drop reordering, rich resizing, annotations).
