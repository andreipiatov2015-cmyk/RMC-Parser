package com.rmc.auth;

import java.util.Map;
import java.util.Optional;

/**
 * Запрос на авторизацию.
 * 
 * <p>Содержит данные для входа:</p>
 * <ul>
 *   <li>URL авторизации</li>
 *   <li>Логин</li>
 *   <li>Пароль</li>
 *   <li>Дополнительные параметры</li>
 * </ul>
 */
public class LoginRequest {
    
    private final String loginUrl;
    private final String username;
    private final String password;
    private final Map<String, String> additionalParams;
    private final String contentType;
    
    private LoginRequest(Builder builder) {
        this.loginUrl = builder.loginUrl;
        this.username = builder.username;
        this.password = builder.password;
        this.additionalParams = Map.copyOf(builder.additionalParams);
        this.contentType = builder.contentType;
    }
    
    /**
     * Создать Builder для построения запроса.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return URL авторизации
     */
    public String getLoginUrl() {
        return loginUrl;
    }
    
    /**
     * @return Имя пользователя
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * @return Пароль
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * @return Дополнительные параметры
     */
    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }
    
    /**
     * @return MIME тип контента
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * @param key ключ параметра
     * @return значение параметра или empty
     */
    public Optional<String> getParam(String key) {
        return Optional.ofNullable(additionalParams.get(key));
    }
    
    @Override
    public String toString() {
        return String.format("LoginRequest{url=%s, username=%s}", loginUrl, username);
    }
    
    /**
     * Builder для создания LoginRequest.
     */
    public static class Builder {
        
        private String loginUrl;
        private String username;
        private String password;
        private Map<String, String> additionalParams = Map.of();
        private String contentType = "application/x-www-form-urlencoded";
        
        public Builder loginUrl(String url) {
            this.loginUrl = url;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        
        public Builder additionalParams(Map<String, String> params) {
            this.additionalParams = params;
            return this;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public LoginRequest build() {
            validate();
            return new LoginRequest(this);
        }
        
        private void validate() {
            if (loginUrl == null || loginUrl.isEmpty()) {
                throw new IllegalArgumentException("Login URL is required");
            }
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
        }
    }
}
