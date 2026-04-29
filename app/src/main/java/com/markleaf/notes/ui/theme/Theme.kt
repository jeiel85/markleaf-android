package com.markleaf.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF5C6BC0),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E2FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001A41),
    secondary = androidx.compose.ui.graphics.Color(0xFF575E71),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFDADFE8),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF141726),
    background = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surface = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFACD2FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002F64),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0D459E),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD8E2FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFBEC4D7),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF2A2F3F),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF404659),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFDADFE8),
    background = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE2E2E6),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE2E2E6),
)

@Composable
fun MarkleafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
