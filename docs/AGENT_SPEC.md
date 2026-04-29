# Markleaf Android - 에이전트 코딩 설계서

> 코드명: **Markleaf**  
> 대상: Android 네이티브 Markdown 메모 앱  
> 포지셔닝: 가볍고, 로컬 우선이며, 태그 기반으로 정리하는 Android 글쓰기 앱  
> GitHub Repository: `https://github.com/jeiel85/markleaf-android.git`  
> Android Application ID / Package Name: `com.markleaf.notes`

---

## 0. 프로젝트 한 줄 정의

**Markleaf는 빠르게 쓰고, 태그로 정리하고, 즉시 검색하며, Markdown 파일로 안전하게 보관할 수 있는 Android용 로컬 우선 메모 앱이다.**

---

## 1. 핵심 방향

Markleaf는 Bear 같은 현대적인 Markdown 메모 앱의 장점에서 영감을 받지만, 특정 앱을 복제하지 않는다.

이 프로젝트의 목표는 기존 앱의 이름, 아이콘, 로고, 색상, 화면 구성, 문구, 브랜드 정체성을 따라 하는 것이 아니다.  
핵심은 다음과 같은 일반적인 제품 경험을 Android 환경에 맞게 독립적으로 구현하는 것이다.

- 빠르게 열리는 메모 앱
- Markdown 기반 글쓰기
- 태그 중심 정리
- 로컬 우선 저장
- 데이터 소유권 보장
- 단순하고 아름다운 UI
- F-Droid 친화적인 오픈소스 방향성
- API 연동 없는 개인정보 보호 중심 MVP

---

## 2. 제품 원칙

### 2.1 빠름 우선

- 앱 실행은 즉각적이어야 한다.
- 새 메모 작성까지의 단계는 최소화한다.
- 정리 기능보다 쓰기 경험이 먼저다.
- 무거운 기능보다 반응성을 우선한다.

### 2.2 로컬 우선

- MVP에서는 모든 노트를 기기 내부에 저장한다.
- 계정 가입이나 로그인 없이 사용할 수 있어야 한다.
- 사용자가 직접 내보내기 전까지 데이터가 외부로 나가지 않아야 한다.
- 클라우드 동기화는 MVP 범위에서 제외한다.

### 2.3 Plain Text / Markdown 친화

- Markdown을 노트 본문의 기본 저장 형식으로 사용한다.
- 사용자의 노트는 특정 앱에 갇히지 않아야 한다.
- `.md` 파일로 내보내기 쉬워야 한다.
- 나중에 다른 Markdown 도구로 옮겨도 이해 가능한 구조를 유지한다.

### 2.4 태그 기반 정리

- 폴더보다 태그를 중심으로 정리한다.
- 사용자는 노트 본문 안에 `#태그`를 입력할 수 있다.
- 하나의 노트는 여러 태그에 속할 수 있다.
- 태그는 검색과 필터링의 핵심 축이 된다.

### 2.5 단순하지만 허전하지 않게

- UI는 조용하고 깔끔해야 한다.
- 기능 수를 늘리는 것보다 핵심 사용 흐름을 다듬는 것이 중요하다.
- 복잡한 기능은 숨기고, 자주 쓰는 기능은 쉽게 접근 가능해야 한다.

### 2.6 안전 기본값

- 삭제 같은 파괴적 작업은 가능한 한 복구 가능해야 한다.
- 실수로 데이터가 날아가는 상황을 피해야 한다.
- 영리한 UX보다 데이터 안정성이 우선이다.

### 2.7 오픈소스 우선

- 공개 저장소에서 개발 가능한 구조를 유지한다.
- 다른 개발자가 코드를 읽고, 빌드하고, 검증할 수 있어야 한다.
- 폐쇄형 SDK, 숨겨진 서비스, 알 수 없는 바이너리 의존성을 피한다.
- 라이선스가 명확한 오픈소스 라이브러리를 우선 사용한다.

### 2.8 개인정보 보호 중심

- MVP에서는 로그인, 계정, 분석, 광고, 추적, 원격 설정, API 연동을 넣지 않는다.
- 노트, 태그, 메타데이터, 사용 패턴을 외부 서버로 보내지 않는다.
- 네트워크 기능은 향후 별도 설계 검토 후 결정한다.

### 2.9 기능 수보다 속도와 디자인

- 앱은 빠르고, 단순하고, 보기 좋아야 한다.
- 다른 앱에 있는 기능이라고 해서 무조건 추가하지 않는다.
- 모든 기능은 `열기 → 쓰기 → 정리 → 찾기 → 내보내기` 흐름을 강화해야 한다.

---

## 3. 저장소 정보

최종 GitHub 저장소:

```text
https://github.com/jeiel85/markleaf-android.git
```

권장 기본 브랜치:

```text
main
```

권장 저장소 설명:

```text
A lightweight, local-first Markdown note app for Android.
```

