package com.rmc.version;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

/**
 * Service providing version management operations.
 * Single source of truth for the current application version.
 */
public class VersionService {

    private static final Logger logger = AppLogger.getLogger();
    
    // Single source of truth for application version
    private static final String CURRENT_VERSION_STRING = "0.1.3";

    private VersionService() {
        // Utility class
    }

    /**
     * Get the current application version.
     *
     * @return The current Version object
     */
    public static Version getCurrentVersion() {
        try {
            Version version = VersionParser.parse(CURRENT_VERSION_STRING);
            logger.info("Current Version: {}", version);
            return version;
        } catch (VersionParseException e) {
            logger.error("Failed to parse current version: {}", CURRENT_VERSION_STRING, e);
            throw new IllegalStateException("Application version is misconfigured", e);
        }
    }

    /**
     * Get the current application version string.
     *
     * @return The version string (e.g., "0.1.0")
     */
    public static String getCurrentVersionString() {
        logger.info("Current Version String: {}", CURRENT_VERSION_STRING);
        return CURRENT_VERSION_STRING;
    }

    /**
     * Parse a version string into a Version object.
     *
     * @param versionString The version string to parse
     * @return The parsed Version object
     * @throws VersionParseException if parsing fails
     */
    public static Version parse(String versionString) throws VersionParseException {
        logger.debug("Parsing version string: {}", versionString);
        try {
            Version version = VersionParser.parse(versionString);
            logger.info("Parsed Version: {}", version);
            return version;
        } catch (VersionParseException e) {
            logger.warn("Failed to parse version: {}", versionString, e);
            throw e;
        }
    }

    /**
     * Compare two version strings.
     *
     * @param version1 First version string
     * @param version2 Second version string
     * @return The comparison result (NEWER, OLDER, or EQUAL)
     * @throws VersionParseException if either string cannot be parsed
     */
    public static VersionComparison compare(String version1, String version2) throws VersionParseException {
        logger.info("Comparing versions: {} vs {}", version1, version2);
        
        Version v1 = VersionParser.parse(version1);
        Version v2 = VersionParser.parse(version2);
        
        VersionComparison result = VersionComparator.compare(v1, v2);
        
        logger.info("Comparison Result: {} is {} than {}", version1, getComparisonDescription(result), version2);
        
        return result;
    }

    /**
     * Compare two Version objects.
     *
     * @param first  First version
     * @param second Second version
     * @return The comparison result (NEWER, OLDER, or EQUAL)
     */
    public static VersionComparison compare(Version first, Version second) {
        VersionComparison result = VersionComparator.compare(first, second);
        logger.debug("Comparing {} vs {}: {}", first, second, result);
        return result;
    }

    private static String getComparisonDescription(VersionComparison comparison) {
        return switch (comparison) {
            case NEWER -> "newer";
            case OLDER -> "older";
            case EQUAL -> "equal";
        };
    }
}