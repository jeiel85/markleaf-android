<#
.SYNOPSIS
    Nightly GitHub-issue bot for Markleaf. Entry point for the Windows Task
    Scheduler (daily 03:00, "only if the PC is on").

.DESCRIPTION
    Deterministic orchestration lives here; judgment-heavy work (writing a
    friendly comment, attempting a fix, opening a PR) is delegated to a single
    headless `claude -p` run scoped by issue-bot.prompt.md.

    Flow:
      1. Load config.local.psd1 (SMTP creds + options).
      2. git fetch; create an isolated worktree off origin/main so the user's
         main checkout is never touched.
      3. List open issues. NONE -> log and exit quietly (no email).
      4. Run headless claude in the worktree to comment / fix / open PRs.
      5. Build an unsigned candidate APK per opened PR into Desktop\Build\.
      6. Email a summary. Clean up the worktree and gradle daemons.

    SAFETY: never bumps versions, never tags, never pushes to main, never
    releases. Those stay the user's morning decision after reviewing the PRs.

.PARAMETER DryRun
    No side effects: claude only reads + drafts (no comments, no pushes, no
    PRs), no candidate build, and the summary is printed instead of emailed.
    Use this to test the pipeline safely.

.PARAMETER MaxIssues
    Override the per-night issue cap (default from config, fallback 5).
