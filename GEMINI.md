# Markleaf Project Instructions

이 문서는 Markleaf 프로젝트의 개발 원칙과 에이전트 작업 지침을 정의합니다.

## 📌 버전 관리 및 배포 지침

- **자동 버전 업 (Automatic Versioning):** 
  - 새로운 기능 추가(`feat`), 버그 수정(`fix`), 또는 릴리즈 준비 시 에이전트는 **반드시** `app/build.gradle.kts`의 `versionCode`를 이전보다 1 크게 증가시켜야 한다.
  - `versionName`은 [Semantic Versioning (SemVer)](https://semver.org/) 규칙에 따라 업데이트한다. (예: 1.0.1 -> 1.0.2 또는 1.1.0)
  - 이는 안드로이드 시스템의 업데이트 충돌(Conflict)을 방지하기 위한 필수 조치이다.

- **이력 관리 자동화:**
  - 버전 정보 수정 시 `CHANGELOG.md`에 변경 사항을 기록하고, `HISTORY.md`에 작업 내역을 상세히 남긴다.
  - 버전업이 포함된 커밋은 `release:` 또는 `fix: version up`과 같은 명확한 접두어를 사용한다.

## 🏗 아키텍처 가이드라인

- **Local-first:** 모든 데이터는 기본적으로 로컬 SQLite(Room)에 저장하며, 네트워크 권한 없이 동작함을 원칙으로 한다.
- **Markdown Centric:** 모든 문서는 표준 Markdown 문법을 따르며, 호환성을 최우선으로 한다.
