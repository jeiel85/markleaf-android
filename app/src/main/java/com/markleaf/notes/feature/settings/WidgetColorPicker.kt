package com.markleaf.notes.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.widget.readableTextColorOn

/**
 * Settings → Appearance → Widget color (#469).
 *
 * Input: the stored custom colour, or null for "follow the Colors palette".
 * Output: [onChange] with the new colour, or null to go back to the palette.
 *
 * The custom choice is Android 12+ only, like Material You: tinting the
 * widgets' rounded card from a `RemoteViews` needs `setColorStateList`, which
 * is API 31. Below that the chip is shown disabled with the reason, rather
 * than accepting a choice the widgets would silently ignore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetColorSetting(
    customColor: Int?,
    onChange: (Int?) -> Unit,
    supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
) {
    var picking by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.widget_color_label),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (customColor == null) {
            Button(onClick = {}) { Text(stringResource(R.string.widget_color_app)) }
            OutlinedButton(onClick = { picking = true }, enabled = supported) {
                Text(stringResource(R.string.widget_color_custom))
            }
        } else {
            OutlinedButton(onClick = { onChange(null) }) {
                Text(stringResource(R.string.widget_color_app))
            }
            // Selected, but still tappable: it is how the colour gets changed.
            Button(onClick = { picking = true }, enabled = supported) {
                Swatch(customColor, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.widget_color_custom))
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(
            if (supported) R.string.widget_color_description
            else R.string.widget_color_requires_android_12
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (picking) {
        WidgetColorPickerDialog(
            initial = customColor ?: DEFAULT_PICKER_COLOR,
            onDismiss = { picking = false },
            onConfirm = { color ->
                picking = false
                onChange(color)
            }
        )
    }
}

/**
 * Hue / saturation / brightness sliders, a hex field and a few presets, with a
 * preview card drawn the way the widgets will draw it — including the text
 * colour [readableTextColorOn] will choose, so what you confirm is what the
 * home screen shows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetColorPickerDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var brightness by remember { mutableFloatStateOf(start[2]) }
    var hexText by remember { mutableStateOf(formatHexColor(initial)) }

    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
    fun setFrom(picked: Int) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(picked, it) }
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexText = formatHexColor(picked)
    }
    fun onSlider() {
        hexText = formatHexColor(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_color_label)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(color), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(R.string.widget_color_preview),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(readableTextColorOn(color))
                    )
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESETS.forEach { preset ->
                        val label = formatHexColor(preset)
                        Swatch(
                            preset,
                            Modifier
                                .size(32.dp)
                                .clickable { setFrom(preset) }
                                .semantics { contentDescription = label }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LabeledSlider(R.string.widget_color_hue, hue, 0f..360f) { hue = it; onSlider() }
                LabeledSlider(R.string.widget_color_saturation, saturation, 0f..1f) {
                    saturation = it; onSlider()
                }
                LabeledSlider(R.string.widget_color_brightness, brightness, 0f..1f) {
                    brightness = it; onSlider()
                }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { typed ->
                        hexText = typed
                        parseHexColor(typed)?.let { parsed ->
                            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(parsed, it) }
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    label = { Text(stringResource(R.string.widget_color_hex)) },
                    singleLine = true,
                    isError = parseHexColor(hexText) == null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) {
                Text(stringResource(R.string.widget_color_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun LabeledSlider(
    labelRes: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
}

@Composable
private fun Swatch(color: Int, modifier: Modifier) {
    Box(
        modifier
            .background(Color(color), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

/**
 * `#RRGGBB` or `RRGGBB`, any case, surrounding spaces ignored → opaque ARGB.
 * Anything else — including a 3-digit or alpha form — is null, because the
 * widget background is always opaque and the opacity is its own setting.
 */
internal fun parseHexColor(text: String): Int? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6 || !digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return (0xFF000000 or digits.toLong(16)).toInt()
}

/** Opaque ARGB → `#RRGGBB`, the form [parseHexColor] reads back. */
internal fun formatHexColor(color: Int): String = "#%06X".format(color and 0xFFFFFF)

/** Markleaf green — the widgets' own colour — so "Custom" starts from what is on screen. */
private const val DEFAULT_PICKER_COLOR = 0xFF4CAF50.toInt()

private val PRESETS = listOf(
    0xFF4CAF50, 0xFF1E88E5, 0xFF8E24AA, 0xFFD81B60,
    0xFFF4511E, 0xFFFDD835, 0xFF546E7A, 0xFF212121, 0xFFFAFAFA
).map { it.toInt() }
