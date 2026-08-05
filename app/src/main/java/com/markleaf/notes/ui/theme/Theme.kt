package com.markleaf.notes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Internal rather than private so `EditorColorContrastTest` can assert the
 *  editor's colours against the backgrounds they are actually drawn on. */
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF003300),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF111F0F),
    tertiary = Color(0xFF38656A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF0),
    onTertiaryContainer = Color(0xFF002023),
    background = Color(0xFFF9FBF9),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFF9FBF9),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B38),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFA0CFD4),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D52),
    onTertiaryContainer = Color(0xFFBCEBF0),
    background = Color(0xFF191C19),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF191C19),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
)

val Typography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
).withContentTextDirection()

/**
 * Returns a copy of this type scale that lays each paragraph out based on its
 * own content: a paragraph starting with a strong right-to-left character
 * (Arabic, Hebrew, Persian, …) becomes right-to-left even while the app UI
 * stays left-to-right, and Latin-first paragraphs are unchanged. Applied once
 * to the whole scale so both the editor and the Markdown preview honour the
 * direction of what you actually typed (#146). Left-to-right text renders
 * identically, so existing snapshots are unaffected.
 */
private fun androidx.compose.material3.Typography.withContentTextDirection():
    androidx.compose.material3.Typography = copy(
    displayLarge = displayLarge.copy(textDirection = TextDirection.Content),
    displayMedium = displayMedium.copy(textDirection = TextDirection.Content),
    displaySmall = displaySmall.copy(textDirection = TextDirection.Content),
    headlineLarge = headlineLarge.copy(textDirection = TextDirection.Content),
    headlineMedium = headlineMedium.copy(textDirection = TextDirection.Content),
    headlineSmall = headlineSmall.copy(textDirection = TextDirection.Content),
    titleLarge = titleLarge.copy(textDirection = TextDirection.Content),
    titleMedium = titleMedium.copy(textDirection = TextDirection.Content),
    titleSmall = titleSmall.copy(textDirection = TextDirection.Content),
    bodyLarge = bodyLarge.copy(textDirection = TextDirection.Content),
    bodyMedium = bodyMedium.copy(textDirection = TextDirection.Content),
    bodySmall = bodySmall.copy(textDirection = TextDirection.Content),
    labelLarge = labelLarge.copy(textDirection = TextDirection.Content),
    labelMedium = labelMedium.copy(textDirection = TextDirection.Content),
    labelSmall = labelSmall.copy(textDirection = TextDirection.Content),
)

/**
 * Returns a copy of this type scale with [family] applied to every text role.
 * Used to switch the whole writing surface to a serif face. Code blocks keep
 * their explicit [FontFamily.Monospace] because they set it at the render site,
 * not via the theme default.
 */
private fun androidx.compose.material3.Typography.withFontFamily(
    family: FontFamily
): androidx.compose.material3.Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

@Composable
fun MarkleafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    useSerif: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // Default (sans) keeps the original Typography untouched — zero rendering
    // change for existing users; only the serif option swaps the family.
    val typography = if (useSerif) Typography.withFontFamily(FontFamily.Serif) else Typography

    // Keep the system status / nav bar icon appearance in sync with the
    // composable theme. enableEdgeToEdge() detects light vs dark only at
    // activity start, so a runtime theme switch (e.g. system dark mode flips,
    // or the user toggles between Markleaf green and Material You) used to
    // leave the status bar icons stale — visible on the wrong background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
