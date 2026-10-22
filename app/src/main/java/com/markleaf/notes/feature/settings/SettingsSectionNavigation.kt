package com.markleaf.notes.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import kotlin.math.roundToInt

/**
 * The categories of Settings, in list order: one row each, one page each.
 *
 * [groups] are the headings inside the page, in page order. Search reads them
 * too, so "Lock" finds Privacy & security and "Export" finds Sync & export.
 */
internal enum class SettingsCategory(@StringRes val label: Int, @StringRes val groups: List<Int>) {
    APPEARANCE(R.string.settings_appearance, listOf(R.string.settings_group_widget)),
    EDITOR(R.string.settings_editor, listOf(R.string.settings_group_text, R.string.settings_markdown)),
    NOTES(
        R.string.settings_notes,
        listOf(R.string.settings_group_list, R.string.settings_group_opening, R.string.settings_group_new_notes)
    ),
    PRIVACY(R.string.settings_privacy_security, listOf(R.string.settings_group_lock, R.string.settings_group_screen)),
    SYNC(R.string.settings_sync_export, listOf(R.string.sync_title, R.string.settings_group_export)),
    ABOUT(R.string.settings_about, listOf(R.string.settings_app, R.string.settings_open_source))
}

/**
 * Where each setting sits on its page, so a search result can scroll it to
 * the top. Positions are read when the page lands rather than kept as
 * offsets: a setting's place changes with the page's state (a sync folder
 * chosen, a custom font picked).
 */
internal class SectionAnchors {
    var viewport: LayoutCoordinates? = null
    private val items = mutableMapOf<SettingsItem, LayoutCoordinates>()
    private val itemFocus = SettingsItem.entries.associateWith { FocusRequester() }

    /**
     * Marks where a search result for [item] lands, and what takes focus there.
     *
     * Input: the setting; [focusable] false when the modifier goes on a control
     * that is focusable already (a button), so it is not made a second stop.
     * Output: a modifier for the setting's label.
     *
     * Why the label takes focus: after a jump, TalkBack and a keyboard should
     * carry on from the setting that was asked for, the same reason opening a
     * page focuses its title (#262).
     */
    fun item(item: SettingsItem, focusable: Boolean = true): Modifier {
        val base = Modifier
            .onPlaced { items[item] = it }
            .focusRequester(itemFocus.getValue(item))
        return if (focusable) base.focusable() else base
    }

    fun focus(item: SettingsItem): FocusRequester = itemFocus.getValue(item)

    /** The scroll value that puts [item]'s label [gapPx] below the top, or null before layout. */
    fun scrollTarget(item: SettingsItem, scroll: Int, gapPx: Float): Int? {
        val viewport = viewport?.takeIf { it.isAttached } ?: return null
        val placed = items[item]?.takeIf { it.isAttached } ?: return null
        val top = viewport.localPositionOf(placed, Offset.Zero).y
        return (scroll + top - gapPx).roundToInt().coerceAtLeast(0)
    }
}

/**
 * The query field at the top of the section list.
 *
 * Why it does not take focus on its own: it is on screen every time Settings
 * opens, and most visits are for a row below it, so raising the keyboard
 * would cover the list for nothing.
 */
@Composable
internal fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        placeholder = { Text(stringResource(R.string.settings_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        singleLine = true
    )
}

/** What a query found, in place of the section list, each row naming its section. */
@Composable
internal fun SettingsSearchResults(
    results: List<SettingsSearchEntry>,
    onPick: (SettingsSearchEntry) -> Unit,
    sectionTitle: @Composable (SettingsCategory) -> String,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.item?.name ?: it.section.name }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(entry) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        // A section result is its own heading; naming it twice
                        // says nothing.
                        if (entry.item != null) {
                            Text(
                                text = sectionTitle(entry.section),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
