# GitHub ↔ GitLab 미러 운영 런북

markleaf-android 저장소의 재해 복구(이중 백업) 운영 문서.
설계 원본: `github-gitlab-mirror-final-v4` (v4.0.0)를 이 저장소의 실제 구성에 맞춰 반영.

## 현재 상태 (2026-08-03)

- GitHub와 GitLab 모두 활성 원격이며 같은 `main`과 릴리스 태그를 유지한다.
- **GitLab이 실제로 미러하는 것은 refs뿐이다(D066).** GitLab의 최신 Release는
  `v2.27.2 (2026-07-21)`이고 v2.28.0~v2.32.1 열 릴리스는 Release 객체가 없다.
  릴리스 자산의 영구 사본은 `D:\Build` 하나다.
- GitHub 태그는 GitHub Release와 F-Droid 자동 픽업의 기준이다. GitLab Release
  발행은 GitHub 태그 잡의 두 스텝(`Export versioned release artifacts`,
  `Publish the GitLab release`)이 맡되, 저장소 변수 `GITLAB_RELEASE_MIRROR=true`
  일 때만 돈다 — 2026-08-05부터 켜져 있고, `GITLAB_TOKEN` 스코프가 모자라 매 태그마다
  403으로 실패한다. GitHub Release 발행 뒤에 도는 스텝이라 릴리스 자체에는 영향이
  없다. 해소 방법은 아래 토큰 항목 참조.
- 제공자 간 자동 양방향 mirror는 설정하지 않는다.
- **`main`은 양쪽 모두 보호되어 있다.** GitHub `main`은 2026-07-18부터 PR 필수 ·
  `build` 체크 통과 필수 · admin 예외 없음(`enforce_admins`)이고, GitLab `main`은
  이전부터 `allow_force_push=false`다. **따라서 `git push github main`은 거부된다.**
- 태그(`refs/tags/v*`)는 브랜치 보호 대상이 아니다. 릴리스 절차는 그대로다.

## 원격 구성

| 원격 | URL | 용도 |
|---|---|---|
| `origin` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 평상시 fetch. `main` push는 보호로 막혀 GitHub PR 경유 |
| `gitlab` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 검증·명시적 대상 |
| `github` | `https://github.com/jeiel85/markleaf-android.git` | GitHub Actions·Release·F-Droid 기준 |

인증: GitLab = SSH 키(`~/.ssh/id_ed25519`, GitLab 계정에 등록됨). GitHub = HTTPS + Git Credential Manager.

## 동기화 범위

- **`main`의 정식 경로: GitHub에서 PR을 머지한 뒤 그 커밋을 GitLab으로 전달한다**
  (GitHub → GitLab). `main`이 보호되기 전에는 GitLab 먼저 push했으나, 이제 GitHub
  `main`은 PR로만 전진하므로 순서가 뒤집혔다. 태그는 여전히 GitLab 먼저다.
  이 전달은 `mirror-push.yml`이 자동으로 한다 ("미러 push 자동화 설정" 절).
- GitHub 또는 GitLab 웹 UI에서 직접 만든 커밋은 다른 제공자로 자동 복사되지 않는다.
  단 GitHub `main`에 들어온 커밋은 `mirror-push.yml`이 GitLab으로 전달한다.
- **자동 양방향 mirror는 여전히 쓰지 않는다.** 동시 편집 시 충돌·재전파 루프와 태그
  릴리스 중복 실행 위험 때문이다. `mirror-push.yml`은 이 원칙과 충돌하지 않는다 —
  GitHub → GitLab **단방향**이고, `main` 한 갈래만 다루며(태그 제외), fast-forward만
  가능해 되돌아오는 전파나 릴리스 중복 발동을 만들지 않는다.
- 웹 UI 직접 수정이 꼭 필요하면 먼저 해당 원격을 fetch해 로컬에서 이력을 합친 뒤,
  GitHub는 PR로 올리고 머지된 커밋을 GitLab으로 전달한다.
