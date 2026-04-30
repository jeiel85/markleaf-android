# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v0.3.0 - 2026-04-30

### Added
- SQLite FTS4 기반 고성능 전문 검색(Full Text Search) 도입.
- 이미지 첨부 기능 지원 (미디어 피커 연동 및 로컬 URI 관리).
- 에디터 내 이미지 삽입 버튼 및 미리보기 모드 내 이미지 렌더링 추가.
- 이미지 로딩 최적화를 위한 Coil 라이브러리 통합.

### Changed
- 검색 쿼리 방식을 `LIKE`에서 `MATCH` 기반의 FTS 검색으로 전환 (대용량 노트 대응).
- 데이터베이스 스키마 버전 업그레이드 (v1 -> v3).


### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
