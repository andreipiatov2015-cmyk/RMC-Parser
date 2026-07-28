package com.rmc.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Version class.
 */
class VersionTest {

    @Test
    void testVersionCreation() {
        Version version = new Version(1, 2, 3);
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
    }

    @Test
    void testVersionToString() {
        Version version = new Version(0, 1, 0);
        assertEquals("0.1.0", version.toString());
    }

    @Test
    void testVersionEquals() {
        Version v1 = new Version(0, 1, 0);
        Version v2 = new Version(0, 1, 0);
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testVersionNotEquals() {
        Version v1 = new Version(0, 1, 0);
        Version v2 = new Version(0, 1, 1);
        assertNotEquals(v1, v2);
    }

    @ParameterizedTest
    @CsvSource({
        "0.1.0, 0.1.0, 0",
        "0.1.1, 0.1.0, 1",
        "0.1.0, 0.1.1, -1",
        "1.0.0, 0.9.9, 1",
        "2.0.0, 1.9.9, 1",
        "10.0.0, 9.99.99, 1"
    })
    void testVersionCompareTo(String v1Str, String v2Str, int expectedSign) {
        String[] parts1 = v1Str.split("\\.");
        String[] parts2 = v2Str.split("\\.");
        
        Version v1 = new Version(
            Integer.parseInt(parts1[0]),
            Integer.parseInt(parts1[1]),
            Integer.parseInt(parts1[2])
        );
        Version v2 = new Version(
            Integer.parseInt(parts2[0]),
            Integer.parseInt(parts2[1]),
            Integer.parseInt(parts2[2])
        );
        
        int result = v1.compareTo(v2);
        
        if (expectedSign > 0) {
            assertTrue(result > 0, v1Str + " should be greater than " + v2Str);
        } else if (expectedSign < 0) {
            assertTrue(result < 0, v1Str + " should be less than " + v2Str);
        } else {
            assertEquals(0, result, v1Str + " should equal " + v2Str);
        }
    }
}