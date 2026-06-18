# Welcome to Markleaf

Markleaf is a quiet, local-first Markdown notebook for Android. It opens fast, stays out of the way, and keeps your writing as plain text you own.

## A tiny tour

- Open **A Beautiful Markdown Canvas** to see the writing surface.
- Open **Daily Writing Ritual** for a journal-style example.
- Open **Project Brief** to see tasks, links, and structure.
- Open **Local Folder Mirror** when you want files outside the app.

> [!TIP]
> These are regular notes. Edit them, export them, move them to trash, or delete them when you no longer need the tour.

#start #guide

---markleaf-note---

# A Beautiful Markdown Canvas

![Markleaf sample canvas](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown stays readable as text, then becomes calm and polished in **Preview**.

## What this note demonstrates

- **Bold**, _italic_, ~~strikethrough~~, and `inline code`
- Headings, lists, checklists, quotes, dividers, code blocks, tables, callouts, footnotes, links, and images
- Live syntax styling while you type

> [!NOTE]
> Switch between Edit and Preview from the top bar. The note is still just Markdown.

| Element | Use it for |
| --- | --- |
| `#tag` | organization |
| `[[Project Brief]]` | local note links |
| `![](...)` | image attachments |

```kotlin
fun markleaf() = "local-first markdown"
```

A small footnote keeps details nearby without interrupting the paragraph.[^1]

[^1]: Footnotes, callouts, tables, and code blocks are all rendered locally.

#markdown #showcase

---markleaf-note---

# Daily Writing Ritual

## Morning page

The goal is not to write more. The goal is to make the first sentence easy.

- [x] Capture one thought
- [ ] Turn one task into a note
- [ ] Link related work to [[Project Brief]]

> Keep the note small enough that you will actually return to it.

## Evening close

What moved today?

1. One useful decision
2. One open question
3. One thing to leave for tomorrow

#journal #writing

---markleaf-note---

# Project Brief

This note shows how Markleaf can hold a small project without becoming heavy.

## Outcome

Ship a clean sample notebook that teaches by being useful.

## Plan

- [x] Show Markdown syntax beautifully
- [x] Include an image attachment
- [ ] Try search with `local-first`
- [ ] Open backlinks from **Daily Writing Ritual**

## Notes

Related: [[Daily Writing Ritual]] and [[Tags, Search, and Backlinks]]

#project/markleaf #planning

---markleaf-note---

# Tags, Search, and Backlinks

Type tags directly in the body: #project, #writing, #privacy, #local-first.

## Search ideas

Try searching for:

- `local-first`
- `folder mirror`
- `Project Brief`

## Backlinks

Wikilinks use `[[Note Title]]`. When another note links here, Markleaf can show that relationship locally. No account or server is involved.

See also [[Project Brief]].

#organize #search

---markleaf-note---

# Local Folder Mirror

Markleaf does not need its own cloud. Instead, you can choose a folder and let Android or your sync tool handle that folder.

## What happens

- Markleaf writes each note as a Markdown file.
- Frontmatter keeps the stable `markleaf_id`.
- Attachments stay beside the mirrored notes.
- The app still declares no INTERNET permission.

## Why it matters

Your notes remain readable in other Markdown tools, and sync stays your choice.

#privacy #folder-mirror #local-first
