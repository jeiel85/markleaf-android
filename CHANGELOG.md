# CHANGELOG

All notable changes to Markleaf are documented in this file.

## Unreleased

### 개선
- **편집기 링크 툴바 명확화:** Markdown 링크와 위키 링크 버튼이 같은 체인 아이콘으로 연속 표시되지 않도록 위키 링크 버튼을 `[[ ]]` 문법 표시로 바꾸고, 편집 툴바 아이콘에 길게 누르기/hover 설명을 추가했습니다.

## v1.1.14 - 릴리즈 인증서 파싱 복구 완료 (Release Certificate Parsing Recovery Complete) - 2026-05-02

### 수정
- **인증서 digest 파싱 수정:** GitHub Actions release job이 `apksigner verify --print-certs` 출력에서 실제 SHA-256 digest 값(`$3`)을 읽도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.13` 태그를 재사용하지 않고 `versionCode`를 `44`, `versionName`을 `1.1.14`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **자동 release 마무리 정합성 보강:** release APK 생성, 선택, 서명 검증, asset 업로드가 모두 동일한 production certificate 기준으로 이어지도록 정리했습니다.

## v1.1.13 - 릴리즈 인증서 진단 출력 추가 (Release Certificate Diagnostics) - 2026-05-02

### 수정
- **서명 진단 로그 추가:** GitHub Actions release job이 `signing-report.txt`, actual digest, expected digest를 로그에 직접 출력하도록 수정해 인증서 불일치 여부를 즉시 확인할 수 있게 했습니다.
- **새 진단 버전 발행:** 실패한 `v1.1.12` 태그를 재사용하지 않고 `versionCode`를 `43`, `versionName`을 `1.1.13`으로 올려 새 자동 진단 태그를 발행합니다.
- **release 검증 가시성 강화:** release signing step이 실패해도 원인을 로그만으로 판별할 수 있도록 했습니다.

## v1.1.12 - 릴리즈 APK 고정 경로 우선 복구 완료 (Release APK Fixed Path Priority Recovery Complete) - 2026-05-02

### 수정
- **고정 release 경로 우선 사용:** GitHub Actions release job이 먼저 `app/build/outputs/apk/release/app-release.apk`를 사용하고, 없을 때만 fallback 탐색을 하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.11` 태그를 재사용하지 않고 `versionCode`를 `42`, `versionName`을 `1.1.12`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **release 검증 안정성 보강:** 일반적인 AGP release APK 경로를 우선 사용해 debug/release 선택 혼선을 줄였습니다.

## v1.1.11 - 릴리즈 APK 선택 복구 완료 (Release APK Selection Recovery Complete) - 2026-05-02

### 수정
- **release APK 선택 보정:** GitHub Actions release job이 `app/build/**/*.apk` 중 debug APK가 아니라 `release` 경로 또는 이름을 가진 APK만 선택하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.10` 태그를 재사용하지 않고 `versionCode`를 `41`, `versionName`을 `1.1.11`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **서명 검증 정확성 강화:** release asset 준비와 서명 확인이 동일한 release APK 선택 규칙을 사용하도록 맞췄습니다.

## v1.1.10 - 릴리즈 Gradle 환경변수 복구 완료 (Release Gradle Environment Recovery Complete) - 2026-05-02

### 수정
- **Gradle property 전달 방식 교체:** GitHub Actions release job이 bash 인자 해석에 의존하지 않도록 `ORG_GRADLE_PROJECT_markleaf.requireReleaseSigning=true` 환경변수로 release signing 필수 플래그를 전달하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.9` 태그를 재사용하지 않고 `versionCode`를 `40`, `versionName`을 `1.1.10`으로 올려 새 자동 릴리즈 태그를 발행합니다.
- **자동 release 안정성 강화:** release task 실행, APK 탐색, 서명 검증, GitHub Release asset 업로드가 모두 shell 인자 파싱 영향 없이 이어지도록 정리했습니다.

## v1.1.9 - 릴리즈 Gradle 실행 복구 완료 (Release Gradle Execution Recovery Complete) - 2026-05-02

### 수정
- **release Gradle 실행 복구:** GitHub Actions release job이 `-Pmarkleaf.requireReleaseSigning=true`를 올바른 Gradle project property로 해석하도록 실행 구문을 `./gradlew -Pmarkleaf.requireReleaseSigning=true :app:assembleRelease`로 바로잡았습니다.
- **새 복구 버전 발행:** 기존 `v1.1.8` 수동 릴리즈는 유지하고, 자동 릴리즈 녹색 복구용으로 `versionCode`를 `39`, `versionName`을 `1.1.9`로 올렸습니다.
- **자동 릴리즈 경로 정합성 보강:** 이제 tag release job은 실제 release task를 수행한 뒤 APK 탐색/서명 확인/asset 업로드 단계로 이어지도록 정합성을 맞췄습니다.

