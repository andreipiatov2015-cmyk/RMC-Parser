package com.rmc.driver;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector for Microsoft Edge browser installation.
 */
public class EdgeDetector {

    private static final Logger logger = AppLogger.getLogger();

    private static final String EDGE_EXECUTABLE = "msedge.exe";

    private static final List<Path> EDGE_SEARCH_PATHS = new ArrayList<>();

    static {
        // Standard Windows installation paths
        EDGE_SEARCH_PATHS.add(Paths.get("C:\\Program Files\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
        EDGE_SEARCH_PATHS.add(Paths.get("C:\\Program Files (x86)\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
        // Add more paths if needed
        EDGE_SEARCH_PATHS.add(Paths.get(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
    }

    private EdgeDetector() {
        // Utility class
    }

    /**
     * Detect Microsoft Edge installation.
     *
     * @return EdgeInfo containing path, version, and installation status
     */
    public static EdgeInfo detect() {
        logger.info("Searching Microsoft Edge...");

        for (Path searchPath : EDGE_SEARCH_PATHS) {
            File edgeFile = searchPath.toFile();
            logger.debug("Checking path: {}", searchPath);

            if (edgeFile.exists() && edgeFile.isFile()) {
                String version = getFileVersion(searchPath.toString());
                if (version != null) {
                    logger.info("Found Microsoft Edge");
                    logger.info("Path: {}", searchPath);
                    logger.info("Version: {}", version);
                    return new EdgeInfo(searchPath.toString(), version, true);
                }
            }
        }

        logger.warn("Microsoft Edge not found");
        return EdgeInfo.notInstalled();
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
     * Get all search paths for Edge detection.
     *
     * @return List of paths that will be searched
     */
    public static List<Path> getSearchPaths() {
        return new ArrayList<>(EDGE_SEARCH_PATHS);
    }
}