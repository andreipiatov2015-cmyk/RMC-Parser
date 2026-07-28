# ADR-0006: Driver Validation Engine

## Status

**Superseded** — проект полностью перешёл на HTTP-парсинг (Jsoup + HttpClient), Selenium/EdgeDriver и весь код обнаружения/валидации драйвера удалены из проекта. Документ сохранён как исторический контекст решения.

## Context

Before the RMC Framework can use Microsoft Edge WebDriver, it must verify that:
1. The driver is installed
2. The driver is the correct version
3. The driver is usable (not corrupted)

Previously, this validation was implicit in the Download Service and Driver Detection subsystems. However, there was no centralized way to:
- Track driver metadata over time
- Support future update workflows
- Provide detailed validation reports to users

## Decision

We implemented the **Driver Validation Engine** (`com.rmc.driver.validation`) with the following components:

### Architecture

```
com.rmc.driver.validation/
├── ValidationStatus.java   # Enum: VALID, INVALID, MISSING, VERSION_MISMATCH, UNKNOWN
├── DriverManifest.java      # Immutable container for driver metadata
├── ValidationResult.java   # Result container with manifest and status
└── DriverValidator.java    # Validation logic
```

### Key Design Decisions

#### 1. DriverManifest as Central Object

The `DriverManifest` is designed to be the **central object** for Driver Manager:

```java
DriverManifest manifest = DriverManifest.builder()
    .browserVersion("146.0.3856.109")
    .driverVersion("146.0.3856")
    .driverPath("C:\\Users\\...\\msedgedriver.exe")
    .platform(Platform.WINDOWS)
    .architecture(Architecture.X64)
    .installationTime(LocalDateTime.now())
    .validationTime(LocalDateTime.now())
    .status(ValidationStatus.VALID)
    .build();
```

**Why this matters:**
- Future Driver Update Engine will work only with `DriverManifest`
- All driver operations (download, update, verify) produce/update manifests
- Manifest can be persisted and compared over time
- Single source of truth for driver state

#### 2. ValidationResult Instead of Boolean

Using `ValidationResult` instead of a simple boolean provides:

```java
// Instead of:
boolean isValid = validateDriver();

// We use:
ValidationResult result = DriverValidator.validate(...);
if (result.isValid()) {
    DriverManifest manifest = result.getManifest();
    // Access detailed metadata
}
```

**Benefits over boolean:**
- Contains the validated manifest with all metadata
- Provides human-readable validation message
- Distinguishes between different failure modes (MISSING vs VERSION_MISMATCH)
- Extensible for future status types
- Enables detailed logging and user feedback

### Validation Process

1. **Existence Check**: Does the driver file exist?
2. **File Check**: Is it a regular file and readable?
3. **Version Check**: Does the file version match expected?
4. **Platform Check**: Is the platform Windows?
5. **Architecture Check**: Is the architecture supported?

### Why This Approach

1. **Immutable Manifest**: Once created, manifest cannot be modified
2. **Builder Pattern**: Flexible construction with validation
3. **Comprehensive Status**: Clear distinction between validation states
4. **Central Metadata**: Single object for all driver information
5. **Future-Ready**: Designed for Driver Update Engine integration

## Alternatives Considered

### Alternative 1: Simple Boolean Validation

```java
public static boolean isDriverValid(String path, String expectedVersion);
```

**Rejected because:**
- No metadata returned (what version is installed?)
- No distinction between failure modes
- Cannot track installation/validation history
- Poor UX - can't tell user WHY validation failed

### Alternative 2: Validation Without Manifest

Return validation status and details separately:

```java
public static ValidationStatus validate(Path path, String expectedVersion);
public static String getValidationMessage();
```

**Rejected because:**
- Two calls required to get complete information
- State management issues
- Not thread-safe
- More complex API

### Alternative 3: Mutable DriverInfo Class

Store validation state in a mutable DriverInfo object:

```java
public class DriverInfo {
    private String version;
    private ValidationStatus status;
    // ... setters ...
}
```

**Rejected because:**
- Mutable objects are harder to reason about
- Thread-safety concerns
- Cannot be safely shared or cached
- Violates immutability principle

## Consequences

### Advantages

- **Immutable Manifest**: Safe to share, cache, and store
- **Detailed Results**: Clear status, message, and metadata
- **Future Integration**: Driver Update Engine can use manifests
- **Testability**: Pure validation logic is easily tested
- **Extensibility**: Easy to add new validation checks

### Limitations

- **More Objects**: More classes than simple boolean
- **Platform Lock-In**: Currently only supports Windows
- **No Caching**: Each validation reads file version

### Future Improvements

- Persist manifests to disk for offline checks
- Add validation for driver capabilities
- Support cross-platform validation
- Add validation timestamps to manifest

## Dependencies

### This Module Uses
- `com.rmc.driver.resolver.Platform` - Platform detection
- `com.rmc.driver.resolver.Architecture` - Architecture detection
- `com.rmc.logging.AppLogger` - Structured logging
- Java Standard Library only

### Modules Using This Module
- `com.rmc.download.DownloadService` - Validate after download
- `com.rmc.driver.DriverService` - Integrate with detection workflow
- **Future**: Driver Update Engine (TASK-010+)

## Public API

### Classes

| Class | Visibility | Purpose |
|-------|------------|---------|
| `ValidationStatus` | Public | Enum for validation outcomes |
| `DriverManifest` | Public | Immutable driver metadata |
| `ValidationResult` | Public | Validation result container |
| `DriverValidator` | Public | Validation logic |

### ValidationStatus

```java
VALID              // Driver is valid and usable
INVALID            // Driver exists but is invalid
MISSING            // Driver file does not exist
VERSION_MISMATCH   // Version doesn't match
UNKNOWN            // Validation failed (error)
```

### DriverManifest

```java
// Builder usage
DriverManifest manifest = DriverManifest.builder()
    .browserVersion(String)
    .driverVersion(String)
    .driverPath(String)
    .platform(Platform)
    .architecture(Architecture)
    .installationTime(LocalDateTime)
    .validationTime(LocalDateTime)
    .status(ValidationStatus)
    .build();

// Accessors
String getBrowserVersion()
String getDriverVersion()
String getDriverPath()
Platform getPlatform()
Architecture getArchitecture()
ValidationStatus getStatus()
boolean isValid()
boolean isMissing()
```

### ValidationResult

```java
// Factory methods
ValidationResult.success(manifest)
ValidationResult.failure(status, message)
ValidationResult.missing()
ValidationResult.versionMismatch(manifest, expected, actual)

// Accessors
ValidationStatus getStatus()
String getMessage()
DriverManifest getManifest()
boolean isValid()
```

## Future Work

### Driver Update Engine (TASK-010+)

The Driver Update Engine will use `DriverManifest` as its central object:

```java
// Future update workflow
DriverManifest currentManifest = DriverValidator.validate(...);
DriverManifest latestManifest = queryLatestDriver(browserVersion);

if (currentManifest.isOlderThan(latestManifest)) {
    // Prompt user or auto-update
}
```

### Manifest Persistence

Future enhancement to persist manifests:

```java
// Store manifest to disk
ManifestStore.save(manifest);

// Load manifest from disk
DriverManifest stored = ManifestStore.load();
```

### Cross-Platform Validation

Extend validation for Linux and macOS:

```java
// Future: Validate on all platforms
if (manifest.getPlatform() == Platform.LINUX) {
    validateLinuxDriver(manifest);
}
```
