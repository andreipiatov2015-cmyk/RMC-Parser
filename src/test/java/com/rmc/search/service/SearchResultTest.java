package com.rmc.search.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchResult.
 */
class SearchResultTest {
    
    @Test
    @DisplayName("Should create successful result")
    void testSuccessfulResult() {
        SearchResult result = SearchResult.builder()
                .success(true)
                .html("<html><body>Results</body></html>")
                .requestUrl("https://example.com/search?q=test")
                .statusCode(200)
                .responseTimeMs(150)
                .build();
        
        assertTrue(result.isSuccess());
        assertEquals("<html><body>Results</body></html>", result.getHtml());
        assertEquals("https://example.com/search?q=test", result.getRequestUrl());
        assertEquals(200, result.getStatusCode());
        assertEquals(150, result.getResponseTimeMs());
        assertEquals(32, result.getContentLength());
        assertTrue(result.hasHtml());
    }
    
    @Test
    @DisplayName("Should create failed result")
    void testFailedResult() {
        SearchResult result = SearchResult.builder()
                .success(false)
                .requestUrl("https://example.com/search?q=test")
                .responseTimeMs(5000)
                .errorMessage("Connection timeout")
                .build();
        
        assertFalse(result.isSuccess());
        assertNull(result.getHtml());
        assertEquals("Connection timeout", result.getErrorMessage().orElse(null));
    }
    
    @Test
    @DisplayName("Should handle null HTML")
    void testNullHtml() {
        SearchResult result = SearchResult.builder()
                .success(true)
                .requestUrl("https://example.com/search")
                .statusCode(204)
                .build();
        
        assertNull(result.getHtml());
        assertFalse(result.hasHtml());
    }
    
    @Test
    @DisplayName("Should calculate content length")
    void testContentLength() {
        SearchResult result = SearchResult.builder()
                .success(true)
                .html("Test content")
                .build();
        
        assertEquals(12, result.getContentLength());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        SearchResult result = SearchResult.builder()
                .success(true)
                .html("<html>")
                .requestUrl("https://example.com")
                .statusCode(200)
                .responseTimeMs(100)
                .build();
        
        String str = result.toString();
        assertTrue(str.contains("SearchResult"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("200"));
    }
}
