package com.markleaf.notes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMarkdownLinkTest {
    @Test fun resolvesOnlyFilesInTheSelectedFolder() {
        assertEquals("Some Link.md", LocalMarkdownLink.fileName("Some%20Link.md"))
        assertEquals("note.txt", LocalMarkdownLink.fileName("./note.txt"))
        assertNull(LocalMarkdownLink.fileName("../note.md"))
        assertNull(LocalMarkdownLink.fileName("subdir/note.md"))
        assertNull(LocalMarkdownLink.fileName("https://example.com/note.md"))
        assertNull(LocalMarkdownLink.fileName("file:///secret.md"))
    }
}
