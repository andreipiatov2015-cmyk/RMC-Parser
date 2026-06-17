package com.rmc.app;

import com.rmc.driver.DriverStatus;
import com.rmc.driver.validation.ValidationStatus;

import java.time.LocalDateTime;

/**
 * Report containing the results of the application lifecycle.
 */
public class LifecycleReport {

    // Configuration
    private boolean configLoaded;
    private String configError;
    private String repositoryOwner;
    private String repositoryName;
    private String updateChannel;

    // Logging
    private boolean loggingInitialized;

    // Version
    private boolean versionEngineLoaded;
    private String versionError;
    private String applicationVersion;
    private String latestVersion;

    // Updates
    private boolean updateCheckCompleted;
    private boolean updateAvailable;
    private String updateCheckError;

    // Driver Detection
    private boolean edgeDetected;
    private String edgeVersion;
    private String edgePath;
    private boolean driverDetected;
    private String driverVersion;
    private String driverPath;
    private DriverStatus driverStatus;
    private String driverDetectionError;

    // Validation
    private boolean validationCompleted;
    private ValidationStatus validationStatus;
    private String validationError;

    // Overall
    private boolean applicationReady;
    private LocalDateTime startTime;
    private String error;

    // Getters
    public boolean isConfigLoaded() { return configLoaded; }
    public String getConfigError() { return configError; }
    public String getRepositoryOwner() { return repositoryOwner; }
    public String getRepositoryName() { return repositoryName; }
    public String getUpdateChannel() { return updateChannel; }

    public boolean isLoggingInitialized() { return loggingInitialized; }

    public boolean isVersionEngineLoaded() { return versionEngineLoaded; }
    public String getVersionError() { return versionError; }
    public String getApplicationVersion() { return applicationVersion; }
    public String getLatestVersion() { return latestVersion; }

    public boolean isUpdateCheckCompleted() { return updateCheckCompleted; }
    public boolean isUpdateAvailable() { return updateAvailable; }
    public String getUpdateCheckError() { return updateCheckError; }

    public boolean isEdgeDetected() { return edgeDetected; }
    public String getEdgeVersion() { return edgeVersion; }
    public String getEdgePath() { return edgePath; }
    public boolean isDriverDetected() { return driverDetected; }
    public String getDriverVersion() { return driverVersion; }
    public String getDriverPath() { return driverPath; }
    public DriverStatus getDriverStatus() { return driverStatus; }
    public String getDriverDetectionError() { return driverDetectionError; }

    public boolean isValidationCompleted() { return validationCompleted; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public String getValidationError() { return validationError; }

    public boolean isApplicationReady() { return applicationReady; }
    public LocalDateTime getStartTime() { return startTime; }
    public String getError() { return error; }

    // Setters
    public void setConfigLoaded(boolean configLoaded) { this.configLoaded = configLoaded; }
    public void setConfigError(String configError) { this.configError = configError; }
    public void setRepositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    public void setUpdateChannel(String updateChannel) { this.updateChannel = updateChannel; }

    public void setLoggingInitialized(boolean loggingInitialized) { this.loggingInitialized = loggingInitialized; }

    public void setVersionEngineLoaded(boolean versionEngineLoaded) { this.versionEngineLoaded = versionEngineLoaded; }
    public void setVersionError(String versionError) { this.versionError = versionError; }
    public void setApplicationVersion(String applicationVersion) { this.applicationVersion = applicationVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }

    public void setUpdateCheckCompleted(boolean updateCheckCompleted) { this.updateCheckCompleted = updateCheckCompleted; }
    public void setUpdateAvailable(boolean updateAvailable) { this.updateAvailable = updateAvailable; }
    public void setUpdateCheckError(String updateCheckError) { this.updateCheckError = updateCheckError; }

    public void setEdgeDetected(boolean edgeDetected) { this.edgeDetected = edgeDetected; }
    public void setEdgeVersion(String edgeVersion) { this.edgeVersion = edgeVersion; }
    public void setEdgePath(String edgePath) { this.edgePath = edgePath; }
    public void setDriverDetected(boolean driverDetected) { this.driverDetected = driverDetected; }
    public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }
    public void setDriverPath(String driverPath) { this.driverPath = driverPath; }
    public void setDriverStatus(DriverStatus driverStatus) { this.driverStatus = driverStatus; }
    public void setDriverDetectionError(String driverDetectionError) { this.driverDetectionError = driverDetectionError; }

    public void setValidationCompleted(boolean validationCompleted) { this.validationCompleted = validationCompleted; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }
    public void setValidationError(String validationError) { this.validationError = validationError; }

    public void setApplicationReady(boolean applicationReady) { this.applicationReady = applicationReady; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setError(String error) { this.error = error; }

    /**
     * Generate a summary string.
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Application Lifecycle Summary ===\n");
        sb.append("Application Version: ").append(applicationVersion).append("\n");
        sb.append("Application Ready: ").append(applicationReady).append("\n");
        sb.append("\n");
        sb.append("Edge: ").append(edgeDetected ? "Detected (" + edgeVersion + ")" : "Not Detected").append("\n");
        sb.append("Driver: ").append(driverDetected ? "Detected (" + driverVersion + ")" : "Not Detected").append("\n");
        sb.append("Validation: ").append(validationStatus != null ? validationStatus : "Not Completed").append("\n");
        return sb.toString();
    }
}
