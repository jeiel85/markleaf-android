package com.markleaf.notes.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsUseVisibleMarkdownAndComfortableLineWidth() {
        val settings = AppSettings()

        assertEquals(MarkdownSyntaxVisibility.SHOW, settings.markdownSyntaxVisibility)
        assertEquals(EditorLineWidth.COMFORTABLE, settings.lineWidth)
        assertEquals(800, settings.lineWidth.maxWidthDp)
    }
}
