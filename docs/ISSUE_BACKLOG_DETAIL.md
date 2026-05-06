# Markleaf Issue Backlog & Technical Specifications

이 문서는 Markleaf 프로젝트의 기존 이슈 53건과 신규 제안 이슈 50건에 대한 상세 기술 명세서입니다. 모든 이슈는 '로컬 우선(Local-first)' 및 '개인정보 보호' 원칙을 준수하며 구현되어야 합니다.

---

## 📂 [A] 기존 이슈 보완 및 상세화 (53건)

### 🛡️ Security & Privacy (10건)
#### #72 [Security] SQLCipher 기반 DB 전체 암호화
- **Goal**: 기기 탈취 시 물리적인 DB 파일 접근으로부터 사용자 데이터 보호.
- **Tech Spec**: `net.zetetic:sqlcipher-android` 도입. `SupportOpenHelperFactory`를 Room에 연결.
- **Implementation**: `AppDatabase` 빌더 수정 및 Keystore 기반 키 관리 로직 추가.
- **Verification**: SQLite Viewer에서 DB 파일 오픈 실패 확인.

#### #74 [Security] No-Cloud 보증 인증 문서화
- **Goal**: 사용자에게 데이터가 유출되지 않음을 증명.
- **Tech Spec**: `AndroidManifest.xml`에서 `INTERNET` 권한 배제 확인 문서 작성.
- **Implementation**: `PRIVACY.md` 업데이트 및 빌드 시 권한 감사 스크립트 추가.

#### #73 [Security] 인앱 프라이버시 대시보드
- **Goal**: 현재 데이터 저장 위치 및 권한 상태 가시화.
- **Tech Spec**: `SettingsScreen` 하위에 프라이버시 정보 섹션 UI 추가.

#### #49 [Security] 이미지 메타데이터(EXIF) 자동 제거
- **Goal**: 사진 첨부 시 위치 정보 등 민감 데이터 노출 방지.
- **Tech Spec**: `androidx.exifinterface:exifinterface` 사용.
- **Implementation**: 이미지 저장 전 `ExifInterface`로 모든 속성 clear 처리.

#### #48 [Security] 프라이빗 시크릿 노트 모드
- **Goal**: 특정 노트를 목록에서 숨기고 생체 인증으로만 접근.
- **Tech Spec**: `NoteEntity`에 `isPrivate: Boolean` 필드 추가 및 필터링 로직 구현.

#### #47 [Security] 민감 노트 스크린샷 방지 옵션
- **Goal**: 앱 외부로 화면 정보가 유출되는 것 방지.
- **Tech Spec**: `Window.setFlags(WindowManager.LayoutParams.FLAG_SECURE)` 사용.

#### #45 [Security] 암호화된 로컬 백업 파일 생성
- **Goal**: ZIP 백업 시 비밀번호를 통한 AES 암호화 지원.
- **Tech Spec**: `Zip4j` 라이브러리 사용.

#### #44 [Security] 긴급 상황 패닉 트리거
- **Goal**: 특정 액션 시 앱을 즉시 잠그거나 데이터 숨김.
- **Tech Spec**: 볼륨 버튼 리스너 또는 쉐이크 센서 연동.

#### #46 [Security] 권한 최소화 및 개인정보 보호 감사
- **Goal**: 불필요한 권한 요청 제거.
- **Tech Spec**: `lint`를 통한 권한 체크 및 `AndroidManifest` 최적화.

#### #50 [Security] 자동 로컬 백업 스케줄러
- **Goal**: 정기적으로 내부 저장소에 백업본 생성.
- **Tech Spec**: `WorkManager`를 사용하여 주기적 백업 태스크 실행.

---

### ✍️ Editor & Markdown (15건)
#### #71 [Feature] LaTeX 수식 지원
- **Goal**: 수학 기호 렌더링.
- **Tech Spec**: `KaTeX` JS를 WebView 또는 네이티브 캔버스로 렌더링.
- **Implementation**: `$$` 및 `$` 패턴 감지기 추가.

