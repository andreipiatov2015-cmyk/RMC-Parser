package com.rmc.driver.resolver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverResolverException.
 */
class DriverResolverExceptionTest {

    @Test
    void testAllErrorTypesExist() {
        assertNotNull(DriverResolverException.ErrorType.UNSUPPORTED_PLATFORM);
        assertNotNull(DriverResolverException.ErrorType.UNSUPPORTED_ARCHITECTURE);
        assertNotNull(DriverResolverException.ErrorType.INVALID_VERSION);
        assertNotNull(DriverResolverException.ErrorType.UNKNOWN_BROWSER_VERSION);
        assertNotNull(DriverResolverException.ErrorType.NETWORK_ERROR);
        assertNotNull(DriverResolverException.ErrorType.UNKNOWN_ERROR);
    }

    @Test
    void testUnsupportedPlatform() {
        DriverResolverException ex = DriverResolverException.unsupportedPlatform(Platform.LINUX);
        
        assertEquals(DriverResolverException.ErrorType.UNSUPPORTED_PLATFORM, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Unsupported platform"));
        assertTrue(ex.getMessage().contains("LINUX"));
    }

    @Test
    void testUnsupportedArchitecture() {
        DriverResolverException ex = DriverResolverException.unsupportedArchitecture(Architecture.UNKNOWN);
        
        assertEquals(DriverResolverException.ErrorType.UNSUPPORTED_ARCHITECTURE, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Unsupported architecture"));
    }

    @Test
    void testInvalidVersion() {
        DriverResolverException ex = DriverResolverException.invalidVersion(null);
        
        assertEquals(DriverResolverException.ErrorType.INVALID_VERSION, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Invalid browser version"));
    }

    @Test
    void testUnknownBrowserVersion() {
        DriverResolverException ex = DriverResolverException.unknownBrowserVersion("999.999.999");
        
        assertEquals(DriverResolverException.ErrorType.UNKNOWN_BROWSER_VERSION, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Unknown browser version"));
    }

    @Test
    void testNetworkError() {
        Exception cause = new java.net.SocketTimeoutException("Connection timed out");
        DriverResolverException ex = DriverResolverException.networkError(cause);
        
        assertEquals(DriverResolverException.ErrorType.NETWORK_ERROR, ex.getErrorType());
        assertEquals(cause, ex.getCause());
    }
}