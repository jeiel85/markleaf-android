# GitHub ↔ GitLab 미러 운영 런북

markleaf-android 저장소의 재해 복구(이중 백업) 운영 문서.
설계 원본: `github-gitlab-mirror-final-v4` (v4.0.0)를 이 저장소의 실제 구성에 맞춰 반영.

## 현재 상태 (2026-07-13)

- GitHub와 GitLab 모두 활성 원격이며 같은 `main`과 릴리스 태그를 유지한다.
- GitLab은 공개 소스 미러이자 독립 서명 릴리스 채널이다.
- GitHub 태그는 GitHub Release와 F-Droid 자동 픽업의 기준이고, GitLab 태그는
  GitLab CI의 독립 빌드와 Generic Package Registry 기반 Release를 발동한다.

## 원격 구성

| 원격 | URL | 용도 |
|---|---|---|
| `origin` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 평상시 fetch/push — GitLab 우선 |
| `gitlab` | `git@gitlab.com:jeiel85/markleaf-android.git` (SSH) | 검증·명시적 대상 |
| `github` | `https://github.com/jeiel85/markleaf-android.git` | GitHub Actions·Release·F-Droid 기준 |

인증: GitLab = SSH 키(`~/.ssh/id_ed25519`, GitLab 계정에 등록됨). GitHub = HTTPS + Git Credential Manager.

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

## 원칙 (설계 준수)

- Push 순서: **GitLab 먼저 → GitHub 다음** (GitHub 장애가 백업을 막지 않도록)
- 자동 force push 금지
- GitHub Actions: 빌드/릴리스/F-Droid 태그 인계용으로 활성.
- GitLab CI: 브랜치·MR 검증과 보호된 `vX.Y.Z` 태그의 독립 서명 릴리스용으로 활성.
- 두 채널 모두 production 인증서 SHA-256(`0be97352…f91a`)을 검증한다.
- GitLab Release 파일은 만료되는 job artifact가 아니라 Generic Package Registry에 보존한다.
- 워크플로는 저장소로 산출물을 되커밋하지 않는다.
