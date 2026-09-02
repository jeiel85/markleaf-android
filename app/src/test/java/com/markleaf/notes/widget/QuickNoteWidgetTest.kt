package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * That building the widget's views runs to completion.
 *
 * A smoke test, and honest about its reach: `ShadowPendingIntent` records flags
 * without running the platform's checks, so this passes even with the
 * FLAG_IMMUTABLE + FLAG_MUTABLE pair that crashed the receiver for 44 releases.
 * [com.markleaf.notes.PendingIntentFlagsTest] is what catches that; this covers
 * the rest of the method, which nothing else exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickNoteWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `building the widget views does not throw`() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = shadowOf(manager)
            .createWidgets(QuickNoteWidget::class.java, R.layout.widget_quick_note, 1)

        QuickNoteWidget.updateAppWidget(context, manager, ids.first())
    }
}
