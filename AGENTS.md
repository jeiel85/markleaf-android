# AGENTS.md

이 문서는 Markleaf 프로젝트에서 AI 코딩 에이전트가 매번 반드시 읽어야 하는 작업 규칙입니다.

## Project

Markleaf는 Android용 가벼운 로컬 우선 Markdown 메모 앱입니다.

Repository:

```text
https://github.com/jeiel85/markleaf-android.git
```

Source of truth:

```text
docs/AGENT_SPEC.md
```

Application ID:

```text
com.markleaf.notes
```

## Non-Negotiable Rules

다음 규칙은 MVP에서 절대 어기지 않습니다.

- Android `applicationId`는 반드시 `com.markleaf.notes`를 사용한다.
- MVP에서는 `android.permission.INTERNET`을 추가하지 않는다.
- MVP에서는 API 연동을 추가하지 않는다.
- MVP에서는 로그인/계정 기능을 추가하지 않는다.
- MVP에서는 분석, 광고, 추적 기능을 추가하지 않는다.
- MVP에서는 Firebase, remote config, proprietary crash reporting SDK, closed SDK를 추가하지 않는다.
- 프로젝트는 F-Droid 친화적으로 유지한다.
- 사용자의 노트, 태그, 메타데이터는 사용자가 직접 export/share 하기 전까지 기기 밖으로 나가지 않는다.
- 기존 메모 앱의 이름, 아이콘, 색상, 화면 구성, 문구, 브랜딩을 복사하지 않는다.
- 기능 수보다 속도, 안정성, 디자인, 데이터 소유권을 우선한다.

## Preferred Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Kotlin Coroutines + Flow
- Gradle Kotlin DSL

## Workflow For Each Ralph Loop

각 루프에서는 다음 순서를 따른다.

1. `AGENTS.md`를 읽는다.
2. `docs/AGENT_SPEC.md`를 읽는다.
3. `.agent/tasks.md`를 읽는다.
4. `.agent/progress.md`를 읽는다.
5. `.agent/decisions.md`를 읽는다.
6. 가장 우선순위가 높은 첫 번째 unchecked task를 하나만 선택한다.
7. 선택한 task 하나만 구현한다.
8. 관련 테스트 또는 빌드를 실행한다.
9. 실패하면 원인을 읽고 수정한 뒤 다시 실행한다.
10. 완료되면 `.agent/tasks.md`를 갱신한다.
11. `.agent/progress.md`에 진행 기록을 남긴다.
12. 중요한 결정은 `.agent/decisions.md`에 기록한다.
13. 체크가 통과하면 commit한다.

## Quality Checks

가능하면 다음 명령을 사용한다.

```bash
./gradlew test
./gradlew assembleDebug
```

CI 또는 릴리즈 검증 시에는 APK 산출물 확인을 반드시 포함한다.

- `app/build/outputs/apk/debug/app-debug.apk` 파일 존재 여부 확인
- GitHub Actions/Release와 GitLab Release에서 APK 다운로드 가능 여부 확인
- 두 배포 채널에서 다운로드한 APK 파일 크기가 0보다 큰지 확인
- GitLab Release 자산이 만료되는 job artifact가 아니라 Generic Package Registry 영구 링크인지 확인

Android 프로젝트가 아직 초기화되지 않았다면 먼저 표준 Kotlin + Jetpack Compose Android 프로젝트를 생성한다.

## Release Artifact Export

사용자가 "새 버전 만들기"를 요청하면 버전 bump, changelog, fastlane changelog, 검증, commit/tag 작업과 함께 실제 산출물 디렉터리인 `D:\Build`에 Play Console 제출용 파일을 내보낸다. 바탕화면의 `Build`는 이 디렉터리를 가리키는 바로가기일 뿐이며, 릴리스 task는 바로가기를 경유하지 않는다. 이 dump는 전용 Gradle task로 자동화되어 있다:

```bash
./gradlew :app:exportReleaseToBuildDrive
```

