# Markleaf Project Instructions (GEMINI.md)

이 파일은 `AGENTS.md`의 간소화된 버전입니다. 전체 작업 규칙은 `AGENTS.md`를 참조하세요.

## 핵심 규칙 요약

- `applicationId` = `com.markleaf.notes`
- `android.permission.INTERNET` 사용 금지
- API 연동, 로그인/계정, 분석/광고/추적 금지
- F-Droid 친화적 오픈소스 의존성만 사용
- 노트 데이터는 사용자 명시 export 전까지 기기 밖으로 나가지 않음
- 기능 수보다 속도, 안정성, 디자인 우선
- Conventional Commits 사용

## 기술 스택

Kotlin + Jetpack Compose + Material 3 + Room + DataStore + Coroutines/Flow + Gradle Kotlin DSL

## 참조

- 전체 규칙: `AGENTS.md`
- 설계서: `docs/AGENT_SPEC.md`
- 작업 목록: `.agent/tasks.md`

