# GitHub ↔ GitLab 미러 운영 런북

markleaf-android 저장소의 재해 복구(이중 백업) 운영 문서.
설계 원본: `github-gitlab-mirror-final-v4` (v4.0.0)를 이 저장소의 실제 구성에 맞춰 반영.

## 현재 상태 (2026-07-18)

- GitHub와 GitLab 모두 활성 원격이며 같은 `main`과 릴리스 태그를 유지한다.
- GitLab은 공개 소스 미러이자 독립 서명 릴리스 채널이다.
- GitHub 태그는 GitHub Release와 F-Droid 자동 픽업의 기준이고, GitLab 태그는
  GitLab CI의 독립 빌드와 Generic Package Registry 기반 Release를 발동한다.
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
- GitHub 또는 GitLab 웹 UI에서 직접 만든 커밋은 다른 제공자로 자동 복사되지 않는다.
- 자동 양방향 mirror는 동시 편집 시 충돌·재전파 루프와 태그 릴리스 중복 실행 위험이
  있으므로 사용하지 않는다.
- 웹 UI 직접 수정이 꼭 필요하면 먼저 해당 원격을 fetch해 로컬에서 이력을 합친 뒤,
  GitHub는 PR로 올리고 머지된 커밋을 GitLab으로 전달한다.
- `scripts/verify-mirror.ps1 -IncludeGitHub`가 두 원격의 ref 일치를 판정하는 최종 게이트다.

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

머지된 `main`을 GitLab으로 전달한다. 로컬 `main`을 checkout하지 않고 원격 추적
ref를 그대로 밀면 다른 워크트리의 체크아웃을 건드리지 않는다:

```
git fetch github
git push gitlab github/main:refs/heads/main
```

릴리스 태그는 브랜치 보호와 무관하므로 종전대로 GitLab 먼저, GitHub 다음이다:

```
git push gitlab vX.Y.Z
git push github vX.Y.Z
```

## 백업 검증

```
pwsh scripts/verify-mirror.ps1
```

항상 `-IncludeGitHub`로 로컬·GitLab·GitHub 3자 ref를 대조한다.

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
      GitHub Release·F-Droid 픽업·GitLab Release가 모두 재실행된다. 이미 배포된
      버전 번호는 재사용하지 말고 새 patch 버전으로 올리는 쪽을 우선한다.
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
| required check | `build` | 하드 게이트. `launch-smoke`는 에뮬레이터 flake 때문에 제외 |
| `strict`(최신화 강제) | 아니오 | 병렬 세션에서 매번 재실행되는 것을 피한다 |
| `enforce_admins` | **예** | 모든 세션이 admin 계정으로 돌기 때문에, 끄면 보호가 무의미하다 |
| force push · 삭제 | 금지 | GitLab `allow_force_push=false`와 대칭 |

재적용:

```
gh api -X PUT repos/jeiel85/markleaf-android/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": { "strict": false, "contexts": ["build"] },
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
- GitLab Release 파일은 만료되는 job artifact가 아니라 Generic Package Registry에 보존한다.
- 워크플로는 저장소로 산출물을 되커밋하지 않는다.
