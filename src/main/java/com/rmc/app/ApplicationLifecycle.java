package com.rmc.app;

import com.rmc.config.UpdateConfig;
import com.rmc.download.DownloadResult;
import com.rmc.download.DownloadService;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverService;
import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.DriverInfo;
import com.rmc.driver.DriverStatus;
import com.rmc.driver.validation.DriverManifest;
import com.rmc.driver.validation.DriverValidator;
import com.rmc.driver.validation.ValidationResult;
import com.rmc.driver.validation.ValidationStatus;
import com.rmc.logging.AppLogger;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Manages the application lifecycle from startup to shutdown.
 * 
 * <p>Startup sequence:</p>
 * <pre>
 * Application start
 *       ↓
 * Load Configuration
 *       ↓
 * Initialize Logging (already done)
 *       ↓
 * Load Version Engine
 *       ↓
 * Check Updates
 *       ↓
 * Driver Detection
 *       ↓
 * Driver Validation
 *       ↓
 * Application Ready
 * </pre>
 */
public class ApplicationLifecycle {

    private static final Logger logger = AppLogger.getLogger();

    private LifecycleState currentState;
    private LifecycleReport report;

    public ApplicationLifecycle() {
        this.currentState = LifecycleState.NOT_STARTED;
        this.report = new LifecycleReport();
    }

    /**
     * Execute the full startup sequence.
     *
     * @return LifecycleReport containing results of each step
     */
    public LifecycleReport start() {
        logger.info("=================================================");
        logger.info("Application Lifecycle - Starting");
        logger.info("=================================================");

        try {
            // Step 1: Load Configuration
            step1_LoadConfiguration();

            // Step 2: Initialize Logging (already done, just log it)
            step2_InitializeLogging();

            // Step 3: Load Version Engine
            step3_LoadVersionEngine();

            // Step 4: Check Updates
            step4_CheckUpdates();

            // Step 5: Driver Detection
            step5_DriverDetection();

            // Step 6: Driver Validation
            step6_DriverValidation();

            // Step 7: Application Ready
            step7_ApplicationReady();

            logger.info("=================================================");
            logger.info("Application Lifecycle - Completed Successfully");
            logger.info("=================================================");

        } catch (Exception e) {
            logger.error("Application Lifecycle - Failed", e);
            report.setError(e.getMessage());
            currentState = LifecycleState.FAILED;
        }

        return report;
    }

    private void step1_LoadConfiguration() {
        currentState = LifecycleState.LOADING_CONFIG;
        logger.info("[1/7] Loading Configuration...");

        try {
            UpdateConfig config = UpdateConfig.load();
            String jsonUrl = config.getJsonUrl();
            String channel = config.getChannel();
            
            // Extract owner/repo from JSON URL
            String owner = "unknown";
            String repo = "unknown";
            if (jsonUrl != null && jsonUrl.contains("github.com")) {
                String[] parts = jsonUrl.split("github.com/");
                if (parts.length > 1) {
                    String[] repoParts = parts[1].split("/");
                    if (repoParts.length >= 2) {
                        owner = repoParts[0];
                        repo = repoParts[1].replace(".json", "");
                    }
                }
            }

            report.setConfigLoaded(true);
            report.setRepositoryOwner(owner);
            report.setRepositoryName(repo);
            report.setUpdateChannel(channel);

            logger.info("Configuration loaded - JSON URL: {}", jsonUrl);
            logger.info("Update Channel: {}", channel);
            logger.info("Configuration: OK");
            currentState = LifecycleState.CONFIG_LOADED;

        } catch (java.io.IOException e) {
            logger.error("Failed to load configuration", e);
            report.setConfigLoaded(false);
            report.setConfigError(e.getMessage());
            currentState = LifecycleState.CONFIG_LOADED;
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            report.setConfigLoaded(false);
            report.setConfigError(e.getMessage());
            currentState = LifecycleState.CONFIG_LOADED;
        }
    }

    private void step2_InitializeLogging() {
        currentState = LifecycleState.INITIALIZING_LOGGING;
        logger.info("[2/7] Initializing Logging...");

        // Logging is already initialized at this point
        // This step just logs that it's done
        report.setLoggingInitialized(true);
        logger.info("Logging: OK");
        currentState = LifecycleState.LOGGING_INITIALIZED;
    }

    private void step3_LoadVersionEngine() {
        currentState = LifecycleState.LOADING_VERSION_ENGINE;
        logger.info("[3/7] Loading Version Engine...");

        try {
            String version = VersionService.getCurrentVersionString();

            report.setVersionEngineLoaded(true);
            report.setApplicationVersion(version);

            logger.info("Version Engine loaded - Current version: {}", version);
            logger.info("Version Engine: OK");
            currentState = LifecycleState.VERSION_ENGINE_LOADED;

        } catch (Exception e) {
            logger.error("Failed to load Version Engine", e);
            report.setVersionEngineLoaded(false);
            report.setVersionError(e.getMessage());
            throw e;
        }
    }

