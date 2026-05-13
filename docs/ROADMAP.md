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

## v2.x — Bear-class 확장 (완료)

v2.0–v2.14에서 Bear급 경험을 위한 다음 항목들을 완료했습니다:

- [x] Bear-class 라이브 프리뷰 (Inline Rich Rendering, v2.0)
- [x] SAF 폴더 미러 다중 기기 동기화 (v2.1)
- [x] Macrobenchmark 성능 측정 인프라 (v2.2)
- [x] CommonMark 표준 파서 도입 (v2.3)
- [x] `[[Title]]` 위키링크 + 백링크 부활 (v2.4)
- [x] 이미지 첨부 부활 (v2.5)
- [x] 테마 선택 (Markleaf 그린 / Material You, v2.5.1)
- [x] 태블릿 UX 다듬기 (v2.5.2)
- [x] 문서 정리 (v2.5.3)
- [x] 동기화 완성 — 영구 삭제 연동, 첨부 파일 동기화 (v2.6)
- [x] 위키링크 자동완성 (v2.7)
- [x] 이미지 alt 편집 다이얼로그 (v2.8)
- [x] 동기화 충돌 시 사본 보존 (v2.9)
- [x] 표준 마크다운 링크 클릭 동작 (v2.9.2)
- [x] 코드 블록 syntax highlighting (v2.10)
- [x] GFM 테이블 부활 (v2.11)
- [x] 빠른 이동 (Quick switcher / Ctrl+K, v2.12)
- [x] 노트 안에서 찾기/바꾸기 (v2.13)
- [x] 각주 ref ↔ def 클릭 점프 (v2.14)

---

## GitHub Open Issues — 향후 작업

현재 GitHub에 열려 있는 이슈 기반 우선순위. 상세 스펙은 `docs/ISSUE_BACKLOG_DETAIL.md` 참조.

### 완료 (GitHub Close 필요)
- [#27] 예측 뒤로 가기 제스처 — v1.5.0 완료
- [#65] SQLite FTS5 통합 검색 — v0.3.0 완료
- [#76] WCAG 접근성 최적화 — v1.7.0 완료

### 보류
- [#51] 다국어 지원 확대 (JP/FR/DE) — native 화자 검증 필요
- [#53] 스토어 스크린샷 리디자인 — Play Console 등록 시점
- [#54] 전문 브랜드 피처 그래픽 업데이트 — Play Store 등록 시점

### 계획
- [#37] 생체 인식 앱 잠금
- [#38] 고품질 PDF 내보내기
- [#39] 홈 화면 위젯
- [#52] 전략적 인앱 리뷰 요청 UI
- [#55] 오픈소스 투명성 강조
- [#57] 인터랙티브 온보딩 가이드

모든 작업은 §2 로컬 우선 / 개인정보 보호 원칙을 유지하며, `android.permission.INTERNET`을 추가하지 않는다.
