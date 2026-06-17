# ADR-0007: Application Lifecycle and Developer Diagnostics

## Status

**Accepted**

## Context

Before implementing the Browser Engine (Selenium integration), the development team identified the need for:

1. **Testing Interface** - A way to test each subsystem independently
2. **Diagnostic Capabilities** - Real-time visibility into application state
3. **Startup Sequence** - A defined, logged initialization process
4. **Troubleshooting** - Easy access to logs and environment information

Previously, testing required:
- Running the application
- Manually checking log files
- Using external tools to inspect system state

This was time-consuming and error-prone.

## Decision

We implemented two major components:

### 1. Application Lifecycle Manager

```java
public class ApplicationLifecycle {
    public LifecycleReport start() {
        // Step 1: Load Configuration
        // Step 2: Initialize Logging
        // Step 3: Load Version Engine
        // Step 4: Check Updates
        // Step 5: Driver Detection
        // Step 6: Driver Validation
        // Step 7: Application Ready
    }
}
```

**Startup Sequence:**
```
Application start
       ↓
Load Configuration
       ↓
Initialize Logging
       ↓
Load Version Engine
       ↓
Check Updates
       ↓
Driver Detection
       ↓
Driver Validation
       ↓
Application Ready
```

### 2. Developer Diagnostics Window

A comprehensive diagnostics interface with:

- **Live Log Console** - Real-time log display with auto-scroll
- **System Status Panel** - Visual indicators for each subsystem
- **Environment Information** - System and application details
- **Diagnostic Buttons** - One-click testing for each subsystem

**Buttons:**
- Check Updates
- Check Driver
- Download Driver
- Validate Driver
- Run Startup Sequence
- Create Diagnostic Report
- Open Log Folder
- Clear Driver
- Clear Logs
- Exit

## Why Application Lifecycle

### 1. Predictable Startup

The application has a defined startup sequence. Each step is:
- Logged to the console and file
- Captured in a LifecycleReport
- Independently testable

### 2. Error Isolation

If a step fails, we know exactly:
- Which step failed
- What error occurred
- What state the application is in

### 3. Report Generation

LifecycleReport provides a complete picture:

```java
public class LifecycleReport {
    // Configuration
    private boolean configLoaded;
    
    // Version
    private String applicationVersion;
    
    // Edge
    private boolean edgeDetected;
    private String edgeVersion;
    
    // Driver
    private boolean driverDetected;
    private String driverVersion;
    
    // Validation
    private ValidationStatus validationStatus;
    
    // Overall
    private boolean applicationReady;
}
```

## Why Developer Diagnostics

### 1. Independent Subsystem Testing

Every subsystem can be tested independently:

```java
// Test version engine
btnCheckUpdates.setOnAction(e -> {
    UpdateService.UpdateCheckResult result = UpdateService.checkForUpdates();
    log("Latest version: " + result.getLatestVersion());
});

// Test driver detection
btnCheckDriver.setOnAction(e -> {
    EdgeInfo edge = EdgeDetector.detect();
    DriverInfo driver = DriverDetector.detect();
    log("Edge: " + edge.getVersion());
    log("Driver: " + driver.getVersion());
});

// Test validation
btnValidateDriver.setOnAction(e -> {
    ValidationResult result = DriverValidator.validate(...);
    log("Validation: " + result.getStatus());
});
```

### 2. Live Feedback

The log console shows:
- All log messages in real-time
- Clear formatting with timestamp, level, and source
- Auto-scroll to latest messages

### 3. System Status

Visual indicators show subsystem health:

```
Config: OK       (Green)
Logging: OK      (Green)
Version: OK      (Green)
Update: OK       (Green)
Driver: OK       (Green)
Validation: OK    (Green)
Internet: OK     (Green)
```

## Why Every Subsystem Must Be Testable

### 1. Independent Verification

Each subsystem must be testable without:
- Running the entire application
- Setting up external dependencies
- Manually triggering events

