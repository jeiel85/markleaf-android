# Release Signing and GitHub Releases

Markleaf release builds are signed only when release signing values are supplied.
The release keystore is a secret and must not be committed.

The production release certificate is fixed. GitHub tag releases verify the signed APK against this SHA-256 certificate digest before creating a release:

```text
0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a
```

Do not replace the production keystore for normal releases. Android treats APKs signed with a different certificate as a different update lineage, so existing users cannot update over the previously installed app.

## Local Signed Release Build

Create a local `release-signing.properties` file:

```properties
MARKLEAF_RELEASE_STORE_FILE=.secrets/markleaf-release.p12
MARKLEAF_RELEASE_STORE_PASSWORD=your-keystore-password
MARKLEAF_RELEASE_KEY_ALIAS=markleaf-release
MARKLEAF_RELEASE_KEY_PASSWORD=your-keystore-password
```

For PKCS12 keystores, keep `MARKLEAF_RELEASE_KEY_PASSWORD` the same as `MARKLEAF_RELEASE_STORE_PASSWORD`.

Then build:

```bash
./gradlew assembleRelease
```

For release-candidate verification, require signing explicitly:

```bash
./gradlew assembleRelease -Pmarkleaf.requireReleaseSigning=true
```

On PowerShell, quote the Gradle property:

```powershell
./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'
```

The signed APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## R8 (Minification + Resource Shrinking)

Release builds run R8 with resource shrinking enabled. ProGuard rules live in
`app/proguard-rules.pro` and should stay minimal — every keep rule there
documents an undocumented coupling, so adding a rule should always come with a
comment explaining *why*.

R8 produces a mapping file used to deobfuscate stack traces from production
crash reports:

```text
app/build/outputs/mapping/release/mapping.txt
```

Keep the mapping that corresponds to each released APK. GitHub Actions
attaches `markleaf-vX.Y.Z.mapping.txt` to every tag release so historical
versions can still be deobfuscated.

If R8 strips something at runtime (NoClassDefFoundError, missing reflection
target, lost Compose Composer slot), the fix is to add a precise `-keep`
rule in `app/proguard-rules.pro` — not to disable R8.

## CI Gates

Every push to `main` and every pull request runs:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew verifyRoborazziDebug`
- `./gradlew :app:lintRelease` — fails on any Error-severity lint issue in the release variant
- `./gradlew :app:assembleRelease` — proves R8 still produces a valid APK

The `launch-smoke` job (emulator-based) currently runs on a debug APK and is
marked `continue-on-error: true` because of historical emulator flakiness on
GitHub-hosted runners. A release-APK runtime smoke (R8-shrunk, debug-signed)
is a planned follow-up.

## GitHub Actions Secrets

Add these repository secrets in GitHub:

```text
MARKLEAF_RELEASE_KEYSTORE_BASE64
MARKLEAF_RELEASE_STORE_PASSWORD
MARKLEAF_RELEASE_KEY_ALIAS
MARKLEAF_RELEASE_KEY_PASSWORD
```

`MARKLEAF_RELEASE_KEYSTORE_BASE64` is the Base64-encoded PKCS12 keystore file.

On tag pushes matching `v*`, GitHub Actions runs tests, builds the signed
release APK and AAB, attaches the R8 mapping file, and creates a GitHub
Release with all three assets attached:

- `markleaf-vX.Y.Z.apk` — signed, R8-shrunk APK for sideload installs and Releases mirror
- `markleaf-vX.Y.Z.aab` — signed Android App Bundle for Play Store upload
- `markleaf-vX.Y.Z.mapping.txt` — R8 mapping for deobfuscating crash stack traces

The release job fails before publishing if the keystore secret is missing,
if the APK certificate SHA-256 digest differs from the fixed production
certificate, or if any of the three artifacts is missing.

Example:

```bash
git tag v0.1.0
git push origin v0.1.0
```
