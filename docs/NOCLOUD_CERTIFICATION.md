# Markleaf No-Cloud Certification

## Certification Statement

**Date:** 2024  
**Application:** Markleaf Android  
**Version:** 1.1.15+  
**Certification ID:** MARKLEAF-NC-2024-001

---

## Executive Summary

Markleaf is certified as a **100% No-Cloud Application**. This certification verifies that the application operates entirely without cloud service dependencies for its core functionality, maintaining all user data locally on the device.

---

## Certification Criteria

### 1. Network Independence
- [x] **No INTERNET Permission**: The application does not declare `android.permission.INTERNET` in its manifest
- [x] **No Network Operations**: All functionality works completely offline
- [x] **No Server Communication**: No communication with external servers or APIs

### 2. Data Storage
- [x] **Local-First Storage**: All user data stored exclusively on device
- [x] **Room Database**: Uses Android's local SQLite database via Room
- [x] **No Cloud Sync**: No automatic or manual cloud synchronization features
- [x] **User-Controlled Export**: Data export only occurs through explicit user action

### 3. Third-Party Services
- [x] **No Analytics**: No Google Analytics, Firebase Analytics, or similar services
- [x] **No Crash Reporting**: No proprietary crash reporting SDKs
- [x] **No Advertising**: No ad networks or tracking libraries
- [x] **No Remote Config**: No remote configuration services

### 4. Open Source Verification
- [x] **Fully Auditable**: All source code available for public audit
- [x] **Transparent Dependencies**: All dependencies are open source with clear licenses
- [x] **Buildable from Source**: Application can be built without proprietary tools

---

## Technical Architecture

### Data Flow Diagram

```
User Input → Local Processing → Room Database → Local Storage
                                              ↓
User Export ← File System ← Explicit User Action
```

### Key Components

1. **Local Database Layer**
   - Room Database (SQLite)
   - Local FTS (Full-Text Search)
   - No external database connections

2. **Storage Mechanisms**
   - Internal app storage for notes
   - Shared storage for exports only (explicit user action)
   - No cloud storage adapters

3. **Processing Pipeline**
   - All processing occurs on-device
   - No external API calls
   - No server-side processing

---

## Permissions Analysis

### Declared Permissions

| Permission | Purpose | Cloud-Related? |
|------------|---------|----------------|
| `POST_NOTIFICATIONS` | Local notifications only | No |
| `READ_EXTERNAL_STORAGE` | Image attachments (user-selected) | No |
| `READ_MEDIA_IMAGES` | Image picker functionality | No |
| `READ_MEDIA_VIDEO` | Video attachments | No |
| `VIBRATE` | Haptic feedback | No |

### Notable Absences

The following permissions are **NOT** declared:
- `INTERNET` - No network access
- `ACCESS_NETWORK_STATE` - No network monitoring
- `ACCESS_WIFI_STATE` - No WiFi state checking

---

## Verification Methods

### 1. Static Analysis
```bash
# Check for INTERNET permission
grep -r "android.permission.INTERNET" app/src/main/AndroidManifest.xml
# Result: No matches found

# Check for network-related imports
grep -r "java.net\|okhttp\|retrofit" app/src/main/java/
# Result: No matches found
```

### 2. Manifest Analysis
- Application ID: `com.markleaf.notes`
- Target SDK: 35
- Minimum SDK: 26
- No cloud service metadata
- No API keys in manifest

### 3. Dependency Analysis
All dependencies verified as:
- AndroidX libraries (local-only)
- Jetpack Compose (UI framework)
- Room (local database)
- Kotlin Coroutines (local threading)
- Material Design Components

**No cloud SDKs found:**
- No Firebase SDK
- No Google Play Services dependencies
- No AWS/Azure/GCP SDKs

---

## Data Privacy Guarantee

### What Stays Local
- ✅ All note content
- ✅ All tags and metadata
- ✅ All attachments (images)
- ✅ All user preferences
- ✅ Search indexes
- ✅ Backup files (user-initiated only)

### What Leaves the Device
- ❌ Nothing (unless explicitly exported by user)

---

## Compliance Statements

### GDPR Compliance
- No personal data transmission
- User has complete data control
- No data processing agreements required
- No data protection officer required for this app

### F-Droid Compatibility
- Meets all F-Droid inclusion criteria
- No proprietary dependencies
- Fully reproducible build
- Open source license (Apache 2.0)

---

## Certification Validity

This certification is valid for:
- **Markleaf Android version 1.1.15 and above**
- **All builds from the official repository**
- **F-Droid builds**

### Re-certification Required When:
- Network permissions are added
- Cloud services are integrated
- New dependencies with network capabilities are introduced

---

## Verification Contact

For verification of this certification:
- **Repository:** https://github.com/jeiel85/markleaf-android
- **License:** Apache 2.0
- **Verification:** Full source code available for audit

---

## Conclusion

Markleaf Android meets all criteria for **No-Cloud Certification**. The application demonstrates a commitment to user privacy through its local-first architecture, absence of network dependencies, and transparent open-source development model.

**Certified By:** Markleaf Development Team  
**Date of Issue:** 2024-05-04  
**Valid Until:** Next major version release (requires re-certification)

---

## Appendices

### Appendix A: Build Verification

To verify this certification:

1. Clone the repository:
   ```bash
   git clone https://github.com/jeiel85/markleaf-android.git
   ```

2. Check manifest permissions:
   ```bash
   cat app/src/main/AndroidManifest.xml | grep "uses-permission"
   ```

3. Verify no network code:
   ```bash
   find app/src/main/java -name "*.kt" -exec grep -l "URL\|HttpClient\|Network" {} \;
   ```

### Appendix B: Dependency List

See `app/build.gradle.kts` for complete dependency list. All dependencies are:
- Open source
- Available in Maven Central
- Do not require network access

### Appendix C: Architecture Diagrams

See project documentation for detailed architecture diagrams showing data flow and component interactions.

---

*This document serves as official certification of Markleaf's No-Cloud architecture and may be used for app store submissions, privacy compliance documentation, and user transparency initiatives.*