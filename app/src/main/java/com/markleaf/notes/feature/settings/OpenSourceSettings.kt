package com.markleaf.notes.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import androidx.compose.ui.platform.LocalContext

/** The Open source group on the About page: the licence and where the code and builds live. */
@Composable
internal fun OpenSourceSettings(
    anchors: SectionAnchors
) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.oss_explainer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    SettingLine(
        stringResource(R.string.oss_license_label) + ": " +
            stringResource(R.string.oss_license_value)
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.oss_source_url))
                        )
                    )
                }
            },
            modifier = anchors.item(SettingsItem.VIEW_SOURCE, focusable = false).weight(1f)
        ) {
            Text(stringResource(R.string.oss_view_source))
        }
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.oss_fdroid_url))
                        )
                    )
                }
            },
            modifier = anchors.item(SettingsItem.VIEW_FDROID, focusable = false).weight(1f)
        ) {
            Text(stringResource(R.string.oss_view_fdroid))
        }
    }
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.oss_license_url))
                    )
                )
            }
        },
        modifier = anchors.item(SettingsItem.VIEW_LICENSE, focusable = false).fillMaxWidth()
    ) {
        Text(stringResource(R.string.oss_view_license))
    }
}