이 task는 `bundleRelease`에 의존하므로 **서명 AAB**도 함께 빌드된다(서명: repo 루트 `release-signing.properties` + `.secrets/markleaf-release.p12` 키스토어; cert는 Play 업로드 키와 동일). 산출물은 모두 `D:\Build\` 평면 배치, 공통 stem `markleaf-v<semver>-vc<versionCode>`:

- `markleaf-v<semver>-vc<versionCode>.aab` — `app/build/outputs/bundle/release/app-release.aab`(서명) 복사본
- `markleaf-v<semver>-vc<versionCode>.mapping.txt` — R8 mapping(Play 크래시 deobfuscation; 있으면 복사)
- `markleaf-v<semver>-vc<versionCode>-release-notes.txt` — **모든 스토어 로케일**(ko-KR / en-US / ja-JP / de-DE / fr-FR / es-ES)의 `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`를 읽어 하나의 파일에 로케일 태그 블록을 연달아 묶음

TXT 파일은 바로 복사/붙여넣기 가능해야 하며, 각 로케일 릴리즈 노트만 아래 태그로 감싼다. 태그 밖에는 어떤 설명이나 문구도 넣지 않는다 (Gradle task가 이 형식을 보장한다).

```text
<ko-KR>
...
</ko-KR>
<en-US>
...
</en-US>
<ja-JP>
...
</ja-JP>
<de-DE>
...
</de-DE>
<fr-FR>
...
</fr-FR>
<es-ES>
...
</es-ES>
```

따라서 cut 전에 **6개 스토어 로케일 fastlane changelog 전부**(BCP 47 region 대문자)가 새 versionCode 파일명으로 작성되어 있어야 task가 통과한다. 하나라도 누락되거나 로케일당 500자를 넘으면 task가 명시적으로 실패한다.

GitLab CI용 산출물은 `-Pmarkleaf.releaseExportDir=<dir>`와 함께
`:app:exportReleaseArtifacts`를 실행한다. 이 task는 위 3개 파일에 서명 APK를 더해 내보내며,
로컬 Play 핸드오프인 `D:\Build`에는 APK를 추가하지 않는다. GitLab의 `v*` 태그는 보호되고,
서명 변수는 masked/hidden/protected로 유지한다. 공개 Release 링크는 만료되는 CI artifact가
아니라 Generic Package Registry 자산을 사용한다.

## GitHub 이슈 대응

메인테이너가 "GitHub 이슈 대응"을 요청하면 다음 순서로 진행하고 완료를 보고한다.
이는 메인테이너 지시 기반의 유인(有人) 플로다 — `scripts/nightly`의 무인 봇이 댓글·PR만
하고 릴리스는 하지 않는 것과 구분된다.

1. **트리아지 + 감사 댓글.** 열린 이슈를 확인하고 보고자를 식별한다. 외부 보고자에게는
   보고자 언어로 감사 댓글을 단다(메인테이너 본인이 연 이슈는 생략). 질문·중복·범위 밖
   이슈는 댓글로만 응대하고 코드 수정 없이 닫을 수 있다.
2. **수정.** 재현 후 고치고, 푸시 전 [Quality Checks](#quality-checks)의 하드 게이트
   (`testDebugUnitTest` + `lintRelease`)를 통과시킨다. 프리뷰/타이포 렌더링을 바꿨으면
   Roborazzi 골든을 Linux CI 러너에서 재기록한다 (`android-build.yml`의 `record_roborazzi`
   workflow_dispatch — 로컬 재기록은 폰트 힌팅 차이로 CI verify와 어긋난다).
3. **F-Droid — 태그 푸시로 자동 배포.** versionCode/versionName bump + `CHANGELOG.md`(영어,
   릴리즈 노트 원본) + `CHANGELOG.ko.md`(한국어판) + fastlane changelog 작성 후 main에
   푸시하고 `vX.Y.Z` 태그를 GitLab 먼저, GitHub 다음으로 푸시한다. 같은 태그가 양쪽에서
   독립적으로 서명 빌드를 돌리지만 **릴리스에 붙는 자산은 다르다** — GitHub Release는 APK와
   R8 mapping 두 개만, GitLab Release는 Generic Package Registry를 통해 AAB까지 포함한다
   (AAB를 GitHub에 올리지 않는 이유는 D062). GitHub 태그는 F-Droid 자동 픽업도 발동하므로
   별도 F-Droid 제출 단계는 없다.
   릴리스 커밋은 `git add -A`로 만들지 않는다 — 변경 파일을 명시적으로 stage하거나 커밋 전
   working tree가 릴리스 대상만 담고 있는지 확인한다(무관한 작업이 태그에 섞여 나가는 것을
   막기 위함, v2.24.0에서 landing-i18n 유입 사고 → #154).
4. **Play Store — 산출물 핸드오프.** [Release Artifact Export](#release-artifact-export)에 따라
   Play 제출용 서명 AAB와 릴리즈 노트 TXT를 내보낸다(서명 AAB는 CI 릴리스 산출물 기준).
   업로드는 메인테이너가 수동으로 한다.
5. **마감 + 보고.** 해결된 이슈에 감사 댓글을 달고 닫되, 사용자측 확인이 남으면 열어 둔다.
   굵직한 변경이면 README·랜딩 페이지의 버전 표기를 6개 언어 모두 갱신하고
   `scripts/verify-landing-versions.ps1`로 검증한다. 마지막으로 무엇이 나갔는지,
   이슈 상태, 릴리스 링크를 보고한다.

코드 수정이 실제로 들어간 경우에만 3·4단계(태그·Play 핸드오프)를 진행한다. 댓글로 끝나는
이슈는 1·5단계만 적용한다.

## Stop Conditions

다음 상황에서는 임의로 진행하지 말고 중단 후 보고한다.

- task가 API 연동을 요구하는 경우
- task가 네트워크 권한을 요구하는 경우
- task가 proprietary SDK를 요구하는 경우
- task가 `docs/AGENT_SPEC.md`와 충돌하는 경우
- Gradle/Android 설정이 현재 환경에서 확인 불가능한 경우
- F-Droid 방향성과 충돌하는 의존성이 필요한 경우

## Commit Style

Conventional Commits를 사용한다.

예:

```text
chore: initialize android project
chore: add compose navigation skeleton
feat: add notes list placeholder
feat: add note editor autosave
feat: add tag parser
test: add tag parser tests
docs: update project decisions
```

## Documentation And History

공통 템플릿 문서를 이 프로젝트에 통합해 아래 문서를 운영한다.

- `CHANGELOG.md`: 사용자에게 공개 가능한 변경 요약 (**영어** — GitHub 릴리즈 노트의 원본)
- `CHANGELOG.ko.md`: 같은 변경 요약의 한국어판
- `HISTORY.md`: 작업 과정, 검증, 후속 작업 기록

코드 변경 시 문서 반영 원칙:

- 사용자 영향 변경은 `CHANGELOG.md`에 기록
- 작업 단위 이력은 `HISTORY.md`에 기록
- 중요한 기술 결정은 `.agent/decisions.md`에 기록

### 공개 문서 언어 — 영어 기본

Markleaf 사용자 대부분이 영어를 쓰므로 공개 문서의 기본 언어는 **영어**이고, 다른 언어는 번역판으로 병기한다.

`CHANGELOG.md`는 영어로 작성한다. GitHub Actions가 태그 푸시 때 이 파일에서 해당 버전 섹션을 그대로 잘라 릴리즈 제목·본문으로 쓰므로(`.github/workflows/android-build.yml`의 `Prepare release notes` 스텝), **CHANGELOG의 언어가 곧 GitHub 릴리즈 노트의 언어다.** 별도로 릴리즈 노트를 작성하는 단계는 없다.

- 헤딩은 `## vX.Y.Z - Title - YYYY-MM-DD` 형식을 지킨다. CI awk 파서가 `## v<version>` 접두와 말미 날짜 패턴에 의존하며, 릴리즈 제목은 날짜를 뗀 `vX.Y.Z - Title`이 된다. 이 형식을 벗어나면 릴리즈 job이 `test -s release-title.txt`에서 실패한다.
- 한국어판은 같은 버전 섹션을 `CHANGELOG.ko.md`에 병기한다. 영어판이 원본이고 한국어판이 번역이다.
- 이슈 번호(`#123`), 코드 식별자, 파일 경로, 제품명은 번역하지 않고 그대로 둔다.
- v2.16.0 이전 항목은 영어화 대상이 아니다. `CHANGELOG.md`의 "Earlier releases (Korean)" 이후 구간과 `CHANGELOG.ko.md`에 한국어로 보존되어 있으며 소급 번역하지 않는다.

Play/F-Droid용 `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`는 기존대로 **6개 스토어 로케일 전부** 작성한다. 이 지침은 [Release Artifact Export](#release-artifact-export)의 6개 로케일 요구사항을 대체하지 않는다.

README와 GitHub Pages 랜딩도 영어가 기본이며 앱이 지원하는 6개 언어(en · ko · ja · de · es · fr)로 병기한다. 영어판이 canonical이고 `x-default`다. 공개 표면의 버전 표기를 갱신할 때는 6개 언어를 함께 갱신하고 `scripts/verify-landing-versions.ps1`로 검증한다.
