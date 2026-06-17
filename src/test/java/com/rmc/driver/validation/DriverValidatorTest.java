package com.rmc.driver.validation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverValidator utility methods.
 */
class DriverValidatorTest {

    @Test
    void testNormalizeVersionWithFourParts() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856.109");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithThreeParts() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithTwoParts() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0");
        assertEquals("146.0", result);
    }

    @Test
    void testNormalizeVersionWithOnePart() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146");
        assertEquals("146", result);
    }

    @Test
    void testNormalizeVersionNull() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, (String) null);
        assertEquals("", result);
    }

    @Test
    void testNormalizeVersionEmpty() throws Exception {
        Method method = DriverValidator.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "");
        assertEquals("", result);
    }

    @Test
    void testValidateMissingDriver() {
        // Use a path that definitely doesn't exist
        ValidationResult result = DriverValidator.validate(
                Paths.get("/nonexistent/path/msedgedriver.exe"),
                "146.0.3856.109",
                "146.0.3856"
        );

        assertFalse(result.isValid());
        assertTrue(result.isMissing());
        assertEquals(ValidationStatus.MISSING, result.getStatus());
    }

    @Test
    void testExistsWithNonexistentPath() {
        boolean exists = DriverValidator.exists(Paths.get("/nonexistent/path/msedgedriver.exe"));
        assertFalse(exists);
    }
}