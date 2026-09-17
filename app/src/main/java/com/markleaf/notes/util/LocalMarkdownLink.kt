package com.markleaf.notes.util

import java.net.URI

/** A Markdown link to a file in the configured notes folder, never a web URI. */
object LocalMarkdownLink {
    fun fileName(href: String): String? {
        val uri = runCatching { URI(href.replace(" ", "%20")) }.getOrNull() ?: return null
        if (uri.isAbsolute || uri.rawAuthority != null || uri.rawQuery != null) return null
        val path = uri.path ?: return null
        val name = path.removePrefix("./")
        if (name.isBlank() || name == "." || name == ".." ||
            '/' in name || '\\' in name || uri.fragment != null && uri.fragment.isBlank()
        ) return null
        return name.takeIf { it.endsWith(".md", true) || it.endsWith(".txt", true) }
    }
}