---

## 4. 패키지명 정책

최종 Android Application ID:

```text
com.markleaf.notes
```

주의사항:

Play Store에 한 번 배포한 뒤 Application ID를 바꾸면 기존 앱의 업데이트가 아니라 별도 앱으로 인식된다.  
따라서 첫 배포 전에 반드시 Application ID를 확정해야 한다.

---

## 5. F-Droid 및 오픈소스 배포 방향

Markleaf는 향후 F-Droid 배포가 가능할 정도의 오픈소스 친화성을 유지해야 한다.

### 5.1 목표

- 전체 앱이 공개 소스만으로 빌드 가능해야 한다.
- 초기 버전에서는 독점 서비스에 의존하지 않는다.
- 폐쇄형 SDK를 사용하지 않는다.
- 필수 네트워크 접근을 요구하지 않는다.
- 분석, 광고, 추적, 크래시 리포팅 SDK를 MVP에 넣지 않는다.
- 의존성은 최소화하고, 라이선스를 명확히 관리한다.
- 릴리스 과정도 가능한 한 투명하게 유지한다.

### 5.2 의존성 정책

우선 사용 가능:

- AndroidX
- Jetpack Compose
- Kotlin Coroutines
- Room
- DataStore
- Material 3
- 라이선스가 명확한 성숙한 오픈소스 라이브러리

MVP에서 피해야 할 것:

- Firebase
- Google Analytics
- AdMob 또는 광고 SDK
- 원격 설정 서비스
- 독점 크래시 리포팅 SDK
- 로그인/Auth SDK
- 클라우드 스토리지 SDK
- AI API
- 백엔드 API 클라이언트
- 폐쇄형 바이너리 의존성

---

## 6. 개인정보 보호 및 No-API MVP 정책

초기 버전은 오프라인 중심 앱으로 설계한다.  
MVP에서는 API 의존성을 만들지 않는다.

### 6.1 MVP에서 금지

- 로그인
- 계정 시스템
- 분석 도구
- 광고
- 추적
- 클라우드 동기화
- 원격 DB
- 외부 API 연동
- 서버 사이드 처리
- 노트, 태그, 파일명, 메타데이터 자동 업로드
- 백그라운드 네트워크 통신

### 6.2 MVP에서 허용되는 데이터 이동

사용자가 직접 실행한 경우에만 데이터가 앱 밖으로 나갈 수 있다.

- 단일 노트를 Markdown으로 내보내기
- 전체 노트를 Markdown 파일로 내보내기
- Android 공유 시트를 통해 노트 텍스트 공유
- 클립보드로 복사

### 6.3 Android 권한 정책

MVP는 가능한 한 권한을 요청하지 않는다.

- `android.permission.INTERNET` 사용하지 않음
- 연락처 권한 사용하지 않음
- 위치 권한 사용하지 않음
- 마이크 권한 사용하지 않음
- 카메라 권한 사용하지 않음
- 계정 권한 사용하지 않음

파일 내보내기는 Android Storage Access Framework를 사용하여 사용자가 직접 선택한 위치에만 저장한다.

---

## 7. MVP 범위

### 반드시 포함

- 노트 생성
- 노트 수정
- 휴지통 이동
- 휴지통에서 복원
- 휴지통에서 영구 삭제
- 전체 노트 목록
- 노트 고정/고정 해제
- 노트 검색
- Markdown 텍스트 작성
- 첫 번째 제목 또는 첫 번째 유효 라인에서 제목 자동 추출
- 본문 내 `#태그` 감지
- 태그별 필터링
- 라이트 모드 / 다크 모드
- Room 기반 로컬 저장
- 단일 노트 Markdown 내보내기
- 전체 노트 Markdown 내보내기

### 있으면 좋은 기능

- 기본 Markdown 미리보기
- 체크리스트 문법 지원: `- [ ]`, `- [x]`
- 글자 수 / 단어 수 표시
- 아카이브
- 노트 텍스트 공유
- Android 공유 시트로 받은 텍스트 저장
- 기본 태블릿 레이아웃

### MVP 명시적 제외 범위

- 실시간 클라우드 동기화
- API 연동
- 네트워크 의존 기능
- 로그인/계정 시스템
- 분석/텔레메트리
- 광고/추적 SDK
- 협업 기능
- 웹 앱
- 데스크톱 앱
- 복잡한 데이터베이스/테이블 블록
- 칸반 보드
- 캘린더 연동
- AI 글쓰기 도우미
- 종단 간 암호화
- WYSIWYG 블록 에디터

---

## 8. 권장 기술 스택

- 언어: Kotlin
- UI: Jetpack Compose
- 디자인 시스템: Material 3
- 아키텍처: MVVM 또는 가벼운 MVI
- 로컬 DB: Room
- 설정 저장: DataStore
- 의존성 주입: Hilt, Koin 또는 수동 DI
- 비동기 처리: Kotlin Coroutines + Flow
- 화면 이동: Jetpack Navigation Compose
- 빌드 시스템: Gradle Kotlin DSL
- Minimum SDK: 26 이상 권장
- Target SDK: 최신 안정 Android SDK

