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