#### #70 [Feature] 코드 구문 강조(Syntax Highlighting)
- **Goal**: 코드 블록 가독성 향상.
- **Tech Spec**: `MarkdownSyntaxHighlighter` 확장. 언어별 스팬(Span) 컬러링.

#### #66 [Feature] 위키 링크(Wiki-links) 지원
- **Goal**: `[[Note Name]]` 문법으로 상호 연결.
- **Tech Spec**: `TagParser`와 유사한 방식으로 링크 추출 및 `NoteRepository` 검색 연동.

#### #63 [UI/UX] 편집기 툴바 사용자 지정
- **Goal**: 사용자가 자주 쓰는 버튼만 노출.
- **Tech Spec**: `DataStore`에 버튼 리스트 저장 및 동적 렌더링.

#### #65 [Feature] SQLite FTS5 통합 검색 고도화
- **Goal**: 대량 노트에서 고속 검색.
- **Tech Spec**: `Fts4` 또는 `Fts5` 가상 테이블 구현 및 검색 쿼리 최적화.

#### #60 [UI/UX] 리치 에디터 애니메이션
- **Goal**: 입력 시 부드러운 UI 반응.
- **Tech Spec**: Compose `AnimatedVisibility` 및 `AnimateContentSize` 적용.

#### #35 [UI/UX] 집중 모드(Focus Mode) UI
- **Goal**: 글쓰기에만 집중할 수 있는 환경.
- **Tech Spec**: UI 요소를 숨기는 상태 머신(State Machine) 구현.

#### #33 [UI/UX] 편집기 툴바 사용자 지정 (중복 확인 및 병합 대상)
- **#63과 동일 작업으로 처리.**

#### #21 [Feature] 실시간 마크다운 인라인 하이라이팅
- **Goal**: 편집 중 문법 요소(Bold, Italic 등) 강조.
- **Tech Spec**: `VisualTransformation`을 활용한 실시간 스패닝.

#### #18 [UI/UX] 편집기 링크 툴바 버튼 명확화
- **Goal**: 링크 삽입/수정 UX 개선.
- **Tech Spec**: 아이콘 가독성 및 툴팁 추가.

#### #30 [UI/UX] 마크다운 미리보기 전환 애니메이션
- **Goal**: 편집/미리보기 전환 시 자연스러운 전환.
- **Tech Spec**: Shared Element Transition 또는 크로스페이드(Crossfade).

#### #28 [UI/UX] 사용자 지정 폰트 지원
- **Goal**: 가독성 향상을 위한 외부 폰트 파일 로드.
- **Tech Spec**: `FontFamily` 동적 생성 로직 추가.

#### #38 [Feature] 고품질 PDF 내보내기
- **Goal**: 마크다운을 서식 유지한 PDF로 변환.
- **Tech Spec**: `Android PdfDocument` 또는 HTML 렌더링 후 프린트 API 사용.

#### #42 [Feature] 음성 인식 마크다운 입력
- **Goal**: 음성을 텍스트로 변환하여 바로 입력.
- **Tech Spec**: `SpeechRecognizer` API 연동.

#### #43 [Feature] 노트 내 그리기/필기 스케치 통합
- **Goal**: 손글씨나 간단한 약도 삽입.
- **Tech Spec**: `Canvas` 기반 드로잉 컴포넌트 개발 및 이미지로 저장.

---

### 🎨 UI/UX & Aesthetics (15건)
#### #59 [UI/UX] Material You 다이내믹 컬러
- **Goal**: 시스템 색상 테마 동기화.
- **Tech Spec**: `dynamicDarkColorScheme` 적용.

#### #61 [UI/UX] 드래그 앤 드롭 노트 재정렬
- **Goal**: 수동 순서 조정.
- **Tech Spec**: `LazyColumn` 아이템 재정렬 로직.

