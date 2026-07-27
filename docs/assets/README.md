# Public assets

What each file here is for, and — for the ones recorded from a running build —
which version they show. Two things went unrecorded before this file existed:
why unreferenced files are kept, and how anyone would notice a clip has gone
stale (#258).

## Demo clips

| File | Surface | Recorded on |
|---|---|---|
| `markleaf-tablet-<lang>.gif` (6) | Landing story 01 + README hero, one per language | v2.30.0 |
| `markleaf-demo.gif` | Older single-language demo | v2.23.0 |

`scripts/verify-landing-versions.ps1` asserts that each language surface points
at its **own** clip and that the file exists — a page referencing another
language's GIF renders fine and would otherwise ship silently.

Nothing asserts that a clip still matches the current UI. That is a judgement
call, not a check: re-record when a change would make the clip misleading, and
update the version above when you do. A clip showing an older layout is only a
problem once the layout it shows is gone.

## Screenshots

`markleaf-editor-*.webp`, `markleaf-privacy-*.webp`, `markleaf-tags-*.webp` —
the landing page screenshots, in 720px and 1280px variants. The version they
show is carried in each page's `<figcaption>`, which
`verify-landing-versions.ps1` requires to agree across the six languages.

### `markleaf-preview-720.webp` / `markleaf-preview-1280.webp` — kept deliberately

**Nothing in this repository references these two.** They are kept anyway, and
this is the record of that decision so a future cleanup does not have to guess.

They were served from GitHub Pages under stable URLs
(`https://jeiel85.github.io/markleaf-android/assets/markleaf-preview-1280.webp`)
while they were in use, and those URLs may be linked from places outside this
repository — a store listing draft, a forum post, a bookmark. Deleting the
files breaks those links for no benefit: together they are ~93 KB and cost
nothing to keep.

Remove them only if the Pages site itself moves or is retired, which breaks the
URLs regardless.

## Store and brand

| File | Use |
|---|---|
| `markleaf-feature-graphic-1024x500.png` | Play Console feature graphic |
| `markleaf-playstore-icon-512.png` | Play Console app icon |
| `logo.svg` | Landing header |

## Badges

| File | Use |
|---|---|
| `trendshift-daily-kotlin.svg` | Landing hero award badge, all six languages |

### Why a local copy and not Trendshift's live badge URL

Trendshift serves this badge from `trendshift.io/api/badge/…`, and that is what
the six READMEs embed: GitHub proxies README images through camo, so a reader
never contacts Trendshift. GitHub Pages proxies nothing. Embedding the live URL
here would have every visitor's browser hit a third party, on the page whose
whole argument is that Markleaf does not phone home — so the landing pages serve
this checked-in copy instead.

The cost is that the file no longer follows the live badge. That is acceptable
because it records a dated achievement — #1 Kotlin repository of the day, first
reached 23 Jul 2026 — and not a current standing. Re-download it only if the
award itself changes:

```
curl -L "https://trendshift.io/api/badge/trendshift/repositories/58116/daily?language=Kotlin" -o docs/assets/trendshift-daily-kotlin.svg
```
