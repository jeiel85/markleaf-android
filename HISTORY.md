# HISTORY

## 2026-04-30
- Work: v0.3.0 핵심 기능(FTS 검색 & 이미지 첨부) 개발 완료. DB 스키마 확장 및 에디터 기능 강화.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/local/entity/NoteFtsEntity.kt` (created)
  - `app/src/main/java/com/markleaf/notes/data/local/entity/AttachmentEntity.kt` (created)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/AttachmentDao.kt` (created)
  - `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteDao.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt` (updated)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (updated)
  - `app/build.gradle.kts` (updated)
- Verification:
  - FTS 검색 동작 및 성능 확인
  - 이미지 선택 및 미리보기 렌더링 확인
  - 스키마 마이그레이션 및 영속성 확인
- Result: Success

