# Nightly issue bot

Runs every night (default 03:00) **only while this PC is on**. It looks at open
GitHub issues and, for each one:

1. posts a **friendly acknowledgment comment** in the reporter's language,
2. if the issue is a clear, low-risk bug, **fixes it on a branch**, runs the
   hard gates (`testDebugUnitTest` + `lintRelease`), and **opens a PR** (never
   pushes to `main`),
3. builds an **unsigned candidate APK** per opened PR into `Desktop\Build\`
   (device testing only),
4. **emails you a summary** of what it did.

If there are **no open issues, it does nothing** (no email).

## What it deliberately does NOT do

It never bumps the version, never tags, never releases, never pushes to `main`,
never touches signing/secrets. Cutting a real release (tag push → F-Droid/Play)
stays your morning decision after you review the PR. That asymmetry is the whole
point: a bad auto-fix as a PR costs a click to close; a bad auto-release ships to
real users.

## One-time setup

1. **SMTP creds.** Copy the template and fill in a Gmail app password:
   ```powershell
   Copy-Item scripts\nightly\config.local.psd1.template scripts\nightly\config.local.psd1
   notepad scripts\nightly\config.local.psd1
   ```
   App password: https://myaccount.google.com/apppasswords (2-Step Verification
   must be ON). `config.local.psd1` is git-ignored.

2. **Dry run** (safe — no comments, no PRs, no email; prints a preview):
   ```powershell
   pwsh -File scripts\nightly\nightly-issue-bot.ps1 -DryRun
   ```

3. **One real run, watched**, to confirm comments/PRs/email look right:
   ```powershell
   pwsh -File scripts\nightly\nightly-issue-bot.ps1
   ```

4. **Schedule it:**
   ```powershell
   pwsh -File scripts\nightly\register-task.ps1
   ```
   Remove later with `unregister-task.ps1`.

## Files

| File | Purpose |
|------|---------|
| `nightly-issue-bot.ps1` | Orchestrator / scheduler entry point |
| `issue-bot.prompt.md` | The headless `claude -p` instructions (the brain) |
| `lib/Send-NightlyMail.ps1` | SMTP email helper |
| `config.local.psd1.template` | Copy → `config.local.psd1`, fill SMTP |
| `register-task.ps1` / `unregister-task.ps1` | Scheduled-task install/remove |
| `.runtime/` | Logs + `result.json` (git-ignored) |

## Notes

- Work happens in an isolated `git worktree` under `%TEMP%`, so your main
  checkout is never switched or dirtied.
- "Only when the PC is on": the task uses `StartWhenAvailable=$false` and no
  wake timer, so a PC that's off or asleep at 03:00 just skips that night.
- It runs **only while you're logged on**, so `claude` and `gh` use your normal
  credentials (no stored password).
- The headless run uses `--dangerously-skip-permissions` (unattended can't
  answer prompts). The prompt's hard prohibitions are the guardrail; the
  worktree + "no push to main" keep the blast radius to reviewable PRs.
