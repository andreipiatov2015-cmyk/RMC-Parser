package com.rmc.model;

public class UpdateCheckResult {

    private final int httpStatus;
    private final long responseLength;
    private final String errorMessage;
    private final Exception exception;

    private UpdateCheckResult(int httpStatus, long responseLength, String errorMessage, Exception exception) {
        this.httpStatus = httpStatus;
        this.responseLength = responseLength;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    public static UpdateCheckResult success(int httpStatus, long responseLength) {
        return new UpdateCheckResult(httpStatus, responseLength, null, null);
    }

    public static UpdateCheckResult error(int httpStatus, String errorMessage, Exception exception) {
        return new UpdateCheckResult(httpStatus, -1, errorMessage, exception);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public long getResponseLength() {
        return responseLength;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isSuccess() {
        return errorMessage == null && exception == null;
    }
}