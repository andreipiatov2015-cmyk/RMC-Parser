package com.rmc.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for version strings in format "major.minor.patch".
 */
public final class VersionParser {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private VersionParser() {
        // Utility class
    }

    /**
     * Parse a version string into a Version object.
     *
     * @param versionString The version string to parse (e.g., "0.1.0")
     * @return The parsed Version object
     * @throws VersionParseException if the string is null, empty, or invalid
     */
    public static Version parse(String versionString) throws VersionParseException {
        if (versionString == null) {
            throw new VersionParseException("Version string cannot be null");
        }

        String trimmed = versionString.trim();
        if (trimmed.isEmpty()) {
            throw new VersionParseException("Version string cannot be empty");
        }

        Matcher matcher = VERSION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new VersionParseException("Invalid version format: '" + versionString + "'. Expected format: major.minor.patch (e.g., 0.1.0)");
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new VersionParseException("Invalid version numbers in: '" + versionString + "'", e);
        }
    }

    /**
     * Try to parse a version string without throwing an exception.
     *
     * @param versionString The version string to parse
     * @return The parsed Version, or null if parsing fails
     */
    public static Version tryParse(String versionString) {
        try {
            return parse(versionString);
        } catch (VersionParseException e) {
            return null;
        }
    }
}