package com.rmc.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchRequestBuilder.
 */
class SearchRequestBuilderTest {
    
    @Test
    @DisplayName("Should create empty builder")
    void testCreate() {
        SearchRequestBuilder builder = SearchRequestBuilder.create();
        
        assertNotNull(builder);
        assertFalse(builder.hasParameters());
        assertEquals(0, builder.getParameterCount());
    }
    
    @Test
    @DisplayName("Should add parameters")
    void testAddParameter() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("activity", "3")
                .addParameter("activity", "8")
                .addParameter("direction", "2");
        
        assertTrue(builder.hasParameters());
        assertEquals(3, builder.getParameterCount());
        
        Map<String, String> params = builder.getParameters();
        assertEquals("3", params.get("activity"));
        assertEquals("2", params.get("direction"));
    }
    
    @Test
    @DisplayName("Should not add empty parameters")
    void testSkipEmptyParameters() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("valid", "value")
                .addParameter("empty", "")
                .addParameter(null, "value");
        
        assertEquals(1, builder.getParameterCount());
        assertTrue(builder.getParameters().containsKey("valid"));
    }
    
    @Test
    @DisplayName("Should add parameters from Map")
    void testAddFromMap() {
        Map<String, String> params = Map.of(
                "activity", "5",
                "direction", "1",
                "school", "mit"
        );
        
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addFrom(params);
        
        assertEquals(3, builder.getParameterCount());
    }
    
    @Test
    @DisplayName("Should add parameters from null Map")
    void testAddFromNullMap() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("test", "value")
                .addFrom((Map<String, String>) null);
        
        assertEquals(1, builder.getParameterCount());
    }
    
    @Test
    @DisplayName("Should build SearchRequest")
    void testBuild() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .baseUrl("https://rmc.example.com")
                .searchPath("/search")
                .addParameter("activity", "3")
                .addParameter("direction", "2");
        
        SearchRequest request = builder.build();
        
        assertNotNull(request);
        assertEquals("https://rmc.example.com/search", request.getBaseUrl());
        assertEquals("3", request.getParameter("activity"));
        assertEquals("2", request.getParameter("direction"));
    }
    
    @Test
    @DisplayName("Should build URL directly")
    void testBuildUrl() {
        String url = SearchRequestBuilder.create()
                .baseUrl("https://rmc.example.com")
                .addParameter("query", "test")
                .buildUrl();
        
        assertTrue(url.contains("https://rmc.example.com?"));
        assertTrue(url.contains("query=test"));
    }
    
    @Test
    @DisplayName("Should use default base URL")
    void testDefaultBaseUrl() {
        SearchRequest request = SearchRequestBuilder.create()
                .addParameter("test", "value")
                .build();
        
        assertNotNull(request.getFullUrl());
        assertTrue(request.getFullUrl().contains("rmc.example.com"));
    }
    
    @Test
    @DisplayName("Should handle search path with leading slash")
    void testSearchPathWithSlash() {
        SearchRequest request = SearchRequestBuilder.create()
                .baseUrl("https://example.com")
                .searchPath("/search/")
                .addParameter("q", "test")
                .build();
        
        assertEquals("https://example.com/search/?q=test", request.getFullUrl());
    }
    
    @Test
    @DisplayName("Should handle search path without leading slash")
    void testSearchPathWithoutSlash() {
        SearchRequest request = SearchRequestBuilder.create()
                .baseUrl("https://example.com/")
                .searchPath("search")
                .addParameter("q", "test")
                .build();
        
        assertEquals("https://example.com/search?q=test", request.getFullUrl());
    }
    
    @Test
    @DisplayName("Should clear parameters")
    void testClear() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("test", "value")
                .clear();
        
        assertFalse(builder.hasParameters());
        assertEquals(0, builder.getParameterCount());
    }
    
    @Test
    @DisplayName("Should remove parameter")
    void testRemoveParameter() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("keep", "value")
                .addParameter("remove", "value")
                .removeParameter("remove");
        
        assertEquals(1, builder.getParameterCount());
        assertTrue(builder.getParameters().containsKey("keep"));
        assertFalse(builder.getParameters().containsKey("remove"));
    }
    
    @Test
    @DisplayName("Should chain multiple addFrom calls")
    void testChainedAddFrom() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addParameter("first", "1")
                .addFrom(Map.of("second", "2"))
                .addParameter("third", "3");
        
        assertEquals(3, builder.getParameterCount());
    }
    
    @Test
    @DisplayName("Should add multiple values for same parameter")
    void testMultiParameter() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addMultiParameter("activity", java.util.List.of("1", "2", "3"));
        
        Map<String, String> params = builder.getParameters();
        assertEquals("1,2,3", params.get("activity"));
    }
    
    @Test
    @DisplayName("Should add multiple values with empty filter")
    void testMultiParameterEmpty() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .addMultiParameter("activity", java.util.List.of("1", "", null, "3"));
        
        Map<String, String> params = builder.getParameters();
        assertEquals("1,3", params.get("activity"));
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        SearchRequestBuilder builder = SearchRequestBuilder.create()
                .baseUrl("https://example.com")
                .addParameter("key", "value");
        
        String str = builder.toString();
        assertTrue(str.contains("SearchRequestBuilder"));
        assertTrue(str.contains("example.com"));
    }
}
