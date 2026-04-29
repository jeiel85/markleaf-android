package com.markleaf.notes.util

object SlugGenerator {
    fun generateSlug(input: String): String {
        return input.lowercase()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("[^\\p{L}0-9-]"), "")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}
