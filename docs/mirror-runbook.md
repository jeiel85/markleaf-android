# GitHub ↔ GitLab 미러 운영 런북

markleaf-android 저장소의 재해 복구(이중 백업) 운영 문서.
설계 원본: `github-gitlab-mirror-final-v4` (v4.0.0)를 이 저장소의 실제 구성에 맞춰 반영.

## 현재 상태 (2026-07-01)

- **GitHub: flagged 상태로 로그인/접근 불가 → fetch·push·검증 전부에서 제외 중**
- **GitLab이 현재 유일한 원격 백업이자 작업 원격**
- 백업 완료: 로컬 전체 이력(브랜치 2, 태그 87 = 89 refs)이 GitLab에 미러링됨. 로컬 ↔ GitLab ref 대조 일치 확인.

## 원격 구성

| 원격 | URL | 용도 |
|---|---|---|
| `origin` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 평상시 fetch/push — 현재 GitLab |
| `gitlab` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 검증·명시적 대상 |
| `github` | `https://github.com/jeiel85/markleaf-android.git` | **비활성** — flagged 해제 후 복구용. 현재 어떤 작업에서도 접촉하지 않음. |

인증: GitLab = SSH 키(`~/.ssh/id_ed25519`, GitLab 계정에 등록됨). GitHub = 기존 HTTPS + Git Credential Manager(복구 후 사용).

## 평상시 작업 (현재: GitLab 전용)

```
git add .
git commit -m "..."
git push           # origin(GitLab)로 전송, 태그 자동 포함(push.followTags)
```

## 백업 검증

```
pwsh scripts/verify-mirror.ps1
```

기본은 로컬 ↔ GitLab 대조. GitHub 복구 후에는 `-IncludeGitHub`로 3자 대조.

## GitHub 복구 시 (flagged 해제 후)

1. GitHub 접근 복구 확인.
2. GitHub를 이중 push에 다시 편입 (GitLab 먼저 → GitHub 다음):
   ```
   git config --unset-all remote.origin.pushurl
   git remote set-url --add --push origin git@gitlab.com:jeiel85/markleaf-android.git
   git remote set-url --add --push origin https://github.com/jeiel85/markleaf-android.git
   ```
   (origin fetch는 GitLab 유지 권장. GitHub를 다시 기준으로 삼으려면
   `git remote set-url origin https://github.com/jeiel85/markleaf-android.git`로 fetch만 되돌림.)
3. 장애 기간 동안 로컬/GitLab이 GitHub보다 앞서 있으므로 fast-forward로 밀어넣기:
   ```
   git push github HEAD
   git push github --tags
   ```
   이력이 갈라졌으면 **자동 덮어쓰지 말고** 수동 확인.
4. `pwsh scripts/verify-mirror.ps1 -IncludeGitHub`로 3자 일치 확인.

## 원칙 (설계 준수)

- Push 순서: **GitLab 먼저 → GitHub 다음** (GitHub 장애가 백업을 막지 않도록)
- 자동 force push 금지
- GitHub Actions / 제3자 Actions 미사용
