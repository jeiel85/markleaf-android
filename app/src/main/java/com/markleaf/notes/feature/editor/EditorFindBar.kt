package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

@Composable
internal fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentIndex: Int,
    totalMatches: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.find_in_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (totalMatches == 0) "0/0"
                    else "${currentIndex + 1}/$totalMatches",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onPrev, enabled = totalMatches > 0) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.find_previous_match)
                    )
                }
                IconButton(onClick = onNext, enabled = totalMatches > 0) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.find_next_match)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text(stringResource(R.string.replace_in_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onReplaceOne,
                    enabled = totalMatches > 0
                ) {
                    Text(stringResource(R.string.replace_match))
                }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = totalMatches > 0
                ) {
                    Text(stringResource(R.string.replace_all_matches))
                }
            }
        }
    }
}
