# HISTORY

## 2026-04-30
- Work: v0.4.0 핵심 기능(노트 간 링크) 개발 완료. 위키 링크 파싱 및 네비게이션 연동.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/local/entity/NoteLinkEntity.kt` (created)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteLinkDao.kt` (created)
  - `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (updated)
  - `app/build.gradle.kts` (updated)
- Verification:
  - `[[제목]]` 파싱 및 시각적 스타일 확인
  - 링크 클릭 시 검색 화면 이동 확인
  - DB 스키마 v4 마이그레이션 확인
- Result: Success

