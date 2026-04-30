# Markleaf Decisions

이 파일은 프로젝트의 중요한 결정 사항을 기록합니다.  
에이전트는 구현 중 새로운 구조적 결정이 생기면 이 파일에 추가합니다.

---

## Confirmed Decisions

### D001 - Repository

Final repository:

```text
https://github.com/jeiel85/markleaf-android.git
```

### D002 - Application ID

Final Android application ID:

```text
com.markleaf.notes
```

Reason:

- `io.github.*`는 샘플 프로젝트처럼 보일 수 있다.
- `dev.*`는 개발용 빌드처럼 보일 수 있다.
- `com.markleaf.notes`는 운영 배포 앱에 적합하고 앱의 성격을 명확히 보여준다.

### D003 - Distribution Direction

Markleaf는 향후 F-Droid 배포가 가능할 정도의 오픈소스 친화성을 유지한다.

Implications:

- 공개 소스만으로 빌드 가능해야 한다.
- closed SDK를 피한다.
- private token이나 secret file 없이 빌드 가능해야 한다.
- 의존성 라이선스를 명확히 관리한다.

### D004 - MVP No-API Policy

MVP에서는 API 연동을 하지 않는다.

금지:

- 로그인
- 계정 시스템
- 클라우드 동기화
- 분석
- 광고
- 추적
- Firebase
- remote config
- proprietary crash reporting SDK
- backend API client
- `android.permission.INTERNET`

### D005 - Product Focus

Markleaf는 기능 많은 생산성 도구가 아니라 빠르고 예쁜 Markdown 메모 앱이다.

Priority:

1. 속도
2. 안정성
3. 디자인
4. 로컬 저장
5. 데이터 소유권
6. 태그 기반 정리

### D006 - Release Signing Secrets

릴리즈 키스토어와 서명 비밀번호는 저장소에 커밋하지 않는다.

Implications:

- 로컬 서명은 `release-signing.properties` 또는 환경변수로만 활성화한다.
- GitHub Actions는 `MARKLEAF_RELEASE_KEYSTORE_BASE64` 시크릿을 복원해 태그 릴리즈에서만 signed APK를 생성한다.
- 일반 PR/main 빌드는 debug 빌드와 테스트만 수행해 공개 소스 빌드 가능성을 유지한다.

---

## Pending Decisions

- Hilt vs Koin vs manual dependency wiring
- Minimum SDK final value
- Markdown preview library 사용 여부
- SQLite FTS 도입 시점
- 이미지 첨부 지원 여부
- tablet two-pane layout 도입 시점
- Play Store와 F-Droid flavor 분리 필요 여부

### D006 - Documentation Baseline Integration

템플릿 기반 운영 문서를 프로젝트 루트에 통합한다.

Implications:

- `CHANGELOG.md`를 사용자 영향 변경의 공식 요약 문서로 사용
- `HISTORY.md`를 작업/검증 이력 문서로 사용
- 기존 Markleaf 전용 정책(`AGENTS.md`, `docs/AGENT_SPEC.md`)을 우선 적용

### D007 - CI APK Verification Policy

CI 성공 판정에 APK 산출물 존재 및 다운로드 가능 여부를 포함한다.

Implications:

- `assembleDebug` 이후 `app-debug.apk` 파일 존재를 워크플로우에서 강제 검증
- Actions artifact 업로드 실패 시 CI 실패 처리
- 릴리즈 전 artifact 다운로드 가능 여부를 확인 가능하게 유지

### D008 - Actions Runtime Baseline

GitHub Actions 워크플로우는 Node 24 지원 버전을 기본으로 유지한다.

Implications:

