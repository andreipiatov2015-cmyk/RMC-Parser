package com.rmc.driver.resolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Platform enum.
 */
class PlatformTest {

    @Test
    void testAllPlatformValuesExist() {
        assertNotNull(Platform.WINDOWS);
        assertNotNull(Platform.LINUX);
        assertNotNull(Platform.MAC);
    }

    @Test
    void testPlatformCount() {
        assertEquals(3, Platform.values().length);
    }

    @Test
    void testGetUrlSuffix() {
        assertEquals("win64", Platform.WINDOWS.getUrlSuffix());
        assertEquals("linux64", Platform.LINUX.getUrlSuffix());
        assertEquals("mac64", Platform.MAC.getUrlSuffix());
    }

    @Test
    void testGetArchiveExtension() {
        assertEquals(".zip", Platform.WINDOWS.getArchiveExtension());
        assertEquals(".zip", Platform.LINUX.getArchiveExtension());
        assertEquals(".zip", Platform.MAC.getArchiveExtension());
    }

    @Test
    void testGetDriverExecutableName() {
        assertEquals("msedgedriver.exe", Platform.WINDOWS.getDriverExecutableName());
        assertEquals("msedgedriver", Platform.LINUX.getDriverExecutableName());
        assertEquals("msedgedriver", Platform.MAC.getDriverExecutableName());
    }
}