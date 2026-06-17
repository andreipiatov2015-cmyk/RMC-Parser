package com.rmc.driver.validation;

import com.rmc.driver.resolver.Architecture;
import com.rmc.driver.resolver.Platform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationResult class.
 */
class ValidationResultTest {

    @Test
    void testSuccess() {
        DriverManifest manifest = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .status(ValidationStatus.VALID)
                .build();

        ValidationResult result = ValidationResult.success(manifest);

        assertTrue(result.isValid());
        assertFalse(result.isMissing());
        assertFalse(result.isVersionMismatch());
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals(manifest, result.getManifest());
        assertNotNull(result.getMessage());
    }

    @Test
    void testFailure() {
        ValidationResult result = ValidationResult.failure(
                ValidationStatus.INVALID, 
                "Driver is corrupted"
        );

        assertFalse(result.isValid());
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("Driver is corrupted", result.getMessage());
    }

    @Test
    void testMissing() {
        ValidationResult result = ValidationResult.missing();

        assertFalse(result.isValid());
        assertTrue(result.isMissing());
        assertEquals(ValidationStatus.MISSING, result.getStatus());
    }

    @Test
    void testVersionMismatch() {
        DriverManifest manifest = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("145.0.3856")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        ValidationResult result = ValidationResult.versionMismatch(
                manifest, 
                "146.0.3856", 
                "145.0.3856"
        );

        assertFalse(result.isValid());
        assertTrue(result.isVersionMismatch());
        assertEquals(ValidationStatus.VERSION_MISMATCH, result.getStatus());
        assertTrue(result.getMessage().contains("146.0.3856"));
        assertTrue(result.getMessage().contains("145.0.3856"));
        assertEquals(manifest, result.getManifest());
    }

    @Test
    void testInvalid() {
        ValidationResult result = ValidationResult.invalid("File not readable");

        assertFalse(result.isValid());
        assertTrue(result.isInvalid());
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("File not readable", result.getMessage());
    }

    @Test
    void testUnknown() {
        ValidationResult result = ValidationResult.unknown("Unexpected error");

        assertFalse(result.isValid());
        assertTrue(result.isUnknown());
        assertEquals(ValidationStatus.UNKNOWN, result.getStatus());
        assertEquals("Unexpected error", result.getMessage());
    }

    @Test
    void testToString() {
        ValidationResult result = ValidationResult.success(
                DriverManifest.builder()
                        .status(ValidationStatus.VALID)
                        .build()
        );

        String str = result.toString();
        assertTrue(str.contains("VALID"));
        assertTrue(str.contains("manifest"));
    }

    @Test
    void testEquals() {
        ValidationResult result1 = ValidationResult.missing();
        ValidationResult result2 = ValidationResult.missing();

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testNotEquals() {
        ValidationResult result1 = ValidationResult.missing();
        ValidationResult result2 = ValidationResult.success(
                DriverManifest.builder()
                        .status(ValidationStatus.VALID)
                        .build()
        );

        assertNotEquals(result1, result2);
    }
}