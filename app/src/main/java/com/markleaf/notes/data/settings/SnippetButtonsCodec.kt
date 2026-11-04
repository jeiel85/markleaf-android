package com.markleaf.notes.data.settings

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The snippet buttons as one DataStore string (#436): a JSON array of
 * `{"label": …, "text": …}`, in row order.
 *
 * JSON rather than a home-made separator because a snippet is free text — it
 * can hold any character a separator could be, newlines included.
 */
internal object SnippetButtonsCodec {
    fun encode(buttons: List<SnippetButton>): String {
        val array = JSONArray()
        buttons.forEach { button ->
            array.put(JSONObject().put(LABEL, button.label).put(TEXT, button.text))
        }
        return array.toString()
    }

    /**
     * Only [encode] writes this value, so anything unreadable means a damaged
     * file — or a newer version's format after a downgrade — rather than a
     * format to support. The row then shows no buttons instead of failing to
     * open the editor, and Settings says so ([isUnreadable]) before the next
     * change there writes a clean list over it. Entries without text are
     * dropped for the same reason: a button that inserts nothing has no way
     * to have been saved.
     */
    fun decode(raw: String?): List<SnippetButton> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (_: JSONException) {
            return emptyList()
        }
        return (0 until array.length())
            .mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val text = entry.optString(TEXT)
                if (text.isEmpty()) null else SnippetButton(label = entry.optString(LABEL), text = text)
            }
            .take(SnippetButton.MAX_COUNT)
    }

    /**
     * True when a stored value exists but is not a list [decode] can read, so
     * the empty row hides snippets the user wrote rather than meaning none
     * (#262). Dropped entries don't count: the rest of the list still reads.
     */
    fun isUnreadable(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return try {
            JSONArray(raw)
            false
        } catch (_: JSONException) {
            true
        }
    }

    private const val LABEL = "label"
    private const val TEXT = "text"
}
