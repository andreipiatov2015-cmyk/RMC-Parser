package com.rmc.driver.resolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverDownloadInfo class.
 */
class DriverDownloadInfoTest {

    @Test
    void testBuilderCreation() {
        DriverDownloadInfo info = DriverDownloadInfo.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .downloadUrl("https://example.com/driver.zip")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .driverFileName("msedgedriver.exe")
                .archiveName("edgedriver_win64.zip")
                .build();

        assertEquals("146.0.3856.109", info.getBrowserVersion());
        assertEquals("146.0.3856", info.getDriverVersion());
        assertEquals("https://example.com/driver.zip", info.getDownloadUrl());
        assertEquals(Platform.WINDOWS, info.getPlatform());
        assertEquals(Architecture.X64, info.getArchitecture());
        assertEquals("msedgedriver.exe", info.getDriverFileName());
        assertEquals("edgedriver_win64.zip", info.getArchiveName());
    }

    @Test
    void testToString() {
        DriverDownloadInfo info = DriverDownloadInfo.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .downloadUrl("https://example.com/driver.zip")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        String str = info.toString();
        assertTrue(str.contains("146.0.3856.109"));
        assertTrue(str.contains("146.0.3856"));
        assertTrue(str.contains("WINDOWS"));
        assertTrue(str.contains("X64"));
    }

    @Test
    void testEquals() {
        DriverDownloadInfo info1 = DriverDownloadInfo.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .downloadUrl("https://example.com/driver.zip")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        DriverDownloadInfo info2 = DriverDownloadInfo.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .downloadUrl("https://example.com/driver.zip")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    void testNotEquals() {
        DriverDownloadInfo info1 = DriverDownloadInfo.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .downloadUrl("https://example.com/driver.zip")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        DriverDownloadInfo info2 = DriverDownloadInfo.builder()
                .browserVersion("145.0.3856.109")
                .driverVersion("145.0.3856")
                .downloadUrl("https://example.com/driver2.zip")
                .platform(Platform.LINUX)
                .architecture(Architecture.ARM64)
                .build();

        assertNotEquals(info1, info2);
    }
}