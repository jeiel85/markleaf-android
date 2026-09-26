import java.util.Properties
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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

// 사이드로드 전용 업데이트 확인을 켜는 유일한 스위치다(D074, `docs/AGENT_SPEC.md` §15.9).
// 속성을 주지 않으면 스토어 배포 산출물과 완전히 같은 빌드가 나온다 — F-Droid와 Play는
// 이 속성을 모르므로 자동으로 그쪽을 만든다. 그래서 fdroiddata 레시피의 `gradle: - yes`가
// 계속 맞고, 플레이버와 달리 변형 한정 작업 이름도 생기지 않는다.
val sideloadUpdater = providers.gradleProperty("markleaf.updater")
    .map(String::toBoolean)
    .orElse(false)
    .get()

android {
    namespace = "com.markleaf.notes"
    compileSdk = 35

    // 게이트는 여기 한 곳뿐이다. 업데이터의 권한도 코드도 이 블록 밖에는 존재하지 않으므로,
    // 스토어 빌드에서 "꺼져 있는" 것이 아니라 **들어가지 않는다**. 런타임 플래그로 끄는 방식은
    // R8이 지워도 소스 감사에서는 보이고, F-Droid 리뷰어와 privacy 문서 독자가 확인하는 것은
    // 소스다.
    if (sideloadUpdater) {
        // `srcFile`은 병합이 아니라 교체라서 매니페스트 전체 사본이 필요하다. 사본이 낡는 것이
        // 이 방식의 유일한 실패 양식이고, `SideloadManifestParityTest`가 그것을 막는다.
        sourceSets.getByName("main").manifest.srcFile("src/main/AndroidManifest-sideload.xml")

        // 업데이터 테스트도 같은 게이트 뒤에 둔다. `src/test`는 `main`에 대해 컴파일되므로,
        // 게이트가 꺼진 빌드에서는 대상 클래스가 존재하지 않아 테스트가 컴파일될 수 없다.
        // CI는 이 스텝을 `-Pmarkleaf.updater=true`로 한 번 돌려 이 테스트들을 실행한다.
        sourceSets.getByName("test").java.srcDir("src/sideloadTest/java")
    }

    // 두 디렉터리는 **서로 배타적**이며, 공통 UI가 부르는 `UpdateSurface` 하나를 같은 FQN으로
    // 제공한다. 스토어 빌드는 아무것도 그리지 않는 스텁을, 사이드로드 빌드는 실제 구현을 얻는다.
    //
    // 왜 스텁이 필요한가: 설정 화면은 `main`에 있고 `main`은 게이트 뒤의 클래스를 참조할 수 없다.
    // 런타임 플래그 하나로 끄는 방식은 업데이터 코드를 `main`에 두게 되므로 §15.9가 약속한
    // "스토어 산출물에는 업데이터 코드가 들어가지 않는다"가 깨진다. 스텁은 업데이터 코드가 아니라
    // 빈 이음매이고, 스토어 빌드가 얻는 것은 그 빈 함수뿐이다.
    sourceSets.getByName("main").java.srcDir(
        if (sideloadUpdater) "src/sideload/java" else "src/storeStub/java"
    )

    defaultConfig {
        applicationId = "com.markleaf.notes"
        minSdk = 26
        targetSdk = 35
        versionCode = 162
        versionName = "2.53.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 공통 UI가 업데이트 항목을 보여줄지 판단하는 값. 스토어 빌드에서는 항상 false이고,
        // 그 빌드에는 참을 만들 코드 자체가 없다.
        buildConfigField("boolean", "UPDATER", sideloadUpdater.toString())
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
        getByName("debug") {
            // A debug build used to share `com.markleaf.notes` with the shipped
            // app, so installing one over the other failed with
            // INSTALL_FAILED_UPDATE_INCOMPATIBLE — different signing keys — and
            // the only way through was uninstalling the real app and its data.
            // The suffix lets both sit on the same device (#319).
            //
            // Nothing may hardcode the application id as a result. The manifest
            // already derives the FileProvider authority from `${applicationId}`,
            // and `.github/scripts/launch-smoke.sh` reads the id back out of the
            // APK rather than assuming it.
            applicationIdSuffix = ".debug"
        }
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

    // Compose compiler is configured by the org.jetbrains.kotlin.plugin.compose
    // Gradle plugin (Kotlin 2.0+); the old composeOptions.kotlinCompilerExtensionVersion
    // no longer applies.

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
                // Gradle's default "short" format prints one frame of a failing
                // test's exception. That is all the two CalledFromWrongThread
                // runs of 2026-09-19 and 2026-09-20 left behind (#262): a
                // location, no stack, so nobody could say which View call it was.
                it.testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                }
                // Opt-in, not the default: CI passes -Pmarkleaf.testFailFast=true
                // on its `./gradlew test` steps and nothing else does. A Compose
                // test that fails wrongly can leave every later one in the same JVM
                // unable to go idle, and each of those then waits out Espresso's
                // 60 s idle timeout -- 96 of them turned one real failure into a
                // 1 h 35 min run. Stopping bounds that. Gradle stops handing out
                // test *classes* after the first failure, so a class already in
                // flight can still finish -- measured: one extra class, not 96.
                // It is deliberately not applied locally (where seeing every failing test
                // at once is the useful thing) or to verifyRoborazziDebug (where
                // seeing every mismatched screenshot at once is).
                if (providers.gradleProperty("markleaf.testFailFast").orNull == "true") {
                    it.failFast = true
                }
                // The locale tests read config/locales.tsv (LocaleManifest), which
                // is outside the test task's inputs, so editing it alone left the
                // task UP-TO-DATE and the new language unchecked until something
                // else changed. CI checks out clean and never saw it; a person
                // adding a language would have.
                it.inputs.file(rootProject.file("config/locales.tsv"))
                    .withPropertyName("localeManifest")
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
        // Compose UI and Roborazzi tests need the ComponentActivity entry that
        // `ui-test-manifest` contributes only to the debug variant manifest, so
        // they live in the debug-only `src/testDebug` source set rather than in
        // `src/test`. That makes the split structural: the release and benchmark
        // unit-test variants never compile them, so no per-class exclusion list
        // has to be maintained by hand as tests are added (#152).
        // `ComposeTestSourceSetTest` fails the build if one lands in `src/test`.

        // Instrumented tests in CI. `connectedDebugAndroidTest` against a
        // GitHub-hosted emulator does not work: ddmlib's PropertyFetcher times
        // out asking that device for its API level, so Gradle reports "0 of
        // which were compatible" while raw adb answers on the same device
        // (#235). A managed device sidesteps the discovery path entirely —
        // AGP starts, targets and tears down the emulator itself.
        //
        // `aosp-atd` is the automated-test-device image: no Play services, no
        // GApps, smaller and faster to boot than `google_apis`, and the suite
        // needs neither. API 30 matches what `launch-smoke` already runs on.
        managedDevices {
            localDevices {
                create("ciAtdApi30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
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
// Release artifact export
// ---------------------------------------------------------------------------
// Both the local Play hand-off and GitLab CI use this writer so artifact names,
// all-locale notes, and validation cannot drift between distribution channels.
// Local exports omit the APK because D:\Build is the Play Console hand-off;
// GitLab exports include it for direct sideload downloads.
val writeReleaseArtifacts: (File, Boolean) -> Unit = { exportDir, includeApk ->
    val versionName = android.defaultConfig.versionName
        ?: throw GradleException("versionName is not set in defaultConfig")
    val versionCode = android.defaultConfig.versionCode
        ?: throw GradleException("versionCode is not set in defaultConfig")

    if ((!exportDir.exists() && !exportDir.mkdirs()) || !exportDir.isDirectory) {
        throw GradleException("Could not create release export directory at ${exportDir.absolutePath}")
    }
    val stem = "markleaf-v$versionName-vc$versionCode"

    if (includeApk) {
        val apk = File(layout.buildDirectory.get().asFile, "outputs/apk/release/app-release.apk")
        if (!apk.isFile) {
            throw GradleException(
                "Release APK not found at ${apk.absolutePath}. " +
                    "assembleRelease should have produced it — check the build log."
            )
        }
        val apkTarget = File(exportDir, "$stem.apk")
        apk.copyTo(apkTarget, overwrite = true)
        logger.lifecycle("Wrote ${apkTarget.absolutePath} (${apk.length()} bytes)")
    }

    // --- AAB (signed via release-signing.properties + .secrets keystore) ---
    val aab = File(layout.buildDirectory.get().asFile, "outputs/bundle/release/app-release.aab")
    if (!aab.isFile) {
        throw GradleException(
            "Release AAB not found at ${aab.absolutePath}. " +
                "bundleRelease should have produced it — check the build log."
        )
    }
    val aabTarget = File(exportDir, "$stem.aab")
    aab.copyTo(aabTarget, overwrite = true)
    logger.lifecycle("Wrote ${aabTarget.absolutePath} (${aab.length()} bytes)")

    // --- R8 mapping (Play crash deobfuscation; copied if R8 produced it) ---
    val mapping = File(layout.buildDirectory.get().asFile, "outputs/mapping/release/mapping.txt")
    if (mapping.isFile) {
        val mappingTarget = File(exportDir, "$stem.mapping.txt")
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
    // The store locales come from config/locales.tsv — the one language list
    // that the unit tests, the verify scripts and CI all read (#262). A copy
    // here is how #294 and #329 each shipped a language that half the surfaces
    // did not know about. Row order is the order the blocks are written in.
    val localeManifest = rootProject.file("config/locales.tsv")
    if (!localeManifest.isFile) {
        throw GradleException("Locale manifest not found at ${localeManifest.absolutePath}")
    }
    val noteLocales = localeManifest.readLines()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .map { row ->
            row.split(Regex("\\s+")).getOrNull(1)
                ?: throw GradleException("Malformed row in config/locales.tsv: $row")
        }
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

    val txtTarget = File(exportDir, "$stem-release-notes.txt")
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

val releaseExportDirectory = providers.gradleProperty("markleaf.releaseExportDir")

val exportReleaseArtifacts by tasks.registering {
    group = "markleaf"
    description = "Exports the signed APK/AAB, R8 mapping, and all-locale release notes to a configured directory"

    dependsOn("assembleRelease", "bundleRelease")

    doLast {
        val configuredDirectory = releaseExportDirectory.orNull
            ?: throw GradleException(
                "Set -Pmarkleaf.releaseExportDir=<directory> when running exportReleaseArtifacts."
            )
        writeReleaseArtifacts(rootProject.file(configuredDirectory), true)
    }
}

// Drops the Play-ready signed AAB, an all-locale release-notes TXT, and the R8
// mapping directly into D:\Build. The Desktop Build entry is only a shortcut.
val exportReleaseToBuildDrive by tasks.registering {
    group = "markleaf"
    description = "Copies the signed AAB, all-locale release notes, and R8 mapping to D:\\Build"

    dependsOn("bundleRelease")

    doLast {
        writeReleaseArtifacts(File("D:/Build"), false)
    }
}

// Compatibility alias for older local runbooks. New release instructions use
// exportReleaseToBuildDrive so the destination is no longer ambiguous.
tasks.register("exportReleaseToDesktop") {
    group = "markleaf"
    description = "Deprecated alias for exportReleaseToBuildDrive"
    dependsOn(exportReleaseToBuildDrive)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Activity Compose — 1.9+ wires Compose into the platform predictive-back
    // dispatcher so PredictiveBackHandler animates the system back gesture.
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.tracing:tracing:1.2.0")

    // Navigation Compose — 2.8+ adds predictive-back support for composable
    // destinations.
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
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
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.29.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.29.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.29.0")
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

// Roborazzi goldens are rendered by the Linux CI runner. Windows font
// hinting differs enough that a local verify fails roughly half the
// snapshots on noise alone, so it cannot separate a real change from
// platform noise (#262). The canonical visual gate is CI's
// `verifyRoborazziDebug`; golden updates go through the `record_roborazzi`
// workflow dispatch (local re-recording would commit Windows-rendered
// goldens that CI then rejects). The verify/record/compare tasks are
// therefore not offered on Windows: requesting one fails the build at
// configuration time with this explanation, before any test runs and before
// the plugin can inject `roborazzi.test.verify` into the test JVM. The
// `finalizeTestRoborazzi*` bookkeeping tasks stay enabled — they only
// assemble the report after the snapshot tests run, which is harmless and
// useful locally.
//
// Matching is on the requested name, resolved the way Gradle resolves it. A
// plain `contains("Roborazzi")` fails this in both directions: it rejects
// `finalizeTestRoborazziDebug`, which the paragraph above promises stays
// available, and it misses `gradlew vRD`, because Gradle abbreviates task
// names by camel hump and `startParameter.taskNames` keeps the raw token.
val roborazziGatedFamilies = listOf(
    "verifyRoborazzi",
    "recordRoborazzi",
    "compareRoborazzi",
    "verifyAndRecordRoborazzi"
)

/** Splits a task name into the camel humps Gradle abbreviates it by. */
fun camelHumps(name: String): List<String> {
    val humps = mutableListOf<StringBuilder>()
    name.forEach { ch ->
        if (humps.isEmpty() || ch.isUpperCase()) humps += StringBuilder()
        humps.last().append(ch)
    }
    return humps.map { it.toString() }
}

/**
 * True when [requested] could resolve to a task in [family] — the exact name,
 * the family with any variant suffix, or a camel-hump abbreviation of either.
 * Each hump of the request must be a prefix of the matching hump of the family,
 * which is Gradle's own rule, so `vR`, `vRD` and `verifyRoborazziDebug` all
 * match `verifyRoborazzi` while `finalizeTestRoborazziDebug` matches nothing.
 */
fun couldResolveTo(requested: String, family: String): Boolean {
    val requestedHumps = camelHumps(requested.substringAfterLast(':'))
    val familyHumps = camelHumps(family)
    if (requestedHumps.size < familyHumps.size) return false
    return familyHumps.indices.all { i ->
        familyHumps[i].startsWith(requestedHumps[i], ignoreCase = true)
    }
}

if (System.getProperty("os.name").startsWith("Windows")) {
    val gated = gradle.startParameter.taskNames.filter { requested ->
        roborazziGatedFamilies.any { family -> couldResolveTo(requested, family) }
    }
    if (gated.isNotEmpty()) {
        throw GradleException(
            "Roborazzi verify/record/compare is not offered on Windows (requested: " +
                "${gated.joinToString(", ")}): local rendering differs from the Linux CI " +
                "goldens (font hinting), so a local verify cannot separate a real change " +
                "from platform noise (#262). Run the visual gate on CI " +
                "(verifyRoborazziDebug) and update goldens via the record_roborazzi " +
                "workflow dispatch. The finalizeTestRoborazzi* report tasks still run here."
        )
    }
}
