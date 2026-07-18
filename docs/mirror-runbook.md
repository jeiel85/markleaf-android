# GitHub ↔ GitLab 미러 운영 런북

markleaf-android 저장소의 재해 복구(이중 백업) 운영 문서.
설계 원본: `github-gitlab-mirror-final-v4` (v4.0.0)를 이 저장소의 실제 구성에 맞춰 반영.

## 현재 상태 (2026-07-13)

- GitHub와 GitLab 모두 활성 원격이며 같은 `main`과 릴리스 태그를 유지한다.
- GitLab은 공개 소스 미러이자 독립 서명 릴리스 채널이다.
- GitHub 태그는 GitHub Release와 F-Droid 자동 픽업의 기준이고, GitLab 태그는
  GitLab CI의 독립 빌드와 Generic Package Registry 기반 Release를 발동한다.
- 두 원격 모두 이 작업 환경에서 push 가능하지만, 제공자 간 자동 양방향 mirror는
  설정하지 않는다.

## 원격 구성

| 원격 | URL | 용도 |
|---|---|---|
| `origin` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 평상시 fetch/push — GitLab 우선 |
| `gitlab` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 검증·명시적 대상 |
| `github` | `https://github.com/jeiel85/markleaf-android.git` | GitHub Actions·Release·F-Droid 기준 |

인증: GitLab = SSH 키(`~/.ssh/id_ed25519`, GitLab 계정에 등록됨). GitHub = HTTPS + Git Credential Manager.

## 동기화 범위

- 로컬의 같은 커밋을 GitLab 먼저, GitHub 다음으로 push하는 것이 정식 동기화 경로다.
- GitHub 또는 GitLab 웹 UI에서 직접 만든 커밋은 다른 제공자로 자동 복사되지 않는다.
- 자동 양방향 mirror는 동시 편집 시 충돌·재전파 루프와 태그 릴리스 중복 실행 위험이
  있으므로 사용하지 않는다.
- 웹 UI 직접 수정이 꼭 필요하면 먼저 해당 원격을 fetch해 로컬에서 이력을 합친 뒤,
  두 원격에 같은 커밋을 순서대로 push한다.
- `scripts/verify-mirror.ps1 -IncludeGitHub`가 두 원격의 ref 일치를 판정하는 최종 게이트다.

## 평상시 작업

```
git add .
git commit -m "..."
git push gitlab main
git push github main
```

릴리스 태그도 GitLab 먼저, GitHub 다음으로 보낸다:

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

1. 동작하는 원격에 먼저 push하고 실패 원격의 오류를 기록한다.
2. 장애 복구 후 앞선 원격과 로컬의 ref를 대조한다.
3. fast-forward가 확인된 경우에만 뒤처진 원격에 밀어넣는다:
   ```
   git push <recovered-remote> main
   git push <recovered-remote> --tags
   ```
4. 이력이 갈라졌으면 자동 덮어쓰지 않고 수동 확인한다.
5. `pwsh scripts/verify-mirror.ps1 -IncludeGitHub`로 3자 일치를 확인한다.

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
   git revert <bad-sha>
   git push gitlab main
   git push github main
   ```
3. **이미 한쪽을 재작성해 갈라졌다면** 재작성한 쪽을 되돌리는 대신 두 이력을
   로컬에서 merge해 양쪽이 받을 수 있는 공통 커밋을 만든다. v2.24.0은 이 경로로
   해결했다 (merge `4b2dfd9`).
   ```
   git checkout main
   git merge gitlab/main          # 또는 github/main — 뒤처진 쪽을 합친다
   # 충돌은 릴리스에 나가야 할 내용 기준으로 해소한다
   git push gitlab main
   git push github main
   ```
4. merge로도 해결이 안 되는 경우(예: 유출된 시크릿처럼 이력에서 반드시 제거해야
   하는 내용)에만 보호를 임시로 내린다. 이때는 순서를 지킨다.
   1. GitLab → Settings → Repository → Protected branches에서 `main`의
      **Allowed to force push**를 켠다.
   2. `git push gitlab main --force-with-lease` (`--force`가 아니라
      `--force-with-lease` — 다른 세션의 push를 덮어쓰지 않는다).
   3. **즉시 보호를 원상 복구한다.** 이 단계를 잊으면 보호되지 않은 `main`이
      남는다.
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

## 원칙 (설계 준수)

- Push 순서: **GitLab 먼저 → GitHub 다음** (GitHub 장애가 백업을 막지 않도록)
- 자동 force push 금지
- GitHub Actions: 빌드/릴리스/F-Droid 태그 인계용으로 활성.
- GitLab CI: 브랜치·MR 검증과 보호된 `vX.Y.Z` 태그의 독립 서명 릴리스용으로 활성.
- 두 채널 모두 production 인증서 SHA-256(`0be97352…f91a`)을 검증한다.
- GitLab Release 파일은 만료되는 job artifact가 아니라 Generic Package Registry에 보존한다.
- 워크플로는 저장소로 산출물을 되커밋하지 않는다.