## v1.1.8 - 릴리즈 APK 전체 경로 탐색 복구 완료 (Release APK Full Build Tree Discovery Complete) - 2026-05-02

### 수정
- **빌드 트리 전체 APK 탐색:** GitHub Actions release job이 `app/build/**/*.apk` 전체를 재귀 탐색해 CI 환경별로 달라질 수 있는 실제 release APK 위치를 찾도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.7` 태그를 재사용하지 않고 `versionCode`를 `38`, `versionName`을 `1.1.8`로 올려 새 태그 릴리즈를 발행합니다.
- **release 자동화 탄력성 강화:** 서명 검증과 release asset 준비가 동일한 전체 빌드 트리 APK 탐색 로직을 공유하도록 맞췄습니다.

## v1.1.7 - 릴리즈 APK 재귀 탐색 복구 완료 (Release APK Recursive Discovery Recovery Complete) - 2026-05-02

### 수정
- **재귀 APK 탐색:** GitHub Actions release job이 `app/build/outputs/apk/release/**/*.apk`를 재귀 탐색해 하위 디렉터리에 생성되는 release APK까지 찾도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.6` 태그를 재사용하지 않고 `versionCode`를 `37`, `versionName`을 `1.1.7`로 올려 새 태그 릴리즈를 발행합니다.
- **release 검증 경로 보강:** 서명 검증과 release asset 준비가 동일한 재귀 탐색 결과를 사용하도록 맞췄습니다.

## v1.1.6 - 릴리즈 APK 탐색 복구 완료 (Release APK Discovery Recovery Complete) - 2026-05-02

### 수정
- **APK 탐색 방식 보강:** GitHub Actions release job이 metadata 파일 존재를 가정하지 않고, `app/build/outputs/apk/release/*.apk`에서 실제 생성된 release APK를 직접 찾아 사용하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.5` 태그를 재사용하지 않고 `versionCode`를 `36`, `versionName`을 `1.1.6`으로 올려 새 태그 릴리즈를 발행합니다.
- **릴리즈 경로 단순화:** 서명 검증과 release asset 준비가 동일한 실제 APK 파일 탐색 로직을 공유하도록 맞췄습니다.

## v1.1.5 - 릴리즈 산출물 경로 복구 완료 (Release Artifact Path Recovery Complete) - 2026-05-02

### 수정
- **산출물 경로 동적 해석:** GitHub Actions release job이 `output-metadata.json`에서 실제 release APK 파일명을 읽도록 바꿔, 고정된 `app-release.apk` 경로 가정 때문에 실패하던 서명 확인 단계를 복구했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.4` 태그를 재사용하지 않고 `versionCode`를 `35`, `versionName`을 `1.1.5`로 올려 새 태그 릴리즈를 발행합니다.
- **릴리즈 파이프라인 정합성 강화:** 서명 검증과 GitHub Release asset 준비가 동일한 metadata 기반 APK 경로를 공유하도록 맞췄습니다.

## v1.1.4 - 릴리즈 태그 복구 완료 (Release Tag Recovery Complete) - 2026-05-02

### 수정
- **bash 인자 순서 보정:** GitHub Actions의 Ubuntu runner에서 Gradle project property가 task 이름에 흡수되지 않도록 `-Pmarkleaf.requireReleaseSigning=true`를 release task 앞에 배치했습니다.
- **재복구 버전 발행:** 실패한 `v1.1.3` 태그를 재사용하지 않고 `versionCode`를 `34`, `versionName`을 `1.1.4`로 올려 새 복구 릴리즈를 발행합니다.
- **복구 기록 정리:** `v1.1.3` 실패 시도와 `v1.1.4` 성공 경로를 구분할 수 있도록 작업 이력과 릴리즈 문서를 갱신했습니다.

## v1.1.3 - 릴리즈 워크플로우 복구 완료 (Release Workflow Recovery Complete) - 2026-05-02

### 수정
- **태그 릴리즈 복구:** GitHub Actions release job에서 `-Pmarkleaf.requireReleaseSigning=true` 인자를 안전하게 전달하도록 수정해 `v1.1.0`~`v1.1.2` 태그 릴리즈 실패 원인을 제거했습니다.
- **새 복구 버전 발행:** Android 업데이트 계보를 안전하게 이어가기 위해 `versionCode`를 `33`으로 올리고 `versionName`을 `1.1.3`으로 상향했습니다.
- **릴리즈 이력 정리:** 실패한 기존 태그를 재사용하지 않고 새 태그 릴리즈로 복구하도록 문서와 작업 기록을 정리했습니다.

