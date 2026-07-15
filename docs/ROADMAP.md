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

## v2.0–v2.22 — Bear-class 기능 기반 (완료)

v2.0–v2.22에서 Bear급 경험을 위한 기능 기반과 Android 플랫폼 마감을 완료했습니다:

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
- [x] F-Droid 제출 안정화, 홈 위젯, 생체 인증 잠금, PDF 출력, 오픈소스 투명성 (v2.15–v2.16)
- [x] 외부 Markdown/Text 열기·공유 가져오기와 폴더 미러 신뢰성 개선 (v2.17–v2.18)
- [x] 샘플 노트북 온보딩과 제목 기반 미러 파일명 (v2.19)
- [x] 키보드 단축키, 태그 자동완성, 목차, 세리프 글꼴, 태블릿 3-Pane (v2.20)
- [x] 예측형 뒤로가기, 화면 전환·목록 모션, 태그 레일 접기 (v2.21)
- [x] 순수 Markdown을 삽입하는 `/` Quick Insert와 6개 언어 명령 메뉴 (v2.22)

---

## v2.23+ — Bear-class 제품 응집도 (계획)

기준일: 2026-07-15, v2.22.0

Markleaf는 이미 라이브 Markdown, 표, 이미지, 위키링크·백링크, Quick Insert,
전문 검색, 폴더 미러, PDF, 위젯까지 갖췄다. 다음 단계의 목표는 기능 수를 늘리는 것이
아니라 다음 세 흐름을 하나의 조용한 제품 경험으로 묶는 것이다.

- **쓰기:** Markdown 원문 소유권을 유지하면서 마커와 도구가 필요할 때만 나타난다.
- **다시 찾기:** 태그·검색·스마트 컬렉션이 하나의 탐색 구조로 이어진다.
- **빠르게 담기:** Android 공유·선택·바로가기를 통해 앱 밖의 내용을 로컬 노트로 가져온다.

Bear는 기능·상호작용 품질의 벤치마크일 뿐 시각 계약이 아니다. Markleaf의 이름, 아이콘,
녹색 색상 체계, 카피, 정보 구조와 Android 플랫폼 관습은 독립적으로 유지한다.

### Phase 29 / v2.23 — Quiet Editor

목표: 상시 노출된 편집 chrome을 줄이고, 기존 기능을 잃지 않은 채 글이 화면의 주인공이 되게 한다.

작업:

- `DESIGN.md`에 compact formatting entry, selection-context actions, expanded style panel의
  상태·포커스·키보드·접근성 계약을 먼저 고정한다.
- 항상 보이는 가로 스크롤 툴바를 `Aa`/`+` 계열의 작은 진입점과 문맥 패널로 재구성한다.
- Bold, Italic, Link 같은 자주 쓰는 액션은 선택 상태 가까이에 두고 나머지는 확장 패널과
  기존 `/` Quick Insert에서 찾게 한다.
- 상단 앱 바의 편집/미리보기/공유/정보 액션을 재정렬하고 통계·목차·백링크를 하나의
  Info surface로 묶을지 실제 화면 상태를 기준으로 검증한다.
- 노트 목록 excerpt가 제목으로 사용된 첫 heading을 반복하지 않게 하고, Android plurals,
  vector empty-state icon, 태블릿의 sparse Tags/Settings surface를 마감한다.

완료 기준:

- 기존 formatting action 전부가 터치 두 단계 이내 또는 `/` 명령으로 도달 가능하다.
- 외장 키보드 단축키, 자동 저장, 편집 포커스, 태그/위키링크 자동완성 동작이 유지된다.
- phone과 tablet에서 빈 노트, 긴 노트, 선택 영역, 키보드 표시, 미리보기 전환을 수동 검증한다.
- TalkBack 라벨, 48dp 터치 영역, 6개 언어 리소스 parity와 Roborazzi 골든이 통과한다.

### Phase 30 / v2.24 — Living Markdown

목표: 별도 Preview로 이동하지 않아도 편집 화면 자체가 읽기 좋은 Markdown 문서처럼 보이게 한다.

작업:

- 커서가 있는 줄 또는 선택 영역에서는 원문 Markdown 마커를 보여 주고, 비활성 영역에서는
  마커가 시각적으로 물러나는 active-context syntax reveal을 구현한다.
- Compose text offset, selection, copy/paste, undo/redo, IME 조합 입력을 먼저 검증하는
  feasibility harness를 만든 뒤 실제 에디터에 적용한다.
- 체크리스트를 편집/미리보기 양쪽에서 같은 의미로 직접 토글할 수 있게 한다.
- heading과 list folding을 일시적 표시 상태로 제공하되 저장 Markdown은 바꾸지 않는다.
- 표는 별도 블록 데이터 모델을 만들지 않고, 표준 GFM Markdown에 행/열을 추가·삭제하는
  구조화된 편집 액션만 제공한다.

완료 기준:

- DB와 폴더 미러의 본문은 이전과 동일한 순수 Markdown 문자열이다.
- 어떤 표시 상태에서도 cursor/selection offset이 원문과 어긋나지 않는다.
- 한글·일본어 IME, RTL 후보 텍스트, TalkBack, 하드웨어 키보드, 대용량 노트를 검증한다.
- 편집 성능이 기존 Macrobenchmark와 대용량 노트 기준에서 유의하게 회귀하지 않는다.

