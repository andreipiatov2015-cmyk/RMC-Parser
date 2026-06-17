package com.rmc.download;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DownloadService utility methods.
 * Note: extractDriverVersion is now in DriverResolver.
 */
class DownloadServiceTest {

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