package com.rmc.driver.resolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Architecture enum.
 */
class ArchitectureTest {

    @Test
    void testAllArchitectureValuesExist() {
        assertNotNull(Architecture.X64);
        assertNotNull(Architecture.ARM64);
        assertNotNull(Architecture.UNKNOWN);
    }

    @Test
    void testArchitectureCount() {
        assertEquals(3, Architecture.values().length);
    }

    @Test
    void testGetUrlSuffix() {
        assertEquals("x64", Architecture.X64.getUrlSuffix());
        assertEquals("arm64", Architecture.ARM64.getUrlSuffix());
        assertEquals("x64", Architecture.UNKNOWN.getUrlSuffix());
    }

    @Test
    void testIsSupported() {
        assertTrue(Architecture.X64.isSupported());
        assertTrue(Architecture.ARM64.isSupported());
        assertFalse(Architecture.UNKNOWN.isSupported());
    }
}