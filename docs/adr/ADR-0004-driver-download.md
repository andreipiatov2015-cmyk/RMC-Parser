# ADR-0004: Driver Download Engine

## Status

**Superseded** — проект полностью перешёл на HTTP-парсинг (Jsoup + HttpClient), Selenium/EdgeDriver и весь код обнаружения/валидации драйвера удалены из проекта. Документ сохранён как исторический контекст решения.

## Context

After detecting that Microsoft Edge is installed but the correct WebDriver is not present, the RMC Framework must be able to automatically download the matching WebDriver version. 

The challenge is:
1. Matching the WebDriver version exactly to the installed Edge browser version
2. Downloading from official Microsoft sources
3. Extracting and organizing the downloaded driver
4. Storing the driver in a standardized location accessible to the application

## Decision

We implemented the **Driver Download Engine** (`com.rmc.download`) as a standalone subsystem responsible for:

1. **Detecting Edge version** by reusing the Driver Detection Engine
2. **Building download URLs** dynamically based on Edge version
3. **Downloading ZIP archives** from Microsoft's official CDN
4. **Extracting the driver** and placing it in the designated storage location
5. **Creating metadata** (version.txt) for future quick checks
6. **Providing structured results** with detailed error information

### Architecture

```
com.rmc.download/
├── DownloadResult.java     # Immutable result container with error types
└── DownloadService.java    # Download workflow orchestration
```

### Storage Location

```
%LOCALAPPDATA%\RMCParser\drivers\edge\
├── msedgedriver.exe        # The WebDriver executable
└── version.txt            # Version metadata for quick checks
```

**Why this location:**
- `%LOCALAPPDATA%` is user-writable without admin privileges
- Separates user data from application installation directory
- Follows Windows conventions for user-specific application data
- Allows multiple application versions to coexist without conflict

### Download URL Strategy

**Current Implementation:**
The download URL is constructed using the Microsoft Edge WebDriver CDN:

```
https://msedgedriver.azureedge.net/{version}/edgedriver_win64.zip
Example: https://msedgedriver.azureedge.net/146.0.3856/edgedriver_win64.zip
```

**Version Mapping:**
- Input: Full Edge version (e.g., `146.0.3856.109`)
- Output: Driver version (e.g., `146.0.3856` - major.minor.build)
- The fourth segment (revision) is dropped as drivers are compatible across revisions

### Why This Approach

1. **Dynamic URL Generation**: Version is extracted from detected Edge, ensuring match
2. **Official Source**: Microsoft CDN provides verified, official builds
3. **No Bundling**: Drivers are downloaded on-demand, keeping application size small
4. **Separation of Concerns**: Download logic is isolated from detection and execution
5. **Metadata Creation**: version.txt enables quick status checks without re-detection

## Alternatives Considered

### Alternative 1: Bundle Drivers with Application

Include all supported driver versions in the application distribution.

**Rejected because:**
- Dramatically increases application download/installation size
- Requires shipping multiple versions for each Edge update
- Difficult to maintain as Edge releases frequent updates
- Violates the requirement not to bundle drivers in the installer

### Alternative 2: Manual Download with User Intervention

Require users to manually download and configure the driver.

**Rejected because:**
- Poor user experience - automation is a key requirement
- Users may download wrong versions
- Manual configuration adds complexity and potential for errors
- The system should handle this transparently

### Alternative 3: WebDriverManager Integration

Use WebDriverManager library to handle automatic driver downloads.

**Rejected because:**
- Adds third-party dependency
- Less control over download source and storage location
- Project requirements explicitly prohibit WebDriverManager
- Hides important URL resolution logic that should be documented

## Consequences

### Advantages

- **Automatic Resolution**: No user intervention required
- **Version Matching**: Ensures driver matches browser exactly
- **Clean Storage**: Standardized location for all driver data
- **Metadata Available**: version.txt enables efficient status checks
- **Official Source**: Microsoft CDN guarantees authenticity

