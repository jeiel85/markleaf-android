# Markleaf Ralph Loop Prompt

너는 Markleaf 프로젝트에서 작업하는 자율 코딩 에이전트다.

이 작업은 Ralph Loop 반복 작업이다.  
너의 목표는 저장소에 실제로 측정 가능한 진전을 만들고, 검증이 통과하면 commit한 뒤 원격 저장소에 push하는 것이다.  
모든 변경은 작고, 안전하고, 리뷰 가능해야 한다.

Repository:

```text
https://github.com/jeiel85/markleaf-android.git
```

---

## 1. 작업 전 반드시 읽을 파일

코드를 수정하기 전에 아래 파일들을 순서대로 읽어라.

1. `AGENTS.md`
2. `docs/AGENT_SPEC.md`
3. `docs/ARCHITECTURE.md`
4. `docs/ROADMAP.md`
5. `docs/BRANDING.md`
6. `.agent/tasks.md`
7. `.agent/progress.md`
8. `.agent/decisions.md`

---

## 2. 절대 위반하면 안 되는 프로젝트 규칙

다음 규칙은 MVP에서 절대 위반하지 않는다.

- Android `applicationId`는 반드시 `com.markleaf.notes`를 사용한다.
- `android.permission.INTERNET`을 추가하지 않는다.
- API 연동을 추가하지 않는다.
- 로그인 또는 계정 기능을 추가하지 않는다.
- 분석 기능을 추가하지 않는다.
- 광고를 추가하지 않는다.
- 추적 기능을 추가하지 않는다.
- Firebase를 추가하지 않는다.
- remote config를 추가하지 않는다.
- proprietary crash reporting SDK를 추가하지 않는다.
- closed-source 또는 proprietary SDK를 추가하지 않는다.
- 프로젝트를 F-Droid 친화적으로 유지한다.
- 앱은 local-first 방향을 유지한다.
- 사용자의 노트, 태그, 파일명, 메타데이터, 사용 행동은 사용자가 명시적으로 export/share 하지 않는 한 기기 밖으로 나가면 안 된다.
- 앱의 핵심은 속도, 단순함, 디자인, Markdown 글쓰기 경험이다.
- 기존 메모 앱의 브랜딩, UI 자산, 이름, 색상, 문구, 레이아웃을 복사하지 않는다.

---

## 3. 작업 선택 규칙

1. `.agent/tasks.md`를 열어라.
2. 가장 이른 미완료 Phase에서 첫 번째 unchecked task를 선택하라.
3. 한 번의 루프에서는 정확히 하나의 task만 작업하라.
4. 이후 Phase로 건너뛰지 마라.
5. 선택한 task의 범위를 임의로 확장하지 마라.
6. 선택한 task가 실제 코드베이스에서 이미 완료되어 있다면, 그 이유를 설명하고 `.agent/tasks.md`에 완료 표시를 하라. 코드 변경이 필요 없다면 다음 unchecked task로 넘어가도 된다.
7. 선택한 task가 `AGENTS.md` 또는 `docs/AGENT_SPEC.md`와 충돌하면 작업을 중단하고 충돌 내용을 보고하라.

---

## 4. 구현 규칙

- Kotlin을 우선 사용한다.
- UI는 Jetpack Compose를 우선 사용한다.
- 디자인 시스템은 Material 3를 우선 사용한다.
- 빌드는 Gradle Kotlin DSL을 우선 사용한다.
- 아키텍처는 단순하게 유지한다.
- 불필요한 추상화를 만들지 않는다.
- 코드는 읽기 쉽게 작성한다.
- 변경은 작게 유지한다.
- 다음 영역을 수정할 때는 가능한 한 테스트를 추가한다.
  - 태그 파싱
  - 제목 추출
  - excerpt 생성
  - slug 생성
  - repository 동작
  - database logic
  - business rule
- 꼭 필요하지 않다면 무거운 의존성을 추가하지 않는다.
- network, API, analytics, ads, tracking, proprietary dependency를 추가하지 않는다.

---

## 5. 검증 규칙

선택한 task를 구현한 뒤, 관련 있는 검증 명령을 실행하라.

가능하면 다음 명령을 우선 사용한다.

```bash
./gradlew test
./gradlew assembleDebug
```

또한 INTERNET 권한이 추가되지 않았는지 확인한다.

```bash
grep -R "android.permission.INTERNET" .
```

Android 프로젝트가 아직 초기화되지 않았다면, 먼저 표준 Android Kotlin + Jetpack Compose 프로젝트를 초기화한 뒤 가능한 Gradle 명령을 실행하라.

검증이 실패하면 다음 순서로 처리한다.

1. 오류 메시지를 주의 깊게 읽는다.
2. 근본 원인을 수정한다.
3. 검증 명령을 다시 실행한다.
4. 검증이 통과하거나 실제 blocker가 확인될 때까지 반복한다.

---

## 6. 완료 규칙

선택한 task가 완료되면 다음을 수행한다.

1. `.agent/tasks.md`에서 완료한 task를 체크한다.
2. `.agent/progress.md`에 날짜가 포함된 진행 기록을 추가한다.
3. 중요한 구현 결정은 `.agent/decisions.md`에 기록한다.
4. `git status`를 실행한다.
5. 검증이 통과했다면 명확한 conventional commit message로 commit한다.
6. commit이 성공하면 현재 branch를 확인한다.
7. 현재 branch가 원격 저장소와 연결되어 있으면 push한다.
8. 원격 branch가 아직 설정되어 있지 않으면 다음 형식으로 upstream을 설정해 push한다.

```bash
git push -u origin HEAD
```

commit message 예시:

```text
docs: add agent coding documents
chore: initialize android project
chore: add compose navigation skeleton
feat: add notes list placeholder
feat: add note editor autosave
feat: add tag parser
test: add tag parser tests
docs: update project decisions
```

push 전후에는 다음을 확인한다.

```bash
git status
git branch --show-current
git remote -v
```

push가 실패하면 다음을 수행한다.

1. 실패 원인을 확인한다.
2. 인증 문제, 권한 문제, protected branch 문제라면 코드를 추가 수정하지 않는다.
3. `.agent/progress.md`에 push 실패 사유를 기록한다.
4. 최종 응답에 commit hash와 push 실패 사유를 명확히 보고한다.
5. push 실패만으로 이미 성공한 commit을 되돌리지 않는다.

---

## 7. Blocker 규칙

막히면 요구사항을 추측하거나 임의로 만들어내지 마라.

대신 다음을 수행한다.

1. 해당 task는 unchecked 상태로 둔다.
2. `.agent/progress.md`에 blocker를 기록한다.
3. 무엇이 진행을 막고 있는지 정확히 설명한다.
4. 다음으로 가능한 안전한 행동을 제안한다.
5. 깨진 코드는 commit하지 않는다.

---

## 8. 루프 동작 규칙

이 Ralph Loop 프로세스는 다음 조건 중 하나가 발생할 때까지 계속한다.

- `.agent/tasks.md`의 모든 task가 완료됨
- 안전한 진행을 막는 실제 blocker가 발생함
- 빌드 실패를 수정하려면 사람의 결정이 필요함
- task가 no-API, no-INTERNET, no-tracking, F-Droid-friendly 정책을 위반함

---

## 9. 최종 응답 형식

이번 반복 작업이 끝나면 다음 형식으로 보고하라.

```text
Selected task:
- 

What was implemented:
- 

Files changed:
- 

Commands run:
- 

Build/test result:
- 

INTERNET permission check result:
- 

Commit hash:
- 

Push result:
- 

Remaining next task:
- 

Risks or blockers:
- 
```