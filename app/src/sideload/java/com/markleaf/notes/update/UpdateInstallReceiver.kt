package com.markleaf.notes.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/**
 * `PackageInstaller` 세션의 커밋 결과를 받는다. 매니페스트에 등록되며(사이드로드 전용,
 * `AndroidManifest-sideload.xml`), intent-filter가 없다 — [UpdateInstaller]가 이 컴포넌트를
 * 명시적 `Intent`로만 가리키고, 시스템 액션 문자열로 불릴 일이 없다.
 *
 * `STATUS_PENDING_USER_ACTION`만 처리한다. 그 값이 바로 "시스템 설치 확인 팝업을 띄워도
 * 좋다"는 뜻이고, 애초 요청("다운로드에서 실행까지 해주면 안드로이드 자체에서 팝업으로
 * 업데이트 여부를 선택")이 요구하는 지점이 정확히 여기다. 사용자가 그 팝업에서 설치·거부를
 * 고르는 것부터는 시스템 UI의 몫이라 이 앱이 더 알 필요가 없다 — 그래서 다른 상태값
 * (`STATUS_SUCCESS`, 실패 코드들)은 앱에서 따로 다루지 않는다.
 */
internal class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) return

        val confirmIntent = intent.confirmationIntentExtra() ?: return
        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // 브로드캐스트 리시버에는 보여줄 화면이 없다 — 실패해도 조용히 넘어간다. 사용자는
        // 배너를 다시 눌러 처음부터 다시 시도할 수 있다.
        runCatching { context.startActivity(confirmIntent) }
    }

    /** API 33부터 타입 지정 오버로드가 생겼다 — 그 전에는 캐스팅 경고를 감수한 옛 API를 쓴다. */
    @Suppress("DEPRECATION")
    private fun Intent.confirmationIntentExtra(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_INTENT)
        }
}
