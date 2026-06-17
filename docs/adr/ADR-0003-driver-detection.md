# ADR-0003: Driver Detection Engine

## Status

**Accepted**

## Context

The RMC Framework needs to automate web browser interactions using Microsoft Edge. Before any automation can occur, the system must be able to locate and verify the presence of:
1. Microsoft Edge browser (msedge.exe)
2. Microsoft Edge WebDriver (msedgedriver.exe)

The challenge is that these components may or may not be installed on the user's system, and their installation locations vary across Windows configurations.

## Decision

We implemented the **Driver Detection Engine** (`com.rmc.driver`) as a standalone subsystem responsible for:

1. **Detecting Edge browser** at standard Windows installation paths
2. **Detecting WebDriver** at application-relative paths
3. **Reading file versions** without executing applications
4. **Comparing versions** to determine compatibility
5. **Providing structured results** via enums and data classes

### Architecture

```
com.rmc.driver/
├── EdgeDetector.java    # Locates Microsoft Edge executable
├── EdgeInfo.java        # Immutable Edge metadata (path, version, installed)
├── DriverDetector.java   # Locates msedgedriver.exe
├── DriverInfo.java      # Immutable Driver metadata (path, version, installed)
├── DriverStatus.java    # Enum: NOT_INSTALLED, MATCH, OUTDATED, UNKNOWN
└── DriverService.java  # Orchestrates detection and comparison
```

### Detection Strategy

**Edge Detection:**
- Search standard Windows installation directories
- Use PowerShell to read FileVersion metadata without executing the application
- Return structured `EdgeInfo` with all relevant metadata

**Driver Detection:**
- Search application-relative paths: `{app_dir}/drivers/edge/`
- Same version extraction approach as Edge detection
- Return structured `DriverInfo`

**Version Comparison:**
- Extract major.minor.patch from version strings
- Compare numerically (not lexicographically)
- Return `DriverStatus.MATCH` when versions align

### Why This Approach

1. **Separation of Concerns**: Detection logic is isolated from download and update logic
2. **No Execution**: Reading FileVersion via PowerShell is safe and doesn't launch Edge
3. **Structured Results**: Enums and data classes provide type-safe status reporting
4. **Independent**: The subsystem has no dependencies on other application components
5. **Testable**: Pure detection methods can be easily unit tested

## Alternatives Considered

### Alternative 1: Use WebDriverManager Library

WebDriverManager is a popular Java library that automatically handles driver detection and downloading.

**Rejected because:**
- Adds a third-party dependency that must be maintained
- Downloads drivers on-the-fly, which we want to control separately
- Bundles its own HTTP client and caching logic
- The project requirements explicitly prohibit WebDriverManager

### Alternative 2: Execute msedgedriver.exe --version

The driver executable can report its own version when run with `--version` flag.

**Rejected because:**
- Requires actually executing an external process
- The process might fail or hang
- Security concerns with running untrusted executables
- Slower than simply reading file metadata
- Violates the requirement to not execute the target applications

## Consequences

### Advantages

- **Safe Detection**: No applications are executed during detection
- **Fast**: FileVersion lookup is instantaneous
- **Independent**: No dependencies on other subsystems
- **Testable**: Can be tested without mocking external resources
- **Clear API**: Data classes provide clear, immutable interfaces

### Limitations

- **Windows-Only**: PowerShell commands are Windows-specific
- **Limited Search Paths**: Only standard installation paths are searched
- **Manual Version Comparison**: Comparison logic is basic (major.minor.patch only)

### Future Improvements

- Support for user-specified custom search paths
- Detection of driver capabilities beyond version matching
- Integration with Driver Resolver Engine for dynamic URL resolution
- Support for Edge Canary/Dev/Beta channels

## Dependencies

### This Module Uses
- `com.rmc.logging.AppLogger` - For structured logging

### Modules Using This Module
- `com.rmc.download.DownloadService` - Uses EdgeDetector to get Edge version
- `com.rmc.driver.DriverService` - Provides combined detection and comparison

## Public API

### Classes

| Class | Visibility | Purpose |
|-------|------------|---------|
| `EdgeInfo` | Public | Immutable data class for Edge browser information |
| `DriverInfo` | Public | Immutable data class for WebDriver information |
| `DriverStatus` | Public | Enum representing driver status |
| `EdgeDetector` | Public | Static methods for Edge detection |
| `DriverDetector` | Public | Static methods for driver detection |
| `DriverService` | Public | Orchestration service for full detection workflow |

### Key Methods

```java
// Detection
EdgeInfo EdgeDetector.detect()
DriverInfo DriverDetector.detect()
DriverStatus DriverService.detectAndCompare()

// Data access
boolean EdgeInfo.isInstalled()
String EdgeInfo.getVersion()
String EdgeInfo.getPath()

// Status
DriverStatus.DriverStatus.MATCH      // Driver matches browser version
DriverStatus.DriverStatus.OUTDATED  // Driver is older than browser
DriverStatus.DriverStatus.NOT_INSTALLED  // Component not found
DriverStatus.DriverStatus.UNKNOWN    // Comparison failed
```

## Future Work

### TASK-006: Driver Resolver Engine

The Driver Resolver Engine will be responsible for:
- Dynamically resolving official Microsoft download URLs
- Querying the Microsoft Edge WebDriver JSON metadata API
- Validating driver availability for specific Edge versions
- Caching URL resolution results

**Important**: When Driver Resolver Engine is implemented, URL generation responsibility will be moved from `DownloadService` to the new subsystem. The Driver Download Engine must NOT permanently own URL generation logic.

### TASK-005: UI Enhancement

The UI will be enhanced to:
- Display detected Edge and driver information
- Show current DriverStatus
- Trigger detection on demand

### TASK-010: Driver Update Engine

A future subsystem will:
- Compare installed driver with latest available version
- Handle automatic driver updates
- Manage multiple driver versions
