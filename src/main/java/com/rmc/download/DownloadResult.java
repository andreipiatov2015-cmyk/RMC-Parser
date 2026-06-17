package com.rmc.download;

/**
 * Result of a download operation.
 */
public class DownloadResult {

    private final boolean success;
    private final String driverPath;
    private final String version;
    private final String errorMessage;
    private final ErrorType errorType;

    public enum ErrorType {
        NONE,
        EDGE_NOT_INSTALLED,
        ALREADY_EXISTS,
        INTERNET_UNAVAILABLE,
        HTTP_ERROR,
        INVALID_RESPONSE,
        ZIP_ERROR,
        EXTRACTION_ERROR,
        PERMISSION_DENIED,
        UNKNOWN_ERROR
    }

    private DownloadResult(boolean success, String driverPath, String version, 
                          String errorMessage, ErrorType errorType) {
        this.success = success;
        this.driverPath = driverPath;
        this.version = version;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
    }

    public static DownloadResult success(String driverPath, String version) {
        return new DownloadResult(true, driverPath, version, null, ErrorType.NONE);
    }

    public static DownloadResult error(ErrorType errorType, String message) {
        return new DownloadResult(false, null, null, message, errorType);
    }

    public static DownloadResult edgeNotInstalled() {
        return new DownloadResult(false, null, null, 
            "Microsoft Edge is not installed", ErrorType.EDGE_NOT_INSTALLED);
    }

    public static DownloadResult alreadyExists(String driverPath, String version) {
        return new DownloadResult(false, driverPath, version, 
            "Driver already exists at: " + driverPath, ErrorType.ALREADY_EXISTS);
    }

    public static DownloadResult internetUnavailable(Exception e) {
        return new DownloadResult(false, null, null,
            "Internet unavailable: " + e.getMessage(), ErrorType.INTERNET_UNAVAILABLE);
    }

    public static DownloadResult httpError(int statusCode) {
        return new DownloadResult(false, null, null,
            "HTTP error: " + statusCode, ErrorType.HTTP_ERROR);
    }

    public static DownloadResult zipError(String message) {
        return new DownloadResult(false, null, null,
            "Invalid ZIP archive: " + message, ErrorType.ZIP_ERROR);
    }

    public static DownloadResult extractionError(String message) {
        return new DownloadResult(false, null, null,
            "Extraction failed: " + message, ErrorType.EXTRACTION_ERROR);
    }

    public static DownloadResult permissionDenied(String message) {
        return new DownloadResult(false, null, null,
            "Permission denied: " + message, ErrorType.PERMISSION_DENIED);
    }

    public static DownloadResult unknownError(Exception e) {
        return new DownloadResult(false, null, null,
            "Unknown error: " + e.getMessage(), ErrorType.UNKNOWN_ERROR);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDriverPath() {
        return driverPath;
    }

    public String getVersion() {
        return version;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        if (success) {
            return "DownloadResult{success=true, path='" + driverPath + "', version='" + version + "'}";
        }
        return "DownloadResult{success=false, error='" + errorMessage + "'}";
    }
}