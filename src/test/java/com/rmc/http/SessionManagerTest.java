package com.rmc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionManager.
 */
class SessionManagerTest {
    
    @Test
    @DisplayName("Should create SessionManager with defaults")
    void testDefaultBuilder() {
        SessionManager manager = SessionManager.builder().build();
        
        assertNotNull(manager.getHttpClient());
        assertNotNull(manager.getCookieManager());
        assertEquals(0, manager.getCookieCount());
    }
    
    @Test
    @DisplayName("Should create SessionManager with custom timeout")
    void testCustomTimeout() {
        SessionManager manager = SessionManager.builder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("Should create SessionManager with custom timeout in seconds")
    void testCustomTimeoutSeconds() {
        SessionManager manager = SessionManager.builder()
                .connectTimeoutSeconds(45)
                .build();
        
        assertNotNull(manager);
    }
    
    @Test
    @DisplayName("Should add cookie")
    void testAddCookie() {
        SessionManager manager = SessionManager.builder().build();
        URI uri = URI.create("https://example.com");
        
        manager.addCookie(uri, "session", "abc123");
        
        assertTrue(manager.hasCookies(uri));
        assertEquals(1, manager.getCookies(uri).size());
        assertEquals("session", manager.getCookies(uri).get(0).getName());
        assertEquals("abc123", manager.getCookies(uri).get(0).getValue());
    }
    
    @Test
    @DisplayName("Should add multiple cookies")
    void testAddMultipleCookies() {
        SessionManager manager = SessionManager.builder().build();
        URI uri = URI.create("https://example.com");
        
        manager.addCookie(uri, "cookie1", "value1");
        manager.addCookie(uri, "cookie2", "value2");
        
        assertEquals(2, manager.getCookies(uri).size());
    }
    
    @Test
    @DisplayName("Should clear all cookies")
    void testClearCookies() {
        SessionManager manager = SessionManager.builder().build();
        URI uri = URI.create("https://example.com");
        
        manager.addCookie(uri, "session", "abc123");
        assertEquals(1, manager.getCookieCount());
        
        manager.clearCookies();
        
        assertEquals(0, manager.getCookieCount());
        assertFalse(manager.hasCookies(uri));
    }
    
    @Test
    @DisplayName("Should remove cookies for specific URI")
    void testRemoveCookiesForUri() {
        SessionManager manager = SessionManager.builder().build();
        URI uri1 = URI.create("https://example.com");
        URI uri2 = URI.create("https://other.com");
        
        manager.addCookie(uri1, "session", "abc123");
        manager.addCookie(uri2, "session", "xyz789");
        assertEquals(2, manager.getCookieCount());
        
        manager.removeCookies(uri1);
        
        assertEquals(1, manager.getCookieCount());
        assertFalse(manager.hasCookies(uri1));
        assertTrue(manager.hasCookies(uri2));
    }
    
    @Test
    @DisplayName("Should return empty list for URI without cookies")
    void testGetCookiesForUnknownUri() {
        SessionManager manager = SessionManager.builder().build();
        URI uri = URI.create("https://unknown.com");
        
        assertFalse(manager.hasCookies(uri));
        assertTrue(manager.getCookies(uri).isEmpty());
    }
    
    @Test
    @DisplayName("Should respect follow redirects setting")
    void testFollowRedirects() {
        SessionManager withRedirects = SessionManager.builder()
                .followRedirects(true)
                .build();
        
        SessionManager withoutRedirects = SessionManager.builder()
                .followRedirects(false)
                .build();
        
        assertNotNull(withRedirects.getHttpClient());
        assertNotNull(withoutRedirects.getHttpClient());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        SessionManager manager = SessionManager.builder().build();
        String str = manager.toString();
        
        assertTrue(str.contains("SessionManager"));
        assertTrue(str.contains("cookieCount=0"));
    }
    
    @Test
    @DisplayName("Should handle exceptions gracefully in getCookies")
    void testGetCookiesExceptionHandling() {
        SessionManager manager = SessionManager.builder().build();
        
        // Calling with null should not throw
        assertTrue(manager.getCookies(null).isEmpty());
    }
    
    @Test
    @DisplayName("Should handle exceptions gracefully in addCookie")
    void testAddCookieExceptionHandling() {
        SessionManager manager = SessionManager.builder().build();
        
        // Adding cookie to null URI should not throw
        assertDoesNotThrow(() -> manager.addCookie(null, "test", "value"));
    }
}
