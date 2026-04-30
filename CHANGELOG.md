# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v0.1.0 - 2026-04-30

### Added
- Initial Android app foundation with Kotlin, Jetpack Compose, and Material 3.
- Local-first note architecture with Room entities/DAOs/repositories for notes and tags.
- Core screens: notes list, editor, tags, search, trash, and settings.
- Markdown-oriented utilities: title extraction, tag parsing, slug generation, export/share helpers.
- Gradle wrapper/configuration and GitHub Actions Android build workflow.

### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