- 미러 대상은 `refs/heads/main`과 릴리스 태그(`refs/tags/v*`)뿐이다. 기능 브랜치는
  미러하지 않으므로 한쪽 원격에만 있어도 정상이다.
- `scripts/verify-mirror.ps1 -IncludeGitHub`가 두 원격의 ref 일치를 판정하는 최종 게이트다.
  이 스크립트의 비교 범위는 바로 위 미러 대상과 같다 — 한쪽을 바꾸면 다른 쪽도 바꾼다.

## 평상시 작업

`main`은 보호되어 있으므로 직접 push하지 않는다. 브랜치 → PR → `build` 통과 →
머지 순서로만 들어간다(승인은 0건이라 혼자 머지할 수 있다).

```
git switch -c <branch> github/main
git add <파일>
git commit -m "..."
git push github <branch>
gh pr create --base main
# build 통과 후
gh pr merge --merge         # 또는 웹 UI에서 머지
```

머지 방식은 통일되어 있지 않다 — #151·#153은 단일 부모(squash/rebase), #157·#162는
merge commit이다. 미러 정합성에는 어느 쪽도 영향이 없으므로 이 런북은 최근 방식인
`--merge`를 예시로 쓴다. 방식을 통일하고 싶으면 저장소 설정에서 하나만 남기는 편이
문서로 강제하는 것보다 확실하다.

