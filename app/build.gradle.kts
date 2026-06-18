import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("io.github.takahirom.roborazzi")
}

val releaseSigningPropertiesFile = rootProject.file("release-signing.properties")
val legacySigningPropertiesFile = rootProject.file("signing.properties")
val releaseSigningProperties = Properties().apply {
    when {
        releaseSigningPropertiesFile.exists() -> releaseSigningPropertiesFile.inputStream().use(::load)
        legacySigningPropertiesFile.exists() -> legacySigningPropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(name: String, legacyName: String): String? =
    providers.environmentVariable(name).orNull
        ?: providers.environmentVariable(legacyName).orNull
        ?: releaseSigningProperties.getProperty(name)
        ?: releaseSigningProperties.getProperty(legacyName)

val releaseStoreFile = signingValue("MARKLEAF_RELEASE_STORE_FILE", "STORE_FILE")
    ?: providers.environmentVariable("RELEASE_STORE_FILE").orNull
val releaseStorePassword = signingValue("MARKLEAF_RELEASE_STORE_PASSWORD", "STORE_PASSWORD")
    ?: providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = signingValue("MARKLEAF_RELEASE_KEY_ALIAS", "KEY_ALIAS")
    ?: providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = signingValue("MARKLEAF_RELEASE_KEY_PASSWORD", "KEY_PASSWORD")
    ?: providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val requireReleaseSigning = providers.gradleProperty("markleaf.requireReleaseSigning")
    .map(String::toBoolean)
    .orElse(false)
    .get()

if (requireReleaseSigning && !hasReleaseSigningConfig) {
    throw GradleException(
        "Release signing is required, but one or more signing values are missing. " +
            "Set MARKLEAF_RELEASE_STORE_FILE, MARKLEAF_RELEASE_STORE_PASSWORD, " +
            "MARKLEAF_RELEASE_KEY_ALIAS, and MARKLEAF_RELEASE_KEY_PASSWORD."
    )
}

if (requireReleaseSigning && !rootProject.file(releaseStoreFile!!).exists()) {
    throw GradleException("Release signing is required, but the keystore file does not exist: $releaseStoreFile")
}

android {
    namespace = "com.markleaf.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.markleaf.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 98
        versionName = "2.18.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // AGP injects a "Dependency metadata" APK signing block by default. It
    // confuses F-Droid's reproducible-build verification (`fdroid scanner`
    // flags it as an extra signing block) and we don't need the upstream
    // dependency report, so disable it in both APK and AAB outputs.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            // R8 + resource shrinking are required for the Play / production
            // gate. ProGuard rules live in `proguard-rules.pro`; keep that
            // file minimal and document why each rule exists.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // Mirrors release minus signing so the :benchmark module can target
        // a realistic build of the app on any developer machine. Macrobenchmark
        // requires the target apk to be NON-debuggable (debuggable apps
        // produce skewed numbers) but profileable — see the benchmark
        // variant's AndroidManifest for `<profileable shell="true" />`.
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.sqliteMode", "NATIVE")
                // Roborazzi snapshot tests rely on `ui-test-manifest` which only
                // ships a ComponentActivity entry in the debug manifest. Skip them
                // in release-variant unit tests so `:app:test` stays green.
                if (it.name != "testDebugUnitTest") {
                    // Roborazzi/Compose snapshot tests need ui-test-manifest's
                    // ComponentActivity entry, which only ships in the debug
                    // variant manifest. Skip every snapshot test class in
                    // any non-debug-variant unit test (release, benchmark, …).
                    it.exclude("**/preview/*SnapshotTest*")
                }
            }
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// ---------------------------------------------------------------------------
// exportReleaseToDesktop
// ---------------------------------------------------------------------------
// Drops the Play-ready signed AAB, an all-locale release-notes TXT, and the R8
// mapping into the user's Desktop\Build folder, matching the contract in
// AGENTS.md's "Release Artifact Export" section. The TXT pulls its body from the
// fastlane locale changelogs keyed off `versionCode`, so the same
// source-of-truth feeds both F-Droid and the Play Console submission.
//
// Files share the `markleaf-v<semver>-vc<versionCode>` stem and land flat in
// Desktop\Build (per ~/.claude/build-artifacts.md). Locale tags follow BCP 47
// with uppercase region (`ko-KR`, `en-US`, …); the TXT bundles ALL store locales
// (ko/en/ja/de/fr/es) into one file as consecutive tag blocks, no surrounding
// prose.
val exportReleaseToDesktop by tasks.registering {
    group = "markleaf"
    description = "Copies the signed AAB, all-locale release notes, and R8 mapping to Desktop\\Build"

    dependsOn("bundleRelease")

    doLast {
        val versionName = android.defaultConfig.versionName
            ?: throw GradleException("versionName is not set in defaultConfig")
        val versionCode = android.defaultConfig.versionCode
            ?: throw GradleException("versionCode is not set in defaultConfig")

        // Resolve the user-visible Desktop. On Windows the path is often
        // redirected by OneDrive ("OneDrive\Desktop" in English locales,
        // "OneDrive\바탕 화면" in Korean), so a bare $HOME/Desktop write
        // ends up at the non-OneDrive path that the user no longer looks at.
        // Check the redirected locations first, then fall through to the
        // classic path for macOS/Linux/plain Windows.
        val home = File(System.getProperty("user.home"))
        val candidates = listOf(
            File(home, "OneDrive/바탕 화면"),
            File(home, "OneDrive/Desktop"),
            File(home, "Desktop")
        )
        val desktop = candidates.firstOrNull { it.isDirectory }
            ?: throw GradleException(
                "Could not find a Desktop directory. Tried:\n" +
                    candidates.joinToString("\n") { "  - ${it.absolutePath}" }
            )

        // Per ~/.claude/build-artifacts.md, store-upload artifacts land flat in
        // Desktop\Build under a self-describing `markleaf-v<semver>-vc<vc>` stem,
        // not on the bare desktop.
        val buildDir = File(desktop, "Build").apply { mkdirs() }
        val stem = "markleaf-v$versionName-vc$versionCode"

        // --- AAB (signed via release-signing.properties + .secrets keystore) ---
        val aab = File(layout.buildDirectory.get().asFile, "outputs/bundle/release/app-release.aab")
        if (!aab.isFile) {
            throw GradleException(
                "Release AAB not found at ${aab.absolutePath}. " +
                    "bundleRelease should have produced it — check the build log."
            )
        }
        val aabTarget = File(buildDir, "$stem.aab")
        aab.copyTo(aabTarget, overwrite = true)
        logger.lifecycle("Wrote ${aabTarget.absolutePath} (${aab.length()} bytes)")

        // --- R8 mapping (Play crash deobfuscation; copied if R8 produced it) ---
        val mapping = File(layout.buildDirectory.get().asFile, "outputs/mapping/release/mapping.txt")
        if (mapping.isFile) {
            val mappingTarget = File(buildDir, "$stem.mapping.txt")
            mapping.copyTo(mappingTarget, overwrite = true)
            logger.lifecycle("Wrote ${mappingTarget.absolutePath}")
        } else {
            logger.warn("R8 mapping not found at ${mapping.absolutePath} — skipping (is minify enabled?)")
        }

        // --- Release notes TXT ---
        // All store locales go into ONE file as consecutive BCP 47 tag blocks.
        // Every locale must have a changelog for this versionCode; fail fast so
        // a cut never ships notes that silently drop a locale.
        val fastlaneRoot = rootProject.file("fastlane/metadata/android")
        val noteLocales = listOf("ko-KR", "en-US", "ja-JP", "de-DE", "fr-FR", "es-ES")
        val sources = noteLocales.associateWith { File(fastlaneRoot, "$it/changelogs/$versionCode.txt") }

        val missing = noteLocales.filter { !sources.getValue(it).isFile }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing fastlane changelogs for versionCode $versionCode: ${missing.joinToString()}. " +
                    "Author a changelog for every store locale (${noteLocales.joinToString()}) before cutting."
            )
        }
        // Play Console hard-caps release notes at 500 characters per locale.
        // Catch overruns here rather than during the upload step — Play
        // simply refuses the submission and the agent has to bisect why.
        val playLimit = 500
        val overLimit = noteLocales.mapNotNull { loc ->
            val len = sources.getValue(loc).readText().trim().length
            if (len > playLimit) "$loc ($len, ${len - playLimit} over)" else null
        }
        if (overLimit.isNotEmpty()) {
            throw GradleException(
                "Release notes exceed the $playLimit-char Play Console limit per locale: " +
                    "${overLimit.joinToString()}. Trim before re-running."
            )
        }

        val txtTarget = File(buildDir, "$stem-release-notes.txt")
        txtTarget.writeText(
            buildString {
                noteLocales.forEach { loc ->
                    append("<$loc>\n")
                    append(sources.getValue(loc).readText().trim())
                    append("\n</$loc>\n")
                }
            }
        )
        logger.lifecycle("Wrote ${txtTarget.absolutePath} (${noteLocales.size} locales)")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.02")
    implementation(composeBom)
    
    // Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-livedata")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.tracing:tracing:1.2.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // DocumentFile (Storage Access Framework)
    implementation("androidx.documentfile:documentfile:1.0.1")

    // EXIF metadata library (Apache 2.0, F-Droid friendly)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Biometric (AOSP, Apache 2.0 — F-Droid friendly). Used by the
    // optional app-lock gate. Authentication is fully local; the API
    // never reaches the network.
    implementation("androidx.biometric:biometric:1.1.0")

    // Settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Profile installer — lets Macrobenchmark and AOT compile baseline
    // profiles against this app. No-op at runtime when no benchmark is
    // attached, so it costs nothing for normal users. 1.4.0+ adds API 35.
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")

    // Coil image loader for in-preview attachments. Apache 2.0, F-Droid friendly.
    // Loads from app-private File paths so we don't need media permissions.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // CommonMark parser (BSD-2-clause, F-Droid friendly). Replaces the
    // hand-rolled SimpleMarkdownPreview internals while keeping the same
    // PreviewLine output model for the renderer.
    val commonmarkVersion = "0.24.0"
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-yaml-front-matter:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-footnotes:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    
    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.20.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.20.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.20.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
