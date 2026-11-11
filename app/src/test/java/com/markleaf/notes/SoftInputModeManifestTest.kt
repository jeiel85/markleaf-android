package com.markleaf.notes

import android.content.ComponentName
import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The editor keeps its text and toolbar above the keyboard with imePadding().
 * On Android 10 and below that padding only follows the keyboard when the
 * activity resizes for it (#578): left unspecified, the toolbar went under the
 * keyboard, and on a Huawei the padding outlived the keyboard and left half the
 * screen empty. The mode is a manifest attribute, so nothing else would notice
 * it being dropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SoftInputModeManifestTest {

    @Test
    fun mainActivityResizesForTheKeyboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )
        assertEquals(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            info.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST,
        )
    }
}
