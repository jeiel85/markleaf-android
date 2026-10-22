package com.markleaf.notes.feature.settings

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The first Settings screen: search, then one row per section.
 *
 * A query replaces the rows with what it finds instead of covering them, so
 * nothing underneath can take a tap meant for a result.
 *
 * Coming back from a page does not move focus to that page's row: a
 * `clickable` row cannot take focus in touch mode, so TalkBack starts again
 * from the app bar, as it does on any screen it enters.
 */
@Composable
internal fun SettingsHome(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SettingsSearchEntry>,
    onOpenSection: (SettingsCategory) -> Unit,
    onPickResult: (SettingsSearchEntry) -> Unit,
    summary: @Composable (SettingsCategory) -> String,
    selected: SettingsCategory? = null
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            SettingsSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (query.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SettingsCategory.entries.forEach { section ->
                        SettingsSectionRow(
                            section = section,
                            summary = summary(section),
                            selected = selected?.let { it == section },
                            onClick = { onOpenSection(section) }
                        )
                    }
                }
            } else {
                SettingsSearchResults(
                    results = results,
                    onPick = { entry ->
                        // The page it opens is for reading; the keyboard would
                        // cover half of it.
                        keyboard?.hide()
                        onPickResult(entry)
                    },
                    sectionTitle = { stringResource(it.label) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionRow(
    section: SettingsCategory,
    summary: String,
    selected: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            // Only side by side does a row have a selected state (null
            // otherwise): the page beside it is the one it opened, and every
            // row says whether it is that one.
            .background(if (selected == true) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(section.label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            // One line: the row is for finding a category, and a long folder
            // path would push the rows below off a phone screen.
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One section's page, scrolled on its own.
 *
 * Input: [landingPending], true when the page was just opened from the list,
 * a search result or a deep link; [landingItem], the setting it should land
 * on, or null for the top of the page.
 * Output: the page, with [content] given the anchors its settings mark
 * themselves with. [onLanded] runs once the landing is done.
 *
 * [title] is drawn at the top of the page, as its heading and [titleFocus]
 * target, only when the page sits beside the list; on its own the app bar
 * carries the name.
 *
 * Why the landing waits for a frame: a setting's position exists only after
 * the page has been laid out, and the title's focus target only after the app
 * bar has. Why it is a flag the screen keeps: a page that comes back from a
 * rotation should keep the reader's focus and scroll, not jump again.
 */
@Composable
internal fun SettingsPage(
    landingPending: Boolean,
    landingItem: SettingsItem?,
    titleFocus: FocusRequester,
    onLanded: () -> Unit,
    title: String? = null,
    content: @Composable (SectionAnchors) -> Unit
) {
    val scroll = rememberScrollState()
    val anchors = remember { SectionAnchors() }
    val topGap = 16.dp
    val topGapPx = with(LocalDensity.current) { topGap.toPx() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .onPlaced { anchors.viewport = it }
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = topGap)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .semantics { heading() }
                        .focusRequester(titleFocus)
                        .focusable()
                )
            }
            content(anchors)
        }
    }
    LaunchedEffect(landingPending) {
        if (!landingPending) return@LaunchedEffect
        withFrameNanos {}
        val item = landingItem
        var target = item?.let { anchors.scrollTarget(it, scroll.value, topGapPx) }
        // A setting this build does not draw (the update check in a store
        // build) never gets a position; give up after a few frames and land
        // on the page's title instead.
        var frames = 0
        while (item != null && target == null && frames < MAX_LANDING_FRAMES) {
            withFrameNanos {}
            target = anchors.scrollTarget(item, scroll.value, topGapPx)
            frames++
        }
        if (item != null && target != null) {
            scroll.scrollTo(target)
            anchors.focus(item).requestFocus()
        } else {
            titleFocus.requestFocus()
        }
        onLanded()
    }
}

private const val MAX_LANDING_FRAMES = 10
