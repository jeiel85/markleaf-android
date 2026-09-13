# In-App Update Strategy Evaluation

## Date
2026-09-13

## 질문
업데이트가 있는지 앱이 직접 확인해서 배너/모달을 띄우고, 다운로드부터 실행까지 이어주면
설치 여부는 Android가 자기 팝업으로 물어본다. 그 그림 중 어디까지가 이 프로젝트에서
가능한가.

## Conclusion
- 3단계로 나뉜다. **B단계(확인 + 배너/모달 + 브라우저 위임)까지는 낮은 위험으로 가능**하고,
  C단계(앱 내 다운로드 + `PackageInstaller`)도 가능하지만 선행 조건이 많다.
- 배포 채널은 productFlavor `store` / `github`로 나눈다. 어떤 경우에도 F-Droid·Play로
  나가는 산출물에는 INTERNET 권한과 업데이터 코드가 들어가지 않는다.
- **가장 큰 제약은 Play 정책이 아니라 F-Droid 재현 빌드다.** 이 앱은 `Binaries:` +
  `AllowedAPKSigningKeys`로 등록되어 있어서 GitHub Release의 APK와 F-Droid가 소스에서
  다시 빌드한 APK가 일치해야 그 버전이 발행된다. 두 채널이 **같은 파일 하나**를 공유하므로,
  자산 이름을 나누고 업스트림 fdroiddata 레시피를 함께 고치지 않으면 "GitHub APK에만
  업데이터"는 성립하지 않는다.
- Status: **설계 확정, 구현 보류.** `.agent/tasks.md` Phase 34로 등록. 구현 착수에는
  `docs/AGENT_SPEC.md` §15.1·§15.6("INTERNET 권한 영구 금지") 개정을 사람이 명시적으로
  승인하는 절차가 선행한다 — `AGENTS.md` Stop Conditions가 그것을 요구한다.

## 확인된 제약

### 1. 이 저장소의 현재 상태
- 매니페스트가 선언하는 권한은 `VIBRATE` 하나다 — `app/src/main/AndroidManifest.xml`.
- `AGENTS.md` 비협상 규칙에 "MVP에서는 `android.permission.INTERNET`을 추가하지 않는다",
  "프로젝트는 F-Droid 친화적으로 유지한다"가 있다.
- `docs/NOCLOUD_CERTIFICATION.md` 인증 체크리스트의 첫 항목이 **No INTERNET Permission**이다.
- 같은 문장이 사용자에게 보이는 곳 세 군데에 있다: README 8개 언어(`README.ko.md:196` 등),
  `docs/privacy.*.html` 8개(`privacy.ko.html:71`의 `android.permission.INTERNET   없음`),
  그리고 앱이 첫 실행에 심는 노트 `app/src/main/res/raw*/starter_notes.md`(ko 기준 130행).
  단일 빌드에 INTERNET을 넣으면 이 문장이 한꺼번에 거짓이 된다. 채널 분리가 선택이 아니라
  전제인 이유다.
- `docs/NETWORK_FEATURE_NECESSITY_EVALUATION.md`는 네트워크 기능을 보류하면서 재검토 조건
  5개를 적어 두었고, 그 중 4번이 "F-Droid-compatible flavor strategy if needed"다.
  이 문서는 그 조건을 따라 같은 결정을 다시 여는 것이다.

### 2. Google Play 정책 — store 플레이버에 업데이터가 없어야 하는 이유
Play의 Device and Network Abuse 정책은 두 문장을 명시한다.

> "An app distributed via Google Play may not modify, replace, or update itself using any
> method other than Google Play's update mechanism."

> "an app may not download executable code (such as dex, JAR, .so files) from a source
> other than Google Play."

Play 업데이트는 현재 보류 상태지만(README 8개 언어) 앱은 등재된 채이고, D072는 Play를
"포기가 아니라 보류"로 기록했다. 재개하는 순간 그대로 걸리는 조항이므로, Play로 올라갈
산출물에는 업데이터가 처음부터 없어야 한다.

또한 Play Core의 In-App Updates(`AppUpdateManager`)는 후보에서 제외한다. `AGENTS.md`가
비공개 SDK 추가를 금지하고, 애초에 Play 업데이트 경로 자체가 멈춰 있어 얻을 것이 없다.

