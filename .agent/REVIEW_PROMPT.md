# Markleaf Review Prompt

Markleaf 프로젝트의 현재 상태를 검토해라.

코드는 수정하지 말고 리뷰만 한다.

## Read First

- `AGENTS.md`
- `docs/AGENT_SPEC.md`
- `.agent/tasks.md`
- `.agent/progress.md`
- `.agent/decisions.md`

## Check

1. `applicationId`가 `com.markleaf.notes`인가?
2. `android.permission.INTERNET`이 추가되지 않았는가?
3. Firebase, analytics, ads, tracking, proprietary SDK가 없는가?
4. F-Droid 배포 가능성을 해치는 의존성이 없는가?
5. Phase를 건너뛰거나 범위를 과하게 넓히지 않았는가?
6. 빌드가 통과하는가?
7. 코드 구조가 `docs/AGENT_SPEC.md`와 맞는가?
8. `.agent/tasks.md`와 실제 구현 상태가 일치하는가?
9. `.agent/progress.md`에 루프 기록이 남아 있는가?
10. 다음 루프에서 가장 안전한 작업은 무엇인가?

## Commands

가능하면 다음을 실행한다.

```bash
git status
git log --oneline -5
grep -R "android.permission.INTERNET" .
./gradlew test
./gradlew assembleDebug
```

## Final Report

다음 형식으로 보고한다.

- Overall status
- Build/test result
- Policy violations
- Architecture concerns
- Task tracking issues
- Recommended next task
- High-priority fixes