이 단계는 `docs/AGENT_SPEC.md`의 *WYSIWYG 블록 에디터 제외*와 충돌하지 않는다. 저장 모델과
편집 source of truth는 Markdown이며, rich surface는 원문 위의 가역적인 표현 계층이다.

### Phase 31 / v2.25 — Smart Library

목표: 태그 화면을 보조 기능이 아니라 노트를 다시 찾는 주 탐색 표면으로 승격한다.

작업:

- phone navigation sheet와 tablet tag rail이 같은 정보 구조를 사용하게 한다.
- All Notes, Today, Todos, Attachments, Untagged, Conflicts 스마트 컬렉션을 로컬 인덱스로 제공한다.
- 자주 쓰는 태그 고정, 태그 트리 검색·접기, 태그별 노트 수를 제공한다.
- 반복 검색을 저장하고 Quick Switcher와 Search 화면에서 같은 결과 규칙을 사용한다.
- 정렬, preview density, attachment thumbnail 표시 여부를 목록 단위로 조절할 수 있게 한다.

완료 기준:

- 노트 생성 → 태그 입력 → 태그 탐색 → 검색 → 원문 복귀가 phone/tablet 모두 일관된다.
- 스마트 컬렉션은 새 서버·권한 없이 Room/FTS/기존 첨부·충돌 인덱스만 사용한다.
- 10k+ 노트 데이터셋에서 탐색과 필터가 측정 가능한 기준 안에 머문다.

### Phase 32 / v2.26 — Capture Everywhere

목표: Android 어디에서든 선택한 내용을 Markleaf에 빠르게 담되 데이터 이동은 사용자의 명시적
동작으로만 일어나게 한다.

작업:

- `ACTION_PROCESS_TEXT`로 선택한 텍스트를 새 노트 또는 기존 노트에 추가한다.
- 공유 앱이 `EXTRA_HTML_TEXT`를 제공할 때만 기기 안에서 표준 Markdown으로 변환하고,
  제공하지 않은 웹 페이지를 Markleaf가 직접 다운로드하지 않는다.
- 여러 이미지 공유 가져오기와 출처 URL/제목 보존 규칙을 정의한다.
- 런처 바로가기에 새 노트, 검색, 선택한 고정 태그 진입을 제공한다.

완료 기준:

- `android.permission.INTERNET`과 새 백엔드 없이 모든 capture flow가 동작한다.
- 공유 입력 크기, MIME, 실패 메시지를 시스템 경계에서 검증한다.
- 가져온 결과는 표준 Markdown과 기존 attachment 저장 규칙으로 export/mirror round-trip된다.

### Phase 33 / v2.27 — Personal Writing

목표: 브랜드를 희석하지 않는 작은 선택지로 사용자가 자기 글쓰기 표면을 조절하게 한다.

작업:

- Markleaf Paper, Forest Night, Graphite처럼 성격이 분명한 소수의 light/dark palette를 검토한다.
- 본문 크기, 줄 간격, 문단 간격을 제한된 preset으로 제공한다.
- 시스템 sans/serif와 Material You를 계속 지원한다.
- 번들 글꼴은 6개 언어 coverage, 오픈소스 라이선스, APK 크기, 렌더링 성능이 모두 입증될
  때만 검토한다.

완료 기준:

- 각 조합이 WCAG AA, 큰 글꼴, 6개 언어, phone/tablet Roborazzi를 통과한다.
- 테마 선택이 Markleaf의 녹색 identity와 semantic color 역할을 깨지 않는다.

### 공통 전달 게이트

각 phase와 minor release는 다음을 모두 만족해야 한다.

- `android.permission.INTERNET`, 계정, API, 분석, 광고, 추적, remote config 추가 금지
- 순수 Markdown export/import/folder-mirror round-trip 유지
- 비파괴 Room migration과 기존 사용자 데이터 보존
- 관련 unit/UI/Roborazzi 테스트, `testDebugUnitTest`, `lintRelease`, `assembleDebug` 통과
- 실제 phone/tablet에서 핵심 흐름 수동 QA
- 릴리스 시 해당 버전의 hardening candidates를 별도 GitHub Issue 또는 Backlog에 기록

### 명시적 비범위와 보류

- Bear의 이름, 아이콘, 색상, 문구, 정확한 레이아웃·상호작용 복제
- Markleaf 자체 클라우드, 로그인/계정, 실시간 협업, AI 글쓰기 API
- 저장 모델을 블록 구조로 바꾸는 full WYSIWYG editor
- 기능 응집도보다 앞선 OCR, 스케치, DOCX/ePub/JPG export 확장
- 수십 개의 테마와 아이콘 변형
- 개별 노트 암호화/E2EE는 이 로드맵에서 예약하지 않는다. 검색·첨부·폴더 미러와 함께 다룰
  별도 threat model과 migration 설계가 승인된 뒤에만 검토한다.

### 벤치마크 참고 자료

- [Bear 2 공식 기능 개요](https://bear.app/faq/whats-new-in-bear-2/)
- [Bear 공식 도움말 기능 목록](https://bear.app/faq/)

벤치마크는 기능 후보와 interaction quality를 점검하는 용도로만 사용하며, Markleaf의 제품
계약은 `docs/AGENT_SPEC.md`, `DESIGN.md`, 이 문서 순으로 결정한다.
