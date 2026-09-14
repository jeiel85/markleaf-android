package com.markleaf.notes.update

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 앱이 읽는 주소와 릴리스 워크플로가 올리는 자산 이름이 같은지 고정한다.
 *
 * 왜 이 테스트가 있는가: 이 두 사실은 서로 다른 파일에 각각 적혀 있고, 어긋났을 때의 증상이
 * **아무 증상도 아니다.** 자산 이름이 바뀌면 앱의 GET 이 404 를 받고, `UpdateChecker` 는 모든
 * 실패를 조용히 null 로 수렴시키므로(그건 의도된 설계다) 배너는 그냥 영원히 뜨지 않는다.
 * 아무 빌드도 빨개지지 않고, 사용자도 "업데이트가 없구나"로 읽는다. 사람이 두 파일을 대조하는
 * 규칙은 잊히므로 검사로 고정한다 — `SideloadManifestParityTest` 와 같은 이유, 같은 형식이다.
 *
 * 게이트 뒤(`src/sideloadTest/java`)에 있으므로 `-Pmarkleaf.updater=true` 로 돌릴 때만
 * 컴파일·실행된다(D074).
 */
class UpdateManifestUrlParityTest {

    @Test
    fun defaultManifestUrlPointsAtAnAssetTheReleaseWorkflowUploads() {
        // Input : 워크플로의 `gh release create` 파일 인자, UpdateChecker.DEFAULT_MANIFEST_URL
        // Output: 앱이 읽는 파일 이름이 업로드 목록에 없으면 실패
        // 핵심 로직: URL 의 마지막 경로 조각이 곧 자산 이름이다. 그 이름이 올라가지 않으면 404.
        val asset = UpdateChecker.DEFAULT_MANIFEST_URL.substringAfterLast('/')

        assertTrue(
            "UpdateChecker 가 읽는 '$asset' 을 릴리스 워크플로가 올리지 않는다. " +
                "업로드하는 자산: ${releaseAssetArgs()}",
            releaseAssetArgs().contains(asset)
        )
    }

    @Test
    fun defaultManifestUrlIsTheVersionIndependentPermalink() {
        // `releases/latest/download/<이름>` 은 가장 최근 릴리스의 같은 이름 자산으로 302 를
        // 돌려주는 고정 주소다(D075). 여기에 버전이 박히면 그 버전을 쓰는 사람만 업데이트를
        // 보게 되고, 정작 알림이 필요한 오래된 설치가 영영 못 본다.
        val url = UpdateChecker.DEFAULT_MANIFEST_URL

        assertTrue("manifest URL 은 https 여야 한다: $url", url.startsWith("https://github.com/"))
        assertTrue(
            "manifest URL 은 버전 독립 고정 주소여야 한다(releases/latest/download/): $url",
            url.contains("/releases/latest/download/")
        )
        assertTrue(
            "manifest URL 에 버전이 박혀 있다: $url",
            !url.contains("/releases/download/")
        )
    }

    @Test
    fun theApkUrlInTheManifestNamesAnAssetTheReleaseWorkflowUploads() {
        // manifest 안의 apkUrl 과 실제 업로드 이름이 어긋나면 증상은 한 단계 뒤로 밀린다:
        // 배너는 뜨는데 눌렀을 때 404 가 열린다. 그쪽이 더 나쁘다 — 사용자가 무언가 잘못됐다고
        // 느끼지만 무엇이 잘못됐는지 알 수 없다.
        val line = workflow.lines().first { it.contains("MANIFEST_APK_URL=") }
        val asset = line.substringAfterLast('/').substringBefore('"').replace(REF_NAME, TAG)

        assertTrue(
            "manifest 의 apkUrl 이 가리키는 '$asset' 을 릴리스 워크플로가 올리지 않는다. " +
                "업로드하는 자산: ${releaseAssetArgs()}",
            releaseAssetArgs().contains(asset)
        )
    }

    /**
     * Input : 없음 (워크플로 본문)
     * Output: `gh release create` 에 넘어가는 파일 인자들. 태그 자리표시자는 `vX.Y.Z` 로 정규화.
     *
     * 핵심 로직: `gh release create` 와 첫 `--` 옵션 사이의 큰따옴표 토큰이 파일 인자다.
     * `substringAfterLast` 인 이유는 같은 문자열이 이 워크플로의 주석에 세 번 더 나오기
     * 때문이다 — 실제 호출은 언제나 마지막 것이다.
     * 첫 토큰은 태그이므로 뺀다. `scripts/verify-release-assets.ps1` 이 쓰는 규칙과 같은 규칙이고,
     * 여기서는 목록 자체가 아니라 **앱이 아는 이름이 그 목록에 있는지**를 본다.
     */
    private fun releaseAssetArgs(): List<String> =
        Regex("\"([^\"]+)\"")
            .findAll(workflow.substringAfterLast("gh release create").substringBefore("--repo"))
            .map { it.groupValues[1] }
            .filterNot { it == "\$GITHUB_REF_NAME" }
            .map { it.replace(REF_NAME, TAG) }
            .toList()

    private companion object {
        // 단위 테스트의 작업 디렉터리가 app 모듈이다 (SideloadManifestParityTest 와 같다).
        val workflow: String = File("../.github/workflows/android-build.yml").readText()
        const val REF_NAME = "\${GITHUB_REF_NAME}"
        const val TAG = "vX.Y.Z"
    }
}
