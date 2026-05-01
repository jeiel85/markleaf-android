# Markleaf Roadmap

## 방향

Markleaf는 기능 수보다 빠른 글쓰기 경험, 디자인, 로컬 저장, 데이터 소유권을 우선한다.

---

## Phase 0 - 문서와 작업 시스템

목표:

- 에이전트가 반복 작업할 수 있는 기반 마련

작업:

- `docs/AGENT_SPEC.md`
- `AGENTS.md`
- `.agent/tasks.md`
- `.agent/progress.md`
- `.agent/decisions.md`
- `.agent/RALPH_PROMPT.md`
- `README.md`

완료 기준:

- 랄프 루프가 작업 목록을 읽고 한 task씩 진행할 수 있다.

---

## Phase 1 - 프로젝트 기반

목표:

- Android 앱이 실행되고 기본 화면 이동 구조가 있다.

작업:

- Kotlin + Jetpack Compose 프로젝트 생성
- `applicationId = com.markleaf.notes`
- Material 3 theme
- navigation skeleton
- placeholder screens
- GitHub Actions build workflow
- no INTERNET permission 확인

완료 기준:

- `./gradlew assembleDebug` 통과
- 앱 실행 가능
- 빈 Notes List 화면 표시
- placeholder 화면 이동 가능
- `android.permission.INTERNET` 없음

---

## Phase 2 - 로컬 노트

목표:

- 노트 생성, 수정, 저장이 가능하다.

작업:

- Room 설정
- NoteEntity
- NoteDao
- AppDatabase
- NoteRepository
- Notes List
- Editor
- Auto-save
- Title extraction
- Excerpt generation

완료 기준:

- 사용자가 노트를 만들 수 있다.
- 본문을 수정하면 자동 저장된다.
- 앱 재시작 후 노트가 유지된다.
- 기본 unit test가 있다.

---

## Phase 3 - 태그

목표:

- 본문 내 `#태그`로 노트를 정리할 수 있다.

작업:

- TagEntity
- NoteTagCrossRef
- TagDao
- TagParser
- Korean tag support
- tag reindex on save
- tag list screen
- tag filtering

완료 기준:

- `#주일학교`, `#project-alpha` 같은 태그가 파싱된다.
- Markdown heading은 태그로 처리되지 않는다.
- URL fragment는 태그로 처리되지 않는다.
- 태그별 노트 필터링이 가능하다.

---

## Phase 4 - 검색과 휴지통

목표:

- 사용자가 노트를 찾고 안전하게 삭제/복원할 수 있다.

작업:

- Search screen
- Debounced search
- Room LIKE search
- Move to trash
- Restore from trash
- Delete forever confirmation
- Empty states

완료 기준:

- 제목/본문 검색이 가능하다.
- 삭제된 노트는 기본 목록에서 사라진다.
- Trash 화면에서 복원 가능하다.
- 영구 삭제에는 확인 절차가 있다.

---

## Phase 5 - 내보내기와 마감

목표:

- 사용자가 데이터를 Markdown 파일로 가져갈 수 있다.

작업:

- Slug generator
- Single note export
- Export all notes
- Android Storage Access Framework
- Share note
- Settings screen
- App version
- Typography polish
- F-Droid dependency review

완료 기준:

- 단일 노트 Markdown export 가능
- 전체 노트 Markdown export 가능
- 공유 가능
- 최종 빌드 통과
- INTERNET 권한 없음
- tracking/ads/analytics 없음

---

## Later

나중에 검토할 항목:

- Markdown preview renderer
- SQLite FTS
- Image attachments
- `[[note links]]`
- Tablet two-pane layout
- Optional backup
- WebDAV sync
- Google Drive backup
- F-Droid flavor
- Play Store flavor

네트워크 기능은 반드시 별도 설계 검토 후 결정한다.

---

## Phase 9 - Bear-Class Product Polish

목표:

- Android에서 Bear급으로 느껴지는 로컬 Markdown 글쓰기 경험을 만든다.
- 기능적 유사성은 허용하되, 이름, 아이콘, 색상, 화면 구성, 문구, 브랜딩은 복제하지 않는다.
- MVP의 no-INTERNET, no-API, no-login, no-analytics 원칙은 유지한다.

작업:

- 첫 실행 샘플 노트 온보딩
- Markdown 편집 툴바 개선
- 노트 목록과 편집기 빈 상태 개선
- 노트, 태그, 링크를 빠르게 찾는 quick-open 검색
- 태그 화면의 카운트와 탐색 개선
- 백링크 문맥 표시 개선
- 백업/내보내기 상태 메시지 개선
- 대량 노트 성능 점검

상세 평가는 `docs/BEAR_BENCHMARK_GAP.md`에 기록한다.
