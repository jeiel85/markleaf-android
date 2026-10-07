package com.markleaf.notes.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

/**
 * One line above the note list while conflict copies are waiting, and nothing
 * at all when none are (#434).
 *
 * A copy is made by the folder reconcile, in the background, when the app
 * comes back to the foreground — so until now the only way to learn one
 * existed was to spot "(copy from another device …)" in the list. This says so
 * where the list is, and takes you to the Conflict Center. It doesn't open that
 * screen by itself: a conflict is not what the user came back to do, the same
 * rule the update banner beside it follows.
 *
 * It stays while the copies do. A copy is resolved by deleting it, so there is
 * no separate "seen" state to drift out of step with the list it counts.
 */
@Composable
internal fun ConflictCopiesBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    // `Modifier.clickable` rather than `Surface(onClick = ...)`, as in the
    // update banner: same target and shape, without the experimental overload.
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.conflict_copies_banner, count, count),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
