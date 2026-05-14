# Markleaf Security Notes

이 문서는 현재 공개된 Markleaf Android(v2.x) 기준 보안 정책입니다.

## Scope

Markleaf는 자체 네트워크 트래픽이 없는 로컬 우선 앱이다.

- Markleaf 자체에는 `android.permission.INTERNET` 이 없다.
- Markleaf는 자체 백엔드 서버가 없다.
- 사용자의 노트는 사용자의 명시적 export/share/외부 링크 열기/사용자 선택 폴더 미러 동기화 전까지 기기 밖으로 나가지 않는다.

## 금지

다음은 현재 코드 상에서도, 향후 합의 없이 추가하는 것도 금지한다.

- API key / secret token / 사용자 계정 정보 저장
- 원격 서버로의 자동 업로드
- 백그라운드 네트워크 통신
- analytics SDK
- tracking SDK
- 광고 SDK
- 폐쇄형 크래시 리포팅 SDK
- 원격 설정 서비스
- 클로즈드 소스 바이너리 의존성

## 데이터 보호

- 노트 본문/태그/메타데이터는 앱 전용 내부 Room DB(`<dataDir>`)에 저장된다.
- 첨부 이미지는 앱 전용 내부 저장소 `<filesDir>/attachments/<noteId>/` 에 복사 보관된다.
- 삭제는 즉시 영구 삭제 대신 휴지통으로 이동한다.
- 휴지통에서의 영구 삭제는 확인 다이얼로그를 거친다.
- 설정에서 화면 보안(`FLAG_SECURE`) 토글로 최근 앱 미리보기/스크린샷을 차단할 수 있다.

## Android 시스템 백업 / 기기 간 전송

`AndroidManifest.xml` 의 `<application>` 요소에 `android:allowBackup="false"` 를 설정해 Android 자동 백업(Android Auto Backup)과 기기 간 전송(Device-to-Device transfer)에서 Markleaf 데이터를 제외한다.

근거:

- Markleaf는 "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다" 를 약속한다. OS 차원의 자동 클라우드 백업이 사용자에게 활성화되어 있을 경우, 이 약속과 무언의 충돌이 발생할 수 있다.
- 보수적 기본값을 택해 Markleaf 데이터가 사용자 동의 없이 Google 드라이브 등으로 자동 업로드되지 않도록 한다.
- 사용자가 다중 기기 또는 백업이 필요한 경우, Markleaf는 *명시적인* 경로(Markdown export, ZIP/AAB 없이 표준 `.md` 파일, SAF 폴더 미러 동기화) 를 제공한다.

`android:dataExtractionRules` 를 사용하지 않은 이유:

- `dataExtractionRules` 는 Android 12 이상에서 fine-grained 제어가 가능하지만, *모든 데이터* 를 제외할 거라면 `allowBackup="false"` 가 더 단순하고 명시적이다.
- minSdk = 26 인 Markleaf 입장에서 두 메커니즘을 동시에 관리하는 추가 표면적이 정책 이득보다 크다.
- 향후 부분적 백업(예: 사용자 설정만 백업, 노트는 제외) 같은 fine-grained 정책이 필요해지면 그 시점에 `dataExtractionRules` 도입을 재검토한다.

## 사용자 주도 데이터 이동 경로

다음 경로는 사용자의 명시적 행동에 의해 발생하며, 모두 OS 표준 UI(SAF, 시스템 공유 시트, `ACTION_VIEW`)를 거친다.

- 노트 `.md` 내보내기 (단일/전체)
- 시스템 공유 시트로 텍스트/파일 공유
- 본문 외부 링크를 사용자가 탭 → OS 기본 브라우저/앱으로 위임
- 사용자가 선택한 SAF 폴더로 노트 `.md` 자동 미러 — 해당 폴더의 외부 동기화는 사용자가 선택한 외부 앱(Drive/Dropbox/Syncthing/OneDrive/NAS 등) 의 책임

위 경로에서 Markleaf는 네트워크 호출을 직접 수행하지 않는다.

## 향후 검토

다음 기능을 추가하기 전에는 별도 보안 검토가 필요하다.

- 직접 구현하는 클라우드 동기화 (지금은 SAF + 외부 앱에 위임)
- 직접 구현하는 백업 업로드
- 계정 로그인
- 직접 구현하는 WebDAV / Google Drive 클라이언트
- 노트 본문 종단 간 암호화
- 생체 인증 / 앱 잠금
