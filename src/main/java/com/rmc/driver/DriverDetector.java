package com.rmc.driver;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector for Microsoft Edge WebDriver (msedgedriver.exe).
 */
public class DriverDetector {

    private static final Logger logger = AppLogger.getLogger();

    private static final String DRIVER_EXECUTABLE = "msedgedriver.exe";
    private static final String DRIVER_SUBDIR = "drivers" + File.separator + "edge";

    private static final List<Path> DRIVER_SEARCH_PATHS = new ArrayList<>();

    static {
        // Application directory drivers/edge/
        String appDir = System.getProperty("user.dir");
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, DRIVER_SUBDIR, DRIVER_EXECUTABLE));
        // Alternative locations
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, "drivers", DRIVER_EXECUTABLE));
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, "driver", DRIVER_EXECUTABLE));
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, DRIVER_EXECUTABLE));
    }

    private DriverDetector() {
        // Utility class
    }

    /**
     * Detect Microsoft Edge WebDriver installation.
     *
     * @return DriverInfo containing path, version, and installation status
     */
    public static DriverInfo detect() {
        logger.info("Searching Edge WebDriver...");

        for (Path searchPath : DRIVER_SEARCH_PATHS) {
            File driverFile = searchPath.toFile();
            logger.debug("Checking path: {}", searchPath);

            if (driverFile.exists() && driverFile.isFile()) {
                String version = getFileVersion(searchPath.toString());
                if (version != null) {
                    logger.info("Found Edge WebDriver");
                    logger.info("Path: {}", searchPath);
                    logger.info("Version: {}", version);
                    return new DriverInfo(searchPath.toString(), version, true);
                }
            }
        }

        logger.warn("Edge WebDriver not found");
        return DriverInfo.notInstalled();
    }

    /**
     * Get file version using Windows FileVersion API via PowerShell.
     * Does NOT execute the target application.
     *
     * @param filePath Path to the file
     * @return Version string or null if unable to retrieve
     */
    private static String getFileVersion(String filePath) {
        try {
            String powershellCommand = String.format(
                "(Get-Item '%s').VersionInfo.FileVersion",
                filePath.replace("'", "''")
            );

            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", 
                "-Command", powershellCommand
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String version = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode == 0 && !version.isEmpty()) {
                return version;
            }
        } catch (Exception e) {
            logger.debug("Could not get file version for {}: {}", filePath, e.getMessage());
        }
        return null;
    }

    /**
     * Get all search paths for driver detection.
     *
     * @return List of paths that will be searched
     */
    public static List<Path> getSearchPaths() {
        return new ArrayList<>(DRIVER_SEARCH_PATHS);
    }
}