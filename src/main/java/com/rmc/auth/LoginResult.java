package com.rmc.auth;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Результат авторизации.
 * 
 * <p>Содержит:</p>
 * <ul>
 *   <li>Успешность авторизации</li>
 *   <li>Полученные cookies</li>
 *   <li>Сообщение об ошибке (если есть)</li>
 *   <li>Статус код HTTP</li>
 * </ul>
 */
public class LoginResult {
    
    private final boolean success;
    private final List<HttpCookie> cookies;
    private final String errorMessage;
    private final int statusCode;
    private final String responseBody;
    
    private LoginResult(Builder builder) {
        this.success = builder.success;
        this.cookies = List.copyOf(builder.cookies);
        this.errorMessage = builder.errorMessage;
        this.statusCode = builder.statusCode;
        this.responseBody = builder.responseBody;
    }
    
    /**
     * Создать Builder для построения результата.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return true если авторизация успешна
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return Список полученных cookies
     */
    public List<HttpCookie> getCookies() {
        return cookies;
    }
    
    /**
     * @return true если есть cookies
     */
    public boolean hasCookies() {
        return !cookies.isEmpty();
    }
    
    /**
     * @return Сообщение об ошибке или null
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    /**
     * @return HTTP статус код
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * @return Тело ответа
     */
    public Optional<String> getResponseBody() {
        return Optional.ofNullable(responseBody);
    }
    
    @Override
    public String toString() {
        return String.format("LoginResult{success=%s, cookies=%d, status=%d}", 
                success, cookies.size(), statusCode);
    }
    
    /**
     * Builder для создания LoginResult.
     */
    public static class Builder {
        
        private boolean success;
        private List<HttpCookie> cookies = Collections.emptyList();
        private String errorMessage;
        private int statusCode;
        private String responseBody;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder cookies(List<HttpCookie> cookies) {
            this.cookies = cookies;
            return this;
        }
        
        public Builder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        public Builder statusCode(int code) {
            this.statusCode = code;
            return this;
        }
        
        public Builder responseBody(String body) {
            this.responseBody = body;
            return this;
        }
        
        public LoginResult build() {
            return new LoginResult(this);
        }
    }
}
