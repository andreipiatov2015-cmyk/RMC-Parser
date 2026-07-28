package com.rmc.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionParser class.
 */
class VersionParserTest {

    @Test
    void testParseValidVersion() throws VersionParseException {
        Version version = VersionParser.parse("0.1.0");
        
        assertEquals(0, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(0, version.getPatch());
    }

    @Test
    void testParseLargeVersionNumbers() throws VersionParseException {
        Version version = VersionParser.parse("123.456.789");
        
        assertEquals(123, version.getMajor());
        assertEquals(456, version.getMinor());
        assertEquals(789, version.getPatch());
    }

    @Test
    void testParseNullString() {
        VersionParseException exception = assertThrows(
            VersionParseException.class,
            () -> VersionParser.parse(null)
        );
        assertEquals("Version string cannot be null", exception.getMessage());
    }

    @Test
    void testParseEmptyString() {
        VersionParseException exception = assertThrows(
            VersionParseException.class,
            () -> VersionParser.parse("")
        );
        assertEquals("Version string cannot be empty", exception.getMessage());
    }

    @Test
    void testParseWhitespaceOnly() {
        VersionParseException exception = assertThrows(
            VersionParseException.class,
            () -> VersionParser.parse("   ")
        );
        assertEquals("Version string cannot be empty", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.0",           // Missing patch
        "0.1",           // Missing patch
        "1",             // Missing minor and patch
        "1.0.0.0",       // Too many parts
        "1.0.0.0.0",     // Way too many parts
        "v1.0.0",        // Has 'v' prefix
        "1.0.0-SNAPSHOT",// Has suffix
        "1.0.a",         // Non-numeric patch
        "a.0.0",         // Non-numeric major
        "0.a.0",         // Non-numeric minor
        "1_0_0",         // Wrong separator
        "1-0-0",         // Wrong separator
        "1.0.0.",        // Trailing dot
        ".1.0.0",        // Leading dot
        "v.1.0.0",       // Just v prefix
        "a.b.c"          // All non-numeric
    })
    void testParseInvalidVersion(String invalidVersion) {
        VersionParseException exception = assertThrows(
            VersionParseException.class,
            () -> VersionParser.parse(invalidVersion)
        );
        assertTrue(exception.getMessage().contains("Invalid"));
    }

    @Test
    void testTryParseValidVersion() {
        Version version = VersionParser.tryParse("0.1.0");
        
        assertNotNull(version);
        assertEquals(0, version.getMajor());
        assertEquals(1, version.getMinor());
        assertEquals(0, version.getPatch());
    }

    @Test
    void testTryParseInvalidVersion() {
        Version version = VersionParser.tryParse("invalid");
        
        assertNull(version);
    }

    @Test
    void testTryParseNull() {
        Version version = VersionParser.tryParse(null);
        
        assertNull(version);
    }
}