# CHANGELOG

All notable changes to Markleaf are documented in this file.

## v1.0.16 - 노트 목록/편집기 빈 상태 개선 (Improve Empty States) - 2026-05-02

### 추가
- 노트 목록 빈 상태에 📝 아이콘과 안내 문구 개선
- 편집기 빈 상태에 ✏️ 아이콘과 안내 문구 추가
- 빈 상태 레이아웃에 중앙 정렬 및 여백 개선

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.15 - 태블릿 2패널 시각 구분 개선 (Tablet Two-Pane Visual Polish) - 2026-05-01

### 개선
- 태블릿 expanded 화면에서 노트 목록 pane과 에디터 pane의 배경 톤을 분리했습니다.
- 두 pane 사이에 얇은 divider를 추가해 편집 상태에서도 영역 경계가 더 명확하게 보이도록 했습니다.
- 선택된 노트 row에 subtle highlight를 추가했습니다.
- 접힌 목록 rail에도 별도 표면 톤을 적용했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.14 - 한국어 다국어 지원 (Korean Localization) - 2026-05-01

### 추가
- Android string resource 기반 다국어 구조를 추가했습니다.
- 기본 언어는 영어로 유지하고, 한국어 기기에서는 한국어 UI 문구가 표시되도록 `values-ko` 리소스를 추가했습니다.
- 첫 실행 샘플 노트도 영어 기본/한국어 로케일별 본문으로 분리했습니다.

### 개선
- 주요 화면의 제목, 버튼, 빈 상태, 설정 설명, 접근성 문구를 리소스 기반으로 전환했습니다.
- 설정의 line width 선택지도 한국어 환경에서 한국어로 표시되도록 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.13 - 라이브 Markdown 하이라이팅 (Live Markdown Highlighting) - 2026-05-01

### 추가
- Edit 화면에서 Markdown 원문을 유지한 채 heading, bold, italic, link, checkbox 문법을 실시간으로 하이라이팅합니다.
- Preview 토글은 그대로 유지하고, 편집 중인 원문 저장 구조는 변경하지 않았습니다.

### 개선
- 설정의 Markdown syntax 표시 옵션이 Edit 화면 하이라이팅에 반영되도록 연결했습니다.
- 하이라이팅은 문자 변환 없이 동일 offset을 유지해 커서와 선택 흐름이 깨지지 않도록 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.13` release APK 설치 및 실행 확인

## v1.0.12 - 설정 옵션 기반 추가 (Settings Foundation) - 2026-05-01

### 추가
- DataStore Preferences 기반 앱 설정 저장 구조를 추가했습니다.
- 설정 화면에 Markdown syntax 표시/숨김 옵션을 추가했습니다.
- 설정 화면에 Line width 옵션을 추가했습니다: Narrow, Comfortable, Wide.

### 개선
- 태블릿 에디터 최대 폭이 Line width 설정값을 따르도록 연결했습니다.
- 기본 line width는 Comfortable 800dp로 유지합니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.12` release APK 설치 및 실행 확인

## v1.0.11 - 태블릿 노트 목록 접기 (Collapsible Tablet Note List) - 2026-05-01

### 추가
- 태블릿 two-pane 화면에서 왼쪽 노트 목록을 접고 펼칠 수 있는 버튼을 추가했습니다.
- 목록을 접은 상태에서도 다시 펼칠 수 있는 좁은 rail 버튼을 제공합니다.

### 개선
- 목록 접힘 상태에서도 선택된 노트를 유지합니다.
- 넓은 화면에서 에디터가 과도하게 넓어지지 않도록 본문 영역을 최대 800dp 폭으로 중앙 정렬했습니다.
- 폰 화면의 단일 pane 흐름은 변경하지 않았습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.11` release APK 설치 및 실행 확인

## v1.0.10 - Markdown 편집 툴바 추가 (Markdown Editing Toolbar) - 2026-05-01

### 추가
- Edit 화면 하단에 Markdown 편집 툴바를 추가했습니다.
- Bold, Italic, Checkbox, Markdown Link, Wiki Link, Image 삽입 액션을 제공합니다.
- 선택 영역이 있으면 해당 텍스트를 Markdown 문법으로 감싸고, 선택 영역이 없으면 기본 placeholder를 삽입합니다.

### 개선
- 에디터 입력 상태를 선택 영역까지 추적하도록 `TextFieldValue` 기반으로 전환했습니다.
- 이미지 삽입 액션을 편집 툴바로 통합했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.10` release APK 설치 및 실행 확인

## v1.0.9 - Markdown 링크 및 설정 화면 개선 (Markdown Links and Settings Polish) - 2026-05-01

### 수정
- Preview 모드에서 문장 중간의 `[[노트 링크]]`를 링크처럼 표시하도록 개선했습니다.
- 일반 Markdown 링크 `[label](target)`도 Preview 모드에서 링크처럼 표시하도록 개선했습니다.
- 로컬 노트 링크 대상은 기존 검색 흐름으로 연결하고, 외부 URL은 MVP 개인정보/네트워크 원칙에 따라 자동으로 열지 않도록 했습니다.

### 개선
- 설정 화면에 상단 뒤로가기 버튼을 추가했습니다.
- 설정 화면을 데이터 관리, Markdown 안내, 개인정보, 앱 정보 섹션으로 재구성했습니다.
- 백업/복원 실행 후 간단한 결과 메시지를 표시합니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.9` release APK 설치 및 실행 확인

## v1.0.8 - 첫 실행 샘플 노트 온보딩 (Starter Notes Onboarding) - 2026-05-01

### 추가
- 새 설치에서 Markleaf의 핵심 기능을 바로 이해할 수 있도록 샘플 노트 4개를 자동 생성합니다.
- 샘플 노트는 Markdown 작성, 태그, 위키 링크, 백업/내보내기, 로컬 우선 개인정보 원칙을 안내합니다.
- 사용자가 샘플 노트를 삭제한 뒤 앱을 다시 실행해도 자동으로 다시 생성되지 않도록 재시드 방지 플래그를 추가했습니다.

### 문서
- Bear급 Android Markdown 경험을 목표로 하는 갭 리뷰와 Phase 9 제품 개선 계획을 추가했습니다.
- 기능적 유사성은 허용하되, 이름/아이콘/색상/화면 구성/문구/브랜딩은 복제하지 않는 기준을 명확히 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.8` release APK 설치 및 실행 확인

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