    private void step4_CheckUpdates() {
        currentState = LifecycleState.CHECKING_UPDATES;
        logger.info("[4/7] Checking Updates...");

        try {
            String currentVersion = VersionService.getCurrentVersionString();
            UpdateService updateService = new UpdateService();
            com.rmc.model.UpdateCheckResult updateResult = updateService.checkForUpdates();

            boolean updateAvailable = updateResult != null && updateResult.isSuccess();

            report.setUpdateCheckCompleted(true);
            report.setUpdateAvailable(updateAvailable);

            if (updateAvailable) {
                logger.info("Update check successful - HTTP Status: {}", updateResult.getHttpStatus());
            } else {
                logger.info("Update check completed with errors");
            }

            logger.info("Update Check: OK");
            currentState = LifecycleState.UPDATES_CHECKED;

        } catch (Exception e) {
            logger.warn("Failed to check updates (non-critical)", e);
            report.setUpdateCheckCompleted(true);
            report.setUpdateCheckError(e.getMessage());
            currentState = LifecycleState.UPDATES_CHECKED;
        }
    }

    private void step5_DriverDetection() {
        currentState = LifecycleState.DETECTING_DRIVER;
        logger.info("[5/7] Driver Detection...");

        try {
            // Detect Edge
            EdgeInfo edgeInfo = EdgeDetector.detect();
            report.setEdgeDetected(edgeInfo.isInstalled());
            report.setEdgeVersion(edgeInfo.getVersion());
            report.setEdgePath(edgeInfo.getPath());

            if (!edgeInfo.isInstalled()) {
                logger.info("Microsoft Edge: NOT INSTALLED");
            } else {
                logger.info("Microsoft Edge detected - Version: {}, Path: {}",
                        edgeInfo.getVersion(), edgeInfo.getPath());
            }

            // Detect Driver
            DriverInfo driverInfo = DriverDetector.detect();
            report.setDriverDetected(driverInfo.isInstalled());
            report.setDriverVersion(driverInfo.getVersion());
            report.setDriverPath(driverInfo.getPath());

            if (!driverInfo.isInstalled()) {
                logger.info("Edge WebDriver: NOT INSTALLED");
            } else {
                logger.info("Edge WebDriver detected - Version: {}, Path: {}",
                        driverInfo.getVersion(), driverInfo.getPath());
            }

            // Compare versions
            DriverStatus status = DriverService.detectAndCompare();
            report.setDriverStatus(status);

            logger.info("Driver Status: {}", status);
            logger.info("Driver Detection: OK");
            currentState = LifecycleState.DRIVER_DETECTED;

        } catch (Exception e) {
            logger.error("Failed to detect driver", e);
            report.setDriverDetectionError(e.getMessage());
            throw e;
        }
    }

    private void step6_DriverValidation() {
        currentState = LifecycleState.VALIDATING_DRIVER;
        logger.info("[6/7] Driver Validation...");

        try {
            if (!report.isEdgeDetected() || !report.isDriverDetected()) {
                logger.info("Skipping validation - Edge or Driver not detected");
                report.setValidationCompleted(true);
                currentState = LifecycleState.DRIVER_VALIDATED;
                return;
            }

            Path driverPath = Path.of(report.getDriverPath());
            String browserVersion = report.getEdgeVersion();
            String expectedDriverVersion = report.getDriverVersion();

            ValidationResult validationResult = DriverValidator.validate(
                    driverPath, browserVersion, expectedDriverVersion);

            report.setValidationCompleted(true);
            report.setValidationStatus(validationResult.getStatus());

            if (validationResult.isValid()) {
                logger.info("Driver Validation: PASSED");
            } else {
                logger.info("Driver Validation: FAILED - {}", validationResult.getMessage());
            }

            logger.info("Driver Validation: OK");
            currentState = LifecycleState.DRIVER_VALIDATED;

        } catch (Exception e) {
            logger.error("Failed to validate driver", e);
            report.setValidationError(e.getMessage());
            currentState = LifecycleState.DRIVER_VALIDATED;
        }
    }

    private void step7_ApplicationReady() {
        currentState = LifecycleState.READY;
        logger.info("[7/7] Application Ready");

        report.setApplicationReady(true);
        report.setStartTime(LocalDateTime.now());

        logger.info("Application is ready to use");
    }

    /**
     * Get the current lifecycle state.
     */
    public LifecycleState getCurrentState() {
        return currentState;
    }

    /**
     * Get the lifecycle report.
     */
    public LifecycleReport getReport() {
        return report;
    }

    /**
     * Check if the application is ready.
     */
    public boolean isReady() {
        return currentState == LifecycleState.READY;
    }
}