#### #62 [UI/UX] 햅틱 피드백 최적화
- **Goal**: 중요한 액션 시 진동 피드백.
- **Tech Spec**: `HapticFeedback` API 활용.

#### #64 [UI/UX] 상태바 노트 카운트 대시보드
- **Goal**: 총 노트 수 및 오늘 작성 수 표시.
- **Tech Spec**: 홈 화면 상단 영역에 통계 뷰 추가.

#### #31 [UI/UX] 그리드/리스트 뷰 전환
- **Goal**: 목록 레이아웃 취향 반영.
- **Tech Spec**: `LazyVerticalGrid`와 `LazyColumn` 전환 토글.

#### #32 [UI/UX] 폴더/노트부 아이콘 커스터마이징
- **Goal**: 시각적 분류 강화.
- **Tech Spec**: 아이콘 선택 팝업 및 엔티티 저장.

#### #34 [UI/UX] 상단/하단 빠른 스크롤
- **Goal**: 긴 목록 탐색 편의성.
- **Tech Spec**: 스크롤바 인디케이터 클릭 시 이동.

#### #29 [UI/UX] 엣지 투 엣지(Edge-to-Edge) 디자인
- **Goal**: 몰입감 있는 UI.
- **Tech Spec**: `WindowInsets` 대응.

#### #27 [UI/UX] 예측 뒤로 가기 제스처
- **Goal**: 최신 안드로이드 UX 지원.
- **Tech Spec**: `OnBackPressedCallback` 업데이트.

#### #23 [UI/UX] 태블릿 2-Pane 시각적 구분 강화
- **Goal**: 대화면 가독성 개선.
- **Tech Spec**: 구분선 및 배경색 대비 조정.

#### #20 [UI/UX] 마크다운 가독성 설정 (너비 등)
- **Goal**: 화면 폭에 따른 텍스트 배치 최적화.
- **Tech Spec**: 편집기 여백(Padding) 설정 추가.

#### #19 [UI/UX] 태블릿 노트 리스트 접기 기능
- **Goal**: 편집 화면 넓게 쓰기.
- **Tech Spec**: 사이드바 가시성 제어 로직.

#### #17 [UI/UX] 링크 미리보기 및 설정 네비게이션
- **Goal**: 링크 클릭 시 동작 정의.
- **Tech Spec**: 인앱 브라우저 또는 외부 브라우저 선택 옵션.

#### #11 [UI/UX] UI/UX 폴리싱 및 Material You (중복 병합)
- **#59와 병합 처리.**

#### #52 [Growth] 전략적 인앱 리뷰 요청 UI
- **Goal**: 앱 평점 향상.
- **Tech Spec**: 특정 횟수 이상 노트 작성 시 호출.

---

### 📈 Growth & Strategy (13건)
#### #76 [Growth] WCAG 기준 접근성 최적화
- **Goal**: 모든 사용자가 차별 없이 사용 가능하도록 개선.
- **Tech Spec**: `Modifier.semantics` 및 `contentDescription` 전수 조사.

#### #75 [Growth] 커뮤니티 마크다운 템플릿 갤러리
- **Goal**: 유용한 서식 공유(로컬 저장소 기반).
- **Tech Spec**: 기본 제공 템플릿 확장.

#### #69 [Feature] 체크리스트 진행률 시각화
- **Goal**: 할 일 관리 편의성.
- **Tech Spec**: 목록 뷰에 프로그레스 바 표시.

#### #68 [Feature] 온디바이스 OCR (이미지 텍스트 추출)
- **Goal**: 사진 속 텍스트를 메모로 변환.
- **Tech Spec**: `ML Kit` 로컬 OCR 라이브러리 연동.

#### #67 [Feature] 로컬 전용 이미지 첨부 기능
- **Goal**: 미디어 포함 메모 작성.
- **Tech Spec**: 내부 저장소 미디어 관리 및 이미지 태그 자동 삽입.

#### #58 [Growth] 로컬 성능 모니터링
- **Goal**: 앱 버벅임 지점 파악.
- **Tech Spec**: `Jetpack Benchmark` 활용한 성능 측정.

