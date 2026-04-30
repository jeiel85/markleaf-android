# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v0.4.0 - 2026-04-30

### Added
- 노트 간 링크(`[[Note Title]]`) 위키 링크 기능 추가.
- 프리뷰 모드에서 링크 클릭 시 해당 제목으로 검색 화면 이동 기능 구현.
- 링크 관계 저장을 위한 `note_links` 테이블 및 데이터 모델 구축.

### Changed
- 데이터베이스 스키마 버전 업그레이드 (v3 -> v4).
- 프리뷰어 파싱 로직 개선 (위키 링크 인식).


### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