- 공식 액션은 Node 24 지원 메이저를 우선 사용
- 전환기 동안 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`를 설정해 런타임 경고를 선제 대응

### D009 - Tablet UX Priority and Adaptive Two-Pane Direction

태블릿 경험을 Later Versions의 최우선(P0)으로 지정한다.

Implications:

- `Medium/Expanded` 폭에서 리스트+에디터 2-pane 레이아웃을 우선 도입
- `Compact`는 기존 phone 네비게이션 UX를 유지
- 초기 도입은 최소 침습(기존 route 구조 보존) 방식으로 진행

### D010 - Basic Local Markdown Preview Adoption

Editor에 외부 네트워크/SDK 없이 동작하는 기본 Markdown Preview 모드를 도입한다.

Implications:

- 초기 범위는 heading, bullet, checkbox 중심의 lightweight preview
- 복잡한 markdown spec 전체 지원은 후속 이슈로 확장
- 로컬 우선/F-Droid 친화 원칙 유지

### D011 - SQLite FTS Phased Adoption

검색 품질/성능 향상을 위해 FTS를 단계적으로 도입한다.

Implications:

- 기존 LIKE 경로를 즉시 제거하지 않고 fallback 유지
- 스키마 변경은 additive migration으로 진행
- 실제 데이터셋 성능 측정 후 기본 경로 전환

### D012 - Local-First Image Attachment Scope

이미지 첨부는 로컬 저장 기반의 최소 범위로 도입한다.

Implications:

- 원격 업로드/동기화 없이 메타데이터+로컬 URI 참조 모델 사용
- 내보내기 시 markdown과 자산 파일 구조를 함께 정의
- 고급 미디어 편집은 후속 단계로 연기

### D013 - Wiki Links with ID-backed Resolution

`[[note links]]`는 표시 텍스트와 내부 식별자를 분리한 ID 기반으로 도입한다.

Implications:

- 제목 변경 시 링크 무결성 유지 가능
- 저장 시 링크 인덱스 재생성 필요
- 미해결/중복 제목 케이스에 대한 UX 제공 필요

### D014 - Manual Local Backup Strategy

백업은 사용자 주도형 로컬 백업/복원으로 시작한다.

Implications:

- 자동 원격 전송 없이 zip 패키지 기반 백업 제공
- 복원 전 dry-run/conflict preview 필요
- 포맷 버전 관리로 향후 호환성 유지

### D015 - Network Features Deferred

현재 제품 단계에서는 네트워크 기능 도입이 필요하지 않다.

Implications:

- `android.permission.INTERNET` 미사용 정책 유지
- 계정/원격 동기화/API 연동은 후속 재평가 전까지 보류
- 도입 재검토 시 개인정보/운영/배포 가드레일 선충족 필요

### D016 - Release Asset Policy

태그 릴리즈(`v*`) 시 signed release APK만 GitHub Release 자산에 자동 첨부한다.

Implications:

- Debug APK는 CI artifact로만 보관하고 GitHub Release 자산에는 올리지 않는다.
- Release page에서는 `markleaf-vX.Y.Z.apk` 파일명으로 signed APK를 직접 다운로드 가능하게 유지한다.

### D017 - Release Notes Source

GitHub Release 본문은 해당 버전의 `CHANGELOG.md` 섹션을 기준으로 작성한다.

Implications:

- 태그 릴리즈 workflow는 `--generate-notes`를 사용하지 않는다.
- `vX.Y.Z` 태그를 만들기 전에 `CHANGELOG.md`에 `## vX.Y.Z` 섹션이 있어야 한다.
- 릴리즈 본문이 비어 있거나 changelog 섹션을 찾지 못하면 workflow가 실패해야 한다.
- GitHub Release 본문에 들어가는 changelog 섹션은 한글로 작성한다.

### D018 - Release Title Source

GitHub Release 제목은 `CHANGELOG.md`의 해당 버전 heading에서 가져온다.

Format:

```text
## vX.Y.Z - 한국어 제목 (English Title) - YYYY-MM-DD
```

Implications:

- GitHub Release 제목은 trailing date를 제거한 `vX.Y.Z - 한국어 제목 (English Title)` 형식을 사용한다.
- Workflow에서 `Markleaf vX.Y.Z` 같은 고정 title을 사용하지 않는다.
- `v1.0.0 - 정식 출시 (First Major Release)` 형식을 이후 릴리즈에도 유지한다.
