# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v1.0.7 - 안정성 및 MVP 스펙 보강 (Stability and MVP Spec Hardening) - 2026-05-01

### 수정
- 노트 저장 시 제목, 요약, 태그 인덱스가 함께 갱신되도록 수정했습니다.
- 태그와 노트의 관계 테이블이 실제 문자열 노트 ID를 사용하도록 정리했습니다.
- 검색, 태그, 휴지통, 설정 화면으로 이동할 수 있는 상단 액션을 추가했습니다.
- 휴대폰 레이아웃에서 에디터 route가 잘못 생성되던 문제를 수정했습니다.
- DB 스키마 변경 시 사용자 데이터를 삭제할 수 있는 destructive migration을 제거하고 v4에서 v5로 명시 migration을 추가했습니다.
- 위키 링크 저장 시 backlink 인덱스가 갱신되도록 보강했습니다.
- 태블릿 에디터 상단의 Preview/Edit 액션이 잘리는 문제를 줄였습니다.
- 설정 화면 버전 표시가 실제 앱 버전을 따르도록 수정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat connectedDebugAndroidTest`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.7` release APK 실행 확인

## v1.0.6 - 앱 시작 크래시 수정 (Startup Crash Fix) - 2026-05-01

### 수정
- 첫 화면에서 repository 인자가 필요한 ViewModel을 기본 factory로 생성하던 문제를 수정했습니다.
- 앱 시작 시 `NotesViewModel` 생성 실패로 즉시 종료될 수 있던 경로를 명시적인 `MarkleafViewModelFactory`로 교체했습니다.
- Notes, Search, Trash 화면이 동일한 repository 주입 경로를 사용하도록 정리했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.5 - 릴리즈 제목 규칙 보정 (Release Title Rule Fix) - 2026-04-30

### 수정
- GitHub Release 제목이 `v1.0.0 - 정식 출시 (First Major Release)`와 같은 changelog heading 형식을 따르도록 수정했습니다.
- 릴리즈 workflow가 changelog heading에서 끝의 날짜만 제거해 GitHub Release 제목으로 사용하도록 변경했습니다.
- `v1.0.2` 이후 기존 릴리즈 제목을 같은 형식으로 보정했습니다.
- 릴리즈 노트 본문은 한글로 작성하는 규칙을 명문화했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.4 - 릴리즈 노트 규칙 보정 (Release Notes Rule Fix) - 2026-04-30

### 수정
- GitHub 자동 생성 노트 대신 `CHANGELOG.md`의 해당 버전 섹션을 GitHub Release 본문으로 사용하도록 수정했습니다.
- 기존 `v1.0.2`, `v1.0.3` 릴리즈 노트를 문서화된 릴리즈 이력에 맞게 보정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.3 - 릴리즈 자산 규칙 보정 (Release Asset Rule Fix) - 2026-04-30

### 수정
- GitHub Release에는 signed release APK만 첨부되도록 수정했습니다.
- 릴리즈 APK 파일명을 `markleaf-vX.Y.Z.apk` 형식으로 정규화했습니다.
- 태그 릴리즈에서 debug APK를 중복 업로드하던 별도 workflow를 제거했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.2 - 릴리즈 서명 자동화 (Release Signing Automation) - 2026-04-30

### 변경
- GitHub 태그 릴리즈에서 release APK를 자동 서명하도록 구성했습니다.
- 커밋되지 않는 signing properties를 통해 로컬 release 서명을 선택적으로 사용할 수 있게 했습니다.
- 릴리즈 서명 및 GitHub Secrets 설정 문서를 추가했습니다.

## v1.0.0 - 정식 출시 (First Major Release) - 2026-04-30

### Added
- **정식 출시 (First Major Release)**
- **Tablet Two-Pane Layout**: 큰 화면에서 목록과 에디터를 동시에 볼 수 있는 최적화된 레이아웃 지원.
- **Backlinks**: 현재 노트를 참조하는 다른 노트들의 목록을 에디터에서 바로 확인 및 이동 가능.
- **Backup & Restore**: 모든 노트와 이미지 에셋을 포함한 ZIP 파일 백업 및 전체 복구 기능.
- **Material You**: Android 12 이상 기기에서 시스템 배경화면에 맞춘 다이내믹 컬러 테마 지원.
- **Search & Performance**: SQLite FTS4 기반의 초고속 전문 검색 엔진 완성.
- **Media Support**: 이미지 첨부 및 에디터 내 실시간 미리보기 기능 안정화.

### Changed
- 전반적인 UI/UX 폴리싱 및 간격 조정.
- 데이터베이스 스키마 최종 안정화 (v4).


### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
