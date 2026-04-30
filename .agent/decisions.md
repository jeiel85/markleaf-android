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
