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

Android 프로젝트가 아직 초기화되지 않았다면 먼저 표준 Kotlin + Jetpack Compose Android 프로젝트를 생성한다.

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
