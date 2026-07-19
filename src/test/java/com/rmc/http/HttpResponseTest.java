package com.rmc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpResponse.
 */
class HttpResponseTest {
    
    @Test
    @DisplayName("Should create successful response with builder")
    void testBuilder() {
        HttpResponse response = HttpResponse.builder()
                .statusCode(200)
                .body("Hello World")
                .headers(Map.of("Content-Type", "text/plain"))
                .requestUri(URI.create("https://example.com"))
                .requestMethod("GET")
                .responseTime(Duration.ofMillis(100))
                .build();
        
        assertEquals(200, response.getStatusCode());
        assertEquals("Hello World", response.getBody());
        assertEquals("text/plain", response.getHeader("Content-Type").orElse(null));
        assertEquals("https://example.com", response.getRequestUri().toString());
        assertEquals("GET", response.getRequestMethod());
        assertEquals(100, response.getResponseTime().toMillis());
    }
    
    @Test
    @DisplayName("Should detect successful response (2xx)")
    void testIsSuccessful() {
        HttpResponse ok = HttpResponse.builder().statusCode(200).build();
        HttpResponse created = HttpResponse.builder().statusCode(201).build();
        HttpResponse noContent = HttpResponse.builder().statusCode(204).build();
        
        assertTrue(ok.isSuccessful());
        assertTrue(created.isSuccessful());
        assertTrue(noContent.isSuccessful());
        assertTrue(ok.isOk());
    }
    
    @Test
    @DisplayName("Should detect non-successful response")
    void testIsNotSuccessful() {
        HttpResponse badRequest = HttpResponse.builder().statusCode(400).build();
        HttpResponse notFound = HttpResponse.builder().statusCode(404).build();
        HttpResponse serverError = HttpResponse.builder().statusCode(500).build();
        
        assertFalse(badRequest.isSuccessful());
        assertFalse(notFound.isSuccessful());
        assertFalse(serverError.isSuccessful());
        
        assertTrue(badRequest.isClientError());
        assertTrue(notFound.isClientError());
        assertTrue(serverError.isServerError());
    }
    
    @Test
    @DisplayName("Should detect redirect response")
    void testIsRedirect() {
        HttpResponse moved = HttpResponse.builder().statusCode(301).build();
        HttpResponse found = HttpResponse.builder().statusCode(302).build();
        
        assertTrue(moved.isRedirect());
        assertTrue(found.isRedirect());
        assertFalse(moved.isSuccessful());
    }
    
    @Test
    @DisplayName("Should handle null body")
    void testNullBody() {
        HttpResponse response = HttpResponse.builder()
                .statusCode(204)
                .build();
        
        assertNull(response.getBody());
        assertArrayEquals(new byte[0], response.getBodyAsBytes());
    }
    
    @Test
    @DisplayName("Should return empty optional for missing header")
    void testMissingHeader() {
        HttpResponse response = HttpResponse.builder()
                .headers(Map.of("Content-Type", "text/plain"))
                .build();
        
        assertTrue(response.getHeader("X-Custom").isEmpty());
        assertTrue(response.getHeader("content-type").isEmpty());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        HttpResponse response = HttpResponse.builder()
                .statusCode(200)
                .requestUri(URI.create("https://example.com/api"))
                .responseTime(Duration.ofMillis(50))
                .build();
        
        String str = response.toString();
        assertTrue(str.contains("200"));
        assertTrue(str.contains("example.com"));
        assertTrue(str.contains("50ms"));
    }
}
