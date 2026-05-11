package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplaceRangesTest {

    @Test
    fun `replaceRange replaces a single match and places caret after replacement`() {
        val state = TextFieldValue("hello world")
        val match = findAllRanges(state.text, "world").single()
        val result = replaceRange(state, match, "there")
        assertEquals("hello there", result.text)
        assertEquals(TextRange("hello there".length), result.selection)
    }

    @Test
    fun `replaceRange supports empty replacement (deletion)`() {
        val state = TextFieldValue("foo bar baz")
        val match = findAllRanges(state.text, "bar ").single()
        val result = replaceRange(state, match, "")
        assertEquals("foo baz", result.text)
    }

    @Test
    fun `replaceAllRanges replaces every occurrence in a single pass`() {
        val state = TextFieldValue("aa bb aa cc aa")
        val matches = findAllRanges(state.text, "aa")
        val result = replaceAllRanges(state, matches, "XX")
        assertEquals("XX bb XX cc XX", result.text)
    }

    @Test
    fun `replaceAllRanges with empty list returns original state`() {
        val state = TextFieldValue("hello", TextRange(2))
        val result = replaceAllRanges(state, emptyList(), "x")
        assertEquals(state.text, result.text)
    }

    @Test
    fun `replaceAllRanges handles longer replacements without index shift`() {
        val state = TextFieldValue("a a a")
        val matches = findAllRanges(state.text, "a")
        val result = replaceAllRanges(state, matches, "long")
        assertEquals("long long long", result.text)
    }

    @Test
    fun `replaceAllRanges is case-insensitive via findAllRanges`() {
        val state = TextFieldValue("Hello HELLO hello")
        val matches = findAllRanges(state.text, "hello")
        val result = replaceAllRanges(state, matches, "hi")
        assertEquals("hi hi hi", result.text)
    }
}
