package com.rmc.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionComparator class.
 */
class VersionComparatorTest {

    @Test
    void testCompareEqualVersions() throws VersionParseException {
        Version v1 = VersionParser.parse("0.1.0");
        Version v2 = VersionParser.parse("0.1.0");
        
        assertEquals(VersionComparison.EQUAL, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareNewerPatch() throws VersionParseException {
        Version v1 = VersionParser.parse("0.1.1");
        Version v2 = VersionParser.parse("0.1.0");
        
        assertEquals(VersionComparison.NEWER, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareOlderPatch() throws VersionParseException {
        Version v1 = VersionParser.parse("0.1.0");
        Version v2 = VersionParser.parse("0.1.1");
        
        assertEquals(VersionComparison.OLDER, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareNewerMinor() throws VersionParseException {
        Version v1 = VersionParser.parse("0.2.0");
        Version v2 = VersionParser.parse("0.1.9");
        
        assertEquals(VersionComparison.NEWER, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareOlderMinor() throws VersionParseException {
        Version v1 = VersionParser.parse("0.1.9");
        Version v2 = VersionParser.parse("0.2.0");
        
        assertEquals(VersionComparison.OLDER, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareNewerMajor() throws VersionParseException {
        Version v1 = VersionParser.parse("1.0.0");
        Version v2 = VersionParser.parse("0.9.9");
        
        assertEquals(VersionComparison.NEWER, VersionComparator.compare(v1, v2));
    }

    @Test
    void testCompareOlderMajor() throws VersionParseException {
        Version v1 = VersionParser.parse("0.9.9");
        Version v2 = VersionParser.parse("1.0.0");
        
        assertEquals(VersionComparison.OLDER, VersionComparator.compare(v1, v2));
    }

    @ParameterizedTest
    @CsvSource({
        "1.0.0, 0.9.9, NEWER",
        "2.0.0, 1.9.9, NEWER",
        "0.1.1, 0.1.0, NEWER",
        "0.1.0, 0.1.0, EQUAL",
        "0.9.9, 1.0.0, OLDER",
        "1.9.9, 2.0.0, OLDER"
    })
    void testCompareVersions(String v1Str, String v2Str, VersionComparison expected) throws VersionParseException {
        Version v1 = VersionParser.parse(v1Str);
        Version v2 = VersionParser.parse(v2Str);
        
        assertEquals(expected, VersionComparator.compare(v1, v2));
    }

    @Test
    void testIsNewer() throws VersionParseException {
        Version v1 = VersionParser.parse("1.0.0");
        Version v2 = VersionParser.parse("0.9.9");
        
        assertTrue(VersionComparator.isNewer(v1, v2));
        assertFalse(VersionComparator.isNewer(v2, v1));
    }

    @Test
    void testIsOlder() throws VersionParseException {
        Version v1 = VersionParser.parse("0.9.9");
        Version v2 = VersionParser.parse("1.0.0");
        
        assertTrue(VersionComparator.isOlder(v1, v2));
        assertFalse(VersionComparator.isOlder(v2, v1));
    }

    @Test
    void testIsEqual() throws VersionParseException {
        Version v1 = VersionParser.parse("0.1.0");
        Version v2 = VersionParser.parse("0.1.0");
        
        assertTrue(VersionComparator.isEqual(v1, v2));
        assertFalse(VersionComparator.isEqual(v1, VersionParser.parse("0.1.1")));
    }
}