package com.rmc.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionService class.
 */
class VersionServiceTest {

    @Test
    void testGetCurrentVersion() {
        Version version = VersionService.getCurrentVersion();
        
        assertNotNull(version);
        assertEquals(0, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(0, version.getPatch());
    }

    @Test
    void testGetCurrentVersionString() {
        String versionString = VersionService.getCurrentVersionString();
        
        assertEquals("0.1.0", versionString);
    }

    @Test
    void testParseValidVersion() throws VersionParseException {
        Version version = VersionService.parse("1.2.3");
        
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
    }

    @Test
    void testParseInvalidVersion() {
        assertThrows(VersionParseException.class, () -> VersionService.parse("invalid"));
    }

    @Test
    void testCompareStringVersions() throws VersionParseException {
        assertEquals(VersionComparison.NEWER, VersionService.compare("0.1.1", "0.1.0"));
        assertEquals(VersionComparison.OLDER, VersionService.compare("0.1.0", "0.1.1"));
        assertEquals(VersionComparison.EQUAL, VersionService.compare("0.1.0", "0.1.0"));
    }

    @Test
    void testCompareVersions() throws VersionParseException {
        Version v1 = VersionParser.parse("1.0.0");
        Version v2 = VersionParser.parse("0.9.9");
        
        assertEquals(VersionComparison.NEWER, VersionService.compare(v1, v2));
    }

    @Test
    void testCompareSpecialCases() throws VersionParseException {
        // Edge cases as specified in requirements
        assertEquals(VersionComparison.EQUAL, VersionService.compare("0.1.0", "0.1.0"));
        assertEquals(VersionComparison.NEWER, VersionService.compare("0.1.1", "0.1.0"));
        assertEquals(VersionComparison.NEWER, VersionService.compare("1.0.0", "0.9.9"));
        assertEquals(VersionComparison.NEWER, VersionService.compare("2.0.0", "1.9.9"));
    }
}