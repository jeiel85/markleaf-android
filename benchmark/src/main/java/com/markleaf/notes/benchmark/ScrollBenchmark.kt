package com.markleaf.notes.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures scroll jank in the notes list. Run on a real device or emulator
 * (Macrobenchmark does not work under Robolectric):
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * The fling is intentionally vigorous to surface jank. 90th-percentile
 * frame duration above the device's frame budget is a regression.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollNotesList() {
        benchmarkRule.measureRepeated(
            packageName = StartupBenchmark.APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.waitForIdle()
            }
        ) {
            val list = device.findObject(By.scrollable(true))
            if (list != null) {
                list.setGestureMargin(device.displayWidth / 5)
                list.fling(Direction.DOWN)
                device.waitForIdle()
                list.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }
}