### Limitations

- **Network Required**: Download requires internet connectivity
- **Windows-Only**: Uses Windows-specific paths and PowerShell
- **Static URL Pattern**: Current implementation assumes consistent Microsoft CDN structure
- **No Retry Logic**: Failed downloads must be manually retried

### Future Improvements

- **Driver Resolver Engine**: Move URL generation to dedicated subsystem (see below)
- **Progress Callbacks**: Real-time progress updates for UI integration
- **Retry Logic**: Automatic retry with exponential backoff for failed downloads
- **Checksum Verification**: SHA256 verification of downloaded files
- **Multiple Platforms**: Support for macOS and Linux Edge drivers

## Dependencies

### This Module Uses
- `com.rmc.driver.EdgeDetector` - Gets installed Edge version
- `com.rmc.logging.AppLogger` - For structured logging
- Java Standard Library: `java.net.HttpURLConnection`, `java.util.zip.ZipInputStream`

### Modules Using This Module
- Future: UI will trigger downloads via DownloadService
- Future: Update Engine may coordinate driver updates

## Public API

### Classes

| Class | Visibility | Purpose |
|-------|------------|---------|
| `DownloadResult` | Public | Immutable result container with success/error info |
| `DownloadService` | Public | Static methods for download operations |

### DownloadResult

```java
// Success result
DownloadResult.success(String driverPath, String version)

// Error results
DownloadResult.edgeNotInstalled()
DownloadResult.alreadyExists(String path, String version)
DownloadResult.internetUnavailable(Exception e)
DownloadResult.httpError(int statusCode)
DownloadResult.zipError(String message)
DownloadResult.extractionError(String message)
DownloadResult.permissionDenied(String message)

// Accessors
boolean isSuccess()
String getDriverPath()
String getVersion()
String getErrorMessage()
ErrorType getErrorType()
```

### DownloadService

```java
DownloadResult downloadDriver()
```

**Workflow:**
1. Detect Edge version via EdgeDetector
2. Check for existing compatible driver
3. Build download URL
4. Download ZIP archive with progress logging
5. Extract and install driver
6. Create version.txt
7. Cleanup temporary files

## Important: URL Generation Responsibility

**Current State:** The `DownloadService` class currently generates download URLs directly using a static template pattern:

```
https://msedgedriver.azureedge.net/{version}/edgedriver_win64.zip
```

**Future State:** This responsibility will be moved to the **Driver Resolver Engine** (TASK-006).

The Driver Resolver Engine will:
- Query Microsoft's official metadata API for available drivers
- Validate driver availability for specific Edge versions
- Handle different platform/channel combinations
- Cache URL resolution results

**Why this matters:**
1. The current static URL pattern assumes Microsoft's CDN structure is stable
2. Microsoft may change download URLs in the future
3. Centralizing URL resolution makes the system more maintainable
4. The Download Engine should focus on download logistics, not URL logic

**Migration Plan:**
When Driver Resolver Engine is implemented:
1. Create interface `DriverUrlResolver`
2. Implement `MicrosoftDriverUrlResolver` as default
3. Inject resolver into `DownloadService`
4. Move URL template and resolution logic to resolver
5. Update tests accordingly

## Future Work

### TASK-005: UI Enhancement

The UI will be enhanced to:
- Display download progress
- Show download results
- Allow manual download triggers
- Display storage location

### TASK-006: Driver Resolver Engine

The Driver Resolver Engine will:
- Query `https://msedgedriverriverwebdriverinfo.azurewebsites.net/api/Downloadinfo`
- Return official download URLs for any Edge version
- Handle edge cases (version not found, deprecated versions)
- Provide caching to reduce API calls

### TASK-010: Driver Update Engine

A future subsystem will:
- Compare installed driver version with latest available
- Coordinate with Driver Resolver for update URLs
- Handle automatic driver updates
- Notify users of available updates
