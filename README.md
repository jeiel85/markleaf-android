# Markleaf

Markleaf는 Android용 가벼운 로컬 우선 Markdown 메모 앱입니다.

Repository:

```text
https://github.com/jeiel85/markleaf-android.git
```

Application ID:

```text
com.markleaf.notes
```

## 핵심 목표

- 빠른 글쓰기
- Markdown 중심 노트
- 태그 기반 정리
- 로컬 우선 저장
- Plain Markdown 내보내기
- 깔끔한 Android 네이티브 경험
- F-Droid 친화적인 오픈소스 방향성
- MVP에서 API, 로그인, 분석, 광고, 추적 배제

## 상태

초기 개발 단계입니다.

## 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Coroutines + Flow
- Gradle Kotlin DSL

## MVP 원칙

- `android.permission.INTERNET` 사용하지 않음
- API 연동 없음
- 로그인 없음
- 분석/광고/추적 없음
- Firebase/원격 설정/독점 SDK 없음
- 노트 데이터는 사용자 명시적 export/share 전까지 기기 밖으로 나가지 않음

## 문서

- `docs/AGENT_SPEC.md`: 에이전트 코딩 설계서
- `docs/ARCHITECTURE.md`: 아키텍처 설계
- `docs/ROADMAP.md`: 개발 로드맵
- `docs/BRANDING.md`: 브랜딩 가이드
- `AGENTS.md`: AI 에이전트 작업 규칙
- `.agent/tasks.md`: 랄프 루프 작업 목록
- `.agent/progress.md`: 진행 기록
- `.agent/decisions.md`: 결정 사항 기록
- `.agent/RALPH_PROMPT.md`: 반복 실행용 프롬프트

## License

Apache-2.0 권장.
