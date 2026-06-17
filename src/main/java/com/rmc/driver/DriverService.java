package com.rmc.driver;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

/**
 * Service for detecting Edge browser and WebDriver, and comparing their versions.
 */
public class DriverService {

    private static final Logger logger = AppLogger.getLogger();

    private DriverService() {
        // Utility class
    }

    /**
     * Run full driver detection and comparison.
     * Creates detailed logs of the detection process.
     *
     * @return DriverStatus indicating the comparison result
     */
    public static DriverStatus detectAndCompare() {
        logger.info("=================================================");
        logger.info("Driver Detection Engine");
        logger.info("=================================================");

        DriverStatus status = performDetection();

        logger.info("=================================================");
        
        return status;
    }

    /**
     * Detect Edge and Driver, then compare versions.
     *
     * @return DriverStatus
     */
    private static DriverStatus performDetection() {
        EdgeInfo edgeInfo = EdgeDetector.detect();
        DriverInfo driverInfo = DriverDetector.detect();

        return compareVersions(edgeInfo, driverInfo);
    }

    /**
     * Compare Edge browser and WebDriver versions.
     *
     * @param edgeInfo Edge browser information
     * @param driverInfo WebDriver information
     * @return DriverStatus based on comparison
     */
    private static DriverStatus compareVersions(EdgeInfo edgeInfo, DriverInfo driverInfo) {
        if (!edgeInfo.isInstalled()) {
            logger.warn("Cannot compare - Microsoft Edge is not installed");
            return DriverStatus.NOT_INSTALLED;
        }

        if (!driverInfo.isInstalled()) {
            logger.warn("Cannot compare - Edge WebDriver is not installed");
            return DriverStatus.NOT_INSTALLED;
        }

        String browserVersion = normalizeVersion(edgeInfo.getVersion());
        String driverVersion = normalizeVersion(driverInfo.getVersion());

        logger.info("Comparison:");
        logger.info("Browser Version: {}", browserVersion);
        logger.info("Driver Version: {}", driverVersion);

        if (browserVersion.equals(driverVersion)) {
            logger.info("Status: MATCH");
            return DriverStatus.MATCH;
        }

        // Extract major.minor.patch for comparison
        try {
            String[] browserParts = browserVersion.split("\\.");
            String[] driverParts = driverVersion.split("\\.");

            // Compare major.minor.patch
            int browserMajor = Integer.parseInt(browserParts[0]);
            int driverMajor = Integer.parseInt(driverParts[0]);
            
            int browserMinor = browserParts.length > 1 ? Integer.parseInt(browserParts[1]) : 0;
            int driverMinor = driverParts.length > 1 ? Integer.parseInt(driverParts[1]) : 0;
            
            int browserPatch = browserParts.length > 2 ? Integer.parseInt(browserParts[2]) : 0;
            int driverPatch = driverParts.length > 2 ? Integer.parseInt(driverParts[2]) : 0;

            if (browserMajor > driverMajor || 
                (browserMajor == driverMajor && browserMinor > driverMinor) ||
                (browserMajor == driverMajor && browserMinor == driverMinor && browserPatch > driverPatch)) {
                logger.warn("Status: OUTDATED - Driver is older than browser");
                return DriverStatus.OUTDATED;
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse version numbers for comparison");
            return DriverStatus.UNKNOWN;
        }

        logger.info("Status: MATCH");
        return DriverStatus.MATCH;
    }

    /**
     * Normalize version string for comparison.
     * Extracts major.minor.patch from potentially longer version strings.
     *
     * @param version Full version string
     * @return Normalized version (e.g., "146.0.3856.109" -> "146.0.3856")
     */
    private static String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }

        // Handle versions like "146.0.3856.109" - take major.minor.patch
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return version;
    }

    /**
     * Get Edge information without logging.
     *
     * @return EdgeInfo
     */
    public static EdgeInfo getEdgeInfo() {
        return EdgeDetector.detect();
    }

    /**
     * Get Driver information without logging.
     *
     * @return DriverInfo
     */
    public static DriverInfo getDriverInfo() {
        return DriverDetector.detect();
    }
}