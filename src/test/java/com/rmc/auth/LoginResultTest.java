package com.rmc.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.HttpCookie;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoginResult.
 */
class LoginResultTest {
    
    @Test
    @DisplayName("Should create successful result")
    void testSuccessfulResult() {
        HttpCookie cookie = new HttpCookie("session", "abc123");
        
        LoginResult result = LoginResult.builder()
                .success(true)
                .cookies(List.of(cookie))
                .statusCode(200)
                .build();
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getCookies().size());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.hasCookies());
        assertTrue(result.getErrorMessage().isEmpty());
    }
    
    @Test
    @DisplayName("Should create failed result")
    void testFailedResult() {
        LoginResult result = LoginResult.builder()
                .success(false)
                .statusCode(401)
                .errorMessage("Invalid credentials")
                .responseBody("Wrong password")
                .build();
        
        assertFalse(result.isSuccess());
        assertEquals(401, result.getStatusCode());
        assertFalse(result.hasCookies());
        assertEquals("Invalid credentials", result.getErrorMessage().orElse(null));
        assertEquals("Wrong password", result.getResponseBody().orElse(null));
    }
    
    @Test
    @DisplayName("Should handle empty cookies list")
    void testEmptyCookies() {
        LoginResult result = LoginResult.builder()
                .success(true)
                .statusCode(200)
                .build();
        
        assertNotNull(result.getCookies());
        assertTrue(result.getCookies().isEmpty());
        assertFalse(result.hasCookies());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        HttpCookie cookie = new HttpCookie("session", "abc");
        
        LoginResult result = LoginResult.builder()
                .success(true)
                .cookies(List.of(cookie))
                .statusCode(200)
                .build();
        
        String str = result.toString();
        assertTrue(str.contains("LoginResult"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("200"));
    }
}