### MVP 기술 제약

- `android.permission.INTERNET`을 추가하지 않는다.
- 독점 SDK를 추가하지 않는다.
- 분석, 광고, 크래시 리포팅, 원격 설정, API 클라이언트를 추가하지 않는다.
- 공개 저장소만으로 빌드 가능한 Android 프로젝트를 유지한다.
- 의존성 선택 시 F-Droid 호환성을 고려한다.

---

## 9. 아키텍처

간결한 레이어드 아키텍처를 사용한다.

```text
app/
  ui/
    screen/
    component/
    theme/
    navigation/
  feature/
    notes/
    tags/
    search/
    settings/
    export/
  domain/
    model/
    repository/
    usecase/
  data/
    local/
      db/
      dao/
      entity/
      mapper/
    repository/
    export/
  core/
    time/
    text/
    markdown/
    result/
```

### 레이어 규칙

- UI는 Room DAO에 직접 접근하지 않는다.
- UI는 ViewModel과 통신한다.
- ViewModel은 UseCase 또는 Repository를 호출한다.
- Repository는 데이터 소스를 조정한다.
- Domain model은 Room entity를 노출하지 않는다.
- Entity와 Domain model 변환은 data layer에서 처리한다.

---

## 10. 핵심 데이터 모델

### Domain Model: Note

```kotlin
data class Note(
    val id: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pinned: Boolean,
    val archived: Boolean,
    val trashed: Boolean,
    val deletedAt: Instant?,
    val tags: List<Tag>
)
```

### Domain Model: Tag

```kotlin
data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Instant
)
```

---

## 11. 태그 파싱 규칙

유효한 태그는 `#`으로 시작하고 영문, 숫자, 한글, `_`, `-`를 포함할 수 있다.

예:

```text
#idea
#work
#sermon
#주일학교
#project-alpha
#family_notes
```

Markdown heading은 태그로 처리하지 않는다.

```text
# Heading 1
## Heading 2
### Heading 3
```

URL fragment도 태그로 처리하지 않는다.

```text
https://example.com/page#section
```

후보 정규식:

```kotlin
val tagRegex = Regex("""(?<![\\w/])#([A-Za-z0-9가-힣_-]+)""")
```

정규식만 믿지 말고 parser 함수에서 context check를 수행한다.

---

## 12. 제목 추출 규칙

우선순위:

1. 첫 번째 Markdown heading
2. 첫 번째 비어 있지 않은 줄
3. fallback: `Untitled`

정리 규칙:

- Markdown heading marker 제거
- 앞뒤 공백 제거
- 표시 제목은 80자 내외로 제한
- 원본 본문은 별도로 보존

---

## 13. 개발 단계

### Phase 1: 프로젝트 기반

- Android 프로젝트 생성
- Kotlin, Compose, Room, DataStore 설정
- 패키지 구조 구성
- 앱 테마 추가
- navigation skeleton 추가
- GitHub Actions CI 추가
- ktlint 또는 detekt 추가

### Phase 2: 로컬 노트

- Room database 추가
- Note entity와 DAO 추가
- Repository 추가
- 노트 목록 화면 추가
- 편집기 화면 추가
- 자동 저장 구현

### Phase 3: 태그

- Tag entity 추가
- note-tag cross ref 추가
- tag parser 추가
- 저장 시 태그 재색인
- 태그 목록/필터 UI 추가

### Phase 4: 검색과 휴지통

- 검색 화면 추가
- 휴지통 동작 추가
- 복원/영구 삭제 추가
- 아카이브 검토

### Phase 5: 내보내기와 마감

- Markdown 단일 노트 내보내기
- 전체 노트 내보내기
- 공유 기능
- 설정 화면
- 빈 상태 화면
- 타이포그래피 개선

---

## 14. MVP 완료 기준

- 사용자가 노트를 안정적으로 생성/수정할 수 있다.
- 앱 재시작 후에도 노트가 유지된다.
- 본문에서 태그가 파싱된다.
- 태그별 필터링이 가능하다.
- 노트 검색이 가능하다.
- 노트 삭제와 복원이 가능하다.
- 최소한 단일 노트 Markdown 내보내기가 가능하다.
- 시스템 다크 모드를 지원한다.
- MVP에서 인터넷 권한을 요청하지 않는다.
- MVP에 분석, 광고, 추적, 독점 백엔드 SDK가 없다.
- 공개 소스만으로 빌드 가능하다.
- 기본 생성/수정/삭제 흐름에서 알려진 크래시가 없다.
- 기존 메모앱과 혼동될 정도로 유사한 브랜딩 또는 UI 자산이 없다.
