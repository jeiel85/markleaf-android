package com.markleaf.notes.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * 검증까지 끝난 APK 파일을 시스템 설치 앱(Package Installer)에 넘긴다.
 *
 * **`PackageInstaller` 세션이 아니라 `ACTION_INSTALL_PACKAGE`를 쓰는 이유 (D079).** 세션
 * API로 자기 자신을 업데이트하면 시스템은 확인 팝업만 띄우고, 설치가 끝나면 이 앱의
 * 프로세스가 교체되며 조용히 사라진다 — 사용자에게는 "앱이 닫혔다"로만 보이고, 다시 열려면
 * 런처에서 아이콘을 찾아야 한다. 인텐트로 넘기면 설치 확인부터 진행 표시, 그리고 끝난 뒤의
 * "완료 / 열기" 화면까지 시스템 설치 앱이 자기 프로세스에서 그린다. 우리 프로세스가
 * 교체되어도 그 화면은 남으므로 "열기" 한 번으로 새 버전이 뜬다. 텔레그램 등 스토어 밖에서
 * 배포되는 앱이 쓰는 방식이 이것이다.
 *
 * 이 액션은 API 29에서 deprecated지만 API 36까지 시스템 설치 앱이 그대로 처리한다.
 * `ACTION_VIEW`로 바꾸지 않은 이유: APK MIME을 `ACTION_VIEW`로 여는 파일 관리자·서드파티
 * 설치 앱이 선택지에 끼어들 수 있다. "Android 자체 화면만이 설치한다"는 약속
 * (`docs/PRIVACY.md` 네트워크 절)을 지키려면 설치 전용 액션이 맞다.
 *
 * **여기서 하지 않는 것.** 서명이 다른 APK를 앱이 먼저 걸러내려는 시도는 하지 않는다.
 * `getPackageArchiveInfo`로 서명을 미리 읽는 것이 API 26~35에서 어떻게 채워지는지 실기기
 * 확인이 안 된 상태라, 잘못 구현한 사전 검증이 다른 키로 서명된 정상 업데이트까지 막는
 * 쪽이 더 나쁘다고 봤다. 다른 키의 APK는 결국 OS가 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`로
 * 거부한다 — 늦지만 확실한 방어다.
 */
internal object UpdateInstaller {

    /**
     * Input : Activity 컨텍스트, 검증된 APK 파일
     * Output: 시스템 설치 앱을 띄우는 데 성공했는가(설치 자체의 최종 성공 여부가 아니다)
     *
     * 핵심 로직: 파일을 이 앱의 `FileProvider` URI로 바꾸고, 그 URI 하나에만 읽기 권한을
     * 붙여 설치 앱에 넘긴다. 파일을 여기서 지우지 **않는다** — 설치 앱은 이 호출이 돌아온
     * 뒤에 URI를 읽는다. 남은 파일은 다음 실행 때 [deleteLeftovers]가 치운다. 띄우지 못했으면
     * 읽을 쪽이 없으므로 그 자리에서 지운다.
     *
     * Activity 컨텍스트로 부르는 이유: 설치 앱이 이 앱의 태스크 위에 뜨고, 호출자를 이 앱으로
     * 식별해 "출처를 알 수 없는 앱" 허용 여부를 이 앱 기준으로 판단한다. 메인 스레드에서 부른다.
     */
    fun install(context: Context, apkFile: File): Boolean {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        } catch (_: IllegalArgumentException) {
            // file_paths.xml에 없는 경로 — 코드와 설정이 어긋난 것이다. 넘길 방법이 없다.
            apkFile.delete()
            return false
        }

        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            // 시스템 설치 앱이 없거나 비활성화된 기기. 호출부가 "브라우저에서 열기"를 제시한다.
            apkFile.delete()
            false
        }
    }

    /**
     * Input : 앱 캐시 아래 업데이트 디렉터리
     * Output: 없음
     *
     * 지난번 [install]이 설치 앱에 넘기고 남겨 둔 APK를 지운다. 다운로드를 시작하기 전과
     * 배너가 뜰 때 부른다 — 설치가 끝나 새 버전으로 다시 시작된 경우든, 사용자가 설치를
     * 취소한 경우든 그 파일은 더 쓸 데가 없다. IO 스레드에서 부른다.
     */
    fun deleteLeftovers(downloadDir: File) {
        downloadDir.listFiles()?.forEach { it.delete() }
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}
