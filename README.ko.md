# <img src="docs/assets/logo.svg" width="48" height="48" align="center" /> Markleaf

<p align="center">
  <img src="docs/assets/logo.svg" width="160" height="160" alt="Markleaf Logo" />
</p>

<p align="center">
  <strong>가볍게 쌓이는 생각, 정돈된 Markdown 노트</strong><br />
  Android용 로컬 우선(Local-first) 미니멀 Markdown 메모 앱
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="UI" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-D22128" alt="License" />
  <img src="https://img.shields.io/badge/F--Droid-Available-1976D2?logo=fdroid&logoColor=white" alt="F-Droid" />
  <img src="https://img.shields.io/badge/Google%20Play-Updates%20paused-9E9E9E?logo=googleplay&logoColor=white" alt="Google Play" />
</p>

<p align="center">
  <a href="README.md">English</a> ·
  <strong>한국어</strong> ·
  <a href="README.ja.md">日本語</a> ·
  <a href="README.de.md">Deutsch</a> ·
  <a href="README.es.md">Español</a> ·
  <a href="README.fr.md">Français</a>
</p>

<p align="center">
  <a href="https://github.com/jeiel85/markleaf-android">GitHub 저장소</a> ·
  <a href="https://github.com/jeiel85/markleaf-android/discussions">Discussions (피드백)</a> ·
  <a href="https://gitlab.com/jeiel85/markleaf-android">GitLab 공개 미러</a>
</p>

<p align="center">
  <img src="docs/assets/markleaf-demo.gif" width="300" alt="Markleaf 편집기에서 /를 입력하면 빠른 삽입 팔레트가 열리고, 체크리스트를 고르면 표준 Markdown이 삽입되어 미리보기에 렌더링됩니다" />
</p>

<p align="center">
  <sub><code>/</code> 빠른 삽입 → 표준 Markdown → 실시간 미리보기</sub>
</p>

---

## 🍃 Markleaf란?

**Markleaf**는 군더더기를 덜어내고 오직 '기록'과 '정리'에만 집중할 수 있도록 설계된 Android Markdown 메모 앱입니다. 당신의 데이터는 오직 당신의 기기에만 저장되며, 표준 Markdown 형식을 사용하여 데이터의 소유권과 이식성을 완벽히 보장합니다. 동기화도 *당신이 선택한 폴더* 를 통해서만 일어납니다 — Markleaf 자체는 인터넷에 나가지 않습니다.

