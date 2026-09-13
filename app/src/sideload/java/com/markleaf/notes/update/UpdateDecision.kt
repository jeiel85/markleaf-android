package com.markleaf.notes.update

import java.util.concurrent.TimeUnit

/**
 * "확인할 때인가"와 "알릴 만한가"를 판단한다. 순수 함수만 두는 이유는 이 두 판단이 업데이트
 * 기능에서 **틀리면 가장 사용자에게 거슬리는 부분**이기 때문이다 — 너무 자주 확인하면 배경
 * 트래픽이 되고, 판단이 느슨하면 이미 최신인 사용자에게 배너를 띄운다. 네트워크와 저장소를
 * 섞지 않아야 그 규칙을 테스트로 고정할 수 있다.
 *
 * 상태(마지막 확인 시각, 건너뛴 버전)는 인자로 받는다. 저장은 호출부의 몫이다.
 */
internal object UpdateDecision {

    val CHECK_INTERVAL_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /**
     * Input : 마지막으로 확인에 성공한 시각, 현재 시각(둘 다 epoch millis), 확인 간격
     * Output: 지금 네트워크 확인을 해도 되는지
     *
     * 핵심 로직: 마지막 확인이 없으면(0) 확인한다. 그 외에는 간격이 지나야 한다.
     *
     * 미래 시각을 받은 경우도 확인을 허용한다 — 기기 시계가 앞당겨졌다가 돌아오면
     * `now - lastCheckedAt`이 음수가 되어 **영원히 확인하지 않는 상태**에 갇힐 수 있다.
     * 사용자가 시계를 고쳤을 때 기능이 조용히 죽어 있는 것보다 한 번 더 확인하는 편이 낫다.
     */
    fun shouldCheckNow(
        lastCheckedAt: Long,
        now: Long,
        interval: Long = CHECK_INTERVAL_MILLIS,
    ): Boolean {
        if (lastCheckedAt <= 0L) return true
        if (lastCheckedAt > now) return true
        return now - lastCheckedAt >= interval
    }

    /**
     * Input : 받아온 manifest, 설치된 versionCode, 사용자가 건너뛴 versionCode(없으면 0),
     *         기기의 SDK 레벨
     * Output: 배너를 띄울 만한 새 버전인지
     *
     * 핵심 로직: `versionCode` **정수 비교**만 쓴다. `versionName` 문자열 비교는
     * `2.9.0`을 `2.10.0`보다 크게 만든다.
     *
     * 같거나 낮은 versionCode에서 멈추는 것은 개발 빌드를 위한 것이기도 하다 — 로컬에서 올린
     * 버전을 쓰는 사람에게 "구버전으로 업데이트하세요"를 띄우지 않는다.
     *
     * `minSdk` 확인은 설치가 실패할 것을 미리 아는 경우를 거른다. 받으라고 안내한 뒤
     * 시스템이 거부하는 흐름은 사용자가 원인을 알 수 없다.
     */
    fun shouldOffer(
        manifest: UpdateManifest,
        installedVersionCode: Int,
        skippedVersionCode: Int,
        deviceSdk: Int,
    ): Boolean {
        if (manifest.versionCode <= installedVersionCode) return false
        if (manifest.versionCode == skippedVersionCode) return false
        if (deviceSdk < manifest.minSdk) return false
        return true
    }
}
