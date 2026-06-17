package com.rmc.driver.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationStatus enum.
 */
class ValidationStatusTest {

    @Test
    void testAllStatusValuesExist() {
        assertNotNull(ValidationStatus.VALID);
        assertNotNull(ValidationStatus.INVALID);
        assertNotNull(ValidationStatus.MISSING);
        assertNotNull(ValidationStatus.VERSION_MISMATCH);
        assertNotNull(ValidationStatus.UNKNOWN);
    }

    @Test
    void testStatusCount() {
        assertEquals(5, ValidationStatus.values().length);
    }

    @Test
    void testIsValid() {
        assertTrue(ValidationStatus.VALID.isValid());
        assertFalse(ValidationStatus.INVALID.isValid());
        assertFalse(ValidationStatus.MISSING.isValid());
        assertFalse(ValidationStatus.VERSION_MISMATCH.isValid());
        assertFalse(ValidationStatus.UNKNOWN.isValid());
    }

    @Test
    void testIsMissing() {
        assertFalse(ValidationStatus.VALID.isMissing());
        assertFalse(ValidationStatus.INVALID.isMissing());
        assertTrue(ValidationStatus.MISSING.isMissing());
        assertFalse(ValidationStatus.VERSION_MISMATCH.isMissing());
        assertFalse(ValidationStatus.UNKNOWN.isMissing());
    }

    @Test
    void testGetDescription() {
        assertEquals("Driver is valid and ready to use", ValidationStatus.VALID.getDescription());
        assertEquals("Driver exists but is invalid or corrupted", ValidationStatus.INVALID.getDescription());
        assertEquals("Driver file does not exist", ValidationStatus.MISSING.getDescription());
        assertEquals("Driver version does not match expected version", ValidationStatus.VERSION_MISMATCH.getDescription());
        assertEquals("Validation status is unknown", ValidationStatus.UNKNOWN.getDescription());
    }
}