#### #57 [Growth] 인터랙티브 온보딩 가이드
- **Goal**: 첫 사용자 기능 안내.
- **Tech Spec**: 가이드 오버레이 UI 구현.

#### #56 [Growth] 이미지로 공유 카드 생성
- **Goal**: 예쁜 메모 공유.
- **Tech Spec**: Canvas 렌더링 후 공유 `Intent` 실행.

#### #55 [Growth] 오픈소스 투명성 강조
- **Goal**: 신뢰도 향상.
- **Tech Spec**: 설정 화면에 GitHub 링크 및 라이선스 고지 강화.

#### #54 [Growth] 전문 브랜드 피처 그래픽 업데이트
- **Goal**: 스토어 등록 정보 개선.
- **Tech Spec**: 신규 아이콘 반영한 그래픽 리소싱.

#### #53 [Growth] 스토어 스크린샷 리디자인
- **Goal**: 전환율 최적화.
- **Tech Spec**: 주요 기능 강조형 레이아웃 적용.

#### #51 [Growth] 다국어 지원 확대 (JP, FR, DE 등)
- **Goal**: 글로벌 사용자 확보.
- **Tech Spec**: `strings.xml` 번역 추가.

#### #40 [Feature] 앱 숏컷 (아이콘 롱프레스)
- **Goal**: 빠른 접근성.
- **Tech Spec**: `ShortcutManager`에 정적/동적 숏컷 등록.

---

## 🆕 [B] 신규 제안 이슈 (50건)

