# Markleaf Ralph Loop Prompt

You are an autonomous coding agent working on Markleaf.

This is a Ralph Loop iteration. You must make measurable progress, but keep the change small, safe, and reviewable.

## Read First

Before changing anything, read:

- `AGENTS.md`
- `docs/AGENT_SPEC.md`
- `.agent/tasks.md`
- `.agent/progress.md`
- `.agent/decisions.md`

## Project Rules

- The Android `applicationId` must be `com.markleaf.notes`.
- Do not add `android.permission.INTERNET`.
- Do not add API integrations.
- Do not add login/account features.
- Do not add analytics, ads, tracking, Firebase, remote config, crash reporting SDKs, or proprietary SDKs.
- Keep the project F-Droid-friendly.
- Keep the UI simple, fast, and design-focused.
- Do not copy branding, UI assets, colors, names, or wording from existing note apps.

## Task Selection

1. Open `.agent/tasks.md`.
2. Pick the first unchecked task with the highest priority.
3. Work on exactly one task.
4. Do not jump ahead to later phases.
5. Do not expand the scope beyond the selected task.

## Implementation Rules

- Prefer Kotlin.
- Prefer Jetpack Compose for UI.
- Prefer Material 3.
- Keep architecture simple.
- Avoid unnecessary abstraction.
- Keep changes small.
- Add tests when the task touches parsing, title extraction, slug generation, repository behavior, or database logic.

## Verification

After implementation, run the most relevant checks.

Prefer:

```bash
./gradlew test
./gradlew assembleDebug
```

If the Android project is not initialized yet, initialize it first and then run the available Gradle task.

If checks fail:

1. Read the error.
2. Fix the cause.
3. Run checks again.
4. Repeat until checks pass or a real blocker is found.

## Completion Rules

When the selected task is complete:

1. Update `.agent/tasks.md` by checking the completed task.
2. Append a dated entry to `.agent/progress.md`.
3. Add important implementation decisions to `.agent/decisions.md`.
4. Run `git status`.
5. Commit the change with a clear conventional commit message.

Commit examples:

```text
chore: initialize android project
chore: add compose navigation skeleton
feat: add notes list placeholder
feat: add note editor autosave
test: add tag parser tests
docs: update project decisions
```

## Blocker Rules

If blocked, do not invent requirements.

Instead:

1. Leave the task unchecked.
2. Append the blocker to `.agent/progress.md`.
3. Explain exactly what is blocking progress.
4. Suggest the next safe action.

## Final Response

At the end of this iteration, report:

- Selected task
- Files changed
- Commands run
- Test/build result
- Commit hash, if committed
- Remaining next task
- Any risks or blockers
