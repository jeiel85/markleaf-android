# Markleaf Privacy Policy

이 문서는 현재 공개된 Markleaf Android 버전(v2.x) 기준 개인정보 보호 정책입니다.  
MVP 초안 문구는 폐기되었습니다.

## 핵심 원칙

Markleaf는 로컬 우선 노트 앱입니다.

- Markleaf 자체에는 `android.permission.INTERNET` 권한이 없습니다.
- Markleaf는 자체 서버를 운영하지 않으며, 노트/태그/첨부/메타데이터를 자동으로 어떤 외부 서버에도 업로드하지 않습니다.
- 사용자의 노트는 사용자가 직접 export, share, 외부 링크 열기, 또는 외부 앱이 동기화하는 폴더를 동기화 대상으로 선택하기 전까지 기기 안에 남습니다.

## Markleaf가 수집하지 않는 정보

다음 정보는 어떠한 형태로도 수집/전송되지 않습니다.

- 이름, 이메일, 연락처, 위치
- 계정 / 로그인 정보
- 노트 본문, 제목, 첨부 이미지
- 태그, 파일명, 메타데이터
- 사용 통계 / 텔레메트리 / 분석 이벤트
- 광고 식별자 / 기기 식별자
- 크래시 리포트

Markleaf에는 분석, 광고, 추적, 원격 설정, 폐쇄형 SDK가 포함되어 있지 않습니다.

## 네트워크

Markleaf는 `android.permission.INTERNET` 권한을 사용하지 않습니다.

```text
android.permission.INTERNET   ← Markleaf manifest에 없음
```

설치된 APK가 실제로 인터넷 권한을 요청하지 않는지는 다음 명령으로 검증할 수 있습니다.

```bash
rg "android.permission.INTERNET" -n app/src
```

## 데이터 저장 위치

- 노트 본문/제목/태그 인덱스는 앱 내부 Room 데이터베이스에 저장됩니다.
- 사용자 설정은 DataStore에 저장됩니다.
- 첨부 이미지는 앱 내부 저장소(`<filesDir>/attachments/<noteId>/`)에 복사되어 보관됩니다.

위 영역은 모두 앱 전용 내부 저장소이며 Android 권한 모델상 다른 앱이 직접 접근할 수 없습니다.

## Android 시스템 백업 정책

Markleaf는 Android 자동 백업(Android Auto Backup) 및 기기 간 전송(Device-to-Device transfer)에서 앱 데이터를 *제외* 합니다.

- `AndroidManifest.xml`의 `<application>` 요소에 `android:allowBackup="false"`가 설정되어 있습니다.
- 그 결과:
  - Android 6 이상의 Google 드라이브 자동 백업에 Markleaf 노트가 포함되지 않습니다.
  - Android 12 이상의 기기 간 전송 시에도 Markleaf 데이터가 새 기기로 자동 복제되지 않습니다.
- 의도: Markleaf가 명시적으로 보장하는 "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다"는 정책과, 일부 사용자에게 활성화되어 있을 수 있는 OS 차원의 자동 클라우드 백업이 무언의 충돌을 일으키지 않도록 하기 위함입니다.

기기 이전 또는 다중 기기 사용 시에는 아래 "사용자 주도 데이터 이동" 경로 중 하나를 직접 선택해야 합니다.

## 사용자 주도 데이터 이동

다음 동작은 *사용자가 직접 실행한 경우* 에만 일어나며, 모두 Android의 명시적 권한 다이얼로그 또는 사용자 UI 선택을 거칩니다. Markleaf는 어떤 자동 업로드/동기화도 수행하지 않습니다.

- 단일 노트 또는 전체 노트를 Markdown(`.md`)로 내보내기 — 사용자가 Storage Access Framework(SAF)로 선택한 경로에 저장.
- Android 시스템 공유 시트로 노트 텍스트 또는 `.md` 파일 공유 — 사용자가 어떤 앱으로 보낼지 선택.
- 본문 안의 외부 링크를 사용자가 탭하여 `ACTION_VIEW`로 OS/기본 브라우저에 위임 — Markleaf 자체는 네트워크 요청을 수행하지 않습니다.
- 클립보드로 복사.
- SAF 폴더 미러 동기화 — 사용자가 폴더 위치를 직접 선택하면 Markleaf가 그 폴더에 `.md` 파일을 미러링합니다. *해당 폴더가 외부 동기화 클라이언트(Drive, Dropbox, OneDrive, Syncthing, NAS 마운트 등)에 의해 동기화될지는 그 클라이언트의 책임이며*, Markleaf는 그 동기화 자체를 수행하지 않습니다.

위 경로를 사용하면 사용자가 선택한 앱/저장소를 통해 노트 데이터가 기기 밖으로 이동할 수 있습니다. 이 경로의 개인정보 처리 방침은 사용자가 선택한 해당 외부 앱/서비스의 정책을 따릅니다.

## 권한 목록

| 권한 | 용도 |
|------|------|
| `android.permission.VIBRATE` | 일부 인터랙션에서 햅틱 피드백 제공 |

다음 권한은 **선언되어 있지 않습니다**.

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_WIFI_STATE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.READ_EXTERNAL_STORAGE`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.READ_MEDIA_VIDEO`
- 위치, 마이크, 카메라, 연락처 권한

## 향후 변경

향후 동기화, 백업, API, 로그인 등 정책에 영향을 주는 기능이 추가되거나 백업 정책이 변경되는 경우 본 문서를 갱신합니다.
