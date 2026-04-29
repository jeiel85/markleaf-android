package com.markleaf.notes.domain.repository

import com.markleaf.notes.domain.model.Tag

interface TagRepository {
    suspend fun reindexTagsForNote(noteId: Long, content: String)
    suspend fun getTagsForNote(noteId: Long): List<Tag>
    fun observeTagsForNote(noteId: Long): kotlinx.coroutines.flow.Flow<List<Tag>>
    suspend fun getAllTags(): List<Tag>
    fun observeAllTags(): kotlinx.coroutines.flow.Flow<List<Tag>>
    suspend fun getTagByName(name: String): Tag?
}