### 3. F-Droid 포함 정책 — 기본 OFF + 옵트인이어야 하는 이유
> "Applications must not download additional executable binary files (e.g. add-ons,
> auto-updates, etc.) without explicit user consent."

> "Consent means it needs to be opt-in (it must not be harder to decline than to accept or
> presented in a way users are likely to press accept without reading) and structured in a
> way that clearly explains to users that they're choosing to bypass F-Droid's checks if
> they activate it."

즉 F-Droid 배포본에 업데이터가 있어도 **옵트인이면 정책 위반은 아니다.** 다만 이 프로젝트는
위 1번 때문에 F-Droid 배포본에는 넣지 않는 쪽을 택한다. 이 인용은 sideload 빌드의 UX 계약
(기본 OFF, 켤 때 "F-Droid의 검사를 우회한다"는 설명)을 그대로 규정한다.

### 4. F-Droid 재현 빌드 — 설계를 실제로 결정한 제약
`metadata/com.markleaf.notes.yml`은 이렇게 등록되어 있다.

```yaml
Binaries:
  https://github.com/jeiel85/markleaf-android/releases/download/v%v/markleaf-v%v.apk
AllowedAPKSigningKeys: 0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a
Builds:
  - versionName: ...
    subdir: app
    gradle:
      - yes
```

F-Droid 문서는 이 구성의 동작을 이렇게 적는다.

> "if `fdroid publish` can verify that the downloaded APK matches the one built from the
> _fdroiddata_ recipe, the downloaded APK will be published. Otherwise F-Droid will skip
> publishing this version of the app."

결론: F-Droid가 사용자에게 주는 파일은 **우리가 GitHub Release에 올린 그 APK 자체**다.
그래서 같은 자산 이름 하나로 두 채널을 다르게 만들 방법이 없다. 자산을 둘로 나누고,
레시피가 어느 변형을 빌드해야 하는지 업스트림에서 고쳐야 한다(아래 P0).

## 3단계 옵션

| | A: 링크만 | B: 확인 + 안내 (권장 1차) | C: 앱 내 다운로드 + 설치 |
|---|---|---|---|
| 추가 권한 | 없음 | `INTERNET` | `INTERNET` + `REQUEST_INSTALL_PACKAGES` |
| 배너/모달 | **불가** (새 버전이 있는지 앱이 모름) | 가능 | 가능 |
| 다운로드 | 브라우저 | 브라우저 | 앱 |
| 시스템 설치 팝업 | 브라우저가 유발 | 브라우저가 유발 | 앱이 유발 (원하던 그림) |
| Play 정책 위험 | 없음 | store 플레이버에 없으면 없음 | store 플레이버에 없으면 없음 |
| 실패 모드 | 없음 | 확인 실패 = 조용히 무시 | 부분 다운로드·해시 불일치·설치 거부 복구 필요 |
| 문서 파급 | 없음 | sideload 예외 문장 (아래 목록) | B와 동일 |

A는 "설정 → 최신 버전 확인" 버튼이 `ACTION_VIEW`로 릴리스 페이지를 여는 것뿐이다. INTERNET이
필요 없고 no-cloud 문서를 한 글자도 건드리지 않지만, 앱이 새 버전 존재를 알 수 없으니
요청의 핵심인 배너가 성립하지 않는다. 기준선으로만 기록한다.

B가 체감 가치의 대부분을 가져간다. 사용자가 아무것도 안 해도 "새 버전 있어요"가 뜨고,
탭하면 브라우저가 APK를 받고 Android가 설치를 묻는다. 앱이 실행 파일을 직접 내려받지
않으므로 F-Droid 포함 정책의 "download additional executable binary files" 조항 자체에
해당하지 않는다.

C는 다운로드 진행률과 실패 복구를 앱이 책임지는 대신 검증·복구 코드를 모두 우리가 진다.
B가 실사용에서 안정된 뒤 같은 플레이버 안에서 확장한다.

## 채널 분리 설계 (productFlavor)

```text
flavorDimension "distribution"
  store   (기본)  권한 추가 없음, 업데이터 코드 없음  → F-Droid / Play / 재현 빌드 대상
  github          INTERNET + 업데이터                → 사이드로드 전용 자산
```

