package com.rmc.driver.validation;

import java.util.Objects;

/**
 * Result of driver validation.
 * 
 * <p>Contains the validated manifest and validation status with message.</p>
 */
public class ValidationResult {

    private final DriverManifest manifest;
    private final ValidationStatus status;
    private final String message;

    private ValidationResult(DriverManifest manifest, ValidationStatus status, String message) {
        this.manifest = manifest;
        this.status = status;
        this.message = message;
    }

    public static ValidationResult success(DriverManifest manifest) {
        return new ValidationResult(
                manifest,
                ValidationStatus.VALID,
                "Driver validation successful"
        );
    }

    public static ValidationResult failure(ValidationStatus status, String message) {
        return new ValidationResult(
                DriverManifest.empty(),
                status,
                message
        );
    }

    public static ValidationResult failure(DriverManifest manifest, ValidationStatus status, String message) {
        return new ValidationResult(manifest, status, message);
    }

    public static ValidationResult missing() {
        return new ValidationResult(
                DriverManifest.empty(),
                ValidationStatus.MISSING,
                "Driver file does not exist"
        );
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(
                DriverManifest.empty(),
                ValidationStatus.INVALID,
                message
        );
    }

    public static ValidationResult versionMismatch(DriverManifest manifest, String expected, String actual) {
        return new ValidationResult(
                manifest,
                ValidationStatus.VERSION_MISMATCH,
                "Version mismatch: expected " + expected + ", found " + actual
        );
    }

    public static ValidationResult unknown(String message) {
        return new ValidationResult(
                DriverManifest.empty(),
                ValidationStatus.UNKNOWN,
                message
        );
    }

    public DriverManifest getManifest() {
        return manifest;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }

    public boolean isMissing() {
        return status == ValidationStatus.MISSING;
    }

    public boolean isVersionMismatch() {
        return status == ValidationStatus.VERSION_MISMATCH;
    }

    public boolean isInvalid() {
        return status == ValidationStatus.INVALID;
    }

    public boolean isUnknown() {
        return status == ValidationStatus.UNKNOWN;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", manifest=" + manifest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return status == that.status &&
                Objects.equals(message, that.message) &&
                Objects.equals(manifest, that.manifest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifest, status, message);
    }
}