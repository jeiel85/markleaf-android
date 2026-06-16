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
- GitHub Actions artifact에서 APK 다운로드 가능 여부 확인
- 다운로드한 APK 파일 크기가 0보다 큰지 확인

Android 프로젝트가 아직 초기화되지 않았다면 먼저 표준 Kotlin + Jetpack Compose Android 프로젝트를 생성한다.

## Release Artifact Export

사용자가 "새 버전 만들기"를 요청하면 버전 bump, changelog, fastlane changelog, 검증, commit/tag 작업과 함께 바탕화면에 Play Console 제출용 파일을 내보낸다. 이 두 파일 dump는 전용 Gradle task로 자동화되어 있다:

```bash
./gradlew :app:exportReleaseToDesktop
```

이 task는 `bundleRelease`에 의존하므로 AAB도 함께 빌드된다. 산출물:

- AAB 파일: `markleaf-vX.Y.Z.aab` — `app/build/outputs/bundle/release/app-release.aab` 복사본
- 릴리즈 노트 TXT 파일: `markleaf-vX.Y.Z-release-notes.txt` — `fastlane/metadata/android/{ko-KR,en-US}/changelogs/<versionCode>.txt`를 읽어 `<ko-KR>`/`<en-US>` 블록으로 묶음

TXT 파일은 바로 복사/붙여넣기 가능해야 하며, 한글/영문 릴리즈 노트만 아래 태그로 감싼다. 태그 밖에는 어떤 설명이나 문구도 넣지 않는다 (Gradle task가 이 형식을 보장한다).

```text
<ko-KR>
...
</ko-KR>
<en-US>
...
</en-US>
```

따라서 cut 전에 두 fastlane changelog (ko-KR / en-US, BCP 47 region 대문자)가 새 versionCode 파일명으로 작성되어 있어야 task가 통과한다. 누락 시 task가 명시적으로 실패한다.

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
3. **F-Droid — 태그 푸시로 자동 배포.** versionCode/versionName bump + `CHANGELOG.md` +
   fastlane changelog 작성 후 main에 푸시하고 `vX.Y.Z` 태그를 푸시한다. 태그 한 번이 서명
   APK/AAB CI 릴리스와 F-Droid 자동 픽업을 함께 발동한다. 별도 F-Droid 제출 단계는 없다.
4. **Play Store — 산출물 핸드오프.** [Release Artifact Export](#release-artifact-export)에 따라
   Play 제출용 서명 AAB와 릴리즈 노트 TXT를 내보낸다(서명 AAB는 CI 릴리스 산출물 기준).
   업로드는 메인테이너가 수동으로 한다.
5. **마감 + 보고.** 해결된 이슈에 감사 댓글을 달고 닫되, 사용자측 확인이 남으면 열어 둔다.
   굵직한 변경이면 README·랜딩 페이지의 버전 표기를 갱신한다. 마지막으로 무엇이 나갔는지,
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

- `CHANGELOG.md`: 사용자에게 공개 가능한 변경 요약
- `HISTORY.md`: 작업 과정, 검증, 후속 작업 기록

코드 변경 시 문서 반영 원칙:

- 사용자 영향 변경은 `CHANGELOG.md`에 기록
- 작업 단위 이력은 `HISTORY.md`에 기록
- 중요한 기술 결정은 `.agent/decisions.md`에 기록