- 소스 배치: `app/src/github/AndroidManifest.xml`이 INTERNET을 선언하고,
  `app/src/github/java/.../update/`에 구현을 둔다. 공통 UI가 참조하는 이음매 하나
  (예: `UpdateChannel` 인터페이스 + `store`의 no-op 구현)만 플레이버별로 제공한다.
  업데이터 본체를 `main`에 두고 플래그로 끄면 R8이 지워도 **소스 감사에서는 보인다** —
  F-Droid 리뷰어와 privacy 문서를 읽는 사람이 확인하는 것은 소스다.
- 검증은 `store`를 기준 변형으로 삼는다. Roborazzi 골든과 계측 테스트를 양쪽 플레이버에
  중복시키지 않고, `github`에는 업데이터 단위 테스트만 추가한다.

### 비용 (정직하게)
플레이버가 생기면 Gradle 작업 이름과 출력 경로가 변형 한정 이름으로 바뀐다. 지금 저장소는
변형 없는 이름을 여러 곳에서 참조한다.

```bash
grep -rln "app-release\|assembleDebug\|app-debug\|verifyRoborazziDebug\|lintRelease\|bundleRelease" \
  --include=*.yml --include=*.ps1 --include=*.sh --include=*.kts --include=*.md . \
  | grep -v "HISTORY.md\|CHANGELOG"
```

2026-09-13 기준 18개 파일 167줄이 걸린다(`.github/workflows/android-build.yml`,
`.gitlab-ci.yml`, `.github/scripts/launch-smoke.sh`, `scripts/verify-release-export.ps1`,
`AGENTS.md`, `docs/RELEASE.md`, `docs/ROADMAP.md` 등). **실제로 생성되는 작업 이름은
플레이버를 추가한 뒤 `./gradlew tasks`로 확인해서 고친다** — AGP가 어떤 변형 한정 이름을
만드는지 추측으로 일괄 치환하면 CI가 조용히 다른 것을 빌드한다.

### 선행 조건 P0 — fdroiddata 레시피
`gradle: - yes`는 플레이버가 생기면 어느 변형을 빌드해야 하는지 말하지 않는다. 업스트림
fdroiddata에 `store` 변형을 빌드하도록 고치는 MR이 **먼저 머지되어야** 한다. 이 저장소의
`metadata/com.markleaf.notes.yml`은 v2.23.0에서 멈춘 참고 사본이라 여기만 고쳐도 효과가
없다. MR이 머지되기 전에 플레이버를 담은 태그를 밀면 그 버전은 F-Droid에서 발행되지 않는다
(위 인용).

### Release 자산
- `markleaf-vX.Y.Z.apk` = **store** 빌드. F-Droid가 검증하고 재배포하는 파일이므로 이름과
  내용 규칙을 바꾸지 않는다.
- `markleaf-vX.Y.Z-sideload.apk` = **github** 빌드. F-Droid는 `Binaries:`가 가리키는
  파일만 보므로 이 자산의 존재 자체는 검증에 영향이 없다(MR 리뷰에서 확인할 항목).
- 자산 목록은 세 곳에 복사되어 있고 `scripts/verify-release-assets.ps1`이 대조한다(D072).
  셋을 함께 고쳐야 한다.

## 더 싼 대안 (기록용)
Gradle 속성 게이트 `-Pmarkleaf.updater=true`로 매니페스트 `srcFile`과 소스 디렉터리를
바꾸는 방법이 있다. fdroiddata 레시피를 고치지 않아도 되고(`gradle: - yes`가 그대로 store
빌드를 만든다) 위 167줄이 하나도 움직이지 않는다. 대가는 같은 변형 이름이 내용이 다른 두
결과물을 만든다는 점, 그리고 AGP 표준 기법이 아니라는 점이다. 플레이버 도입 비용이 실제로
커지면 이쪽으로 후퇴할 수 있게 남겨 둔다.

## 업데이트 메타데이터 소스
- 권장: GitHub Pages의 정적 JSON(`docs/update.json`)을 릴리스 워크플로가 갱신한다.
- `api.github.com`을 직접 부르지 않는 이유: GitHub 문서는 "The primary rate limit for
  unauthenticated requests is 60 requests per hour"이며 "Unauthenticated requests are
  associated with the originating IP address"라고 적는다. 통신사 NAT 뒤에서는 앱이 아니라
  **같은 IP를 쓰는 남들**이 한도를 소모한다. 정적 JSON은 한도가 없고 스키마를 우리가 통제하며
  응답이 작다.
