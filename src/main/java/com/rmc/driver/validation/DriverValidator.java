package com.rmc.driver.validation;

import com.rmc.driver.resolver.Architecture;
import com.rmc.driver.resolver.Platform;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Validates installed Microsoft Edge WebDriver.
 * 
 * <p>Checks:
 * <ul>
 *   <li>Driver exists at the expected path</li>
 *   <li>Executable file exists and is readable</li>
 *   <li>FileVersion matches expected version</li>
 *   <li>Platform and architecture are correct</li>
 * </ul></p>
 */
public class DriverValidator {

    private static final Logger logger = AppLogger.getLogger();

    private DriverValidator() {
        // Utility class
    }

    /**
     * Validate the installed driver against the expected browser version.
     *
     * @param driverPath Path to the driver executable
     * @param browserVersion Expected browser version
     * @param expectedDriverVersion Expected driver version
     * @return ValidationResult with manifest and status
     */
    public static ValidationResult validate(String driverPath, String browserVersion, String expectedDriverVersion) {
        return validate(Paths.get(driverPath), browserVersion, expectedDriverVersion);
    }

    /**
     * Validate the installed driver against the expected browser version.
     *
     * @param driverPath Path to the driver executable
     * @param browserVersion Expected browser version
     * @param expectedDriverVersion Expected driver version
     * @return ValidationResult with manifest and status
     */
    public static ValidationResult validate(Path driverPath, String browserVersion, String expectedDriverVersion) {
        logger.info("=================================================");
        logger.info("Driver Validation Engine");
        logger.info("=================================================");

        // Detect platform and architecture
        Platform platform = Platform.detect();
        Architecture architecture = Architecture.detect();
        
        logger.info("Driver path: {}", driverPath);
        logger.info("Browser version: {}", browserVersion);
        logger.info("Expected version: {}", expectedDriverVersion);
        logger.info("Platform: {}", platform);
        logger.info("Architecture: {}", architecture);

        // Build base manifest
        DriverManifest.Builder manifestBuilder = DriverManifest.builder()
                .browserVersion(browserVersion)
                .driverVersion(expectedDriverVersion)
                .driverPath(driverPath.toString())
                .platform(platform)
                .architecture(architecture)
                .validationTime(LocalDateTime.now());

        // Check if driver exists
        if (!Files.exists(driverPath)) {
            logger.info("Driver exists: NO");
            logger.info("Validation: FAIL - Driver not found");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.MISSING,
                    ValidationStatus.MISSING.getDescription()
            );
        }

        logger.info("Driver exists: YES");

        // Check if it's a file (not directory)
        if (!Files.isRegularFile(driverPath)) {
            logger.info("Validation: FAIL - Path is not a file");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Driver path is not a regular file"
            );
        }

        // Check if executable
        if (!Files.isReadable(driverPath)) {
            logger.info("Validation: FAIL - File is not readable");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Driver file is not readable"
            );
        }

        // Read driver version
        String actualVersion = readDriverVersion(driverPath);
        
        if (actualVersion == null) {
            logger.info("Driver Version: UNKNOWN");
            logger.info("Validation: FAIL - Could not read version");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.UNKNOWN,
                    "Could not read driver version"
            );
        }

        logger.info("Driver Version: {}", actualVersion);
        logger.info("Expected: {}", expectedDriverVersion);

        // Normalize versions for comparison
        String normalizedActual = normalizeVersion(actualVersion);
        String normalizedExpected = normalizeVersion(expectedDriverVersion);

        // Compare versions
        boolean versionsMatch = normalizedActual.equals(normalizedExpected);

        logger.info("Version Match: {}", versionsMatch ? "YES" : "NO");

        if (!versionsMatch) {
            logger.info("Validation: FAIL - Version mismatch");
            logger.info("=================================================");
            DriverManifest manifest = manifestBuilder
                    .driverVersion(actualVersion)
                    .status(ValidationStatus.VERSION_MISMATCH)
                    .build();
            return ValidationResult.versionMismatch(manifest, normalizedExpected, normalizedActual);
        }

        // Validate platform
        if (platform != Platform.WINDOWS) {
            logger.info("Platform validation: FAIL - Only Windows is supported");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Only Windows platform is supported"
            );
        }

        // Validate architecture
        if (!architecture.isSupported()) {
            logger.info("Architecture validation: FAIL - Unsupported architecture");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Unsupported architecture: " + architecture
            );
        }

        // All checks passed
        logger.info("Validation: PASS");
        logger.info("=================================================");

        DriverManifest manifest = manifestBuilder
                .driverVersion(actualVersion)
                .status(ValidationStatus.VALID)
                .build();

        return ValidationResult.success(manifest);
    }

    /**
     * Quick validation - just check if driver exists.
     *
     * @param driverPath Path to check
     * @return true if exists, false otherwise
     */
    public static boolean exists(Path driverPath) {
        return Files.exists(driverPath) && Files.isRegularFile(driverPath);
    }

    /**
     * Read the driver file version using PowerShell.
     */
    private static String readDriverVersion(Path driverPath) {
        try {
            String command = String.format(
                    "powershell -Command \"(Get-Item '%s').VersionInfo.FileVersion\"",
                    driverPath.toAbsolutePath().toString().replace("'", "''")
            );

            Process process = Runtime.getRuntime().exec(command);
            String version = new String(process.getInputStream().readAllBytes()).trim();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes()).trim();
                logger.warn("Failed to read driver version: {}", error);
                return null;
            }

            if (version.isEmpty()) {
                logger.warn("Empty version returned");
                return null;
            }

            return version;

        } catch (IOException | InterruptedException e) {
            logger.warn("Error reading driver version", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Normalize version string to major.minor.build format.
     */
    static String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }

        // Handle version patterns like "146.0.3856" or "146.0.3856.109"
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return version;
    }
}