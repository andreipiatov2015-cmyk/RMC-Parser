package com.rmc.driver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverService version comparison logic.
 */
class DriverServiceVersionTest {

    @Test
    void testNormalizeVersionWithFourParts() throws Exception {
        // Use reflection to test private method
        Method normalizeMethod = DriverService.class.getDeclaredMethod("normalizeVersion", String.class);
        normalizeMethod.setAccessible(true);
        
        String result = (String) normalizeMethod.invoke(null, "146.0.3856.109");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithThreeParts() throws Exception {
        Method normalizeMethod = DriverService.class.getDeclaredMethod("normalizeVersion", String.class);
        normalizeMethod.setAccessible(true);
        
        String result = (String) normalizeMethod.invoke(null, "146.0.3856");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithTwoParts() throws Exception {
        Method normalizeMethod = DriverService.class.getDeclaredMethod("normalizeVersion", String.class);
        normalizeMethod.setAccessible(true);
        
        String result = (String) normalizeMethod.invoke(null, "146.0");
        assertEquals("146.0", result);
    }

    @Test
    void testNormalizeVersionNull() throws Exception {
        Method normalizeMethod = DriverService.class.getDeclaredMethod("normalizeVersion", String.class);
        normalizeMethod.setAccessible(true);
        
        String result = (String) normalizeMethod.invoke(null, (String) null);
        assertEquals("", result);
    }

    @Test
    void testNormalizeVersionEmpty() throws Exception {
        Method normalizeMethod = DriverService.class.getDeclaredMethod("normalizeVersion", String.class);
        normalizeMethod.setAccessible(true);
        
        String result = (String) normalizeMethod.invoke(null, "");
        assertEquals("", result);
    }
}