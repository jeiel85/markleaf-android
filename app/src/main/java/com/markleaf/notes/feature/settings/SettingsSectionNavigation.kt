package com.markleaf.notes.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextField
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusable
import androidx.compose.material3.SuggestionChip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

/** The sections the shortcut row jumps to, in page order (#517). */
internal enum class SettingsShortcut(@StringRes val label: Int) {
    APPEARANCE(R.string.settings_appearance),
    MARKDOWN(R.string.settings_markdown),
    NOTES(R.string.settings_notes_section),
    PRIVACY(R.string.settings_privacy),
    DATA(R.string.settings_data),
    // The section's own heading is a phrase; a chip needs a word.
    SYNC(R.string.settings_shortcut_sync),
    OPEN_SOURCE(R.string.settings_open_source),
    APP(R.string.settings_app)
}

/**
 * Where each section sits, so a shortcut can scroll its heading to the top.
 * Positions are read when a shortcut is tapped rather than kept as offsets: a
 * section's height changes with its state (a sync folder chosen, a passcode
 * set), and every section below it moves.
 */
internal class SectionAnchors {
    var viewport: LayoutCoordinates? = null
    private val sections = mutableMapOf<SettingsShortcut, LayoutCoordinates>()
    private val headings = SettingsShortcut.entries.associateWith { FocusRequester() }
    private val items = mutableMapOf<SettingsItem, LayoutCoordinates>()
    private val itemFocus = SettingsItem.entries.associateWith { FocusRequester() }

    fun anchor(section: SettingsShortcut): Modifier = Modifier.onPlaced { sections[section] = it }

    /**
     * Marks where a search result for [item] lands, and what takes focus there.
     *
     * Input: the setting; [focusable] false when the modifier goes on a control
     * that is focusable already (a button), so it is not made a second stop.
     * Output: a modifier for the setting's label.
     *
     * Why the label takes focus: after a jump, TalkBack and a keyboard should
     * carry on from the setting that was asked for, the same reason a section
     * jump focuses its heading (#262).
     */
    fun item(item: SettingsItem, focusable: Boolean = true): Modifier {
        val base = Modifier
            .onPlaced { items[item] = it }
            .focusRequester(itemFocus.getValue(item))
        return if (focusable) base.focusable() else base
    }

    /** The section's heading, which takes focus once a jump to it lands. */
    fun focus(section: SettingsShortcut): FocusRequester = headings.getValue(section)

    fun focus(item: SettingsItem): FocusRequester = itemFocus.getValue(item)

    /** The scroll value that puts [section]'s heading [gapPx] below the top, or null before layout. */
    fun scrollTarget(section: SettingsShortcut, scroll: Int, gapPx: Float): Int? =
        target(sections[section], scroll, gapPx)

    fun scrollTarget(item: SettingsItem, scroll: Int, gapPx: Float): Int? =
        target(items[item], scroll, gapPx)

    private fun target(anchor: LayoutCoordinates?, scroll: Int, gapPx: Float): Int? {
        val viewport = viewport?.takeIf { it.isAttached } ?: return null
        val placed = anchor?.takeIf { it.isAttached } ?: return null
        val top = viewport.localPositionOf(placed, Offset.Zero).y
        return (scroll + top - gapPx).roundToInt().coerceAtLeast(0)
    }
}

/**
 * The query field that takes the shortcut row's place while searching.
 *
 * Why it replaces the row instead of sitting above it: on a phone the page
 * already gives up a row to the shortcuts, and a reader searching has no use
 * for them until the search is closed.
 */
@Composable
internal fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    // Opening search is asking to type, so the keyboard comes up with it.
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .focusRequester(focus),
        placeholder = { Text(stringResource(R.string.settings_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        },
        singleLine = true
    )
}

/**
 * What a query found, over the page, each row naming its section.
 *
 * `internal` so a test can check the one thing the page relies on: the
 * results' [Surface] takes every tap over its bounds, even when it holds only
 * the "no match" line, so nothing reaches the settings drawn underneath.
 */
@Composable
internal fun SettingsSearchResults(
    results: List<SettingsSearchEntry>,
    onPick: (SettingsSearchEntry) -> Unit,
    sectionTitle: @Composable (SettingsShortcut) -> String,
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

/** The section's heading as the page prints it — Sync's is a phrase, not the chip's word. */
@Composable
internal fun SettingsShortcut.headingTitle(): String = stringResource(headingRes())

internal fun SettingsShortcut.headingRes(): Int =
    if (this == SettingsShortcut.SYNC) R.string.sync_title else label

@Composable
internal fun SettingsSectionShortcuts(
    onJump: (SettingsShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsShortcut.entries.forEach { section ->
            SuggestionChip(
                onClick = { onJump(section) },
                label = { Text(stringResource(section.label)) }
            )
        }
    }
}
