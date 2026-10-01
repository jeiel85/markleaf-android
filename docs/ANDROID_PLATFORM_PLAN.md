# Android Platform Integration Plan

## Date
2026-10-01 (기준: github/main `51e85143`, v2.58.0)

## 질문
"iOS의 Bear"처럼 "Android의 그것"이라고 하면 Markleaf가 떠오르게 하려면 최신 Android의
무엇을 더 받아들여야 하는가. 그리고 그것을 어떤 순서로, 어떤 제약 안에서 만드는가.

## Conclusion

- **기능 수보다 진입점과 손맛.** 이미 있는 것: 동적 색상, 테마 아이콘, edge-to-edge, 앱별 언어,
  공유 요소 전환, predictive back 옵트인, 생체 인증 잠금, 위젯 2종, Notes 역할(#481, D078),
  스타일러스 필기(편집기가 `BasicTextField`라 Compose 1.7+에서 Android 14+ 자동 지원 —
  기기 확인 전). 남은 차이는 "시스템 곳곳에서 한 번에 Markleaf로 들어오는 길"과 하드웨어
  키보드·드래그 같은 대화면 관습이다.
- **기능 트랙과 SDK 트랙을 분리한다.** 아래 F1–F5는 현재 도구(AGP 8.7.3, compileSdk 35,
  Compose BOM 2024.12.01) 그대로 만들 수 있다. SDK 업그레이드를 기다리지 않는다.
- **Material 3 Expressive는 지금 하지 않는다.** 두 가지 사실 때문이다.
  1. 안정판 `material3 1.4.0`에서 `MaterialExpressiveTheme`·`MotionScheme.expressive()`는
     `internal`이다. public API는 `1.5.0-alpha29`에만 있고, 그 aar은 **compileSdk 37 +
     AGP 9.1.0 + Kotlin 2.2**를 요구한다(아래 버전 매트릭스).
  2. `DESIGN.md` §6이 "nothing bounces or overshoots", "no bouncing, scaling, or decorative
     morph"를 명시한다. Expressive의 핵심이 바로 그것이다. 도입하려면 디자인 계약을 먼저
     바꾸는 결정이 필요하다(Q2).
- 이 문서는 `docs/ROADMAP.md`의 보류된 **Phase 32 — Capture Everywhere** 중 런처 바로가기와
  `ACTION_PROCESS_TEXT`를 다시 꺼내고, 대화면 입력(F3·F4)과 위젯 미리보기(F5)를 더한다.
- Status: **계획 확정, 미착수.** Q1–Q3은 메인테이너 결정 대기.

## 확인한 사실 — 버전 매트릭스 (2026-10-01, Google Maven aar 메타데이터)

| 아티팩트 | minCompileSdk | 최소 AGP | kotlin-stdlib |
|---|---:|---:|---:|
| compose ui 1.11.4 (BOM 2026.06.01) | 35 | 8.6.0 | 2.1.20 |
| compose ui 1.12.x (BOM 2026.08.00+) | **37** | **9.1.0** | 2.1.20 |
| material3 1.4.0 (BOM 2025.10 ~ 2026.09 공통) | 35 | 8.6.0 | 2.0.21 |
| material3 1.5.0-alpha29 | **37** | **9.1.0** | 2.2.20 |
| core 1.17.0 / activity 1.13.0 | 36 | 8.9.1 | 2.1.20 |

- AGP 8.x 최신은 8.13.2, Robolectric 최신은 4.17.
- **AGP 8 안에서 갈 수 있는 마지막 BOM은 2026.06.01**(Compose 1.11.4, material3 1.4.0)이다.
  2026.08.00부터는 AGP 9 이전이 필요하다.

## 트랙 S — SDK·도구 (기능과 독립, 필요 시점에 진행)

| 단계 | 내용 | 근거 / 선행 |
|---|---|---|
| S1 | AGP 8.13.x, Gradle wrapper 맞춤, Robolectric 4.16+, `compileSdk = 36` | `docs/TARGET_API_36_EVALUATION.md` PR 1 그대로 |
| S2 | `targetSdk = 36` + 기기 검증 | 같은 문서 PR 2 |
| S3 | Compose BOM → 2026.06.01, Kotlin 2.1.x(+KSP·compose 플러그인 동반) | F 트랙에 필요 없음. 새 `HapticFeedbackType`(Confirm·ToggleOn/Off 등)을 쓰려면 필요 |
| S4 | AGP 9.1+, `compileSdk = 37`, BOM 2026.08+ | Q2가 "도입"일 때만. AGP 9는 Kotlin 내장·DSL 변화가 있어 단독 PR |

각 단계는 단독 PR이며, Roborazzi 기준 이미지는 CI(Linux)에서만 재기록한다.

## 트랙 F — 기능

### F1. 런처 앱 바로가기 (아이콘 길게 누르기)

사용자 문제: 새 노트·검색까지 "앱 열기 → 버튼"의 두 단계가 필요하다.

동작:
- 고정 항목 2개: **새 노트**, **검색**.
- 선택 항목: **최근 노트** 최대 2개(제목 표시). 기본값은 Q1.
- 고정 노트 바로가기(편집기 메뉴 "홈 화면에 추가", `requestPinShortcut`)는 F1b로 분리.
  단일 노트 위젯과 겹치므로 F1 출시 뒤 필요성을 다시 본다.

구현:
- **정적 `shortcuts.xml`을 쓰지 않는다.** `android:targetPackage`는 리터럴이어야 하는데 debug
  빌드는 `com.markleaf.notes.debug`(#319)라서 패키지를 박을 수 없다(AGENTS.md 규칙). 전부
  `ShortcutManagerCompat.setDynamicShortcuts`로 `context.packageName` 기반 인텐트를 만든다.
  대가: 설치 후 앱을 한 번 열어야 바로가기가 생긴다.
- 인텐트 재사용: 새 노트 = `QuickNoteWidget.ACTION_CREATE_NOTE`(→ `requestsNewNote()`),
  최근 노트 = `QuickNoteWidget.ACTION_OPEN_NOTE` + `EXTRA_NOTE_ID`. 검색만 새 action이 필요하다.
  `MainActivity.onCreate`·`onNewIntent`의 entry 판별과 `MarkleafNavHost`의 dispatch에
  `NavRoutes.SEARCH`를 더한다. 회전 재생 방지(`unlessConsumedBy`, `dispatchLaunchRequest`)
  규칙을 그대로 따른다.
- 갱신 시점: 노트 변경 시 이미 불리는 `WidgetRefresh.notesChanged` 옆에 한 줄로 둔다.
  노트를 열 때 `reportShortcutUsed`.
- **프라이버시 규칙(위젯과 동일, `SingleNoteWidget`의 `note.locked -> null` 선례):** 잠긴
  노트·휴지통 노트는 바로가기에 절대 싣지 않는다. 이미 실린 노트가 잠기거나 휴지통으로 가면
  즉시 제거한다. App lock은 `BiometricLockGate`가 그대로 막는다.

테스트: 바로가기 목록 생성 규칙(잠김·휴지통 제외, 개수 상한) 순수 함수 단위 테스트, 검색
action 라우팅 테스트(`NotesRoleIntentTest`·`LaunchDispatchTest` 패턴), 회전 시 재생 없음.
기기: 런처 길게 누르기 → 3개 경로, 노트 잠금 후 최근 노트 바로가기 사라짐.

### F2. 텍스트 선택 메뉴 "Markleaf에 추가" (`ACTION_PROCESS_TEXT`)

사용자 문제: 다른 앱의 문장을 담으려면 복사 → 앱 전환 → 새 노트 → 붙여넣기의 네 단계가 필요하다.
공유 시트보다 한 단계 짧은, Android에만 있는 경로다.

동작: 선택 메뉴에서 "Markleaf에 추가"를 누르면 그 텍스트로 새 노트가 열린다. 원래 앱의
텍스트는 바꾸지 않는다(`setResult` 없음). 기존 노트에 덧붙이기는 범위 밖이다(Phase 32의 "기존
노트에 추가"는 고를 화면이 필요해 별도로 다룬다).

구현:
- `MainActivity`에 `PROCESS_TEXT`/`text/plain` 필터를 두고, 메뉴 문구는 필터의
  `android:label`로 준다(`ResolveInfo.loadLabel`이 필터 라벨을 먼저 쓴다). 기기에서 라벨이
  "Markleaf"로만 보이면 전용 라벨을 단 `<activity-alias>`로 바꾼다. 사이드로드 매니페스트
  사본에도 같은 블록을 둔다(`SideloadManifestParityTest`).
- `EXTRA_PROCESS_TEXT`(CharSequence → 평문)를 `extractSharedText`와 같은 `ImportedContent`
  경로로 보낸다. `onNewIntent` entry 판별에도 추가한다.
- 문자열 1개 × 11개 로케일.

테스트: 인텐트 → 본문 추출 단위 테스트, 매니페스트 alias·필터 존재 테스트.
기기: Chrome·메시지 앱에서 선택 → 메뉴 표시 → 새 노트, 앱이 열린 상태에서도 1회만 생성.

### F3. 하드웨어 키보드 단축키 도우미

사용자 문제: Ctrl+B/I/K, Ctrl+Shift+S, Ctrl+Z/Y, 목록 Ctrl+K가 이미 있지만 어디에도 보이지
않는다. Android 16 데스크톱 창 모드·외부 디스플레이에서 이 차이가 크다.

동작: Meta+/(시스템 단축키 도우미)에 Markleaf 그룹이 나온다. 새로 Ctrl+N(새 노트),
Ctrl+F(노트 안 찾기 / 목록에서는 검색)를 더한다.

구현:
- `MainActivity.onProvideKeyboardShortcuts`(API 24+)에서 그룹(앱 / 편집기)을 반환한다.
- **단축키 표를 한 곳에 둔다.** `formattingShortcutFor`·`undoShortcutFor`·목록의 Ctrl+K가
  각자 키를 들고 있다. 표 하나에서 핸들러와 도우미 목록을 모두 만들고, "도우미에 보이는 모든
  키에 핸들러가 있다"를 테스트로 고정해 둘이 어긋나지 않게 한다.

테스트: 표 ↔ 핸들러 대응 테스트, Ctrl+N/Ctrl+F Compose 키 이벤트 테스트.
기기: 태블릿 + 물리 키보드(또는 에뮬레이터 키보드)로 Meta+/ 확인.

### F4. 드래그 앤 드롭 (분할 화면·데스크톱 창)

사용자 문제: 분할 화면에서 갤러리 사진이나 브라우저 텍스트를 끌어 넣을 수 없다.

동작:
- 넣기: 편집기에 이미지(`image/*`)를 놓으면 기존 첨부 규칙대로 복사되고 `![](…)`가 커서
  위치에 들어간다. 텍스트는 그대로 들어간다.
- 꺼내기(F4b): 목록의 노트를 끌어 다른 앱에 Markdown 텍스트로 놓는다. 잠긴 노트는 끌 수 없다.

구현:
- 먼저 `Modifier.contentReceiver`(foundation 1.7, experimental)를 스파이크한다. 붙여넣기·드롭·
  키보드 이미지 삽입(Gboard)을 한 경로로 받으므로, 되면 F4 하나로 세 입구가 생긴다. 안 되면
  `dragAndDropTarget` + `requestDragAndDropPermissions`.
- 이미지 삽입은 지금 `imagePickerLauncher` 콜백 안에 있다(`EditorScreen.kt`). 그 몸체를
  `insertImageAtCursor(uri)`로 빼서 선택기·드롭이 같이 쓴다. 실패 메시지(`attachment_failed`)도
  공유한다.

테스트: 추출한 삽입 함수 단위 테스트, 드롭 수신 Compose 테스트(가능한 범위).
기기: 태블릿 분할 화면에서 사진 앱 → 편집기, 브라우저 텍스트 → 편집기.

### F5. 위젯 선택기 미리보기

사용자 문제: 위젯 고르는 화면에 빠른 노트는 `+` 아이콘, 단일 노트는 앱 아이콘만 보인다.

구현: 두 `appwidget-provider`에 `android:previewLayout`(API 31+)을 추가한다. 실제 위젯
레이아웃을 쓰되 예시 내용은 번역된 문자열로 채운다. `previewImage`는 API 26–30 대비로 남긴다.
Android 15의 생성 미리보기(`setWidgetPreview`, 사용자 색 반영)는 rate limit와 갱신 시점 설계가
필요해 이번 범위 밖이다.

기기: API 31+ 런처 위젯 선택기, 라이트/다크.

### 결정이 필요한 항목

- **Q1. 최근 노트 바로가기의 기본값.** 노트 제목이 런처에 보이고, 일부 런처(Pixel 등)는
  바로가기를 런처 검색에도 노출한다. 데이터가 기기를 떠나지는 않지만 잠금 해제된 홈 화면에서는
  보인다. **제안:** 새 노트·검색은 항상 켜고, 최근 노트는 설정 토글을 두어 기본 꺼짐으로 한다
  (이슈 대응 루틴의 "옵션화 + 기존 동작 기본값").
- **Q2. Material 3 Expressive.** 하려면 `DESIGN.md` §6의 "no bounce" 계약 개정 + S4(AGP 9) +
  material3 1.5 안정판이 모두 필요하다. **제안:** 지금은 하지 않는다. 1.5 안정판이 나오면 바운스
  없는 구조적 컴포넌트(태블릿 서식용 floating toolbar, button group)만 따로 평가한다. 손맛은
  S3 이후 햅틱 어휘를 정리하는 것으로 먼저 얻는다(체크박스 토글, 고정, 휴지통 이동).
- **Q3. 잠금 화면 빠른 캡처.** D078의 Implications대로 별도 액티비티와 새 결정이 필요하다
  (App lock·잠긴 노트·빈 노트 처리). **제안:** F1–F5 이후 별도 평가 문서로 다룬다.

## 하지 않는 것

| 항목 | 이유 |
|---|---|
| 빠른 설정 타일 직접 구현 | 새 노트 진입은 Notes 역할의 시스템 진입점(스타일러스 버튼, 지원 기기의 메모 타일)과 F1이 맡는다. 시스템 타일이 없는 기기가 많다는 사용자 증거가 나오면 그때 다시 본다 |
| 온디바이스 AI (ML Kit GenAI / Gemini Nano) | Play 서비스, 즉 폐쇄 SDK. 스펙 §15.5가 AI 글쓰기 도우미를 영구 제외 |
| AppFunctions | 호출 주체가 시스템 어시스턴트(Gemini)라 "사용자 명시 행동 시에만 데이터 이동" 경계가 불분명 |
| Live Updates (ProgressStyle) | 메모 앱에 진행 중 작업이 없다 |
| Glance로 위젯 재작성 | 현 RemoteViews 위젯이 잘 동작한다. 재작성은 사용자 가치가 없다 |

## 출시 단위 (제안)

| 버전 | 내용 | CHANGELOG 제목 후보 |
|---|---|---|
| v2.59.0 | F1 + F2 | Markleaf from anywhere |
| v2.60.0 | F3 | Your keyboard knows Markleaf |
| v2.61.0 | F4 (+F4b) | Drag it in |
| v2.61.x 또는 F1에 동반 | F5 | — |

S1–S3는 기능 릴리스와 섞지 않고 별도 PR로 넣는다. 각 릴리스는 `ROADMAP.md`의 공통 전달 게이트와
`AGENTS.md`의 이슈 대응 3–6단계(태그 → Release 자산 확인 → Discussions 공지 → #262 섹션)를 따른다.

## Risks

- **동적 바로가기만 쓰는 대가**: 설치 직후·데이터 삭제 직후에는 바로가기가 없다. 앱을 열면 생긴다.
- **`contentReceiver`는 experimental**: BOM을 올릴 때 시그니처가 바뀔 수 있다. 사용처를 한 파일에
  가둔다.
- **기기 검증 공백**: Notes 역할 선택은 에뮬레이터에서 불가(D078). F3·F4는 태블릿이 필요하며,
  Lenovo 태블릿의 실사용 설치본은 건드리지 않고 debug(`.debug`) 빌드를 나란히 설치해 확인한다.
