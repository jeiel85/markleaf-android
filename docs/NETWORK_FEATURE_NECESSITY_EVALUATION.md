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
