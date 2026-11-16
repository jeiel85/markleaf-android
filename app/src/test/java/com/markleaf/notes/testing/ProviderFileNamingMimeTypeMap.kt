package com.markleaf.notes.testing

import android.webkit.MimeTypeMap
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Makes `DocumentFile.fromFile(dir).createFile(mime, "Plan.md")` name the file
 * `Plan.md`, as a document provider would, instead of `Plan.md.md`.
 *
 * Unit tests stand a temp directory in for the user's SAF folder through
 * `DocumentFile.fromFile`. That gives a `RawDocumentFile`, whose `createFile`
 * always appends `MimeTypeMap`'s extension to the display name. The providers
 * behind a real folder only add one when the name doesn't already end in it.
 * Robolectric up to 4.14 had an empty `MimeTypeMap`, so the two agreed by
 * accident; 4.15 and later ship the platform's real mappings
 * (`text/markdown` → `md`, `application/json` → `json`), and every test that
 * names a file through this seam would write `x.md.md` and `.json.json`.
 *
 * Only the extension lookup is replaced; `getMimeTypeFromExtension` (which
 * `AttachmentManager` uses) keeps Robolectric's real mappings. Registered for
 * every unit test in `src/test/resources/robolectric.properties`.
 */
@Implements(MimeTypeMap::class)
class ProviderFileNamingMimeTypeMap {
    @Implementation
    protected fun getExtensionFromMimeType(mimeType: String?): String? = null
}