### 2. Clear Contracts

Each subsystem defines:
- Input (what it needs)
- Output (what it produces)
- Side effects (what it logs/does)

### 3. Fail-Fast Behavior

Problems are detected immediately:
- At startup (ApplicationLifecycle)
- During operation (Developer Diagnostics)

## Live Log Console Implementation

### Architecture

```
┌─────────────────┐
│   Logback       │
│   (Log Events) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ UiLogAppender   │  ← Custom Logback Appender
│ (JavaFX Thread) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   TextArea      │
│   (UI Display)  │
└─────────────────┘
```

### Features

1. **Thread-Safe** - Uses CopyOnWriteArrayList for buffering
2. **Non-Blocking** - Platform.runLater() for UI updates
3. **Auto-Scroll** - Automatically scrolls to latest message
4. **Buffer Management** - Trims old messages to prevent memory issues

## Consequences

### Advantages

- **Easy Testing** - One-click subsystem verification
- **Clear Visibility** - Real-time log display
- **Quick Diagnosis** - System status at a glance
- **No External Tools** - Everything built-in
- **Documentation** - Actions are logged automatically

### Limitations

- **JavaFX Dependency** - Requires JavaFX to run
- **UI Complexity** - More code than console-only
- **Platform Lock-In** - Uses JavaFX (cross-platform but requires JVM)

### Future Improvements

- Web-based diagnostics (REST API)
- Remote diagnostics (connect to running instances)
- Export logs to file
- Email alerts for failures

## Dependencies

### This Module Uses

| Component | Purpose |
|-----------|---------|
| JavaFX TextArea | Log display |
| JavaFX Platform | Thread-safe UI updates |
| Logback | Log capture |
| SLF4J | Log facade |

### Used By

| Component | Purpose |
|-----------|---------|
| Main | Startup and window management |
| ApplicationLifecycle | Status reporting |
| All subsystems | Logging during operations |

## API Design

### LifecycleReport

```java
public class LifecycleReport {
    // Configuration
    boolean isConfigLoaded();
    String getRepositoryOwner();
    String getRepositoryName();
    
    // Version
    String getApplicationVersion();
    String getLatestVersion();
    
    // Edge
    boolean isEdgeDetected();
    String getEdgeVersion();
    
    // Driver
    boolean isDriverDetected();
    String getDriverVersion();
    
    // Validation
    ValidationStatus getValidationStatus();
    
    // Overall
    boolean isApplicationReady();
}
```

### LifecycleState Enum

```java
public enum LifecycleState {
    NOT_STARTED,
    LOADING_CONFIG,
    CONFIG_LOADED,
    INITIALIZING_LOGGING,
    LOGGING_INITIALIZED,
    LOADING_VERSION_ENGINE,
    VERSION_ENGINE_LOADED,
    CHECKING_UPDATES,
    UPDATES_CHECKED,
    DETECTING_DRIVER,
    DRIVER_DETECTED,
    VALIDATING_DRIVER,
    DRIVER_VALIDATED,
    READY,
    FAILED
}
```

## Alternatives Considered

### Alternative 1: Console-Only Logging

**Rejected because:**
- Requires file access to view logs
- No real-time feedback during operation
- Difficult for non-technical users
- No visual status indicators

### Alternative 2: Separate Test Application

**Rejected because:**
- Duplication of code
- Harder to maintain
- Not available in production
- More complex build process

### Alternative 3: External Monitoring

**Rejected because:**
- Requires additional software
- Not integrated with application
- Network complexity
- Security concerns

## Conclusion

The Application Lifecycle and Developer Diagnostics provide a solid foundation for:
- Testing individual subsystems
- Diagnosing issues
- Understanding application state
- Supporting end users

Every future subsystem should integrate with:
1. ApplicationLifecycle for startup
2. Logging for diagnostics
3. LifecycleReport for status reporting