### 📚 Knowledge Management (10건)
1. **[Feature] 마크다운 각주(Footnotes) 지원**: `[^1]` 문법 파싱 및 하단 렌더링.
2. **[Feature] 양방향 링크 그래프 뷰**: 노트 간 연결 구조 시각화 컴포넌트 개발.
3. **[Feature] 콜아웃(Callouts) 강조 문법**: `> [!NOTE]` 스타일 박스 렌더링.
4. **[Feature] 템플릿(Templates) 시스템**: 자주 쓰는 서식 저장 및 삽입 로직.
5. **[Feature] 태그 계층화(#parent/child)**: 태그 구조 트리형 탐색 지원.
6. **[Feature] 자동 링크 제안**: 입력 중 제목과 일치하는 단어 발견 시 링크 추천.
7. **[Feature] 중요 노트 상단 고정(Pin)**: `isPinned` 필드 추가 및 목록 정렬 최우선순위 부여.
8. **[Feature] 머메이드(Mermaid.js) 차트 렌더링**: 텍스트 기반 다이어그램 미리보기.
9. **[Feature] Zettelkasten ID 자동 생성**: 파일명/제목에 타임스탬프 기반 ID 부여 옵션.
10. **[Feature] 프론트매터(YAML) 파싱**: 문서 상단 메타데이터를 UI 속성으로 추출.

### 🛠️ Productivity Tools (10건)
11. **[Feature] 부유형 작성 버튼(Chat Head 스타일)**: 타 앱 사용 중 빠른 메모 진입.
12. **[Feature] 멀티 윈도우/분할 화면 최적화**: 다중 인스턴스 대응 로직 개선.
13. **[Feature] 현재 줄 복제 단축키 구현**: 편집기 편의 기능 추가.
14. **[Feature] 시스템 맞춤법 검사기 연동**: 오타 감지 및 수정 제안 강조.
15. **[Feature] 실시간 텍스트 통계**: 글자 수, 단어 수, 읽기 시간 플로팅 뷰.
16. **[Feature] 마크다운 표(Table) 편집 도우미**: 표 생성 및 행/열 조작 툴바 UI.
17. **[Feature] 일괄 태그 수정/삭제(Bulk Actions)**: 다중 선택 모드 기능 확장.
18. **[Feature] 캘린더 기반 탐색**: 작성일/수정일 기준 날짜별 노트 보기.
19. **[Feature] 무제한 실행 취소/재실행(Undo/Redo)**: 편집기 스택 관리 고도화.
20. **[Feature] 할 일(Todo) 통합 뷰**: 모든 노트 내 미완료 체크박스 추출 목록.

### 🖼️ UI/UX & Customization (10건)
21. **[UI/UX] 가로 모드 적응형 레이아웃**: 폰 가로 화면에서도 최적화된 리스트-편집기 뷰.
22. **[UI/UX] 사용자 SVG 아이콘 팩 지원**: 외부 리소스를 태그 아이콘으로 활용.
23. **[UI/UX] 편집 방지(Read-only) 모드**: 뷰어 전용 모드 토글 스위치.
24. **[UI/UX] 스크롤바 인디케이터 커스텀**: 스크롤 위치 시각화 개선.
25. **[UI/UX] 핀치 투 줌 글자 크기 조절**: 제스처 기반 폰트 크기 변경.
26. **[UI/UX] 본문 내 검색어 하이라이트 유지**: 검색 결과 진입 시 해당 위치 강조.
27. **[UI/UX] 목록 내 체크리스트 진행바**: 진행률 가시화 UI.
28. **[UI/UX] 다크 모드 시간 예약제**: 일출/일몰 대응 테마 전환.
29. **[UI/UX] 툴바 텍스트 라벨 모드**: 아이콘 대신 글자로 메뉴 표시 옵션.
30. **[UI/UX] 전자책 스타일 페이지 넘김**: 수평 스와이프 기반 읽기 모드.

### 🔧 System & Integration (10건)
31. **[Feature] 외부 .md 파일 Intent 연결**: 외부 파일 열기 및 앱 내 저장 기능.
32. **[Feature] 커스텀 딥링크(URL Scheme)**: `markleaf://` 기반 외부 연동.
33. **[Feature] Scoped Storage 저장소 선택**: 사용자가 원하는 폴더로 DB 위치 변경.
34. **[Feature] 일일 마크다운 자동 내보내기**: 지정 시간 전체 노트 백업 자동화.
35. **[Feature] ADB 기반 CLI 연동 인터페이스**: 개발자용 터미널 제어 지원.
36. **[Feature] 외부 물리 키보드 단축키 확장**: 데스크톱 수준 생산성 단축키 지원.
37. **[Feature] HTML to Markdown 변환기**: 붙여넣기 시 서식 자동 변환 로직.
38. **[Feature] 미디어 브라우저(Gallery)**: 첨부 이미지 통합 관리 뷰.
39. **[Feature] 로컬 Wi-Fi 웹 서버**: PC 브라우저에서 기기 노트 읽기 전용 접근.
40. **[Feature] Android 14 공유 시트 다이렉트 액션**: 시스템 공유 시 빠른 저장.

### 🌍 Accessibility & Quality (10건)
41. **[Growth] RTL 레이아웃 완벽 지원**: 우측 시작 언어 UI 최적화.
42. **[Growth] TalkBack 음성 안내 정교화**: 복잡한 문법 요소 설명 개선.
43. **[Growth] 고대비(High Contrast) 테마**: 시각 장애 대응 테마 추가.
44. **[Growth] 텍스트 음성 출력(TTS) 모드**: 문서 읽어주기 기능.
45. **[Growth] 로컬 번역 서비스 연동**: ML Kit 기반 기기 내 번역 제안.
46. **[Growth] 전용 용어 사전 관리**: 자동 완성 및 추천 단어 최적화.
47. **[Growth] 대화형 로컬 도움말**: 앱 내 사용법을 노트 형식으로 제공.
48. **[Growth] 폰트 가중치(Weight) 세밀 조절**: 사용자 가독성 취향 대응.
49. **[Growth] '방해 금지' 무음 모드**: 앱 내 모든 시각적 배지 제거.
50. **[Growth] 앱 내 업데이트 로그 안내**: 신규 버전 설치 후 변경점 팝업.
