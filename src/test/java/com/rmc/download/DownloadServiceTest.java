package com.rmc.download;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DownloadService utility methods.
 */
class DownloadServiceTest {

    @Test
    void testExtractDriverVersionWithFourParts() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856.109");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testExtractDriverVersionWithThreeParts() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0.3856");
        assertEquals("146.0.3856", result);
    }

    @Test
    void testExtractDriverVersionWithTwoParts() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146.0");
        assertEquals("146.0", result);
    }

    @Test
    void testExtractDriverVersionWithOnePart() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "146");
        assertEquals("146", result);
    }

    @Test
    void testExtractDriverVersionNull() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, (String) null);
        assertEquals("", result);
    }

    @Test
    void testExtractDriverVersionEmpty() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("extractDriverVersion", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(null, "");
        assertEquals("", result);
    }

    @Test
    void testGetDriverDirectory() throws Exception {
        Method method = DownloadService.class.getDeclaredMethod("getDriverDirectory");
        method.setAccessible(true);
        
        Object result = method.invoke(null);
        assertNotNull(result);
        assertTrue(result.toString().contains("RMCParser"));
        assertTrue(result.toString().contains("drivers"));
        assertTrue(result.toString().contains("edge"));
    }
}