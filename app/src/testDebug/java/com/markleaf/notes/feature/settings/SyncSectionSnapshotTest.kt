package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.data.settings.SyncMetadataMode
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens for the sync settings block (#255).
 *
 * The full-screen Settings capture stops at one viewport — deliberately, so
 * `BuildConfig.VERSION_NAME` in the App section never enters an image — which
 * left everything below the fold visually ungated. This section is the part
 * that gap actually mattered for: its shape depends on whether a folder is
 * chosen and which metadata mode is on (#216), and neither state can be
 * reached from outside the screen.
 *
 * Rendered on its own rather than scrolled to, so the framing does not move
 * when a section above it changes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w800dp-h1200dp-mdpi")
class SyncSectionSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Sync off: the metadata control is hidden, because there is no folder to convert. */
    @Test
    fun noFolderChosen() = snapshot("settings_sync_no_folder", folderUri = null)

    @Test
    fun frontmatterMode() = snapshot("settings_sync_frontmatter")

    /** Sidecar adds the trade-offs paragraph — the costs must stay visible. */
    @Test
    fun sidecarMode() =
        snapshot("settings_sync_sidecar", metadataMode = SyncMetadataMode.SIDECAR)

    /** Mid-conversion: both choices disabled so a second switch cannot start. */
    @Test
    fun switchInProgress() = snapshot("settings_sync_switching", busy = true)

    @Test
    @Config(sdk = [33], qualifiers = "ko-rKR-w800dp-h1200dp-notnight-mdpi")
    fun sidecarKorean() =
        snapshot("settings_sync_sidecar_korean", metadataMode = SyncMetadataMode.SIDECAR)

    @Test
    @Config(sdk = [33], qualifiers = "w800dp-h1200dp-night-mdpi")
    fun sidecarDark() = snapshot(
        "settings_sync_sidecar_dark",
        metadataMode = SyncMetadataMode.SIDECAR,
        darkTheme = true
    )

    @Test
    fun sidecarLargeText() = snapshot(
        "settings_sync_sidecar_large_text",
        metadataMode = SyncMetadataMode.SIDECAR,
        fontScale = 1.5f
    )

    private fun snapshot(
        name: String,
        folderUri: String? = SAMPLE_FOLDER,
        metadataMode: SyncMetadataMode = SyncMetadataMode.FRONTMATTER,
        busy: Boolean = false,
        darkTheme: Boolean = false,
        fontScale: Float = 1f
    ) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(TAG),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SyncSectionHost {
                            SyncSection(
                                folderUri = folderUri,
                                lastSyncedAt = null,
                                metadataMode = metadataMode,
                                metadataBusy = busy,
                                onMetadataModeChange = {},
                                onPickFolder = {},
                                onSyncNow = {},
                                onStopSync = {},
                                onSyncCenterClick = {}
                            )
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithTag(TAG).captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }

    /** The 640dp centred column the real screen puts its sections in (#154). */
    @Composable
    private fun SyncSectionHost(content: @Composable () -> Unit) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                content()
            }
        }
    }

    private companion object {
        const val TAG = "syncSectionSurface"
        const val SAMPLE_FOLDER =
            "content://com.android.externalstorage.documents/tree/primary%3ANextcloud%2FNotes"
    }

    // `lastSyncedAt` is always null here. The screen renders it through
    // `formatRelative`, which measures against `System.currentTimeMillis()` —
    // any fixed value drifts into a different bucket and would fail the golden
    // some time after it was recorded. That helper also returns hardcoded
    // Korean in every language, so a golden built on it would freeze that in
    // place rather than surface it; it is reported separately instead.
}
