package com.markleaf.notes.domain.repository

import com.markleaf.notes.domain.model.Tag

interface TagRepository {
    suspend fun reindexTagsForNote(noteId: String, content: String)
    suspend fun getTagsForNote(noteId: String): List<Tag>
    fun observeTagsForNote(noteId: String): kotlinx.coroutines.flow.Flow<List<Tag>>
    /**
     * Tags reachable from a visible note. Locked and trashed notes are excluded, so
     * these never leak a tag that only a locked note carries (#169).
     */
    suspend fun getVisibleTags(): List<Tag>
    fun observeVisibleTags(): kotlinx.coroutines.flow.Flow<List<Tag>>
    suspend fun getTagByName(name: String): Tag?
}
