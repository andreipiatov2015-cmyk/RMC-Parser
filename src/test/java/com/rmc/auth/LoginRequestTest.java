package com.rmc.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoginRequest.
 */
class LoginRequestTest {
    
    @Test
    @DisplayName("Should create LoginRequest with builder")
    void testBuilder() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .build();
        
        assertEquals("https://example.com/login", request.getLoginUrl());
        assertEquals("user123", request.getUsername());
        assertEquals("secret", request.getPassword());
    }
    
    @Test
    @DisplayName("Should create LoginRequest with additional params")
    void testWithAdditionalParams() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .additionalParams(Map.of("remember", "true", "token", "abc"))
                .build();
        
        assertEquals(2, request.getAdditionalParams().size());
        assertTrue(request.getParam("remember").isPresent());
        assertEquals("true", request.getParam("remember").get());
    }
    
    @Test
    @DisplayName("Should use default content type")
    void testDefaultContentType() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .build();
        
        assertEquals("application/x-www-form-urlencoded", request.getContentType());
    }
    
    @Test
    @DisplayName("Should allow custom content type")
    void testCustomContentType() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .contentType("application/json")
                .build();
        
        assertEquals("application/json", request.getContentType());
    }
    
    @Test
    @DisplayName("Should throw exception for null URL")
    void testNullUrl() {
        assertThrows(IllegalArgumentException.class, () ->
            LoginRequest.builder()
                .username("user123")
                .password("secret")
                .build()
        );
    }
    
    @Test
    @DisplayName("Should throw exception for null username")
    void testNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
            LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .password("secret")
                .build()
        );
    }
    
    @Test
    @DisplayName("Should throw exception for null password")
    void testNullPassword() {
        assertThrows(IllegalArgumentException.class, () ->
            LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .build()
        );
    }
    
    @Test
    @DisplayName("Should return empty for missing param")
    void testMissingParam() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .build();
        
        assertTrue(request.getParam("unknown").isEmpty());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("user123")
                .password("secret")
                .build();
        
        String str = request.toString();
        assertTrue(str.contains("LoginRequest"));
        assertTrue(str.contains("example.com"));
        assertTrue(str.contains("user123"));
        // Password should NOT be in toString
        assertFalse(str.contains("secret"));
    }
}