- 스키마 초안:

```json
{
  "versionCode": 145,
  "versionName": "2.42.0",
  "apkUrl": "https://github.com/.../markleaf-v2.42.0-sideload.apk",
  "apkSizeBytes": 2831155,
  "sha256": "…",
  "minSdk": 26,
  "releaseNotesUrl": "https://github.com/.../releases/tag/v2.42.0",
  "publishedAt": "2026-09-20T00:00:00Z"
}
```

- 비교는 `versionName` 문자열이 아니라 `BuildConfig.VERSION_CODE` 정수로 한다. 문자열
  비교는 `2.9.0` > `2.10.0`을 만든다.
- `apkSizeBytes`와 `sha256`은 릴리스 워크플로가 실제 자산에서 계산해 넣는다. 모달이 약속한
  다운로드 크기를 이 필드 없이 채우려면 클라이언트가 리다이렉트되는 Release 자산에
  `HEAD` 요청을 한 번 더 보내 `Content-Length`를 읽어야 하는데, B단계 클라이언트는 JSON
  하나만 읽고 다운로드를 브라우저에 넘기는 설계다. 릴리스 시점에 아는 값을 런타임에 다시
  물어볼 이유가 없다.
- 의존성은 추가하지 않는다. `HttpsURLConnection` + `org.json`으로 충분하고, APK 크기와
  F-Droid 감사 표면을 늘리지 않는다. cleartext는 차단한다.
- 확인은 하루 1회, 실패는 조용히 무시하고 다음 기회에 다시 시도한다. 업데이트 확인 실패를
  사용자에게 알리는 것은 소음이다.

## UX 계약
- 설정 → 정보에 "업데이트 확인" 토글. **기본 OFF.** 켤 때 무엇을 하는지와, F-Droid의 검사를
  우회하는 경로라는 점을 같은 화면에서 설명한다(F-Droid 포함 정책 인용 참조).
- 배너: 노트 목록 상단 한 줄, 닫을 수 있고 "이 버전 건너뛰기"를 제공한다.
- 모달: 버전·공개일·다운로드 크기와 릴리스 노트 링크. 로컬 `CHANGELOG.md`가 아니라 JSON의
  `releaseNotesUrl`을 쓴다 — 설치된 앱에 들어 있는 changelog는 **새 버전의** 내용을 모른다.
- 앱 시작 화면을 모달로 막지 않는다. 확인은 백그라운드, 표시는 목록 화면에서만.
- 알림은 쓰지 않는다. `POST_NOTIFICATIONS` 권한을 추가할 이유가 없다.
- store 빌드에서 이 항목을 아예 숨길지, "F-Droid에서 업데이트됩니다" 안내로 남길지는
  Open questions 4번.

## C단계 구현 세부 (착수 시)
- `REQUEST_INSTALL_PACKAGES` 선언 + `packageManager.canRequestPackageInstalls()` 확인 +
  거부 시 `ACTION_MANAGE_UNKNOWN_APP_SOURCES`로 설정 화면 유도.
- `Intent.ACTION_INSTALL_PACKAGE`는 API 29에서 deprecated다. `PackageInstaller` 세션을
  쓰고 `STATUS_PENDING_USER_ACTION`을 받아 시스템 설치 확인 팝업을 띄운다.
- 다운로드는 앱 캐시로 받는다(외부 저장 권한 회피). 완료 후 JSON의 `sha256`과 대조하고
  불일치면 삭제한다.
