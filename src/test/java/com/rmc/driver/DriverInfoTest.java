package com.rmc.driver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverInfo class.
 */
class DriverInfoTest {

    @Test
    void testDriverInfoCreation() {
        DriverInfo info = new DriverInfo("C:\\Path\\msedgedriver.exe", "146.0.3856.109", true);
        
        assertEquals("C:\\Path\\msedgedriver.exe", info.getPath());
        assertEquals("146.0.3856.109", info.getVersion());
        assertTrue(info.isInstalled());
    }

    @Test
    void testNotInstalled() {
        DriverInfo info = DriverInfo.notInstalled();
        
        assertNull(info.getPath());
        assertNull(info.getVersion());
        assertFalse(info.isInstalled());
    }

    @Test
    void testToStringInstalled() {
        DriverInfo info = new DriverInfo("C:\\Path\\msedgedriver.exe", "146.0.3856.109", true);
        String str = info.toString();
        
        assertTrue(str.contains("msedgedriver.exe"));
        assertTrue(str.contains("146.0.3856.109"));
    }

    @Test
    void testToStringNotInstalled() {
        DriverInfo info = DriverInfo.notInstalled();
        String str = info.toString();
        
        assertTrue(str.contains("not installed"));
    }
}