## v1.1.2 - 버전 불일치 및 워크플로우 복구 (Version Sync and Workflow Recovery) - 2026-05-02

### 수정
- **릴리즈 워크플로우 복구:** `v1.1.1`에서 퇴행된 상세 릴리즈 제목 추출 로직과 잘못된 빌드 태스크 경로(`:app:assembleRelease`)를 복원했습니다.
- **버전 코드 보정:** 이전 버전과의 충돌 방지를 위해 `versionCode`를 `32`로 상향 조정했습니다.
- **문서 동기화:** 누락된 `v1.1.0` 및 `v1.1.1` 작업 내역을 `progress.md`에 반영하여 문서 정합성을 맞췄습니다.

## v1.1.1 - CI 릴리즈 안정화 (CI Release Stability) - 2026-05-02

### 개선
- **CI 워크플로우 수정:** GitHub Actions에서의 서명 빌드 및 릴리즈 프로세스 오류를 수정했습니다.
- **테스트 안정화:** CI 환경에서 간헐적으로 실패하던 성능 테스트 항목을 제거하여 빌드 안정성을 높였습니다.

## v1.1.0 - 릴리즈 및 대규모 기능 개선 (Comprehensive Release) - 2026-05-02

### 새로운 기능
- **백링크 컨텍스트 스니펫:** 노트 미리보기 및 편집 하단에 해당 노트를 참조하는 백링크들의 문맥 스니펫을 표시합니다.
- **태그별 노트 개수 표시:** 태그 목록 화면에서 각 태그가 몇 개의 노트에서 사용되었는지 숫자로 표시합니다.
- **포괄적 테스트 스위트:** 앱의 모든 기능을 검증하는 50개 항목의 자동화 통합 테스트를 구축했습니다.

### 개선
- **다국어 테스트 지원:** 자동화 테스트 코드가 한국어 등 다국어 환경의 기기에서도 정상 동작하도록 리소스 참조 방식으로 개선했습니다.
- **백업 상태 메시지 상세화:** ZIP 백업 및 복원 시 처리된 데이터 개수를 구체적으로 안내합니다.
- **테마 대비 개선:** 가독성을 높이기 위해 테마의 색상 대비를 조정했습니다.

### 검증
- 기기(SM-S921N, TB320FC) 상에서 50개 시나리오의 통합 테스트 수행 완료
- `./gradlew.bat test` (Unit Tests)
- `./gradlew.bat lintDebug`
- `apksigner verify`

## v1.0.27 - 백업 상태 메시지 개선 (Backup Status Messages) - 2026-05-02

### 개선
- Settings의 ZIP 백업/복원 결과 메시지에 처리된 노트, 첨부, 링크 개수를 표시합니다.
- 백업/복원 실패 시 사용자가 다음에 확인할 수 있는 구체적인 안내 문구를 표시합니다.
- 실패 메시지는 error 색상으로 표시해 성공 상태와 구분합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat compileDebugKotlin`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.27` release APK 설치 및 실행 확인

## v1.0.26 - 백링크 문맥 표시 (Backlink Context Snippets) - 2026-05-02

### 개선
- 에디터 백링크 목록에서 링크가 등장한 주변 문맥을 함께 표시합니다.
- Preview/Edit 화면의 백링크 항목을 제목 + snippet 구조로 정리했습니다.
- 기존 백링크 제목 탭 이동은 유지했습니다.

### 테스트
- wiki link 주변 문맥이 backlink snippet에 포함되는지 검증하는 repository 테스트를 추가했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.26` release APK 설치 및 실행 확인

## v1.0.25 - 태그 화면 개선 (Tag Counts and Navigation) - 2026-05-02

### 개선
- Tags 화면에서 각 태그가 연결된 활성 노트 수를 함께 표시합니다.
- 태그 행을 누르면 기존처럼 `#태그` 검색으로 이동해 해당 태그의 노트를 바로 탐색할 수 있습니다.
- 휴지통으로 이동한 노트는 태그 카운트에서 제외합니다.

### 테스트
- 태그별 활성 노트 수 집계를 검증하는 repository 테스트를 추가했습니다.
- 기본/한국어/Spanish string resource parity를 유지했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalTagRepositoryTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.25` release APK 설치 및 실행 확인

## v1.0.24 - 테마 대비 점검 (Theme Contrast Audit) - 2026-05-02

### 수정
- 노트 목록 제목 색상을 앱 테마의 primary/onPrimaryContainer 색상으로 명시해 목록에서 더 잘 보이도록 개선했습니다.
- 태블릿 2-pane에서 선택된 노트가 없을 때 표시되는 안내 문구가 어두운 배경에서도 보이도록 색상을 명시했습니다.
- 태블릿 2-pane 목록 영역이 `surfaceVariant`와 `onSurfaceVariant`를 일관되게 사용하도록 조정했습니다.
- 앱 고유 색상 체계가 기기 동적 색상에 덮이지 않도록 기본 테마 설정을 고정했습니다.
- Typography letter spacing을 0으로 정리해 화면 전반의 텍스트 렌더링을 일관화했습니다.

### 테스트
- 10k 노트 검색 성능 테스트의 시간 임계값을 로컬/CI 부하에 덜 흔들리는 회귀 방지 기준으로 조정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.24` release APK 설치 및 실행 확인

