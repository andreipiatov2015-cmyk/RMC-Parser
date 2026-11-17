package com.rmc.version;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VersionService class.
 */
class VersionServiceTest {

    @Test
    void currentVersionMatchesMavenBuild() throws Exception {
        String buildVersion;
        try (InputStream pom = Files.newInputStream(Path.of("pom.xml"))) {
            buildVersion = XPathFactory.newInstance().newXPath()
                    .evaluate("/*[local-name()='project']/*[local-name()='version']", new InputSource(pom)).trim();
        }

        assertEquals(buildVersion, VersionService.getCurrentVersionString());
        assertEquals(VersionParser.parse(buildVersion), VersionService.getCurrentVersion());
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