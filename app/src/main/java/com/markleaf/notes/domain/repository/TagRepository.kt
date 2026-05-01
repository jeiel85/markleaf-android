package com.markleaf.notes.domain.repository

import com.markleaf.notes.domain.model.Tag

interface TagRepository {
    suspend fun reindexTagsForNote(noteId: String, content: String)
    suspend fun getTagsForNote(noteId: String): List<Tag>
    fun observeTagsForNote(noteId: String): kotlinx.coroutines.flow.Flow<List<Tag>>
    suspend fun getAllTags(): List<Tag>
    fun observeAllTags(): kotlinx.coroutines.flow.Flow<List<Tag>>
    suspend fun getTagByName(name: String): Tag?
}