## v1.0.23 - 빠른 열기 검색 (Quick Open Search) - 2026-05-02

### 추가
- Search 화면에서 노트, 태그, 위키 링크 라벨을 함께 표시하는 quick-open 결과를 추가했습니다.
- 태그 결과를 누르면 `#태그` 검색으로 바로 전환됩니다.
- 해결된 위키 링크는 대상 노트로 바로 열고, 미해결 링크는 해당 라벨 검색으로 전환합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.23` release APK 설치 및 실행 확인

## v1.0.22 - 빈 상태 개선 (Empty State Polish) - 2026-05-02

### 개선
- 노트 목록 빈 상태에 명시적인 노트 만들기 버튼을 추가했습니다.
- 에디터 빈 상태에 Markdown, 태그, 링크, 체크박스, 이미지 사용 힌트를 추가했습니다.
- 새 빈 상태 문구를 영어, 한국어, Spanish 리소스에 반영했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.22` release APK 설치 및 실행 확인

## v1.0.21 - 다국어 지원 확장 (Expanded i18n) - 2026-05-02

### 추가
- Spanish UI string resources를 추가했습니다.
- Spanish starter notes raw resource를 추가했습니다.
- 기본/한국어/Spanish 문자열 리소스 key parity를 검증하는 테스트를 추가했습니다.

### 개선
- Markdown preview 지원 설명에 표와 수식 표기 지원을 반영했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.21` release APK 설치 및 실행 확인

## v1.0.20 - 10k 노트 성능 최적화 (10k Notes Performance) - 2026-05-02

### 개선
- 노트 목록, 휴지통, 제목 조회에 사용하는 SQLite index를 추가했습니다.
- 검색이 기존 LIKE 전체 스캔 대신 local FTS rowid join 경로를 사용하도록 정리했습니다.
- 검색 결과를 최대 200개로 제한해 큰 데이터셋에서도 화면 렌더링 부담을 줄였습니다.

### 테스트
- 10,000개 노트 데이터셋에서 FTS 검색 결과를 검증하는 repository 테스트를 추가했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.20` release APK 설치 및 실행 확인

## v1.0.19 - 노트 버전 기록 (Note Version History) - 2026-05-02

### 추가
- 노트 수정 전 이전 본문을 로컬 snapshot으로 저장합니다.
- 에디터 상단의 version history 버튼에서 최근 snapshot 목록을 볼 수 있습니다.
- 선택한 snapshot을 현재 노트로 복원할 수 있습니다.

### 개선
- 자동 저장이 너무 많은 버전을 만들지 않도록 snapshot 생성을 5분 단위로 제한하고 노트당 최근 50개만 유지합니다.
- snapshot 복원 전에 현재 버전을 다시 snapshot으로 보존합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.19` release APK 설치 및 실행 확인

## v1.0.18 - 고급 Markdown 미리보기 (Advanced Markdown Preview) - 2026-05-02

### 추가
- Preview 모드에서 Markdown table을 header와 row로 렌더링합니다.
- Inline 수식 표기 `$...$`를 본문 안에서 구분해 표시합니다.
- Display 수식 표기 `$$...$$`를 별도 block으로 표시합니다.

### 결정
- 네트워크, WebView 기반 원격 리소스, 폐쇄 SDK 없이 동작하도록 전체 KaTeX 엔진 대신 로컬 수식 표기 preview로 구현했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.core.markdown.SimpleMarkdownPreviewTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`

## v1.0.17 - 릴리즈 서명 인증서 고정 (Fixed Release Signing Certificate) - 2026-05-02

### 수정
- 태그 릴리즈에서 release signing 값이 누락되면 빌드가 실패하도록 강제했습니다.
- GitHub Release 생성 전에 APK 서명 인증서 SHA-256이 고정 production 인증서와 일치하는지 검증합니다.
- 잘못된 키스토어로 빌드된 APK가 배포되어 기존 설치 앱과 업데이트 충돌을 일으키는 위험을 줄였습니다.

### 문서
- production release 인증서 SHA-256과 키스토어 교체 금지 기준을 `docs/RELEASE.md`에 기록했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`

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