[**브랜딩 페이지 보기**](https://jeiel85.github.io/markleaf-android/) · [현재 버전: v2.29.0](https://github.com/jeiel85/markleaf-android/releases/tag/v2.29.0) · [Privacy Policy](https://jeiel85.github.io/markleaf-android/privacy.html) · [F-Droid](https://f-droid.org/packages/com.markleaf.notes/) · [Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes)

---

## ✨ 핵심 기능

### 작성 & 미리보기
- **`/` 빠른 삽입** — 줄 시작에서 명령을 검색해 제목, 목록, 표, 콜아웃, 위키링크, 이미지 등을 표준 Markdown으로 즉시 삽입
- **실시간 Markdown 미리보기** — 편집과 즉시 전환되는 프리뷰, 또는 *Show Markdown syntax* 옵션으로 라이브 syntax 컬러링
- **GFM 표 / 체크박스 / 인용문 / 콜아웃 (`> [!NOTE]` …)** — 모두 미리보기에 렌더링
- **코드 블록 syntax highlighting** — Kotlin, Java, Python, JavaScript/TypeScript, Bash, JSON, YAML, XML, SQL 10개 언어 토큰 컬러링
- **각주(`[^N]`) ref ↔ def 점프** — 위첨자를 탭하면 정의로 부드럽게 스크롤
- **이미지 첨부 + alt 텍스트 편집** — 앱 내부 저장소에 격리된 사본으로 보관 (미디어 권한 불필요)
- **스마트 Markdown 포맷팅 토글** — 선택 영역 또는 커서 주변 단어를 Bold/Italic/Strike/Inline Code로 감싸고, 이미 감싸진 텍스트는 한 번 더 눌러 자연스럽게 해제
- **키보드 단축키** — 하드웨어 키보드에서 `Ctrl/Cmd+B·I·K·Shift+S`로 굵게·기울임·링크·취소선
- **목차(TOC)** — 미리보기에서 제목(H1–H3) 목록으로 긴 노트의 해당 위치로 점프
- **세리프 / 산세리프 글꼴 선택** — 책 같은 세리프체로 글쓰기 표면을 전환(코드 블록은 항상 고정폭)
- **포커스 모드 / 단어·글자·읽기 시간 통계 / 노트 안에서 찾기·바꾸기**

### 정리 & 탐색
- **태그 기반 분류 + 자동완성** — 본문에 `#태그` 만 쓰면 자동 인덱싱(폴더 없음), 입력 중 기존 태그 자동완성
- **Wikilinks (`[[Title]]`) + 백링크 패널** — 자동 완성, 누가 이 노트를 가리키는지 한눈에
- **빠른 이동 (Quick switcher / Ctrl+K)** — Obsidian 스타일 제목 substring 점프
- **SQLite FTS 기반 전문 검색** — 본문까지 빠르게
- **핀 / 아카이브 / 휴지통** — 휴지통은 영구 삭제 전에 한 번 더 묻습니다

### 동기화 & 내보내기 (No-Cloud 원칙)
- **폴더 미러 동기화** — SAF로 사용자가 선택한 폴더(Drive/Dropbox/Syncthing/OneDrive/NAS 등)에 각 노트를 **노트 제목 이름의** `.md` / `.txt` 파일로 미러링(제목을 바꾸면 폴더 안 파일명도 따라 변경). Markleaf 자체는 인터넷에 안 나가고, 동기화는 *외부 앱이 그 폴더를 동기화하는 방식* 으로 위임
- **외부 `.md` / `.txt` 파일 가져오기** — 파일 관리자에서 파일을 탭하거나 다른 앱에서 공유하면 새 노트로 가져옵니다(제목 머리말이 없으면 파일 이름이 제목). 동기화로 들어온 노트의 태그도 곧바로 인식
- **개별 / 전체 노트 `.md` 내보내기**
- **시스템 공유 시트로 보내기**

### 디자인 & 접근성
- **Markleaf 녹색 테마 + Material You 토글** — 안드로이드 12+ 시스템 월페이퍼 색상도 옵션
- **자동 다크 모드** — 시스템 설정 따라
- **태블릿 3-Pane 레이아웃** — 태그 사이드바 · 노트 목록 · 에디터, 사이드바 태그로 노트 목록 즉석 필터링(노트 목록 접고 펴기 가능)
- **6개 언어 UI** — 한국어 / 영어 / 스페인어 / 일본어 / 프랑스어 / 독일어 리소스 운영
- **스크린샷 / 최근 앱 미리보기 차단 옵션** — 민감한 노트용

---

## 🔗 이미 가지고 있는 Markdown 폴더에서 그대로

Markleaf에는 자체 볼트 형식이 없습니다. 폴더 하나를 지정하면 — Obsidian이나 Logseq, 텍스트 에디터가 이미 열고 있는 폴더라도 — 그 안에 있는 파일을 그대로 다룹니다.

- **원래 당신 것이던 평범한 파일.** 노트 하나가 `.md`(또는 `.txt`) 파일 하나입니다. 기존 파일을 폴더에 넣어두면 Markleaf가 다음에 포그라운드로 올라올 때 노트로 가져옵니다 — 별도의 가져오기 단계가 없습니다.
- **당신의 프론트매터는 보존됩니다.** Markleaf는 기기 간에 파일과 노트를 짝지으려고 작은 YAML 헤더(`markleaf_id`, 타임스탬프, pinned/archived)를 덧붙이고, **자기가 모르는 것은 전부 원문 그대로 다시 내보냅니다** — Obsidian이 태그를 쓰는 들여쓴 블록 목록, 중첩된 맵, 주석과 따옴표까지 포함해서요. 덧붙이는 헤더는 Obsidian·GitHub·VS Code가 모두 파싱하는 표준 YAML의 엄격한 부분집합입니다.
- **쓰던 문법 그대로.** 백링크 패널이 딸린 `[[위키링크]]`, 본문에 바로 쓰는 `#태그`, GFM 표와 체크박스, `> [!NOTE]` 콜아웃, 그리고 Obsidian 스타일 `Ctrl+K` 퀵 스위처.
- **알아서, 그러나 조심스럽게 맞춥니다.** 다른 곳에서 생긴 변경은 Markleaf가 포그라운드로 돌아올 때(분당 1회로 제한) 가져옵니다. 다른 에디터가 Markleaf의 프론트매터를 건드리지 않고 본문만 고쳐도 알아챕니다 — 타임스탬프만이 아니라 본문을 비교하기 때문입니다. 파일이 실제로 더 새로울 때만 반영되고, 양쪽이 모두 바뀌었으면 덮어쓰지 않고 *별도의 노트*로 들여오며, 자동으로 삭제되는 것은 없습니다.

> [!IMPORTANT]
> **실제 볼트를 연결하기 전에 알아둘 두 가지.**
> - **폴더 한 겹만, 하위 폴더는 보지 않습니다.** Markleaf는 지정한 폴더 바로 아래에 있는 파일만 읽고 하위 디렉터리로 내려가지 않습니다. 폴더로 정리된 볼트는 최상위에서만 맞물립니다 — Markleaf는 의도적으로 폴더 대신 태그로 정리합니다.
> - **노트를 편집하면 파일 이름이 바뀝니다.** 미러 파일명은 노트 제목을 따라가므로, 파일명이 제목과 다르면 Markleaf에서 처음 저장하는 순간 이름이 바뀝니다. 볼트 안의 `[[링크]]`가 예전 파일명을 가리키고 있었다면 그 링크는 깨집니다.
>
> 볼트가 여러 겹으로 나뉘어 있거나 링크가 많다면, Markleaf에는 *별도의* 폴더를 지정해서 볼트를 직접 편집하는 두 번째 에디터가 아니라 나중에 합치는 모바일 인박스로 쓰는 편이 낫습니다.

---

## 🛠 기술 스택

Markleaf는 최신 Android 개발 표준을 준수하며, 유지보수가 용이한 현대적인 스택을 사용합니다.

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 + Material You 다이내믹 컬러
- **Architecture**: 단순한 레이어 분리 (core / data / domain / feature / ui) + Repository 패턴
- **Database**: [Room](https://developer.android.com/training/data-storage/room) — SQLite 기반 로컬 퍼시스턴스, FTS4 가상 테이블로 전문 검색
- **Markdown 파서**: [commonmark-java](https://github.com/commonmark/commonmark-java) (CommonMark 0.30 + GFM 확장: 표, 취소선, task lists, 각주, YAML frontmatter)
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Storage Access Framework (SAF)** — 폴더 미러 동기화 + 이미지 첨부
- **이미지 로딩**: [Coil](https://coil-kt.github.io/coil/) — F-Droid 친화적 Apache 2.0
- **DataStore Preferences** — 앱 설정
- **Profile Installer 1.4.0 + Macrobenchmark** — Cold start baseline profile 측정 (TB320FC 기준 326ms)
- **테스트**: JUnit + Robolectric + [Roborazzi](https://github.com/takahirom/roborazzi) 시각 회귀 테스트 (Linux 골든, threshold 0.005)
- **CI**: GitHub Actions + GitLab CI — 독립 빌드·서명 릴리스, launch-smoke, record-roborazzi

---

## 🏗 아키텍처

Markleaf는 관심사 분리와 테스트 가능성을 위해 다음과 같은 레이어 구조를 가집니다.

```text
com.markleaf.notes
├── core          # 마크다운 처리, 첨부, 동기화 등 공통 핵심 로직
├── data          # Room DB, Entity, Repository 구현체 (Data Source)
├── domain        # Model, Repository 인터페이스 (Business Logic)
├── feature       # 화면별 UI 및 ViewModel (Presentation)
│   ├── editor    # 편집기, Find/Replace, Wikilink 자동완성, 콜아웃, 표
│   ├── notes     # 노트 목록, Quick Switcher, 아카이브
│   ├── search    # FTS 기반 전문 검색
│   ├── tags      # 태그 인덱스
│   ├── trash     # 휴지통 / 영구 삭제
│   └── settings  # 테마, 동기화 폴더, 스크린샷 차단 등
├── navigation    # Jetpack Compose Navigation 설정
└── ui            # 테마(Markleaf green / Material You), 공통 컴포넌트
```

---

## 🚀 시작하기

### 설치 방법

> [!NOTE]
> **Google Play 업데이트는 현재 잠정 보류 중입니다.** 1인 개발자의 한국 사업자 등록 요건 관련 정책 이슈가 정리될 때까지 새 버전을 Play Store에 올리지 않습니다. 그동안 **최신 버전은 F-Droid, GitHub Releases 또는 GitLab Releases에서 받아 주세요.** (Play Store에 이미 설치돼 있다면 그대로 사용할 수 있습니다.)

- **F-Droid** *(권장)*: [Markleaf on F-Droid](https://f-droid.org/packages/com.markleaf.notes/) — F-Droid 클라이언트에서 검색하거나 위 링크로 바로 설치할 수 있습니다. 동일 서명 키(SHA-256 `0be97352…f91a`)를 사용하므로 GitHub/GitLab Releases APK로 사이드로드한 경우에도 끊김 없이 업데이트가 이어집니다.
- **APK 직접 설치**: [GitHub v2.29.0](https://github.com/jeiel85/markleaf-android/releases/tag/v2.29.0) 릴리스에서 APK를 다운로드한 뒤 Android 기기에서 실행해 설치합니다.
- **Google Play**: [Markleaf on Google Play](https://play.google.com/store/apps/details?id=com.markleaf.notes) — **업데이트 잠정 보류 중**입니다(위 안내 참고). 이미 설치돼 있으면 계속 쓸 수 있지만, 최신 버전은 F-Droid·GitHub·GitLab에서 받으세요.

### 개발 환경 구축
직접 빌드하거나 기여하고 싶다면 다음 과정을 따르세요.

```bash
# 저장소 복제
git clone https://github.com/jeiel85/markleaf-android.git

# 프로젝트 폴더 이동
cd markleaf-android

# 빌드 및 설치
./gradlew installDebug
```

Markleaf의 버그 수정은 대부분 누군가의 제보에서 시작합니다. 그 사람들을 [THANKS.md](THANKS.md)에 기록해 두었습니다.

---

## 🔒 No-Cloud by design

Markleaf 자체는 절대로 네트워크에 나가지 않습니다. 데이터를 기기 밖으로 보낼지 여부는 *전적으로 당신의 선택* 입니다.

- ✅ `android.permission.INTERNET` 권한 **선언 없음** — Markleaf는 네트워크 요청을 직접 수행하지 않습니다
- ✅ Markleaf 자체 서버 / 자체 백엔드 **없음**
- ✅ 분석 / 광고 / 추적 / 폐쇄형 SDK **없음**
- ✅ `android:allowBackup="false"` — Android 자동 백업 / 기기 간 전송에서 Markleaf 데이터 제외
- ✅ 사용자가 직접 export, share, 외부 링크 열기, SAF 폴더 선택을 수행할 때만 OS 경로를 통해 데이터가 이동
- ✅ 완전한 오픈소스, Apache 2.0 라이선스로 누구나 감사 가능

"never leaves your device" 가 어떻게 정확히 작동하는지는 [Privacy Policy](docs/PRIVACY.md) 와 [No-Cloud Certification](docs/NOCLOUD_CERTIFICATION.md) 에 정리되어 있습니다.

---

## 🗺 로드맵

### v1.x — MVP
- [x] 기본적인 Markdown 편집 및 저장
- [x] 태그 기반 필터링 및 검색
- [x] 새로운 앱 아이콘 및 브랜딩 적용
- [x] 실시간 마크다운 미리보기 및 다크 모드
- [x] SQLite FTS 기반 고성능 검색
- [x] 태블릿용 2-Pane 레이아웃 최적화
- [x] 단일/전체 노트 Markdown 내보내기
- [x] v1.0.0 정식 출시 완료

### v2.x — Bear-class 확장 (현재)
- [x] **v2.3** CommonMark 파서 도입 — 콜아웃, GFM 취소선, task lists, 각주, YAML frontmatter
- [x] **v2.4–2.5** Wikilinks (`[[Title]]`) + 자동 완성 + 백링크 패널
- [x] **v2.6** 이미지 첨부 + alt 텍스트 + 라이트박스
- [x] **v2.7** SAF 폴더 미러 동기화 (Drive/Dropbox/Syncthing 위임형, no INTERNET 유지)
- [x] **v2.8** Material You 토글 + Markleaf 녹색 테마 복원
- [x] **v2.9** 스크린샷 차단 옵션, 시각 회귀 테스트(Roborazzi) 정착
- [x] **v2.10** 코드 블록 syntax highlighting (10개 언어)
- [x] **v2.11** GFM 표 미리보기 부활
- [x] **v2.12** 빠른 이동(Quick Switcher / Ctrl+K)
- [x] **v2.13** 노트 안에서 찾기 / 바꾸기
- [x] **v2.14** 각주 ref ↔ def 클릭 점프
- [x] **v2.15** F-Droid 제출 안정화와 no-cloud 문서 정비
- [x] **v2.16** 홈 화면 위젯, 생체 인증 잠금, 오픈소스 투명성, 스마트 Markdown 포맷팅
- [x] **v2.17** 외부 `.md`/`.txt` 파일 열기·공유 가져오기, 폴더 동기화 중복 노트·태그 인식 개선
- [x] **v2.18** 폴더 동기화 파일명을 노트 제목으로(제목 변경 시 파일도 리네임) + `.md`/`.txt` 선택
- [x] **v2.19** 첫 설치 샘플 노트북(6종) + PDF·Markdown 내보내기 제목 중복 수정
- [x] **v2.20** 키보드 단축키, `#태그` 자동완성, 목차(TOC), 세리프 글꼴, 태블릿 3-Pane(태그 사이드바·즉석 필터) 레이아웃
- [x] **v2.21** 예측형 뒤로가기·세련된 화면 전환, 노트 리스트·카드 애니메이션, 태블릿 노트 전환 페이드·태그 사이드바 접기, 체크박스 완료/미완료 토글
- [x] **v2.22** `/` 빠른 삽입 명령, 터치·외장 키보드 선택, 6개 언어 명령 메뉴
- [x] **Google Play 정식 출시** — Play Store에서 누구나 설치할 수 있습니다

---

## 📜 라이선스

이 프로젝트는 **Apache License 2.0**에 따라 라이선스가 부여됩니다. 자세한 내용은 `LICENSE` 파일을 확인하세요.

---

<p align="center">
  Made with ❤️ by <strong>Markleaf Team</strong>
</p>
