package com.markleaf.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E7D32), // 브랜딩 컬러 반영 (Green)
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFC8E6C9),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003300),
    secondary = androidx.compose.ui.graphics.Color(0xFF52634F),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD5E8CF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF111F0F),
    background = androidx.compose.ui.graphics.Color(0xFFF9FBF9),
    onBackground = androidx.compose.ui.graphics.Color(0xFF191C19),
    surface = androidx.compose.ui.graphics.Color(0xFFF9FBF9),
    onSurface = androidx.compose.ui.graphics.Color(0xFF191C19),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDEE5D9),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF424940),
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF81C784),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00390A),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1B5E20),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFC8E6C9),
    secondary = androidx.compose.ui.graphics.Color(0xFFB9CCB4),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF253423),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3B4B38),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD5E8CF),
    background = androidx.compose.ui.graphics.Color(0xFF191C19),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1E3DF),
    surface = androidx.compose.ui.graphics.Color(0xFF191C19),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1E3DF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF424940),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC2C9BD),
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
