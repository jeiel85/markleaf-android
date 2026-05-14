# Markleaf release ProGuard / R8 rules.
#
# The Android Gradle Plugin's `proguard-android-optimize.txt` already covers
# activities/services/receivers/providers declared in the manifest, custom
# views, Parcelables, Serializables, enum values, native methods, and the
# kotlin.Metadata annotation. Most of our dependencies (Compose, Room,
# Coil, DataStore, kotlinx.coroutines, commonmark-java) ship their own
# consumer ProGuard rules, so this file only adds the things AGP can't
# infer from the manifest or the dependencies.
#
# Keep this file minimal. Every rule here is a hint that an undocumented
# coupling exists; explain WHY each rule is required so it can be removed
# later if the underlying coupling goes away.

# --- Room entities ---
# Room's KSP-generated code is renamed consistently along with the entity
# classes, so basic minification is safe. But entity field names show up as
# string literals in generated SQL when the entity is involved in
# `@RawQuery`, FTS, or `@Embedded` resolution. Markleaf uses Room FTS4
# (`NoteFtsEntity`) and the entities also flow through SAF folder mirror
# round-trip serialization, where field names are used by the frontmatter
# codec. Keep the entity classes and their members by name so neither
# Room's runtime SQL nor the YAML frontmatter codec can be tripped up by
# minification.
-keepclassmembers class com.markleaf.notes.data.local.entity.** { *; }
-keep class com.markleaf.notes.data.local.entity.** { *; }

# --- Settings DTO ---
# `AppSettings` is round-tripped via DataStore Preferences (key/value
# pairs). The serialization itself is by-key, but ToString/copy() from
# data classes show up in error reporting. Keep the class names so any
# future telemetry hook (none today) and stack traces in user bug reports
# remain legible.
-keep class com.markleaf.notes.data.settings.AppSettings { *; }

# --- Frontmatter codec ---
# `SyncFrontmatter` parses YAML by string keys (`markleaf_id`,
# `created_at`, …) and writes them back. The keys are literal strings, not
# field names, so minification is fine, but the codec is a public-ish
# integration surface with files on disk. Keep its members so the parser
# can be debugged from a user-supplied `.md` file in the field.
-keep class com.markleaf.notes.data.sync.SyncFrontmatter { *; }
-keep class com.markleaf.notes.data.sync.NoteFolderMirror { *; }

# --- AppWidget receiver ---
# Already kept via the manifest entry, but listing it here makes the keep
# graph self-documenting and protects against future renames of the
# manifest reference.
-keep class com.markleaf.notes.widget.QuickNoteWidget { *; }

# --- Kotlin coroutines internal volatile fields ---
# Some coroutines internals look up volatile atomic field updaters by
# name. Kotlin coroutines ships consumer rules for this, but keep an
# explicit rule for the rare case where AGP's pinning fails on a future
# coroutines upgrade.
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# --- Strip Android logging in release ---
# `android.util.Log` calls in release builds are noise and a tiny APK win;
# R8 can fold them when proven side-effect-free. Markleaf uses Log
# sparingly (mostly in catch blocks); folding is safe.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
