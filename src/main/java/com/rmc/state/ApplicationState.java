package com.rmc.state;

import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Единое состояние приложения.
 * 
 * <p>Хранит:</p>
 * <ul>
 *   <li>Статус авторизации</li>
 *   <li>Имя пользователя</li>
 *   <li>HTTP-сессию</li>
 *   <li>Время входа</li>
 * </ul>
 * 
 * <p>Является единственным источником истины о состоянии приложения.</p>
 * 
 * <p>Использует паттерн Observer для уведомления об изменениях.</p>
 */
public class ApplicationState {
    
    private static final Logger logger = AppLogger.getLogger();
    
    /** Единственный экземпляр */
    private static volatile ApplicationState instance;
    
    /** Статус авторизации */
    private volatile boolean authenticated;
    
    /** Имя пользователя */
    private volatile String username;
    
    /** HTTP клиент с активной сессией */
    private volatile HttpClientService httpClient;
    
    /** Время входа */
    private volatile LocalDateTime loginTime;
    
    /** CookieManager для сессии */
    private volatile CookieManager cookieManager;
    
    /** Список слушателей изменений */
    private final List<Consumer<AuthStateChange>> listeners = new CopyOnWriteArrayList<>();
    
    private ApplicationState() {
        this.authenticated = false;
        this.username = "";
        this.httpClient = HttpClientService.builder().build();
        this.loginTime = null;
        this.cookieManager = httpClient.getSessionManager().getCookieManager();
    }
    
    /**
     * Получить экземпляр (singleton).
     */
    public static ApplicationState getInstance() {
        if (instance == null) {
            synchronized (ApplicationState.class) {
                if (instance == null) {
                    instance = new ApplicationState();
                }
            }
        }
        return instance;
    }
    
    /**
     * Проверить статус авторизации.
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Получить имя пользователя.
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Получить HTTP клиент.
     */
    public HttpClientService getHttpClient() {
        return httpClient;
    }
    
    /**
     * Получить время входа.
     */
    public Optional<LocalDateTime> getLoginTime() {
        return Optional.ofNullable(loginTime);
    }
    
    /**
     * Получить CookieManager.
     */
    public CookieManager getCookieManager() {
        return cookieManager;
    }
    
    /**
     * Получить количество cookies.
     */
    public int getCookieCount() {
        try {
            return httpClient.getSessionManager().getAllCookies().size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Получить имена всех cookies.
     */
    public List<String> getCookieNames() {
        try {
            return httpClient.getSessionManager().getAllCookies().stream()
                    .map(HttpCookie::getName)
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Выполнить вход (установить состояние авторизации).
     * 
     * @param username имя пользователя
     * @param httpClient HTTP клиент с активной сессией
     */
    public void login(String username, HttpClientService httpClient) {
        this.authenticated = true;
        this.username = username;
        this.httpClient = httpClient;
        this.loginTime = LocalDateTime.now();
        this.cookieManager = httpClient.getSessionManager().getCookieManager();
        
        logger.info("==================================================");
        logger.info("Авторизация выполнена");
        logger.info("Пользователь: {}", username);
        logger.info("Время входа: {}", loginTime);
        logger.info("Cookies: {}", getCookieNames());
        logger.info("==================================================");
        
        notifyListeners(true, username);
    }
    
    /**
     * Выполнить выход (сбросить состояние авторизации).
     */
    public void logout() {
        String oldUsername = this.username;
        this.authenticated = false;
        this.username = "";
        this.loginTime = null;
        
        // Очищаем cookies
        if (this.httpClient != null) {
            this.httpClient.clearCookies();
        }
        
        // Создаём новый HTTP клиент для будущих запросов
        this.httpClient = HttpClientService.builder().build();
        this.cookieManager = this.httpClient.getSessionManager().getCookieManager();
        
        logger.info("==================================================");
        logger.info("Выход выполнен");
        logger.info("Пользователь: {} (вышел)", oldUsername);
        logger.info("==================================================");
        
        notifyListeners(false, "");
    }
    
    /**
     * Добавить слушателя изменений состояния авторизации.
     */
    public void addAuthStateListener(Consumer<AuthStateChange> listener) {
        listeners.add(listener);
    }
    
    /**
     * Удалить слушателя изменений состояния авторизации.
     */
    public void removeAuthStateListener(Consumer<AuthStateChange> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Уведомить всех слушателей об изменении состояния.
     */
    private void notifyListeners(boolean authenticated, String username) {
        AuthStateChange event = new AuthStateChange(authenticated, username);
        for (Consumer<AuthStateChange> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.warn("Ошибка в слушателе состояния: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("ApplicationState{authenticated=%s, username='%s', cookies=%d}",
                authenticated, username, getCookieCount());
    }
    
    /**
     * Событие изменения состояния авторизации.
     */
    public static class AuthStateChange {
        private final boolean authenticated;
        private final String username;
        
        public AuthStateChange(boolean authenticated, String username) {
            this.authenticated = authenticated;
            this.username = username;
        }
        
        public boolean isAuthenticated() {
            return authenticated;
        }
        
        public String getUsername() {
            return username;
        }
    }
}
