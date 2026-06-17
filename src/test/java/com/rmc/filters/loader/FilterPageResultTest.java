package com.rmc.filters.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterPageResult.
 */
class FilterPageResultTest {
    
    @Test
    @DisplayName("Should create successful result")
    void testSuccessfulResult() {
        FilterPageResult result = FilterPageResult.builder()
                .success(true)
                .html("<html><body>Filters</body></html>")
                .statusCode(200)
                .url("https://example.com/programs/")
                .size(100)
                .build();
        
        assertTrue(result.isSuccess());
        assertEquals("<html><body>Filters</body></html>", result.getHtml());
        assertEquals(200, result.getStatusCode());
        assertEquals("https://example.com/programs/", result.getUrl());
        assertEquals(100, result.getSize());
        assertTrue(result.getErrorMessage().isEmpty());
    }
    
    @Test
    @DisplayName("Should create failed result")
    void testFailedResult() {
        FilterPageResult result = FilterPageResult.builder()
                .success(false)
                .url("https://example.com/programs/")
                .errorMessage("Connection timeout")
                .build();
        
        assertFalse(result.isSuccess());
        assertNull(result.getHtml());
        assertEquals(0, result.getStatusCode());
        assertEquals("Connection timeout", result.getErrorMessage().orElse(null));
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        FilterPageResult result = FilterPageResult.builder()
                .success(true)
                .url("https://example.com/programs/")
                .size(500)
                .build();
        
        String str = result.toString();
        assertTrue(str.contains("FilterPageResult"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("500"));
    }
}
