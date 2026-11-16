# Target API 36 Evaluation

## Date
2026-09-17

## 질문
Google Play Console이 "앱이 Android 16(API 36) 이상을 타겟팅해야 함 — 2026-08-31부터 미준수 시
앱 업데이트 출시 불가"를 통보했다(마지막으로 올린 App Bundle의 targetSdk는 35).
무엇을 해야 하고, 언제 해야 하는가.

## Conclusion

- **당장 막히는 것은 Play 업데이트뿐이다.** Play 배포는 보류 중(D072 이후 태그 런이 AAB를 만들지
  않음)이므로 실질 영향은 없다. GitHub Release·F-Droid 배포, 이미 설치한 사용자, 스토어 노출은
  이 정책과 무관하다. 기한 연장 요청은 하지 않는다.
- **그래도 올린다 — 긴급이 아닌 정기 유지보수로.** Play 재개 시 선행 조건이고, 태그 런 APK는
  GitHub·F-Droid 사용자의 API 36 기기에서 그대로 돌며, AGP 8.7·Robolectric 4.14가 이미 뒤처져
  있어 미룰수록 한 번에 올릴 폭이 커진다.
- **지원 기기 범위는 바뀌지 않는다.** `targetSdk`/`compileSdk`는 "어느 버전의 동작 규칙을 따르는가"와
  "어느 API로 컴파일하는가"이고, 설치 가능한 최저 버전은 `minSdk = 26`(Android 8.0)이 정한다.
  이 작업에서 `minSdk`는 손대지 않는다.
- Status: **PR 1(빌드 도구) 진행 — 2026-11-16.** PR 2(`targetSdk = 36`)는 미착수.

## 현재 상태 (github/main `dbe7bda`)

| 항목 | 값 |
|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 35 / 35 / 26 (`app`, `benchmark` 동일) |
| AGP | 8.7.3 (공식 지원 API 35까지) |
| Gradle wrapper | 8.9 |
| Kotlin | 2.0.21 |
| Robolectric | 4.14.1 (SDK 35까지 — target 36이면 JVM 테스트 전체 실패) |

## PR 1 결과 (2026-11-16)

Play Console 정책 상태에 같은 요구가 "이 문제가 있는 앱 업데이트가 거부됩니다"(시행일 2026-11-01)로
다시 떴다. Play 배포 보류는 그대로라 결론은 바뀌지 않았고, 계획대로 PR 1부터 진행했다.

| 항목 | 이전 | 이후 |
|---|---|---|
| AGP | 8.7.3 | 8.13.2 (AGP 8 마지막 패치) |
| Gradle wrapper | 8.9 | 8.14.7 |
| Robolectric | 4.14.1 | 4.16.1 (SDK 36 지원 시작) |
| `compileSdk` | 35 | 36 (`app`, `benchmark`) |
| `targetSdk` / `minSdk` | 35 / 26 | 그대로 |

- Gradle 8.9는 AGP 8.13 플러그인을 해석하지 못한다(플러그인 메타데이터가 더 새 Gradle을 요구) —
  래퍼를 먼저 올려야 한다.
- **Robolectric 4.15부터 `MimeTypeMap`이 실제 매핑을 갖는다.** `DocumentFile.fromFile`(RawDocumentFile)
  은 이름에 확장자를 무조건 덧붙이므로 temp 디렉터리로 SAF 폴더를 흉내 내는 테스트가 `x.md.md`,
  `.json.json`을 만들어 3개 클래스 4건이 실패했다. 실제 SAF 제공자는 이미 확장자가 붙은
  이름에 다시 붙이지 않으므로, `ProviderFileNamingMimeTypeMap` shadow로 확장자 조회만 null로 두고
  `robolectric.properties`로 모든 단위 테스트에 건다(이전 버전에서 우연히 맞던 가정을 명시한 것).
- `compileSdk 36`에서 위젯의 `setRemoteAdapter`·`notifyAppWidgetViewDataChanged`가 deprecated
  경고로 나온다(`RemoteCollectionItems`로 대체 권장). 동작 변화는 없고 PR 2의 위젯 확인 때 함께 본다.

## Plan

두 PR로 나눈다. 도구 회귀와 동작 변경 회귀를 한 diff에 섞지 않기 위해서다.

### PR 1 — 빌드 도구 (targetSdk는 35 유지)

- AGP 8.9.x 이상(가능하면 8.10+), 그에 맞는 Gradle wrapper(8.11.1+).
- Robolectric 4.16.
- `compileSdk = 36` (`app`, `benchmark`).
- 검증: `./gradlew test` 전 variant, R8 release 빌드, CI 통과. Roborazzi 기준 이미지가 흔들리면
  CI(Linux)에서 재기록 — Windows 로컬 대량 실패는 판정 근거가 아니다.

### PR 2 — `targetSdk = 36`

Android 16이 target 36 앱에 적용하는 변경과 코드 대조 결과:

| 변경 | markleaf 상태 | 확인 |
|---|---|---|
| 엣지 투 엣지 opt-out 제거 | `enableEdgeToEdge()` 사용, opt-out 없음 | 시스템 바·IME 인셋 |
| 예측형 뒤로가기 기본, `onBackPressed` 미호출 | `enableOnBackInvokedCallback="true"`, `BackHandler` 사용 | 편집기 서식 패널·개요 닫기 |
| 대화면(sw≥600dp) 방향·리사이즈 제한 무시 | `screenOrientation`·`resizeableActivity` 없음, WindowSizeClass 대응 | 태블릿 가로/세로 |
| `elegantTextHeight` 폐기 | 미사용 | — |
| 위젯 2종 + 설정 Activity | 특이 API 없음 | 배치·설정·갱신 |
| sideload `PackageInstaller` 업데이트 | D076 | **API 36 기기에서 설치 전 과정** |

- 예상 코드 변경은 `targetSdk` 한 줄. 확인 중 결함이 나오면 같은 PR에서 고친다.
- 기기 검증: S24(API 36) 우선, 불가 시 API 36 AVD. 검증 후 debug APK 제거.

### 이후

- 일반 릴리스 절차로 배포(GitHub Release → F-Droid).
- Play 재개 시 `MARKLEAF_PLAY_AAB`로 AAB를 빌드해 테스트 트랙 → 프로덕션.
  Play 기준은 "최신 Android 출시 후 1년 이내"이므로 재개 시점에 다시 확인한다.

## Risks

- AGP 업그레이드가 가장 큰 변수(R8 규칙, lint, Roborazzi 플러그인 호환).
- 새 androidx 버전을 함께 올리게 되더라도 요구 minSdk는 26보다 낮아 기기 지원 범위에 영향 없음.
  PR 1에서 올리는 의존성마다 이것을 확인한다.