- **서명 일치는 OS가 최종 보증한다.** 다른 키로 서명된 APK는
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`로 거부된다(`app/build.gradle.kts:94` 주석,
  `docs/RELEASE.md`의 동일 인증서 설명). 앱이 설치 전에 APK 서명을 미리 검증하는 것도
  바람직하지만, `getPackageArchiveInfo`로 `signingInfo`를 읽는 동작이 API 26~35에서
  어떻게 채워지는지는 실기기 확인이 필요하다 — 확인 전에는 SHA-256 대조를 1차 방어로 둔다.
- 실패 복구: 부분 다운로드 삭제, 재시도 상한, 그리고 최종 수단으로 브라우저 링크 제시.

## Open questions
1. `store`와 `github`가 같은 versionCode를 갖는데, 한 채널에서 다른 채널 APK로 넘어갈 때
   (같은 versionCode, 다른 내용) 재설치가 허용되는지 실기기 확인이 필요하다. 다운그레이드가
   아니므로 통과할 것으로 보지만 확인 전에는 단정하지 않는다.
2. sideload 자산이 하나 더 붙는 것을 F-Droid가 문제로 보는지 — `Binaries:`가 가리키는 파일만
   검증하므로 무해할 것으로 보지만 fdroiddata MR 리뷰에서 확인한다.
3. 배너를 노트 목록 상단에 두는 것이 Phase 31(Smart Library)의 정보 구조와 충돌하는지.
4. store 빌드에서 업데이트 설정 항목을 숨길지 안내로 남길지.

## 문서 파급 (코드 착수 시 함께 고칠 것)
- **`docs/AGENT_SPEC.md`가 먼저다.** `AGENTS.md`가 그 문서를 source of truth로 지정하고,
  그 문서의 §15.6은 "INTERNET 권한 영구 금지", §15.1은 "우리 백엔드 0, INTERNET 권한 0"이라고
  적는다. 그리고 `AGENTS.md`의 Stop Conditions는 "task가 네트워크 권한을 요구하는 경우"와
  "task가 `docs/AGENT_SPEC.md`와 충돌하는 경우" 둘 다에서 **중단 후 보고**를 요구한다.
  즉 Phase 34는 이 설계가 확정된 것과 무관하게, AGENT_SPEC §15.1·§15.6을
  "스토어 배포 산출물에 영구 금지 / 사이드로드 플레이버는 명시적 예외"로 개정하고 그 개정을
  사람이 명시적으로 승인하기 전에는 **첫 항목부터 멈춘다.** 이것은 우회할 절차가 아니라
  이 설계가 통과해야 하는 관문이다.
- `AGENTS.md` 비협상 규칙의 "INTERNET을 추가하지 않는다"를 어떤 산출물을 말하는지로 개정하고,
  Stop Conditions가 개정된 spec을 가리키게 한다. **코드보다 먼저 고친다** — 규칙과 코드가
  어긋난 상태로 커밋이 들어가면 다음 루프가 어느 쪽을 믿어야 할지 알 수 없다.
- `docs/NETWORK_FEATURE_NECESSITY_EVALUATION.md`: 재검토 표시(이 문서로의 포인터).
- `docs/NOCLOUD_CERTIFICATION.md`: "No INTERNET Permission" 항목에 스토어 배포 빌드 기준임을
  명시.
- `docs/PRIVACY.md`, `docs/privacy.*.html` 8개, `README*.md` 8개: sideload 빌드 예외 문장.
- `app/src/main/res/raw*/starter_notes.md` 8개 로케일의 해당 문장.
- `docs/RELEASE.md`: 자산 2개 → 3개, 플레이버 작업 이름.
- `.github/workflows/android-build.yml`, `scripts/verify-release-assets.ps1`,
  `.github/scripts/launch-smoke.sh`.

## Decision
- Status: **설계 확정, 구현 보류.** `.agent/tasks.md` Phase 34.
- **`docs/AGENT_SPEC.md` §15.1·§15.6 개정과 그 개정의 명시적 승인이 구현의 선행 조건이다.**
  승인 없이는 Phase 34 첫 항목에서 `AGENTS.md` Stop Conditions에 걸려 멈춘다.
- 채널 분리는 productFlavor `store` / `github`로 한다(D073).
- 1차 구현 범위는 B단계. C단계는 B가 안정된 뒤 같은 플레이버 안에서 확장한다.
- store / F-Droid / Play 산출물에는 INTERNET 권한도 업데이터 코드도 들어가지 않는다.
- fdroiddata 레시피 MR이 머지되기 전에는 플레이버를 담은 릴리스 태그를 밀지 않는다.

## References
- Google Play Device and Network Abuse policy —
  <https://support.google.com/googleplay/android-developer/answer/9888379>
- F-Droid Inclusion Policy — <https://f-droid.org/en/docs/Inclusion_Policy/>
- F-Droid Reproducible Builds — <https://f-droid.org/en/docs/Reproducible_Builds/>
- GitHub REST API rate limits —
  <https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api>
