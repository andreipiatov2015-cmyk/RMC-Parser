package com.rmc.download;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DownloadResult class.
 */
class DownloadResultTest {

    @Test
    void testSuccessResult() {
        DownloadResult result = DownloadResult.success("/path/to/driver.exe", "146.0.3856");
        
        assertTrue(result.isSuccess());
        assertEquals("/path/to/driver.exe", result.getDriverPath());
        assertEquals("146.0.3856", result.getVersion());
        assertNull(result.getErrorMessage());
        assertEquals(DownloadResult.ErrorType.NONE, result.getErrorType());
    }

    @Test
    void testErrorResult() {
        DownloadResult result = DownloadResult.error(
            DownloadResult.ErrorType.HTTP_ERROR, "HTTP error: 404");
        
        assertFalse(result.isSuccess());
        assertNull(result.getDriverPath());
        assertNull(result.getVersion());
        assertEquals("HTTP error: 404", result.getErrorMessage());
        assertEquals(DownloadResult.ErrorType.HTTP_ERROR, result.getErrorType());
    }

    @Test
    void testEdgeNotInstalled() {
        DownloadResult result = DownloadResult.edgeNotInstalled();
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.EDGE_NOT_INSTALLED, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("not installed"));
    }

    @Test
    void testAlreadyExists() {
        DownloadResult result = DownloadResult.alreadyExists("/path/driver.exe", "146.0.3856");
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.ALREADY_EXISTS, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("already exists"));
    }

    @Test
    void testInternetUnavailable() {
        DownloadResult result = DownloadResult.internetUnavailable(
            new java.net.UnknownHostException("No internet"));
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.INTERNET_UNAVAILABLE, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("Internet unavailable"));
    }

    @Test
    void testHttpError() {
        DownloadResult result = DownloadResult.httpError(404);
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.HTTP_ERROR, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("404"));
    }

    @Test
    void testZipError() {
        DownloadResult result = DownloadResult.zipError("Invalid ZIP format");
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.ZIP_ERROR, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("Invalid ZIP"));
    }

    @Test
    void testExtractionError() {
        DownloadResult result = DownloadResult.extractionError("Corrupt archive");
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.EXTRACTION_ERROR, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("Extraction failed"));
    }

    @Test
    void testPermissionDenied() {
        DownloadResult result = DownloadResult.permissionDenied("/system/dir");
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.PERMISSION_DENIED, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("Permission denied"));
    }

    @Test
    void testToStringSuccess() {
        DownloadResult result = DownloadResult.success("/path/driver.exe", "1.0.0");
        String str = result.toString();
        
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("path"));
    }

    @Test
    void testToStringError() {
        DownloadResult result = DownloadResult.error(
            DownloadResult.ErrorType.UNKNOWN_ERROR, "Something went wrong");
        String str = result.toString();
        
        assertTrue(str.contains("success=false"));
        assertTrue(str.contains("error"));
    }

    @Test
    void testResolverError() {
        DownloadResult result = DownloadResult.resolverError("INVALID_VERSION", "Invalid version format");
        
        assertFalse(result.isSuccess());
        assertEquals(DownloadResult.ErrorType.RESOLVER_ERROR, result.getErrorType());
        assertTrue(result.getErrorMessage().contains("INVALID_VERSION"));
        assertTrue(result.getErrorMessage().contains("Resolver error"));
    }
}