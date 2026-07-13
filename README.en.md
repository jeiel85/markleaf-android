# <img src="docs/assets/logo.svg" width="48" height="48" align="center" /> Markleaf

<p align="center">
  <img src="docs/assets/logo.svg" width="160" height="160" alt="Markleaf Logo" />
</p>

<p align="center">
  <strong>Thoughts that pile up lightly, tidy Markdown notes</strong><br />
  A local-first, minimal Markdown note app for Android
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-D22128" alt="License" />
  <img src="https://img.shields.io/badge/F--Droid-Available-1976D2?logo=fdroid&logoColor=white" alt="F-Droid" />
  <img src="https://img.shields.io/badge/Google%20Play-Updates%20paused-9E9E9E?logo=googleplay&logoColor=white" alt="Google Play" />
</p>

<p align="center">
  <a href="README.md">한국어</a> ·
  <strong>English</strong> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.de.md">Deutsch</a>
</p>

<p align="center">
  <a href="https://github.com/jeiel85/markleaf-android">GitHub repository</a> ·
  <a href="https://gitlab.com/jeiel85/markleaf-android">GitLab public mirror</a>
</p>

---

## 🍃 What is Markleaf?

**Markleaf** is an Android Markdown note app designed to strip away the clutter so you can focus on just two things: capturing and organizing. Your data is stored only on your device, and standard Markdown guarantees full ownership and portability. Even sync happens only through *a folder you choose* — Markleaf itself never goes online.

