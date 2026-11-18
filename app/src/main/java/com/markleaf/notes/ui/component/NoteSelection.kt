package com.markleaf.notes.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note

/**
 * The notes a list has picked for one action (#596): the notes list, the
 * archive and the trash each keep one.
 *
 * [active] is separate from [ids] because a selection can be empty: one
 * started from the ⋮ menu has nothing picked yet, and unpicking the last note
 * keeps the bar up rather than dropping out under the reader's finger. Only
 * Close, Back or an action ends it.
 */
@Stable
class NoteSelection internal constructor(active: Boolean, ids: Set<String>) {
    var active by mutableStateOf(active)
        private set
    var ids by mutableStateOf(ids)
        private set

    /** Starts selecting, with [noteId] picked when one started it. */
    fun start(noteId: String? = null) {
        active = true
        ids = setOfNotNull(noteId)
    }

    fun toggle(noteId: String) {
        ids = if (noteId in ids) ids - noteId else ids + noteId
    }

    fun selectAll(noteIds: Collection<String>) {
        ids = noteIds.toSet()
    }

    /** Ends the selection: Close, Back, or after an action has used it. */
    fun clear() {
        active = false
        ids = emptySet()
    }

    /**
     * Drops ids no longer among [noteIds] — a note that left the list some
     * other way. Without this it would come back already picked if it
     * returned (restored from the trash, a filter cleared).
     */
    fun retain(noteIds: Set<String>) {
        if (!noteIds.containsAll(ids)) ids = ids intersect noteIds
    }

    /** The picked notes among [notes], in the order they are shown. */
    fun of(notes: List<Note>): List<Note> = notes.filter { it.id in ids }

    companion object {
        val Saver = listSaver<NoteSelection, Any>(
            save = { listOf(it.active, ArrayList(it.ids)) },
            restore = {
                @Suppress("UNCHECKED_CAST")
                NoteSelection(it[0] as Boolean, (it[1] as List<String>).toSet())
            }
        )
    }
}

/** A [NoteSelection] that survives rotation and the trip to a note and back. */
@Composable
fun rememberNoteSelection(): NoteSelection =
    rememberSaveable(saver = NoteSelection.Saver) { NoteSelection(false, emptySet()) }

/**
 * The app bar while a selection is up: Close, how many are picked, and the
 * screen's actions. The count is a polite live region, so TalkBack reads the
 * new number after each tap without the reader moving focus to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    count: Int,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground
    )
) {
    TopAppBar(
        title = {
            Text(
                text = if (count == 0) {
                    stringResource(R.string.select_notes)
                } else {
                    pluralStringResource(R.plurals.notes_selected, count, count)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_selection))
            }
        },
        actions = actions,
        colors = colors
    )
}

/**
 * The circle beside each note while selecting: filled and checked when the
 * note is picked. Decorative — the row itself carries the selected state, so
 * TalkBack says it once, with the note's title.
 */
@Composable
fun SelectionMark(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Icon(
        imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = null,
        tint = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        modifier = modifier
            .padding(end = 10.dp)
            .size(size)
    )
}
