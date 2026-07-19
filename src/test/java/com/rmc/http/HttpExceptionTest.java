package com.rmc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpException.
 */
class HttpExceptionTest {
    
    @Test
    @DisplayName("Should create timeout exception")
    void testTimeoutException() {
        URI uri = URI.create("https://example.com/api");
        Exception cause = new java.net.http.HttpTimeoutException("Request timed out");
        
        HttpException ex = HttpException.timeout(uri, "GET", cause);
        
        assertEquals(HttpException.ErrorType.TIMEOUT, ex.getErrorType());
        assertEquals(uri, ex.getRequestUri().orElse(null));
        assertEquals("GET", ex.getMethod().orElse(null));
        assertTrue(ex.isTimeout());
        assertFalse(ex.isDnsError());
        assertFalse(ex.isHttpError());
        assertNotNull(ex.getCause());
    }
    
    @Test
    @DisplayName("Should create connection failed exception")
    void testConnectionFailedException() {
        URI uri = URI.create("https://example.com/api");
        Exception cause = new java.net.ConnectException("Connection refused");
        
        HttpException ex = HttpException.connectionFailed(uri, "POST", cause);
        
        assertEquals(HttpException.ErrorType.CONNECTION_FAILED, ex.getErrorType());
        assertFalse(ex.isTimeout());
        assertFalse(ex.isDnsError());
        assertTrue(ex.getRequestUri().isPresent());
    }
    
    @Test
    @DisplayName("Should detect DNS error from UnknownHostException")
    void testDnsErrorException() {
        URI uri = URI.create("https://unknown-host.com/api");
        Exception cause = new java.net.UnknownHostException("unknown-host.com");
        
        HttpException ex = HttpException.connectionFailed(uri, "GET", cause);
        
        assertEquals(HttpException.ErrorType.DNS_ERROR, ex.getErrorType());
        assertTrue(ex.isDnsError());
    }
    
    @Test
    @DisplayName("Should create HTTP error exception")
    void testHttpErrorException() {
        URI uri = URI.create("https://example.com/api");
        
        HttpException ex = HttpException.httpError(uri, "GET", 404, "Not Found");
        
        assertEquals(HttpException.ErrorType.HTTP_ERROR, ex.getErrorType());
        assertEquals(404, ex.getStatusCode().orElse(0));
        assertEquals("Not Found", ex.getResponseBody().orElse(null));
        assertTrue(ex.isHttpError());
        assertFalse(ex.isTimeout());
    }
    
    @Test
    @DisplayName("Should create SSL error exception")
    void testSslErrorException() {
        URI uri = URI.create("https://example.com/api");
        Exception cause = new javax.net.ssl.SSLException("SSL handshake failed");
        
        HttpException ex = HttpException.sslError(uri, "GET", cause);
        
        assertEquals(HttpException.ErrorType.SSL_ERROR, ex.getErrorType());
    }
    
    @Test
    @DisplayName("Should include all details in message")
    void testMessageContainsDetails() {
        URI uri = URI.create("https://example.com/api");
        
        HttpException ex = HttpException.httpError(uri, "POST", 500, "Internal Server Error");
        
        String message = ex.getMessage();
        assertTrue(message.contains("HTTP_ERROR"));
        assertTrue(message.contains("example.com"));
        assertTrue(message.contains("POST"));
        assertTrue(message.contains("500"));
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        URI uri = URI.create("https://example.com/api");
        
        HttpException ex = HttpException.httpError(uri, "GET", 403, "Forbidden");
        
        String str = ex.toString();
        assertTrue(str.contains("HttpException"));
        assertTrue(str.contains("HTTP_ERROR"));
        assertTrue(str.contains("403"));
    }
    
    @Test
    @DisplayName("Should handle optionals correctly for http error without body")
    void testOptionalsForHttpError() {
        URI uri = URI.create("https://example.com/api");
        
        HttpException ex = HttpException.httpError(uri, "POST", 500, null);
        
        assertTrue(ex.getRequestUri().isPresent());
        assertEquals(uri, ex.getRequestUri().get());
        assertEquals("POST", ex.getMethod().orElse(null));
        assertEquals(500, ex.getStatusCode().orElse(0));
        assertTrue(ex.getResponseBody().isEmpty());
    }
}
