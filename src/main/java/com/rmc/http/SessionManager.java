package com.rmc.http;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Менеджер HTTP сессий.
 * 
 * <p>Управляет:</p>
 * <ul>
 *   <li>HTTP клиентом</li>
 *   <li>CookieManager для автоматического управления cookies</li>
 *   <li>Таймаутами</li>
 * </ul>
 * 
 * <p>Все запросы автоматически сохраняют и отправляют cookies.</p>
 */
public class SessionManager {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClient httpClient;
    private final CookieManager cookieManager;
    
    private SessionManager(Builder builder) {
        this.cookieManager = new CookieManager();
        this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .cookieHandler(this.cookieManager)
                .followRedirects(builder.followRedirects 
                        ? HttpClient.Redirect.NORMAL 
                        : HttpClient.Redirect.NEVER)
                .build();
        
        logger.debug("SessionManager created: connectTimeout={}, followRedirects={}", 
                builder.connectTimeout, builder.followRedirects);
    }
    
    /**
     * Создать Builder для построения SessionManager.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Получить HTTP клиент.
     */
    HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * Получить CookieManager.
     */
    CookieManager getCookieManager() {
        return cookieManager;
    }
    
    /**
     * Добавить cookie для URI.
     * 
     * @param uri URI
     * @param name имя cookie
     * @param value значение cookie
     */
    public void addCookie(URI uri, String name, String value) {
        HttpCookie cookie = new HttpCookie(name, value);
        try {
            cookieManager.getCookieStore().add(uri, cookie);
            logger.debug("Added cookie {}={} for {}", name, value, uri);
        } catch (Exception e) {
            logger.warn("Failed to add cookie: {}", e.getMessage());
        }
    }
    
    /**
     * Получить все cookies для URI.
     * 
     * @param uri URI
     * @return список cookies
     */
    public List<HttpCookie> getCookies(URI uri) {
        try {
            return cookieManager.getCookieStore().get(uri);
        } catch (Exception e) {
            logger.warn("Failed to get cookies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Проверить наличие cookies для URI.
     * 
     * @param uri URI
     * @return true если есть cookies
     */
    public boolean hasCookies(URI uri) {
        return !getCookies(uri).isEmpty();
    }
    
    /**
     * Очистить все cookies.
     */
    public void clearCookies() {
        try {
            cookieManager.getCookieStore().removeAll();
            logger.debug("All cookies cleared");
        } catch (Exception e) {
            logger.warn("Failed to clear cookies: {}", e.getMessage());
        }
    }
    
    /**
     * Удалить cookies для конкретного URI.
     * 
     * @param uri URI
     */
    public void removeCookies(URI uri) {
        try {
            cookieManager.getCookieStore().remove(uri, null);
            logger.debug("Cookies removed for {}", uri);
        } catch (Exception e) {
            logger.warn("Failed to remove cookies: {}", e.getMessage());
        }
    }
    
    /**
     * Получить количество cookies в хранилище.
     * 
     * @return количество cookies
     */
    public int getCookieCount() {
        try {
            return cookieManager.getCookieStore().getCookies().size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return String.format("SessionManager{cookieCount=%d}", getCookieCount());
    }
    
    /**
     * Builder для создания SessionManager.
     */
    public static class Builder {
        
        private Duration connectTimeout = Duration.ofSeconds(30);
        private boolean followRedirects = true;
        
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }
        
        public Builder connectTimeoutSeconds(int seconds) {
            this.connectTimeout = Duration.ofSeconds(seconds);
            return this;
        }
        
        public Builder followRedirects(boolean follow) {
            this.followRedirects = follow;
            return this;
        }
        
        public SessionManager build() {
            return new SessionManager(this);
        }
    }
}
