package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteTagCrossRef
import com.markleaf.notes.data.local.entity.TagEntity
import com.markleaf.notes.domain.model.Tag
import com.markleaf.notes.domain.model.TagSummary
import com.markleaf.notes.domain.repository.TagRepository
import com.markleaf.notes.util.TagParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

class LocalTagRepository(
    private val db: AppDatabase
) : TagRepository {

    override suspend fun reindexTagsForNote(noteId: String, content: String) {
        val tagNames = TagParser.parseTags(content)
        
        // Clear existing tags for this note
        db.tagDao().clearTagsForNote(noteId)
        
        // Process each tag
        tagNames.forEach { tagName ->
            val normalizedName = TagParser.normalizeTagName(tagName)
            
            // Find or create tag
            var tagEntity = db.tagDao().getTagByName(normalizedName)
            
            if (tagEntity == null) {
                val newTagId = db.tagDao().insertTag(
                    TagEntity(name = normalizedName, createdAt = System.currentTimeMillis())
                )
                tagEntity = TagEntity(id = newTagId, name = normalizedName, createdAt = System.currentTimeMillis())
            }
            
            // Create cross-reference
            db.tagDao().insertCrossRef(
                NoteTagCrossRef(noteId = noteId, tagId = tagEntity.id)
            )
        }
    }

    override suspend fun getTagsForNote(noteId: String): List<Tag> {
        return db.tagDao().getTagsForNote(noteId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .first()
    }

    override fun observeTagsForNote(noteId: String): Flow<List<Tag>> {
        return db.tagDao().getTagsForNote(noteId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun getAllTags(): List<Tag> {
        return db.tagDao().getAllTags()
            .map { entities -> entities.map { it.toDomainModel() } }
            .first()
    }

    override fun observeAllTags(): Flow<List<Tag>> {
        return db.tagDao().getAllTags()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    fun observeTagSummaries(): Flow<List<TagSummary>> {
        return db.tagDao().observeTagsWithCounts()
            .map { entities ->
                entities.map { entity ->
                    TagSummary(
                        tag = Tag(
                            id = entity.id,
                            name = entity.name,
                            normalizedName = TagParser.normalizeTagName(entity.name),
                            createdAt = Instant.ofEpochMilli(entity.createdAt)
                        ),
                        noteCount = entity.noteCount
                    )
                }
            }
    }

    override suspend fun getTagByName(name: String): Tag? {
        val normalizedName = TagParser.normalizeTagName(name)
        return db.tagDao().getTagByName(normalizedName)?.toDomainModel()
    }

    private fun TagEntity.toDomainModel(): Tag {
        return Tag(
            id = id,
            name = name,
            normalizedName = TagParser.normalizeTagName(name),
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }
}
