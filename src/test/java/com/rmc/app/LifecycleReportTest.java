package com.rmc.app;

import com.rmc.driver.DriverStatus;
import com.rmc.driver.validation.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleReport class.
 */
class LifecycleReportTest {

    @Test
    void testDefaultValues() {
        LifecycleReport report = new LifecycleReport();
        
        assertFalse(report.isConfigLoaded());
        assertFalse(report.isLoggingInitialized());
        assertFalse(report.isVersionEngineLoaded());
        assertFalse(report.isUpdateCheckCompleted());
        assertFalse(report.isEdgeDetected());
        assertFalse(report.isDriverDetected());
        assertFalse(report.isValidationCompleted());
        assertFalse(report.isApplicationReady());
        assertNull(report.getValidationStatus());
    }

    @Test
    void testConfigLoaded() {
        LifecycleReport report = new LifecycleReport();
        report.setConfigLoaded(true);
        report.setRepositoryOwner("test-owner");
        report.setRepositoryName("test-repo");
        report.setUpdateChannel("stable");
        
        assertTrue(report.isConfigLoaded());
        assertEquals("test-owner", report.getRepositoryOwner());
        assertEquals("test-repo", report.getRepositoryName());
        assertEquals("stable", report.getUpdateChannel());
    }

    @Test
    void testEdgeDetection() {
        LifecycleReport report = new LifecycleReport();
        report.setEdgeDetected(true);
        report.setEdgeVersion("146.0.3856.109");
        report.setEdgePath("C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe");
        
        assertTrue(report.isEdgeDetected());
        assertEquals("146.0.3856.109", report.getEdgeVersion());
        assertNotNull(report.getEdgePath());
    }

    @Test
    void testDriverDetection() {
        LifecycleReport report = new LifecycleReport();
        report.setDriverDetected(true);
        report.setDriverVersion("146.0.3856");
        report.setDriverPath("C:\\Users\\test\\AppData\\Local\\RMCParser\\drivers\\edge\\msedgedriver.exe");
        report.setDriverStatus(DriverStatus.MATCH);
        
        assertTrue(report.isDriverDetected());
        assertEquals("146.0.3856", report.getDriverVersion());
        assertEquals(DriverStatus.MATCH, report.getDriverStatus());
    }

    @Test
    void testValidationStatus() {
        LifecycleReport report = new LifecycleReport();
        report.setValidationCompleted(true);
        report.setValidationStatus(ValidationStatus.VALID);
        
        assertTrue(report.isValidationCompleted());
        assertEquals(ValidationStatus.VALID, report.getValidationStatus());
    }

    @Test
    void testApplicationReady() {
        LifecycleReport report = new LifecycleReport();
        report.setApplicationReady(true);
        report.setStartTime(LocalDateTime.now());
        report.setApplicationVersion("0.1.0");
        
        assertTrue(report.isApplicationReady());
        assertNotNull(report.getStartTime());
        assertEquals("0.1.0", report.getApplicationVersion());
    }

    @Test
    void testUpdateAvailable() {
        LifecycleReport report = new LifecycleReport();
        report.setUpdateCheckCompleted(true);
        report.setUpdateAvailable(true);
        report.setLatestVersion("0.2.0");
        
        assertTrue(report.isUpdateCheckCompleted());
        assertTrue(report.isUpdateAvailable());
        assertEquals("0.2.0", report.getLatestVersion());
    }

    @Test
    void testToSummary() {
        LifecycleReport report = new LifecycleReport();
        report.setApplicationVersion("0.1.0");
        report.setApplicationReady(true);
        report.setEdgeDetected(true);
        report.setEdgeVersion("146.0.3856.109");
        report.setDriverDetected(true);
        report.setDriverVersion("146.0.3856");
        report.setValidationStatus(ValidationStatus.VALID);
        
        String summary = report.toSummary();
        
        assertTrue(summary.contains("0.1.0"));
        assertTrue(summary.contains("true"));
        assertTrue(summary.contains("Edge"));
        assertTrue(summary.contains("Driver"));
        assertTrue(summary.contains("Validation"));
    }

    @Test
    void testErrorTracking() {
        LifecycleReport report = new LifecycleReport();
        report.setConfigError("Failed to load config");
        report.setVersionError("Version file not found");
        report.setDriverDetectionError("Edge not installed");
        report.setValidationError("Driver corrupted");
        
        assertEquals("Failed to load config", report.getConfigError());
        assertEquals("Version file not found", report.getVersionError());
        assertEquals("Edge not installed", report.getDriverDetectionError());
        assertEquals("Driver corrupted", report.getValidationError());
    }
}