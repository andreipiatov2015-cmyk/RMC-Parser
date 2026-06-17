package com.rmc.driver.validation;

import com.rmc.driver.system.Architecture;
import com.rmc.driver.system.Platform;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverManifest class.
 */
class DriverManifestTest {

    @Test
    void testBuilderCreation() {
        LocalDateTime now = LocalDateTime.now();
        DriverManifest manifest = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .driverPath("C:\\Users\\test\\msedgedriver.exe")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .installationTime(now)
                .validationTime(now)
                .status(ValidationStatus.VALID)
                .build();

        assertEquals("146.0.3856.109", manifest.getBrowserVersion());
        assertEquals("146.0.3856", manifest.getDriverVersion());
        assertEquals("C:\\Users\\test\\msedgedriver.exe", manifest.getDriverPath());
        assertEquals(Platform.WINDOWS, manifest.getPlatform());
        assertEquals(Architecture.X64, manifest.getArchitecture());
        assertEquals(now, manifest.getInstallationTime());
        assertEquals(now, manifest.getValidationTime());
        assertEquals(ValidationStatus.VALID, manifest.getStatus());
    }

    @Test
    void testIsValid() {
        DriverManifest validManifest = DriverManifest.builder()
                .status(ValidationStatus.VALID)
                .build();
        assertTrue(validManifest.isValid());

        DriverManifest invalidManifest = DriverManifest.builder()
                .status(ValidationStatus.INVALID)
                .build();
        assertFalse(invalidManifest.isValid());
    }

    @Test
    void testIsMissing() {
        DriverManifest missingManifest = DriverManifest.builder()
                .status(ValidationStatus.MISSING)
                .build();
        assertTrue(missingManifest.isMissing());

        DriverManifest validManifest = DriverManifest.builder()
                .status(ValidationStatus.VALID)
                .build();
        assertFalse(validManifest.isMissing());
    }

    @Test
    void testHasMethods() {
        DriverManifest manifest = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .driverPath("C:\\Users\\test\\msedgedriver.exe")
                .build();

        assertTrue(manifest.hasBrowserVersion());
        assertTrue(manifest.hasDriverVersion());
        assertTrue(manifest.hasDriverPath());

        DriverManifest emptyManifest = DriverManifest.empty();
        assertFalse(emptyManifest.hasBrowserVersion());
        assertFalse(emptyManifest.hasDriverVersion());
        assertFalse(emptyManifest.hasDriverPath());
    }

    @Test
    void testEmpty() {
        DriverManifest empty = DriverManifest.empty();
        assertEquals(ValidationStatus.MISSING, empty.getStatus());
        assertFalse(empty.hasBrowserVersion());
        assertFalse(empty.hasDriverVersion());
        assertFalse(empty.hasDriverPath());
    }

    @Test
    void testEquals() {
        DriverManifest manifest1 = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .driverPath("C:\\Users\\test\\msedgedriver.exe")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        DriverManifest manifest2 = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .driverPath("C:\\Users\\test\\msedgedriver.exe")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        assertEquals(manifest1, manifest2);
        assertEquals(manifest1.hashCode(), manifest2.hashCode());
    }

    @Test
    void testNotEquals() {
        DriverManifest manifest1 = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .driverPath("C:\\Users\\test\\msedgedriver.exe")
                .platform(Platform.WINDOWS)
                .architecture(Architecture.X64)
                .build();

        DriverManifest manifest2 = DriverManifest.builder()
                .browserVersion("145.0.3856.109")
                .driverVersion("145.0.3856")
                .driverPath("C:\\Users\\test2\\msedgedriver.exe")
                .platform(Platform.LINUX)
                .architecture(Architecture.ARM64)
                .build();

        assertNotEquals(manifest1, manifest2);
    }

    @Test
    void testToString() {
        DriverManifest manifest = DriverManifest.builder()
                .browserVersion("146.0.3856.109")
                .driverVersion("146.0.3856")
                .status(ValidationStatus.VALID)
                .build();

        String str = manifest.toString();
        assertTrue(str.contains("146.0.3856.109"));
        assertTrue(str.contains("146.0.3856"));
        assertTrue(str.contains("VALID"));
    }
}