**머지된 `main`의 GitLab 전달은 자동이다.** `.github/workflows/mirror-push.yml`이
`main`에 push가 들어올 때마다 그 커밋을 GitLab `main`으로 민다 (#167). 평상시에는
머지 후 아무것도 하지 않아도 된다.

수동 전달은 자동화가 실패했을 때의 폴백으로 남긴다. 로컬 `main`을 checkout하지 않고
원격 추적 ref를 그대로 밀면 다른 워크트리의 체크아웃을 건드리지 않는다:

```
git fetch github
git push gitlab github/main:refs/heads/main
```

릴리스 태그는 브랜치 보호와 무관하므로 종전대로 GitLab 먼저, GitHub 다음이다.
**태그는 자동화 대상이 아니다** — 이 순서를 뒤집으면 양쪽 릴리스가 중복 발동한다:

```
git push gitlab vX.Y.Z
git push github vX.Y.Z
```

## 미러 push 자동화 설정

`.github/workflows/mirror-push.yml`은 GitLab에 쓸 수 있는 자격증명이 필요하다. 읽기
전용인 미러 *검사*와 달리 push는 익명으로 할 수 없다. 아래는 **사람이 직접 해야 하는
설정이다** — 토큰 값은 저장소에 커밋하지 않는다.

1. **GitLab에서 Project Access Token 발급.** 프로젝트 → Settings → Access Tokens.
   - Role: **Maintainer** (보호된 `main`에 push하려면 필요하다)
   - Scope: **`write_repository`** + **`api`**
   - Expiry: GitLab이 만료일을 강제한다(최대 1년). **날짜를 적어 둘 것** — 아래 갱신 항목 참조.

   `api`는 #252에서 추가로 필요해졌다. 같은 토큰을 릴리스 태그 잡도
   쓰는데(`.github/scripts/mirror-release-to-gitlab.sh`), Package Registry 업로드와
   Release 생성은 `write_repository`로는 안 된다. 이미 `write_repository`만으로
   발급된 토큰이 있다면 **재발급이 필요하다** — GitLab은 기존 토큰의 스코프를
   나중에 넓혀 주지 않는다. 재발급 후 같은 이름(`GITLAB_TOKEN`)으로 GitHub 시크릿을
   덮어쓰면 mirror-push는 그대로 동작한다.

   스코프가 모자란 채로 태그를 밀면 릴리스 잡이 업로드 전에 멈추고
   `The token needs the 'api' scope` 를 남긴다. 절반만 올라간 상태로 끝나지 않는다.

   **저장소 변수 `GITLAB_RELEASE_MIRROR`는 이미 `true`다(2026-08-05, v2.32.2부터).**
   원래는 토큰 재발급 뒤에 켜기로 했으나(D066), 실패를 직접 확인하려고 먼저 켰다.
   따라서 지금은 **토큰만 재발급하면 바로 미러가 동작한다** — 변수는 건드릴 필요가 없다.
   v2.32.0·v2.32.1·v2.32.2 세 태그가 이 메시지로 빨갛게 끝났고(GitHub Release는 매번
   이미 발행된 상태로), 되돌리려면 변수를 지우면 두 스텝이 다시 skip된다.
2. **GitHub에 시크릿 등록.** 저장소 → Settings → Secrets and variables → Actions →
   New repository secret. 이름은 정확히 **`GITLAB_TOKEN`**.
3. **확인.** Actions 탭 → **Mirror push** → Run workflow. 또는 아무 PR이나 머지하면
   자동으로 돈다. 성공하면 job summary에 `GitLab main ← <sha>`가 찍힌다.

**만료 시 동작: 조용히 멈추지 않고 시끄럽게 실패한다.** 토큰이 만료되면 push가 인증
오류로 실패하고 잡이 빨개진다. 미러가 뒤처지기 시작하면 매일 도는 `mirror-check`도
함께 빨개진다. 두 신호가 같이 뜨면 토큰 만료를 먼저 의심할 것.

**이 자동화가 할 수 없는 일.** `--force` 계열을 쓰지 않으므로 GitLab을 fast-forward로
전진시키는 것만 가능하다. GitHub 쪽 이력이 재작성되면 push가 거부되고(GitLab
`main`의 `allow_force_push=false`도 서버에서 같은 것을 막는다) 잡이 실패한다. 즉
**재작성된 이력은 자동으로 전파되지 않는다** — 그때는 "이력이 갈라졌을 때 복구"를
사람이 따라야 한다. 되감기 push가 거부되고 대상 ref가 그대로 유지되는 것은 실측으로
확인했다.

## 백업 검증

```
pwsh scripts/verify-mirror.ps1 -IncludeGitHub
```

항상 `-IncludeGitHub`로 로컬·GitLab·GitHub 3자 ref를 대조한다. 스위치 없이 실행하면
로컬 vs GitLab만 보므로 **미러 일치는 판정되지 않는다.**

비교 범위는 위 "동기화 범위"와 같은 `refs/heads/main` + `refs/tags/v*`뿐이다. 기능
브랜치는 애초에 미러 대상이 아니므로 양쪽 원격 어디에 있든 비교에서 제외한다 —
범위에 넣으면 작업 브랜치가 남아 있는 정상 작업 사본이 매번 실패해 게이트가
무의미해진다. 출력의 `95/104`는 "범위 내 95개 / 전체 104개"라는 뜻이다.

종료 코드로 실패 종류를 구분한다:

| 코드 | 의미 | 대응 |
|---|---|---|
| 0 | 요청한 검사가 모두 통과 | — |
| 1 | GitLab ref를 읽지 못함 | 원격·인증 확인 |
| 2 | 로컬이 GitLab과 불일치 | `git fetch`로 로컬을 최신화. 미러 자체는 정상 |
| 3 | **GitHub와 GitLab의 미러 ref가 갈라짐** | 아래 "이력이 갈라졌을 때 복구"로 간다 |
| 4 | GitHub를 읽지 못해 미러를 판정하지 못함 | 미러 상태 미판정. 통과로 취급하지 않는다 |

둘 이상 해당하면 `3 > 4 > 2` 순으로 심각한 쪽을 반환한다. `3`은 사람이 개입해야
하는 신호이고, `2`는 로컬 사정이라 미러 정합성과는 무관하다.

### 자동화·CI에서 (`-MirrorOnly`)

```
pwsh scripts/verify-mirror.ps1 -MirrorOnly
```

로컬 비교를 건너뛰고 GitHub vs GitLab만 판정한다. 미러를 보려면 GitHub를 읽어야
하므로 이 스위치는 `-IncludeGitHub`를 함축한다 — 단독으로 완결된 호출이다. 종료
코드는 `0`/`1`/`3`/`4`만 나오고 `2`는 발생하지 않는다.

CI 러너에서는 이 스위치가 **필수다.** `actions/checkout`은 기본이 shallow라 태그를
가져오지 않으므로, 로컬 비교를 켜면 러너의 ref 1개가 GitLab의 95개와 대조되어
미러가 멀쩡해도 매번 `2`로 실패한다(실측: 태그 94건이 전부 `<missing>`). 로컬 ref는
작업 사본이 아니라 체크아웃 설정의 부산물이므로 비교 자체가 무의미하다.

두 원격 모두 익명 HTTPS로 읽히므로 시크릿이 필요 없다. 원격 이름 대신 URL을 직접
넘길 수 있어 원격이 설정되지 않은 러너에서도 그대로 동작한다:

```
pwsh scripts/verify-mirror.ps1 -MirrorOnly `
  -GitLabRemote https://gitlab.com/jeiel85/markleaf-android.git `
  -GitHubRemote https://github.com/jeiel85/markleaf-android.git
```

### 자동 감시 (`.github/workflows/mirror-check.yml`)

같은 검사를 매일 03:17 UTC에 GitHub Actions가 돌린다. 수동 실행은 Actions 탭의
**Mirror check → Run workflow**(`workflow_dispatch`)로 한다. 시크릿이 없으므로 fork나
다른 체크아웃에서도 그대로 동작한다.

잡이 실패하면 무엇이 잘못됐는지는 **`::error::` 주석**에 있다. Actions의 pwsh 셸은
스텝을 `pwsh -command ". '<file>'"`로 실행하는데 이 형태에서는 `exit 3`이 프로세스
종료 코드 1로 뭉개져서, 잡의 성공/실패는 정확해도 3·4·1의 구분은 종료 코드에 남지
않는다. 실행 로그의 주석을 읽을 것.

**한계: GitHub가 내려가면 이 감시도 함께 멈춘다.** 감시자를 감시 대상 위에 올린
대가다. 2026-07-10 flagged 사례처럼 GitHub가 장기간 막히면 미러 상태를 알려줄 주체가
없어진다 — 그 구간에는 로컬에서 `pwsh scripts/verify-mirror.ps1 -IncludeGitHub`를
직접 돌린다. GitLab CI에 같은 잡을 두면 이 사각지대가 사라지지만(그쪽은 GitHub 장애
중에도 돌아 `4`를 보고할 수 있다), 스케줄을 GitLab UI에서 따로 만들어야 해서 저장소에
남지 않는다. 지금은 단순함을 택했다.

## 한쪽 원격 장애 시

> [!IMPORTANT]
> **GitHub 장애는 이제 `main` 전진을 막는다.** `main`이 GitHub PR로만 전진하므로,
> GitHub가 내려가면 새 커밋을 `main`에 넣을 수 없다. 보호 이전에는 GitLab 먼저
> push해서 GitHub 장애를 우회할 수 있었으나 그 경로가 닫혔다 — 보호를 admin까지
> 적용한 대가다. 급하면 아래 5번의 임시 해제 절차를 쓴다.

1. **GitLab 장애**: GitHub PR 머지는 평소대로 진행하고 GitLab 전달만 보류한다.
   복구 후 `git push gitlab github/main:refs/heads/main`로 따라잡는다.
2. **GitHub 장애**: `main`은 멈춘다. 작업은 로컬 브랜치와 GitLab에 보존해 두고
   (`git push gitlab <branch>`) 복구 후 PR로 올린다.
3. 장애 복구 후 앞선 원격과 로컬의 ref를 대조한다.
4. fast-forward가 확인된 경우에만 뒤처진 원격에 밀어넣는다. 태그는 양쪽 모두
   직접 push할 수 있다:
   ```
   git push gitlab github/main:refs/heads/main   # GitLab이 뒤처진 경우
   git push <remote> --tags
   ```
   GitHub `main`이 뒤처진 경우는 직접 push할 수 없다 — PR로 올린다.
5. 이력이 갈라졌으면 자동 덮어쓰지 않고 수동 확인한다.
6. `pwsh scripts/verify-mirror.ps1 -IncludeGitHub`로 3자 일치를 확인한다.

## 이력이 갈라졌을 때 복구 (보호 브랜치)

한쪽 원격의 `main`을 재작성(reset + force push)하면 다른 원격은 따라갈 수 없다.
GitLab `main`은 `allow_force_push=false`로 보호되어 있어 재작성된 이력을 그대로
받을 수 없고, 두 원격이 갈라진 채로 남는다. v2.24.0에서 실제로 발생했다 — 릴리스
커밋이 무관한 landing-i18n 변경을 쓸어담아 GitHub 쪽을 되돌렸고, GitLab은 옛 이력을
유지한 상태로 며칠간 divergent했다 (#154).

**원칙: 재작성이 아니라 전진 수정(forward fix)으로 복구한다.** 두 원격 모두
fast-forward로 받을 수 있는 새 커밋을 만드는 방법이 보호 설정을 건드리지 않고,
이미 태그를 받아간 F-Droid·다운로더의 이력도 깨지 않는다.

1. 갈라진 지점을 먼저 확인한다. 되돌릴 대상이 무엇인지 눈으로 본 뒤에 움직인다.
   ```
   git fetch gitlab && git fetch github
   git log --oneline --graph gitlab/main github/main
   git merge-base gitlab/main github/main
   ```
2. **아직 force push하지 않았다면** 잘못된 커밋을 `git revert`로 되돌린다.
   `git reset --hard` + force push는 쓰지 않는다. revert 커밋은 양쪽 모두
   fast-forward이므로 보호 설정을 그대로 두고 평소 순서로 push하면 끝난다.
   ```
   git switch -c fix/revert-<bad-sha> github/main
   git revert <bad-sha>
   git push github fix/revert-<bad-sha>
   gh pr create --base main        # build 통과 후 머지
   git fetch github && git push gitlab github/main:refs/heads/main
   ```
3. **이미 한쪽을 재작성해 갈라졌다면** 재작성한 쪽을 되돌리는 대신 두 이력을
   로컬에서 merge해 양쪽이 받을 수 있는 공통 커밋을 만든다. v2.24.0은 이 경로로
   해결했다 (merge `4b2dfd9`).
   ```
   git switch -c fix/reconcile-mirrors github/main
   git merge gitlab/main          # 뒤처진/갈라진 쪽을 합친다
   # 충돌은 릴리스에 나가야 할 내용 기준으로 해소한다
   git push github fix/reconcile-mirrors
   gh pr create --base main       # build 통과 후 머지 커밋으로 들어간다
   git fetch github && git push gitlab github/main:refs/heads/main
   ```
4. merge로도 해결이 안 되는 경우(예: 유출된 시크릿처럼 이력에서 반드시 제거해야
   하는 내용)에만 보호를 임시로 내린다. **이제 양쪽 모두 내려야 한다.**
   1. GitLab → Settings → Repository → Protected branches에서 `main`의
      **Allowed to force push**를 켠다.
   2. GitHub `main`의 보호를 끈다:
      ```
      gh api -X DELETE repos/jeiel85/markleaf-android/branches/main/protection
      ```
   3. `git push gitlab main --force-with-lease` 후 `git push github main
      --force-with-lease` (`--force`가 아니라 `--force-with-lease` — 다른 세션의
      push를 덮어쓰지 않는다).
   4. **즉시 양쪽 보호를 원상 복구한다.** 이 단계를 잊으면 보호되지 않은 `main`이
      남는다. GitHub 쪽 복구 설정은 아래 "GitHub `main` 보호 설정" 절의 값을 그대로
      다시 적용하고, `gh api repos/jeiel85/markleaf-android/branches/main --jq .protected`
      가 `true`인지 확인한다.
   4. 태그도 재작성해야 하면 보호된 `v*` 태그를 지우고 다시 만들어야 하며,
      GitHub Release와 F-Droid 픽업이 재실행된다(GitLab Release 스텝은 미러가 켜져
      있을 때만). 이미 배포된 버전 번호는 재사용하지 말고 새 patch 버전으로 올리는
      쪽을 우선한다.
5. 어느 경로든 마지막에 3자 일치를 확인한다.
   ```
   pwsh scripts/verify-mirror.ps1 -IncludeGitHub
   ```

예방: 릴리스 커밋은 `git add -A`로 만들지 않는다. 변경 파일을 명시적으로 stage하거나
커밋 전 working tree가 릴리스 대상만 담고 있는지 확인한다 (AGENTS.md의 릴리스 절차).
갈라짐을 만들지 않는 것이 갈라짐을 복구하는 것보다 항상 싸다.

## GitHub `main` 보호 설정

2026-07-18 적용. 검증된 PR이 CI를 기다리는 동안 다른 세션이 같은 변경을 `main`에
직접 push해 중복 작업이 발생한 뒤 도입했다 — 무보호 상태에서는 빠르고 무검증인
경로가 느리고 검증된 경로를 이긴다.

| 설정 | 값 | 이유 |
|---|---|---|
| PR 필수 | 예 (승인 0건) | 1인 저장소라 승인자를 요구하면 스스로 막힌다 |
| required check | `build`, `instrumented-tests` | 하드 게이트. `instrumented-tests`는 2026-08-05 승격(#235 마이그레이션 어서션을 실제로 게이트하기 위해). `launch-smoke`는 에뮬레이터 flake 때문에 계속 제외 |
| `strict`(최신화 강제) | 아니오 | 병렬 세션에서 매번 재실행되는 것을 피한다 |
| `enforce_admins` | **예** | 모든 세션이 admin 계정으로 돌기 때문에, 끄면 보호가 무의미하다 |
| force push · 삭제 | 금지 | GitLab `allow_force_push=false`와 대칭 |

재적용:

```
gh api -X PUT repos/jeiel85/markleaf-android/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": { "strict": false, "contexts": ["build", "instrumented-tests"] },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 0
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```

`git push --dry-run`으로는 보호 동작을 확인할 수 없다 — dry-run은 GitHub의
pre-receive 훅까지 가지 않아 거부돼야 할 push도 성공으로 보고한다. 확인은
`gh api .../branches/main --jq .protected`로 한다.

## 원칙 (설계 준수)

- `main` push 순서: **GitHub PR 머지 먼저 → GitLab 전달 다음** (2026-07-18 보호 적용으로
  종전 GitLab 우선에서 뒤집힘). 태그는 종전대로 **GitLab 먼저 → GitHub 다음**.
- 자동 force push 금지
- GitHub Actions: 빌드/릴리스/F-Droid 태그 인계용으로 활성.
- GitLab CI: 브랜치·MR 검증과 보호된 `vX.Y.Z` 태그의 독립 서명 릴리스용으로 활성.
- 두 채널 모두 production 인증서 SHA-256(`0be97352…f91a`)을 검증한다.
- GitLab Release 파일은 만료되는 job artifact가 아니라 Generic Package Registry에 보존한다
  (미러가 동작할 때. 지금은 변수는 켜져 있으나 토큰 스코프가 모자라 매 태그마다 실패하므로
  실제로 보존되는 것이 없고, 영구 사본은 `D:\Build` 하나다 — D066).
- 워크플로는 저장소로 산출물을 되커밋하지 않는다.
