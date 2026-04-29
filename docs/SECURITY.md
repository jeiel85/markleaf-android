# Markleaf Security Notes

## MVP Security Scope

MVP는 네트워크 기능이 없는 로컬 우선 앱이다.

## 금지

- API key 저장
- secret token 저장
- 사용자 계정 정보 저장
- 원격 서버 업로드
- 백그라운드 네트워크 통신
- analytics SDK
- tracking SDK
- ads SDK

## 데이터 보호

- 노트는 로컬 DB에 저장한다.
- 사용자의 명시적 export/share 없이는 외부로 보내지 않는다.
- 삭제는 우선 휴지통으로 이동한다.
- 영구 삭제에는 확인 절차가 필요하다.

## 향후 검토

다음 기능을 추가하기 전에는 별도 보안 검토가 필요하다.

- 클라우드 동기화
- 백업
- 계정 로그인
- WebDAV
- Google Drive
- 암호화
- 생체 인증
- 이미지 첨부
