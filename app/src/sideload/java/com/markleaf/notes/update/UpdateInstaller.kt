package com.markleaf.notes.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File

/**
 * 검증까지 끝난 APK 파일을 `PackageInstaller` 세션에 넘긴다.
 *
 * `Intent.ACTION_INSTALL_PACKAGE`를 쓰지 않는 이유: API 29에서 deprecated고, 세션 API가
 * 시스템 설치 확인 팝업으로 이어지는 상태(`STATUS_PENDING_USER_ACTION`)를 명시적인 콜백으로
 * 준다(`docs/UPDATE_STRATEGY_EVALUATION.md` "C단계 구현 세부"). 그 콜백을 받아 팝업을 실제로
 * 띄우는 쪽은 [UpdateInstallReceiver]다 — `session.commit()`은 즉시 결과를 돌려주지 않고
 * `PendingIntent`로 비동기 콜백하므로, 여기서는 세션을 열고 커밋하는 데까지만 책임진다.
 *
 * 호출부가 IO 디스패처에서 부른다(`UpdateDownloader`와 같은 관례) — 파일 전체를 세션에
 * 복사하는 블로킹 I/O이기 때문이다.
 *
 * **여기서 하지 않는 것.** 서명이 다른 APK를 앱이 먼저 걸러내려는 시도는 하지 않는다.
 * `getPackageArchiveInfo`로 서명을 미리 읽는 것이 API 26~35에서 어떻게 채워지는지 실기기
 * 확인이 안 된 상태라, 잘못 구현한 사전 검증이 다른 키로 서명된 정상 업데이트까지 막는
 * 쪽이 더 나쁘다고 봤다. 다른 키의 APK는 결국 OS가 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`로
 * 거부한다 — 늦지만 확실한 방어다.
 */
internal object UpdateInstaller {

    /**
     * Input : 검증된 APK 파일
     * Output: 시스템에 설치 절차를 넘기는 데 성공했는가(설치 자체의 최종 성공 여부가 아니다)
     *
     * 핵심 로직: 세션을 만들고, 파일을 그대로 복사하고, 커밋한다. 무엇이 실패하든 세션을
     * 정리하고(`abandonSession`) 파일을 지운다 — 이 파일은 이 함수가 마지막 소비자이므로,
     * 성공하든 실패하든 캐시에 계속 남아 있을 이유가 없다.
     */
    fun install(context: Context, apkFile: File): Boolean {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionId = try {
            packageInstaller.createSession(
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            )
        } catch (_: Exception) {
            apkFile.delete()
            return false
        }

        return try {
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite(SESSION_NAME, 0, apkFile.length()).use { sessionOutput ->
                    apkFile.inputStream().use { input -> input.copyTo(sessionOutput) }
                    session.fsync(sessionOutput)
                }
                session.commit(createStatusIntentSender(context, sessionId))
            }
            true
        } catch (_: Exception) {
            runCatching { packageInstaller.abandonSession(sessionId) }
            false
        } finally {
            apkFile.delete()
        }
    }

    private fun createStatusIntentSender(context: Context, sessionId: Int) =
        PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(context, UpdateInstallReceiver::class.java),
            // FLAG_MUTABLE: 시스템이 이 PendingIntent를 발사할 때 EXTRA_STATUS/EXTRA_INTENT를
            // 채워 넣어야 한다. minSdk(26)는 이 플래그가 API 31에서 생기기 전이라 그때는
            // 기본값이 이미 mutable이었으므로 아무 영향이 없고, 31 이상에서는 이 플래그가
            // 없으면 시스템이 이 요청 자체를 거부한다.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        ).intentSender

    private const val SESSION_NAME = "markleaf-update"
}
