package com.markleaf.notes.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures Markleaf cold/warm/hot startup time. Run on a real device or
 * emulator (Macrobenchmark does not work under Robolectric):
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * The TimeToInitialDisplay metric tells us how long it takes from app
 * launch until the first frame is drawn — that's what the user actually
 * perceives as "the app opened." We want this <500ms cold for §2.1.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = startup(StartupMode.COLD)

    @Test
    fun startupWarm() = startup(StartupMode.WARM)

    @Test
    fun startupHot() = startup(StartupMode.HOT)

    private fun startup(mode: StartupMode) {
        benchmarkRule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = mode,
            setupBlock = { pressHome() }
        ) {
            startActivityAndWait()
        }
    }

    companion object {
        const val APP_PACKAGE = "com.markleaf.notes"
    }
}
