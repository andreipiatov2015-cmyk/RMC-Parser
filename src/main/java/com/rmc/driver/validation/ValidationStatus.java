package com.rmc.driver.validation;

/**
 * Status of driver validation.
 */
public enum ValidationStatus {
    /**
     * Driver is valid and usable.
     */
    VALID,
    
    /**
     * Driver exists but is invalid or corrupted.
     */
    INVALID,
    
    /**
     * Driver file does not exist.
     */
    MISSING,
    
    /**
     * Driver version does not match expected version.
     */
    VERSION_MISMATCH,
    
    /**
     * Validation could not be completed (unknown error).
     */
    UNKNOWN;

    /**
     * Check if the status indicates a successful validation.
     *
     * @return true if status is VALID
     */
    public boolean isValid() {
        return this == VALID;
    }

    /**
     * Check if the status indicates the driver is missing.
     *
     * @return true if status is MISSING
     */
    public boolean isMissing() {
        return this == MISSING;
    }

    /**
     * Get a human-readable description of the status.
     *
     * @return Description string
     */
    public String getDescription() {
        switch (this) {
            case VALID:
                return "Driver is valid and ready to use";
            case INVALID:
                return "Driver exists but is invalid or corrupted";
            case MISSING:
                return "Driver file does not exist";
            case VERSION_MISMATCH:
                return "Driver version does not match expected version";
            case UNKNOWN:
            default:
                return "Validation status is unknown";
        }
    }
}