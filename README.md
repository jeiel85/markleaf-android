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
</p>

---

## 🍃 Markleaf란?

**Markleaf**는 군더더기를 덜어내고 오직 '기록'과 '정리'에만 집중할 수 있도록 설계된 Android 메모 앱입니다. 당신의 데이터는 오직 당신의 기기에만 저장되며, 표준 Markdown 형식을 사용하여 데이터의 소유권과 이식성을 완벽히 보장합니다.

[**브랜딩 페이지 보기**](https://jeiel85.github.io/markleaf-android/)

---

## ✨ 핵심 기능

- **Markdown First**: 표준 Markdown 문법을 지원하며, 실시간 미리보기를 통해 작성 중인 내용을 즉시 확인할 수 있습니다.
- **Tag-based Organization**: 복잡한 폴더 구조 대신 `#태그`를 통해 유연하게 노트를 분류하고 검색합니다.
- **Local-first Privacy**: 인터넷 연결 없이 작동하며, 어떠한 개인정보도 외부 서버로 전송하지 않습니다.
- **Fast & Lightweight**: 불필요한 애니메이션이나 무거운 라이브러리를 배제하여 즉각적인 실행 속도를 제공합니다.
- **Plain Text Export**: 작성한 모든 노트를 언제든지 개별 Markdown 파일이나 전체 압축 파일로 내보낼 수 있습니다.

---

## 🛠 기술 스택

Markleaf는 최신 Android 개발 표준을 준수하며, 유지보수가 용이한 현대적인 스택을 사용합니다.

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - 선언형 UI 프레임워크
- **Architecture**: MVVM + Clean Architecture 구조
- **Database**: [Room](https://developer.android.com/training/data-storage/room) - SQLite 기반 로컬 퍼시스턴스
- **Dependency Injection**: 가벼운 의존성 주입 패턴 적용
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Theme**: Material 3 (Material You 지원 예정)

---

## 🏗 아키텍처

Markleaf는 관심사 분리와 테스트 가능성을 위해 다음과 같은 레이어 구조를 가집니다.

```text
com.markleaf.notes
├── core          # 마크다운 처리, 유틸리티 등 공통 핵심 로직
├── data          # Room DB, Entity, Repository 구현체 (Data Source)
├── domain        # Model, Repository 인터페이스 (Business Logic)
├── feature       # 화면별 UI 및 ViewModel (Presentation)
├── navigation    # Jetpack Compose Navigation 설정
└── ui            # 테마 및 공통 컴포넌트
```

---

## 🚀 시작하기

### 설치 방법
1. [Releases](https://github.com/jeiel85/markleaf-android/releases) 페이지에서 최신 버전의 APK를 다운로드합니다.
2. Android 기기에서 APK를 실행하여 설치합니다.

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

---

## 🗺 로드맵

- [x] 기본적인 Markdown 편집 및 저장
- [x] 태그 기반 필터링 및 검색
- [x] 새로운 앱 아이콘 및 브랜딩 적용
- [ ] 이미지 첨부 기능 지원
- [ ] 노트 간 링크(Note Link) 기능
- [ ] 다크 모드 및 테마 최적화
- [ ] 태블릿용 2-Pane 레이아웃 지원

---

## 📜 라이선스

이 프로젝트는 **Apache License 2.0**에 따라 라이선스가 부여됩니다. 자세한 내용은 `LICENSE` 파일을 확인하세요.

---

<p align="center">
  Made with ❤️ by <strong>Markleaf Team</strong>
</p>
