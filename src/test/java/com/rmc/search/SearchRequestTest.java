package com.rmc.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchRequest.
 */
class SearchRequestTest {
    
    @Test
    @DisplayName("Should create SearchRequest with builder")
    void testBuilder() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .parameters(Map.of("query", "test", "page", "1"))
                .build();
        
        assertEquals("https://example.com/search", request.getBaseUrl());
        assertEquals(2, request.getParameterCount());
        assertEquals("test", request.getParameter("query"));
        assertEquals("1", request.getParameter("page"));
    }
    
    @Test
    @DisplayName("Should build full URL with query string")
    void testFullUrl() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .parameters(Map.of("query", "test", "category", "books"))
                .build();
        
        String fullUrl = request.getFullUrl();
        
        assertTrue(fullUrl.contains("https://example.com/search?"));
        assertTrue(fullUrl.contains("query=test"));
        assertTrue(fullUrl.contains("category=books"));
    }
    
    @Test
    @DisplayName("Should encode URL parameters")
    void testUrlEncoding() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .parameters(Map.of("query", "test value"))
                .build();
        
        String fullUrl = request.getFullUrl();
        assertTrue(fullUrl.contains("query=test+value"));
    }
    
    @Test
    @DisplayName("Should handle null parameters")
    void testNullParameters() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .build();
        
        assertFalse(request.hasParameters());
        assertEquals(0, request.getParameterCount());
    }
    
    @Test
    @DisplayName("Should check parameter existence")
    void testHasParameter() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .parameters(Map.of("query", "test"))
                .build();
        
        assertTrue(request.hasParameter("query"));
        assertFalse(request.hasParameter("nonexistent"));
    }
    
    @Test
    @DisplayName("Should return query string")
    void testQueryString() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com/search")
                .parameters(Map.of("a", "1", "b", "2"))
                .build();
        
        String queryString = request.getQueryString();
        
        assertTrue(queryString.contains("a=1"));
        assertTrue(queryString.contains("b=2"));
        assertFalse(queryString.contains("?"));
    }
    
    @Test
    @DisplayName("Should create from Map")
    void testFrom() {
        SearchRequest request = SearchRequest.from(Map.of("query", "test"));
        
        assertNotNull(request.getFullUrl());
        assertTrue(request.hasParameter("query"));
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        SearchRequest request = SearchRequest.builder()
                .baseUrl("https://example.com")
                .parameters(Map.of("key", "value"))
                .build();
        
        String str = request.toString();
        assertTrue(str.contains("SearchRequest"));
        assertTrue(str.contains("https://example.com"));
        assertTrue(str.contains("key=value"));
    }
}
