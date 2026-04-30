# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v1.0.4 - 2026-04-30

### Fixed
- GitHub Release notes now use the matching `CHANGELOG.md` version section instead of GitHub auto-generated notes.
- Existing `v1.0.2` and `v1.0.3` release notes were corrected to match the documented release history.

### Verification
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.3 - 2026-04-30

### Fixed
- GitHub Release now attaches only the signed release APK.
- Release APK asset name is normalized to `markleaf-vX.Y.Z.apk`.
- Removed duplicate debug APK upload workflow from tag releases.

### Verification
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.2 - 2026-04-30

### Changed
- Added release signing automation for GitHub tag releases.
- Added optional local release signing configuration through ignored signing properties.
- Added release signing documentation.

## v1.0.0 - 2026-04-30

### Added
- **정식 출시 (First Major Release)**
- **Tablet Two-Pane Layout**: 큰 화면에서 목록과 에디터를 동시에 볼 수 있는 최적화된 레이아웃 지원.
- **Backlinks**: 현재 노트를 참조하는 다른 노트들의 목록을 에디터에서 바로 확인 및 이동 가능.
- **Backup & Restore**: 모든 노트와 이미지 에셋을 포함한 ZIP 파일 백업 및 전체 복구 기능.
- **Material You**: Android 12 이상 기기에서 시스템 배경화면에 맞춘 다이내믹 컬러 테마 지원.
- **Search & Performance**: SQLite FTS4 기반의 초고속 전문 검색 엔진 완성.
- **Media Support**: 이미지 첨부 및 에디터 내 실시간 미리보기 기능 안정화.

### Changed
- 전반적인 UI/UX 폴리싱 및 간격 조정.
- 데이터베이스 스키마 최종 안정화 (v4).


### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