[**View the branding page**](https://jeiel85.github.io/markleaf-android/) · [Current version: v2.22.0](https://github.com/jeiel85/markleaf-android/releases/tag/v2.22.0) · [GitLab release mirror](https://gitlab.com/jeiel85/markleaf-android/-/releases/v2.22.0) · [Privacy Policy](https://jeiel85.github.io/markleaf-android/privacy.html) · [F-Droid](https://f-droid.org/packages/com.markleaf.notes/) · [Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes)

---

## ✨ Key Features

### Writing & Preview
- **`/` Quick Insert** — search commands at the start of a line to insert headings, lists, tables, callouts, wikilinks, images, and more as standard Markdown
- **Live Markdown preview** — toggle instantly between editing and preview, or use the *Show Markdown syntax* option for live syntax coloring
- **GFM tables / checkboxes / blockquotes / callouts (`> [!NOTE]` …)** — all rendered in preview
- **Code block syntax highlighting** — token coloring for 10 languages: Kotlin, Java, Python, JavaScript/TypeScript, Bash, JSON, YAML, XML, SQL
- **Footnote (`[^N]`) ref ↔ def jump** — tap the superscript to scroll smoothly to the definition
- **Image attachments + alt-text editing** — kept as isolated copies in the app's internal storage (no media permission required)
- **Smart Markdown formatting toggle** — wrap the selection or the word around the cursor in Bold/Italic/Strikethrough/Inline Code, and tap again to cleanly unwrap text that's already wrapped
- **Keyboard shortcuts** — Ctrl/Cmd+B, I, K, Shift+S for bold, italic, link and strikethrough on a hardware keyboard
- **Table of contents (TOC)** — in preview mode, jump to H1–H3 headings to navigate long notes
- **Serif / Sans font choice** — switch the writing surface to a serif face for a book-like feel; code blocks always stay monospaced
- **Focus mode / word, character & reading-time stats / find & replace within a note**

### Organizing & Navigating
- **Tag-based classification + autocomplete** — just write `#tags` in the body for automatic indexing, no folders; existing tags autocomplete as you type `#`
- **Wikilinks (`[[Title]]`) + backlinks panel** — autocomplete, and see at a glance what points to this note
- **Quick switcher (Ctrl+K)** — Obsidian-style title substring jump
- **SQLite FTS full-text search** — fast, down to the body text
- **Pin / archive / trash** — trash asks once more before permanent deletion

### Sync & Export (No-Cloud principle)
- **Folder mirror sync** — mirrors each note as a **title-named** `.md` / `.txt` file to a folder you pick via SAF (Drive/Dropbox/Syncthing/OneDrive/NAS, etc.); rename a note and its file follows. Markleaf itself stays offline; sync is delegated to *whatever external app syncs that folder*
- **Import external `.md` / `.txt` files** — tap a file in your file manager or share one from another app to bring it in as a new note (the file name becomes the title when there's no heading). Tags in synced-in notes are recognized right away
- **Export individual / all notes as `.md`**
- **Send via the system share sheet**

### Design & Accessibility
- **Markleaf green theme + Material You toggle** — Android 12+ system wallpaper colors optional
- **Automatic dark mode** — follows the system setting
- **Tablet 3-pane layout** — tag sidebar · note list · editor; tap a tag in the sidebar to filter the note list in place (note list still collapsible)
- **UI in 6 languages** — Korean / English / Spanish / Japanese / French / German resources
- **Block screenshots / recent-apps preview option** — for sensitive notes

---

## 🛠 Tech Stack

Markleaf follows current Android development standards with a modern, maintainable stack.

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 + Material You dynamic color
- **Architecture**: simple layered separation (core / data / domain / feature / ui) + Repository pattern
- **Database**: [Room](https://developer.android.com/training/data-storage/room) — SQLite-backed local persistence, FTS4 virtual tables for full-text search
- **Markdown parser**: [commonmark-java](https://github.com/commonmark/commonmark-java) (CommonMark 0.30 + GFM extensions: tables, strikethrough, task lists, footnotes, YAML frontmatter)
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Storage Access Framework (SAF)** — folder mirror sync + image attachments
- **Image loading**: [Coil](https://coil-kt.github.io/coil/) — F-Droid-friendly Apache 2.0
- **DataStore Preferences** — app settings
- **Profile Installer 1.4.0 + Macrobenchmark** — cold-start baseline profile measurement (326ms on a TB320FC)
- **Testing**: JUnit + Robolectric + [Roborazzi](https://github.com/takahirom/roborazzi) visual regression tests (Linux goldens, threshold 0.005)
- **CI**: GitHub Actions + GitLab CI — independent builds and signed releases, launch-smoke, record-roborazzi

---

## 🏗 Architecture

Markleaf uses the following layered structure for separation of concerns and testability.

```text
com.markleaf.notes
├── core          # shared core logic: markdown processing, attachments, sync
├── data          # Room DB, entities, repository implementations (data source)
├── domain        # models, repository interfaces (business logic)
├── feature       # per-screen UI and ViewModels (presentation)
│   ├── editor    # editor, find/replace, wikilink autocomplete, callouts, tables
│   ├── notes     # note list, quick switcher, archive
│   ├── search    # FTS full-text search
│   ├── tags      # tag index
│   ├── trash     # trash / permanent delete
│   └── settings  # theme, sync folder, screenshot blocking, etc.
├── navigation    # Jetpack Compose Navigation setup
└── ui            # theme (Markleaf green / Material You), shared components
```

---

## 🚀 Getting Started

### Installation

> [!NOTE]
> **Google Play updates are currently on hold.** New versions won't be pushed to the Play Store until a Korean business-registration policy requirement for the solo developer is resolved. In the meantime, **get the latest version from F-Droid, GitHub Releases, or GitLab Releases.** (If you already installed it from the Play Store, it keeps working.)

- **F-Droid** *(recommended)*: [Markleaf on F-Droid](https://f-droid.org/packages/com.markleaf.notes/) — search in the F-Droid client or install via the link above. It uses the same signing key (SHA-256 `0be97352…f91a`), so updates continue seamlessly even if you sideloaded an APK from GitHub or GitLab Releases.
- **Direct APK install**: download the APK from the [GitHub v2.22.0 release](https://github.com/jeiel85/markleaf-android/releases/tag/v2.22.0) or the [GitLab v2.22.0 release](https://gitlab.com/jeiel85/markleaf-android/-/releases/v2.22.0), then run it on your Android device.
- **Google Play**: [Markleaf on Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes) — **updates are paused** (see the note above). If you already have it, it keeps working, but get the latest version from F-Droid, GitHub, or GitLab.

### Building from source
If you'd like to build or contribute, follow these steps.

```bash
# Clone the repository
git clone https://github.com/jeiel85/markleaf-android.git

# Enter the project folder
cd markleaf-android

# Build and install
./gradlew installDebug
```

---

## 🔒 No-Cloud by design

Markleaf itself never goes to the network. Whether your data leaves the device is *entirely your choice*.

- ✅ **No** `android.permission.INTERNET` declared — Markleaf makes no network requests itself
- ✅ **No** Markleaf server / backend
- ✅ **No** analytics / ads / tracking / closed-source SDKs
- ✅ `android:allowBackup="false"` — Markleaf data is excluded from Android auto-backup / device transfer
- ✅ Data only ever moves through OS paths when *you* export, share, open an external link, or pick a SAF folder
- ✅ Fully open source, auditable by anyone under Apache 2.0

How "never leaves your device" works exactly is documented in the [Privacy Policy](docs/PRIVACY.md) and the [No-Cloud Certification](docs/NOCLOUD_CERTIFICATION.md).

---

## 🗺 Roadmap

### v1.x — MVP
- [x] Basic Markdown editing and saving
- [x] Tag-based filtering and search
- [x] New app icon and branding
- [x] Live Markdown preview and dark mode
- [x] High-performance SQLite FTS search
- [x] Tablet 2-pane layout optimization
- [x] Single / all-note Markdown export
- [x] v1.0.0 stable release

### v2.x — Bear-class expansion (current)
- [x] **v2.3** CommonMark parser — callouts, GFM strikethrough, task lists, footnotes, YAML frontmatter
- [x] **v2.4–2.5** Wikilinks (`[[Title]]`) + autocomplete + backlinks panel
- [x] **v2.6** Image attachments + alt text + lightbox
- [x] **v2.7** SAF folder mirror sync (Drive/Dropbox/Syncthing delegation, still no INTERNET)
- [x] **v2.8** Material You toggle + Markleaf green theme restored
- [x] **v2.9** Screenshot blocking option, visual regression testing (Roborazzi) established
- [x] **v2.10** Code block syntax highlighting (10 languages)
- [x] **v2.11** GFM table preview revived
- [x] **v2.12** Quick switcher (Ctrl+K)
- [x] **v2.13** Find / replace within a note
- [x] **v2.14** Footnote ref ↔ def click jump
- [x] **v2.15** F-Droid submission stabilization and no-cloud documentation
- [x] **v2.16** Home screen widget, biometric lock, open-source transparency, smart Markdown formatting
- [x] **v2.17** Open/share import of external `.md`/`.txt` files, folder-sync duplicate-note and tag recognition fixes
- [x] **v2.18** Folder-sync files named after the note title (rename follows) + `.md`/`.txt` choice
- [x] **v2.19** Six sample notes on first launch + PDF/Markdown export no longer duplicates the title
- [x] **v2.20** Keyboard shortcuts, `#tag` autocomplete, table of contents, serif font, tablet 3-pane (tag sidebar + in-place filter) layout
- [x] **v2.21** Predictive back, polished transitions, list/card motion, foldable tablet tag rail, checklist toggles
- [x] **v2.22** `/` Quick Insert commands with touch, hardware-keyboard selection, and six localized menus
- [x] **Google Play public launch** — anyone can install it from the Play Store

---

## 📜 License

This project is licensed under the **Apache License 2.0**. See the `LICENSE` file for details.

---

<p align="center">
  Made with ❤️ by <strong>Markleaf Team</strong>
</p>
