package com.rmc.driver.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverResolver utility methods.
 */
class DriverResolverTest {

    @Test
    void testNormalizeVersionWithFourParts() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856.109");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithThreeParts() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testNormalizeVersionWithTwoParts() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0");
        assertEquals("146.0", result);
    }

    @Test
    void testNormalizeVersionNull() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, (String) null);
        assertEquals("", result);
    }

    @Test
    void testNormalizeVersionEmpty() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("normalizeVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "");
        assertEquals("", result);
    }

    @Test
    void testExtractDriverVersion() throws Exception {
        Method method = DriverResolver.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856.109");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testIsValidVersion() {
        assertTrue(DriverResolver.isValidVersion("146.0.3856"));
        assertTrue(DriverResolver.isValidVersion("146.0.3856.109"));
        assertTrue(DriverResolver.isValidVersion("1.2.3"));
        assertFalse(DriverResolver.isValidVersion(null));
        assertFalse(DriverResolver.isValidVersion(""));
        assertFalse(DriverResolver.isValidVersion("invalid"));
        assertFalse(DriverResolver.isValidVersion("1.2"));
        assertFalse(DriverResolver.isValidVersion("1.2.3.4.5"));
    }

    @Test
    void testGetCurrentPlatformAndArchitecture() {
        Object[] result = DriverResolver.getCurrentPlatformAndArchitecture();
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertNotNull(result[0]);
        assertNotNull(result[1]);
        assertTrue(result[0] instanceof Platform);
        assertTrue(result[1] instanceof Architecture);
    }
}