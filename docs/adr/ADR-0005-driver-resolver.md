# ADR-0005: Driver Resolver Engine

## Status

**Superseded** — проект полностью перешёл на HTTP-парсинг (Jsoup + HttpClient), Selenium/EdgeDriver и весь код обнаружения/валидации драйвера удалены из проекта. Документ сохранён как исторический контекст решения.

## Context

The Driver Download Engine needs to know where to download Microsoft Edge WebDriver. Initially, the download URL was constructed directly in `DownloadService` using a simple template:

```
https://msedgedriver.azureedge.net/{version}/edgedriver_win64.zip
```

This approach has several problems:
1. The URL template assumes Microsoft's CDN structure is stable and will never change
2. The download logic is coupled with URL generation logic
3. There's no way to validate that a driver exists before attempting download
4. Future support for other platforms (Linux, macOS) requires hardcoded changes
5. Architecture detection (x64, ARM64) is not considered

## Decision

We implemented the **Driver Resolver Engine** (`com.rmc.driver.resolver`) as a standalone subsystem responsible ONLY for resolving download metadata.

### Architecture

```
com.rmc.driver.resolver/
├── Platform.java           # Enum: WINDOWS, LINUX, MAC
├── Architecture.java      # Enum: X64, ARM64, UNKNOWN
├── DriverDownloadInfo.java # Immutable container for download metadata
├── DriverResolverException.java # Exception with error types
└── DriverResolver.java    # Main resolution service
```

### Key Responsibilities

1. **Platform Detection**: Detect current OS (Windows, Linux, macOS)
2. **Architecture Detection**: Detect CPU architecture (x64, ARM64)
3. **Version Normalization**: Extract major.minor.build from full version
4. **URL Construction**: Build Microsoft CDN URLs for the correct platform/arch
5. **Metadata Packaging**: Return all download information in one object

### DriverDownloadInfo Structure

```java
DriverDownloadInfo info = DriverDownloadInfo.builder()
    .browserVersion("146.0.3856.109")
    .driverVersion("146.0.3856")
    .downloadUrl("https://msedgedriver.azureedge.net/146.0.3856/edgedriver_win64.zip")
    .platform(Platform.WINDOWS)
    .architecture(Architecture.X64)
    .driverFileName("msedgedriver.exe")
    .archiveName("edgedriver_win64.zip")
    .build();
```

### Design Pattern

```
DownloadService
       ↓
DriverResolver.resolve(edgeVersion)
       ↓
DriverDownloadInfo (contains URL + metadata)
       ↓
DownloadService.download(url, info)
```

**DownloadService no longer builds URLs** - it receives them from DriverResolver.

### Why This Approach

1. **Single Responsibility**: DriverResolver ONLY resolves URLs, doesn't download
2. **Separation of Concerns**: URL generation is decoupled from download logic
3. **Extensibility**: Easy to add new platforms or change CDN sources
4. **Testability**: Resolver can be tested with mocked platform detection
5. **Future-Proof**: Can integrate with Microsoft API for dynamic URL resolution

## Alternatives Considered

### Alternative 1: Keep URL Building in DownloadService

Continue building URLs directly in `DownloadService` with a template.

**Rejected because:**
- Couples URL generation with download logic
- Makes it difficult to change CDN sources
- No validation before download attempt
- Violates single responsibility principle

### Alternative 2: Query Microsoft EdgeDriver Info API

Use `https://msedgedriver.azureedge.net/api/Downloadinfo` to dynamically get download URLs.

**Rejected because:**
- Adds network call to resolution process
- More complex error handling
- API may not be available in all environments
- Current static template approach is simpler and sufficient
- Can be implemented as enhancement in future

## Consequences

### Advantages

- **Clean Architecture**: Clear separation between resolution and download
- **Extensible**: Easy to add new platforms, CDNs, or resolution strategies
- **Testable**: Resolver logic can be unit tested without network
- **Maintainable**: URL changes only affect one class
- **Future-Ready**: Can integrate with Microsoft API later

### Limitations

- **Additional Abstraction**: More classes to maintain
- **Platform Detection**: Currently limited to Windows (others will fail gracefully)
- **Static URLs**: Still uses static template; not yet querying dynamic API

### Future Improvements

- Query Microsoft EdgeDriver Info API for validated download URLs
- Add support for Linux and macOS platforms
- Add ARM64 support for Windows ARM devices
- Cache resolution results to reduce repeated calls
- Add retry logic for failed API calls

## Dependencies

### This Module Uses
- `com.rmc.logging.AppLogger` - For structured logging
- Java Standard Library only (no external dependencies)

### Modules Using This Module
- `com.rmc.download.DownloadService` - Uses DriverResolver to get download URLs

## Public API

### Classes

| Class | Visibility | Purpose |
|-------|------------|---------|
| `Platform` | Public | Enum for OS detection (WINDOWS, LINUX, MAC) |
| `Architecture` | Public | Enum for CPU architecture (X64, ARM64) |
| `DriverDownloadInfo` | Public | Immutable container for download metadata |
| `DriverResolverException` | Public | Exception with error types |
| `DriverResolver` | Public | Main resolution service |

### DriverResolver

```java
// Main method
DriverDownloadInfo resolve(String browserVersion) throws DriverResolverException

// Utility methods
boolean isValidVersion(String version)
Platform[] getCurrentPlatformAndArchitecture()
```

### DriverDownloadInfo

```java
String getBrowserVersion()
String getDriverVersion()
String getDownloadUrl()
Platform getPlatform()
Architecture getArchitecture()
String getDriverFileName()
String getArchiveName()
```

### DriverResolverException

```java
// Error types
ErrorType.UNSUPPORTED_PLATFORM
ErrorType.UNSUPPORTED_ARCHITECTURE
ErrorType.INVALID_VERSION
ErrorType.UNKNOWN_BROWSER_VERSION
ErrorType.NETWORK_ERROR
ErrorType.UNKNOWN_ERROR

// Factory methods
DriverResolverException.unsupportedPlatform(Platform)
DriverResolverException.unsupportedArchitecture(Architecture)
DriverResolverException.invalidVersion(String)
DriverResolverException.unknownBrowserVersion(String)
```

## Future Work

### TASK-006+: Integrate Microsoft EdgeDriver Info API

Future enhancement to query Microsoft's dynamic API:

```java
// Instead of static template:
String url = "https://msedgedriver.azureedge.net/" + version + "/edgedriver_win64.zip";

// Use dynamic API:
String url = queryMicrosoftApi(browserVersion); // Returns verified URL
```

### TASK-007: Cross-Platform Support

Extend platform detection and URL generation for:
- Linux (Ubuntu, Debian, Fedora, etc.)
- macOS (Intel and Apple Silicon)

### TASK-008: Driver Update Engine

Use DriverResolver in update workflow:
1. Get current driver version
2. Query for latest available version
3. Compare and prompt for update if needed
