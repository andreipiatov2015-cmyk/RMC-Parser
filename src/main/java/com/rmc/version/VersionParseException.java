package com.rmc.version;

/**
 * Exception thrown when a version string cannot be parsed.
 */
public class VersionParseException extends Exception {

    public VersionParseException(String message) {
        super(message);
    }

    public VersionParseException(String message, Throwable cause) {
        super(message, cause);
    }
}