package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.markleaf.notes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopAppBar(
    title: String,
    isPreviewMode: Boolean,
    isFocusMode: Boolean,
    showMore: Boolean,
    moreExpanded: Boolean,
    onBack: () -> Unit,
    onTogglePreview: () -> Unit,
    onExitFocusMode: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenMore: () -> Unit,
    onDismissMore: () -> Unit,
    moreMenuContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
        },
        actions = {
            if (isFocusMode) {
                IconButton(onClick = onExitFocusMode) {
                    Icon(
                        Icons.Default.CenterFocusWeak,
                        contentDescription = stringResource(R.string.exit_focus_mode)
                    )
                }
            } else {
                IconButton(onClick = onTogglePreview) {
                    Icon(
                        imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (isPreviewMode) R.string.edit else R.string.preview
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onOpenInfo) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.note_information)
                    )
                }
                if (showMore) {
                    Box {
                        IconButton(onClick = onOpenMore) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = onDismissMore,
                            content = moreMenuContent
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}
