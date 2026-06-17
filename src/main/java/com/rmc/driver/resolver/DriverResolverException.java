package com.rmc.driver.resolver;

/**
 * Exception thrown by DriverResolver when resolution fails.
 */
public class DriverResolverException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        UNSUPPORTED_PLATFORM,
        UNSUPPORTED_ARCHITECTURE,
        INVALID_VERSION,
        UNKNOWN_BROWSER_VERSION,
        NETWORK_ERROR,
        UNKNOWN_ERROR
    }

    public DriverResolverException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public DriverResolverException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public static DriverResolverException unsupportedPlatform(Platform platform) {
        return new DriverResolverException(
            ErrorType.UNSUPPORTED_PLATFORM,
            "Unsupported platform: " + platform
        );
    }

    public static DriverResolverException unsupportedArchitecture(Architecture architecture) {
        return new DriverResolverException(
            ErrorType.UNSUPPORTED_ARCHITECTURE,
            "Unsupported architecture: " + architecture
        );
    }

    public static DriverResolverException invalidVersion(String version) {
        return new DriverResolverException(
            ErrorType.INVALID_VERSION,
            "Invalid browser version format: " + version
        );
    }

    public static DriverResolverException unknownBrowserVersion(String version) {
        return new DriverResolverException(
            ErrorType.UNKNOWN_BROWSER_VERSION,
            "Unknown browser version: " + version + ". No matching driver available."
        );
    }

    public static DriverResolverException networkError(Throwable cause) {
        return new DriverResolverException(
            ErrorType.NETWORK_ERROR,
            "Network error during resolution: " + cause.getMessage(),
            cause
        );
    }

    public static DriverResolverException unknownError(Throwable cause) {
        return new DriverResolverException(
            ErrorType.UNKNOWN_ERROR,
            "Unknown error during resolution: " + cause.getMessage(),
            cause
        );
    }
}