#>
[CmdletBinding()]
param(
    [switch] $DryRun,
    [int]    $MaxIssues = 0
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# --- Paths --------------------------------------------------------------------
$ScriptDir   = $PSScriptRoot
$RepoRoot    = (Resolve-Path (Join-Path $ScriptDir '..\..')).Path
$RuntimeDir  = Join-Path $ScriptDir '.runtime'
$LogDir      = Join-Path $RuntimeDir 'logs'
$ConfigPath  = Join-Path $ScriptDir 'config.local.psd1'
$PromptPath  = Join-Path $ScriptDir 'issue-bot.prompt.md'
$MailLib     = Join-Path $ScriptDir 'lib\Send-NightlyMail.ps1'
$ResultPath  = Join-Path $RuntimeDir 'result.json'
$RenderedPrompt = Join-Path $RuntimeDir 'prompt.rendered.md'
$WorktreeDir = Join-Path $env:TEMP 'markleaf-nightly-wt'

New-Item -ItemType Directory -Force -Path $RuntimeDir, $LogDir | Out-Null

$Stamp   = (Get-Date).ToString('yyyyMMdd-HHmmss')
$LogPath = Join-Path $LogDir "nightly-$Stamp.log"

# --- Logging ------------------------------------------------------------------
$script:LogLines = New-Object System.Collections.Generic.List[string]
function Log {
    param([string] $Message, [string] $Level = 'INFO')
    $line = "{0} [{1}] {2}" -f (Get-Date).ToString('HH:mm:ss'), $Level, $Message
    $script:LogLines.Add($line)
    Add-Content -Path $LogPath -Value $line -Encoding UTF8
    Write-Host $line
}

# Resolve a Desktop\Build path that honors OneDrive redirection (see global
# windows-paths rule). [Environment]::GetFolderPath follows KNOWNFOLDERID.
function Get-DesktopBuildDir {
    $desktop = [Environment]::GetFolderPath('Desktop')
    if ([string]::IsNullOrWhiteSpace($desktop)) { $desktop = Join-Path $env:USERPROFILE 'Desktop' }
    $build = Join-Path $desktop 'Build'
    New-Item -ItemType Directory -Force -Path $build | Out-Null
    return $build
}

function Stop-GradleNoise {
    # v2.16.3 lesson: under RAM pressure gradle daemons turn zombie. Stop them
    # and reap stray java so the next run starts clean.
    try { & (Join-Path $WorktreeDir 'gradlew.bat') --stop 2>&1 | Out-Null } catch {}
    try { & (Join-Path $RepoRoot 'gradlew.bat') --stop 2>&1 | Out-Null } catch {}
}

$cfg = $null
try {
    Log "Nightly issue bot starting (DryRun=$DryRun). Repo: $RepoRoot"

    # --- Config ---------------------------------------------------------------
    if (-not (Test-Path $ConfigPath)) {
        throw "Missing $ConfigPath. Copy config.local.psd1.template -> config.local.psd1 and fill in SMTP."
    }
    $cfg = Import-PowerShellDataFile -Path $ConfigPath
    . $MailLib
    if ($MaxIssues -le 0) {
        $MaxIssues = if ($cfg.MaxIssues) { [int]$cfg.MaxIssues } else { 5 }
    }
    $buildCandidates = $true
    if ($null -ne $cfg.BuildCandidates) { $buildCandidates = [bool]$cfg.BuildCandidates }

    # --- Resolve claude exe ---------------------------------------------------
    $claudeExe = (Get-Command claude -ErrorAction SilentlyContinue).Source
    if (-not $claudeExe) { $claudeExe = Join-Path $env:USERPROFILE '.local\bin\claude.exe' }
    if (-not (Test-Path $claudeExe)) { throw "claude CLI not found (tried PATH and $claudeExe)." }
    Log "Using claude: $claudeExe"

    # --- Refresh git + list issues (in main checkout, read-only) --------------
    Push-Location $RepoRoot
    try {
        & git fetch origin --prune 2>&1 | ForEach-Object { Log "git: $_" }
    } finally { Pop-Location }

    Log "Listing open issues (cap $MaxIssues)..."
    $issuesJson = & gh issue list --repo jeiel85/markleaf-android --state open --limit $MaxIssues --json number,title,url 2>&1
    if ($LASTEXITCODE -ne 0) { throw "gh issue list failed: $issuesJson" }
    $openIssues = @($issuesJson | ConvertFrom-Json)

    if ($openIssues.Count -eq 0) {
        Log "No open issues. Nothing to do — exiting quietly (no email)."
        return
    }
    Log ("Found {0} open issue(s): {1}" -f $openIssues.Count, ($openIssues.number -join ', '))

    # --- Create isolated worktree off origin/main -----------------------------
    if (Test-Path $WorktreeDir) {
        Push-Location $RepoRoot
        try { & git worktree remove --force $WorktreeDir 2>&1 | Out-Null } catch {} finally { Pop-Location }
        if (Test-Path $WorktreeDir) { Remove-Item -Recurse -Force $WorktreeDir -ErrorAction SilentlyContinue }
    }
    Push-Location $RepoRoot
    try {
        & git worktree add --detach $WorktreeDir origin/main 2>&1 | ForEach-Object { Log "worktree: $_" }
        if ($LASTEXITCODE -ne 0) { throw "git worktree add failed." }
    } finally { Pop-Location }

    # local.properties (SDK path) is git-ignored, so copy it into the worktree
    # or gradle in the worktree can't find the Android SDK.
    $srcLocalProps = Join-Path $RepoRoot 'local.properties'
    if (Test-Path $srcLocalProps) {
        Copy-Item $srcLocalProps (Join-Path $WorktreeDir 'local.properties') -Force
        Log "Copied local.properties into worktree."
    } else {
        Log "No local.properties in repo root; relying on ANDROID_HOME for SDK." 'WARN'
    }

    # --- Render the prompt ----------------------------------------------------
    $modeBlock = if ($DryRun) {
        "DRY-RUN. Do NOT post any comment, do NOT create branches, push, or open PRs. " +
        "Only READ issues and DRAFT what you would say/do. Still write the summary JSON " +
        "with action values reflecting what you WOULD have done (and commentPosted=false)."
    } else {
        "LIVE. Act for real: post comments, open PRs as described below."
    }
    $promptText = Get-Content -Path $PromptPath -Raw -Encoding UTF8
    $promptText = $promptText.Replace('{{MODE_BLOCK}}', $modeBlock).Replace('{{RESULT_PATH}}', ($ResultPath -replace '\\','/'))
    Set-Content -Path $RenderedPrompt -Value $promptText -Encoding UTF8

    # --- Run headless claude in the worktree ----------------------------------
    if (Test-Path $ResultPath) { Remove-Item $ResultPath -Force }
    $claudeOut = Join-Path $LogDir "claude-$Stamp.out.log"
    $claudeErr = Join-Path $LogDir "claude-$Stamp.err.log"
    Log "Launching claude (timeout 45 min)..."
    $proc = Start-Process -FilePath $claudeExe `
        -ArgumentList @('-p', '--dangerously-skip-permissions') `
        -WorkingDirectory $WorktreeDir `
        -RedirectStandardInput $RenderedPrompt `
        -RedirectStandardOutput $claudeOut `
        -RedirectStandardError $claudeErr `
        -NoNewWindow -PassThru
    if (-not $proc.WaitForExit(45 * 60 * 1000)) {
        Log "claude exceeded 45 min — killing." 'WARN'
        try { $proc.Kill($true) } catch { try { Stop-Process -Id $proc.Id -Force } catch {} }
    }
    Log "claude exited (code $($proc.ExitCode)). Output -> $claudeOut"

    # --- Parse result ---------------------------------------------------------
    $result = $null
    if (Test-Path $ResultPath) {
        try { $result = Get-Content $ResultPath -Raw -Encoding UTF8 | ConvertFrom-Json }
        catch { Log "Failed to parse result.json: $_" 'WARN' }
    }
    if (-not $result) {
        Log "claude produced no parseable result.json." 'WARN'
    }

    # --- Build candidate APKs for opened PRs ----------------------------------
    $builtApks = New-Object System.Collections.Generic.List[string]
    if (-not $DryRun -and $buildCandidates -and $result -and $result.issues) {
        $prIssues = @($result.issues | Where-Object { $_.action -eq 'pr-opened' -and $_.branch })
        if ($prIssues.Count -gt 0) {
            $buildDir = Get-DesktopBuildDir
            foreach ($it in $prIssues) {
                try {
                    Log "Building candidate APK for issue #$($it.number) (branch $($it.branch))..."
                    Push-Location $WorktreeDir
                    try {
                        & git checkout $it.branch 2>&1 | ForEach-Object { Log "git: $_" }
                        & .\gradlew.bat --no-daemon :app:assembleRelease 2>&1 | Tee-Object -FilePath (Join-Path $LogDir "build-issue$($it.number)-$Stamp.log") | Out-Null
                    } finally { Pop-Location }
                    $apk = Get-ChildItem (Join-Path $WorktreeDir 'app\build\outputs\apk\release') -Filter '*.apk' -ErrorAction SilentlyContinue |
                           Sort-Object LastWriteTime -Descending | Select-Object -First 1
                    if ($apk) {
                        $sha = (& git -C $WorktreeDir rev-parse --short HEAD).Trim()
                        $dest = Join-Path $buildDir ("markleaf-issue{0}-candidate-{1}.apk" -f $it.number, $sha)
                        Copy-Item $apk.FullName $dest -Force
                        $builtApks.Add($dest)
                        Log "Candidate APK -> $dest"
                    } else {
                        Log "No APK produced for issue #$($it.number)." 'WARN'
                    }
                } catch {
                    Log "Candidate build for issue #$($it.number) failed: $_" 'WARN'
                }
            }
        }
    }

    # --- Compose summary ------------------------------------------------------
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("Markleaf nightly issue bot — $(Get-Date -Format 'yyyy-MM-dd HH:mm')")
    [void]$sb.AppendLine(("Mode: {0}" -f $(if ($DryRun) { 'DRY-RUN' } else { 'LIVE' })))
    [void]$sb.AppendLine(("Open issues handled: {0}" -f $openIssues.Count))
    [void]$sb.AppendLine('')
    $prCount = 0; $commentCount = 0
    if ($result -and $result.issues) {
        foreach ($it in $result.issues) {
            [void]$sb.AppendLine(("#{0} [{1}] {2}" -f $it.number, $it.action, $it.title))
            if ($it.summary)  { [void]$sb.AppendLine("   $($it.summary)") }
            if ($it.url)      { [void]$sb.AppendLine("   issue: $($it.url)") }
            if ($it.prUrl)    { [void]$sb.AppendLine("   PR:    $($it.prUrl)"); $prCount++ }
            if ($it.commentPosted) { $commentCount++ }
            [void]$sb.AppendLine('')
        }
        if ($result.notes) { [void]$sb.AppendLine("Notes: $($result.notes)"); [void]$sb.AppendLine('') }
    } else {
        [void]$sb.AppendLine("(No result.json — see claude log: $claudeOut)")
        [void]$sb.AppendLine('')
    }
    if ($builtApks.Count -gt 0) {
        [void]$sb.AppendLine("Candidate APKs (Desktop\Build, unsigned — for device testing only):")
        foreach ($a in $builtApks) { [void]$sb.AppendLine("   $a") }
        [void]$sb.AppendLine('')
    }
    [void]$sb.AppendLine("Reminder: version bump + tag (= release to F-Droid/Play) is NOT automated.")
    [void]$sb.AppendLine("Review the PR(s) in the morning, then cut the release with your normal flow.")
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine("Log: $LogPath")
    $body = $sb.ToString()

    $subject = "[Markleaf 새벽봇] 이슈 {0}건 — PR {1}건, 댓글 {2}건{3}" -f `
        $openIssues.Count, $prCount, $commentCount, $(if ($DryRun) { ' (DRY-RUN)' } else { '' })

    # --- Notify ---------------------------------------------------------------
    if ($DryRun) {
        Log "DRY-RUN — email skipped. Summary below:"
        Write-Host "`n========== EMAIL PREVIEW ==========`nSubject: $subject`n`n$body`n==================================="
    } else {
        Send-NightlyMail -Config $cfg -Subject $subject -Body $body
        Log "Notification email sent to: $($cfg.NotifyTo -join ', ')"
    }

    Log "Done."
}
catch {
    $err = $_ | Out-String
    Log "FATAL: $err" 'ERROR'
    # Best-effort failure email so a broken night doesn't fail silently.
    if ($cfg -and -not $DryRun) {
        try {
            Send-NightlyMail -Config $cfg `
                -Subject "[Markleaf 새벽봇] 실패 — 확인 필요" `
                -Body ("새벽 이슈봇이 오류로 중단됐습니다.`n`n$err`n`nLog: $LogPath")
        } catch { Log "Failure email also failed: $_" 'ERROR' }
    }
    exit 1
}
finally {
    # --- Cleanup --------------------------------------------------------------
    Stop-GradleNoise
    if (Test-Path $WorktreeDir) {
        Push-Location $RepoRoot
        try { & git worktree remove --force $WorktreeDir 2>&1 | Out-Null } catch {} finally { Pop-Location }
        if (Test-Path $WorktreeDir) { Remove-Item -Recurse -Force $WorktreeDir -ErrorAction SilentlyContinue }
    }
    try { & git -C $RepoRoot worktree prune 2>&1 | Out-Null } catch {}
}
