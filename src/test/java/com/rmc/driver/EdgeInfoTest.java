package com.rmc.driver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EdgeInfo class.
 */
class EdgeInfoTest {

    @Test
    void testEdgeInfoCreation() {
        EdgeInfo info = new EdgeInfo("C:\\Path\\msedge.exe", "146.0.3856.109", true);
        
        assertEquals("C:\\Path\\msedge.exe", info.getPath());
        assertEquals("146.0.3856.109", info.getVersion());
        assertTrue(info.isInstalled());
    }

    @Test
    void testNotInstalled() {
        EdgeInfo info = EdgeInfo.notInstalled();
        
        assertNull(info.getPath());
        assertNull(info.getVersion());
        assertFalse(info.isInstalled());
    }

    @Test
    void testToStringInstalled() {
        EdgeInfo info = new EdgeInfo("C:\\Path\\msedge.exe", "146.0.3856.109", true);
        String str = info.toString();
        
        assertTrue(str.contains("msedge.exe"));
        assertTrue(str.contains("146.0.3856.109"));
    }

    @Test
    void testToStringNotInstalled() {
        EdgeInfo info = EdgeInfo.notInstalled();
        String str = info.toString();
        
        assertTrue(str.contains("not installed"));
    }
}