package com.markleaf.notes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.markleafSettingsDataStore by preferencesDataStore(name = "markleaf_settings")

class AppSettingsRepository(
    private val context: Context
) {
    val settings: Flow<AppSettings> = context.markleafSettingsDataStore.data.map { preferences ->
        AppSettings(
            markdownSyntaxVisibility = preferences[MARKDOWN_SYNTAX_VISIBILITY]
                ?.let { value -> enumValueOrDefault(value, MarkdownSyntaxVisibility.SHOW) }
                ?: MarkdownSyntaxVisibility.SHOW,
            lineWidth = preferences[LINE_WIDTH]
                ?.let { value -> enumValueOrDefault(value, EditorLineWidth.COMFORTABLE) }
                ?: EditorLineWidth.COMFORTABLE,
            toolbarConfig = ToolbarConfig(
                showBold = preferences[TOOLBAR_SHOW_BOLD] ?: true,
                showItalic = preferences[TOOLBAR_SHOW_ITALIC] ?: true,
                showCheckbox = preferences[TOOLBAR_SHOW_CHECKBOX] ?: true,
                showMarkdownLink = preferences[TOOLBAR_SHOW_MARKDOWN_LINK] ?: true,
                showWikiLink = preferences[TOOLBAR_SHOW_WIKI_LINK] ?: true,
                showImage = preferences[TOOLBAR_SHOW_IMAGE] ?: true
            )
        )
    }

    suspend fun setMarkdownSyntaxVisibility(visibility: MarkdownSyntaxVisibility) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[MARKDOWN_SYNTAX_VISIBILITY] = visibility.name
        }
    }

    suspend fun setLineWidth(lineWidth: EditorLineWidth) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[LINE_WIDTH] = lineWidth.name
        }
    }

    suspend fun setToolbarConfig(config: ToolbarConfig) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[TOOLBAR_SHOW_BOLD] = config.showBold
            preferences[TOOLBAR_SHOW_ITALIC] = config.showItalic
            preferences[TOOLBAR_SHOW_CHECKBOX] = config.showCheckbox
            preferences[TOOLBAR_SHOW_MARKDOWN_LINK] = config.showMarkdownLink
            preferences[TOOLBAR_SHOW_WIKI_LINK] = config.showWikiLink
            preferences[TOOLBAR_SHOW_IMAGE] = config.showImage
        }
    }

    private fun <T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
        return runCatching {
            java.lang.Enum.valueOf(default.declaringJavaClass, value)
        }.getOrDefault(default)
    }

    private companion object {
        val MARKDOWN_SYNTAX_VISIBILITY = stringPreferencesKey("markdown_syntax_visibility")
        val LINE_WIDTH = stringPreferencesKey("line_width")
        val TOOLBAR_SHOW_BOLD = booleanPreferencesKey("toolbar_show_bold")
        val TOOLBAR_SHOW_ITALIC = booleanPreferencesKey("toolbar_show_italic")
        val TOOLBAR_SHOW_CHECKBOX = booleanPreferencesKey("toolbar_show_checkbox")
        val TOOLBAR_SHOW_MARKDOWN_LINK = booleanPreferencesKey("toolbar_show_markdown_link")
        val TOOLBAR_SHOW_WIKI_LINK = booleanPreferencesKey("toolbar_show_wiki_link")
        val TOOLBAR_SHOW_IMAGE = booleanPreferencesKey("toolbar_show_image")
    }
}
