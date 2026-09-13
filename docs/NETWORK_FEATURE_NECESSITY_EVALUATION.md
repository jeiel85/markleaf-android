# Network Feature Necessity Evaluation (Issue #7)

## Date
2026-04-30

## Conclusion
No network feature is necessary for current product stage. Keep offline-first baseline.

## Assessment
- Core value proposition (fast writing, local ownership, privacy) is already delivered offline.
- Mandatory network features would increase policy, privacy, and operational complexity.
- Current roadmap priorities (tablet UX, local data features) provide higher user value per cost.

## Candidate Network Features Reviewed
- Cloud sync: high complexity, account/auth burden, conflict resolution overhead.
- Remote backup: duplicates manual export goals with privacy tradeoffs.
- AI/API helpers: outside MVP direction, adds cost/dependency.

## Decision Framework for Future Revisit
Require all of the following before any adoption:
1. Clear user problem validated with local-first alternatives failing.
2. Explicit privacy model and data minimization design.
3. Opt-in only behavior and transparent controls.
4. F-Droid-compatible flavor strategy if needed.
5. Operational ownership plan (availability, incidents, key rotation, abuse handling).

## Final Decision
- Status: **Defer network features**.
- Keep `android.permission.INTERNET` excluded.
- Re-evaluate only after tablet/adaptive UX and local feature roadmap milestones are complete.

## Revisit (2026-09-13)

이 문서의 "Decision Framework for Future Revisit" 4번(`F-Droid-compatible flavor strategy
if needed`)을 따라 업데이트 확인 기능 하나만 다시 열었다. 결과는
`docs/UPDATE_STRATEGY_EVALUATION.md`와 D073에 있다.

- 위 결정은 **그대로 유효하다.** cloud sync / remote backup / AI 헬퍼는 여전히 보류다.
- 바뀌는 것은 `android.permission.INTERNET`을 "어느 산출물에서" 제외하는지의 범위다.
  store · F-Droid · Play 산출물에서는 계속 제외하고, 사이드로드 전용 `github` 플레이버만
  예외가 된다. 구현은 Phase 34로 보류되어 있어 현재 코드에는 아직 적용되지 않았다.
