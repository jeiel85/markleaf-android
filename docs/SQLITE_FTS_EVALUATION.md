# SQLite FTS Evaluation (Issue #2)

## Date
2026-04-30

## Conclusion
Adopt SQLite FTS in a phased rollout after current LIKE search baseline.

## Why
- Current LIKE search is simple but degrades as note volume grows.
- FTS provides better query latency and ranking options for larger datasets.
- Room supports FTS entities, so integration can stay Android-native.

## Recommended Approach
1. Introduce FTS virtual table for searchable note text (title + excerpt + markdown body).
2. Keep current tables as source of truth.
3. Sync FTS index on insert/update/delete via repository-layer transaction.
4. Feature-flag FTS query path, fallback to LIKE for safe rollout.

## Migration Safety
- No destructive schema migration.
- Additive migration only: create FTS table and backfill from existing notes.
- Keep rollback path by preserving LIKE query implementation.

## Korean Search Notes
- 기본 SQLite tokenizer 한계가 있으므로 한글 검색 품질을 실제 데이터셋으로 검증 필요.
- 초기에는 prefix + exact match 중심으로 시작하고, 필요 시 tokenizer 전략 검토.

## Performance Check Plan
- Dataset sizes: 1k / 5k / 20k notes.
- Metrics: query latency p50/p95, index update latency, DB size overhead.
- Compare LIKE vs FTS in same device profile.

## Decision
- Proceed: **Yes (phased)**.
- Start with additive schema + dual query path.
