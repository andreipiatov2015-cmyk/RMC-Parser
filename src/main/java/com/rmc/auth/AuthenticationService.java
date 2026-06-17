package com.rmc.auth;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.http.HttpResponse;
import com.rmc.http.SessionManager;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис авторизации.
 * 
 * <p>Выполняет:</p>
 * <ul>
 *   <li>POST запрос на URL авторизации</li>
 *   <li>Получение cookies из ответа</li>
 *   <li>Сохранение cookies в SessionManager</li>
 *   <li>Проверку статуса авторизации</li>
 * </ul>
 * 
 * <p>НЕ использует Selenium, браузер или WebDriver.</p>
 */
public class AuthenticationService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClientService httpClient;
    private boolean authenticated;
    
    private AuthenticationService(Builder builder) {
        this.httpClient = builder.httpClient;
        this.authenticated = false;
    }
    
    /**
     * Создать Builder для построения AuthenticationService.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Выполнить авторизацию.
     * 
     * @param loginRequest запрос на авторизацию
     * @return результат авторизации
     */
    public LoginResult login(LoginRequest loginRequest) {
        logger.info(LOG_AUTH_START);
        
        try {
            // Формируем тело запроса
            String body = buildRequestBody(loginRequest);
            
            // Выполняем POST запрос
            HttpResponse response = httpClient.post(
                loginRequest.getLoginUrl().toString(),
                body,
                loginRequest.getContentType()
            );
            
            // Получаем cookies из ответа
            List<HttpCookie> cookies = extractCookies(response);
            
            // Сохраняем cookies в SessionManager
            saveCookies(loginRequest.getLoginUrl(), cookies);
            
            // Проверяем успешность авторизации
            boolean success = isAuthSuccess(response);
            
            if (success) {
                authenticated = true;
                logger.info(LOG_AUTH_SUCCESS);
                logger.info(LOG_SESSION_RECEIVED, cookies.size());
                
                return LoginResult.builder()
                    .success(true)
                    .cookies(cookies)
                    .statusCode(response.getStatusCode())
                    .build();
            } else {
                logger.warn(LOG_AUTH_FAILED, response.getStatusCode());
                
                return LoginResult.builder()
                    .success(false)
                    .cookies(cookies)
                    .statusCode(response.getStatusCode())
                    .responseBody(response.getBody())
                    .errorMessage("Авторизация неуспешна: статус " + response.getStatusCode())
                    .build();
            }
            
        } catch (HttpException e) {
            logger.error(LOG_AUTH_ERROR, e.getMessage());
            
            if (e.isTimeout()) {
                throw AuthenticationException.timeout("Таймаут при авторизации");
            } else if (e.isDnsError()) {
                throw AuthenticationException.networkError("Ошибка DNS: " + e.getMessage());
            } else {
                throw AuthenticationException.networkError(e.getMessage());
            }
        }
    }
    
    /**
     * Выполнить выход из системы.
     */
    public void logout() {
        if (authenticated) {
            logger.info(LOG_LOGOUT);
            authenticated = false;
            // Cookies остаются в SessionManager, но пользователь считается вышедшим
            logger.info(LOG_LOGOUT_COMPLETE);
        } else {
            logger.debug(LOG_LOGOUT_NOT_AUTHENTICATED);
        }
    }
    
    /**
     * Проверить статус авторизации.
     * 
     * @return true если авторизован
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    /**
     * Получить количество сохранённых cookies.
     * 
     * @return количество cookies
     */
    public int getCookieCount() {
        return httpClient.getSessionManager().getCookieCount();
    }
    
    /**
     * Проверить наличие cookies для URL.
     * 
     * @param url URL
     * @return true если есть cookies
     */
    public boolean hasCookies(String url) {
        return httpClient.hasCookies(url);
    }
    
    private String buildRequestBody(LoginRequest request) {
        StringBuilder body = new StringBuilder();
        body.append("username=").append(encode(request.getUsername()));
        body.append("&password=").append(encode(request.getPassword()));
        
        // Добавляем дополнительные параметры
        for (Map.Entry<String, String> entry : request.getAdditionalParams().entrySet()) {
            body.append("&").append(encode(entry.getKey()));
            body.append("=").append(encode(entry.getValue()));
        }
        
        return body.toString();
    }
    
    private String encode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
    
    private List<HttpCookie> extractCookies(HttpResponse response) {
        return response.getHeaders().entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase("Set-Cookie") 
                      || e.getKey().equalsIgnoreCase("set-cookie"))
            .flatMap(e -> {
                try {
                    URI uri = response.getRequestUri();
                    return HttpCookie.parse(e.getValue()).stream();
                } catch (Exception ex) {
                    logger.warn("Failed to parse cookie: {}", ex.getMessage());
                    return java.util.stream.Stream.empty();
                }
            })
            .collect(Collectors.toList());
    }
    
    private void saveCookies(String url, List<HttpCookie> cookies) {
        URI uri = URI.create(url);
        for (HttpCookie cookie : cookies) {
            httpClient.getSessionManager().addCookie(uri, cookie.getName(), cookie.getValue());
        }
    }
    
    private boolean isAuthSuccess(HttpResponse response) {
        int status = response.getStatusCode();
        // Успешная авторизация обычно возвращает 200, 201 или 302
        return (status >= 200 && status < 300) || status == 302 || status == 303;
    }
    
    @Override
    public String toString() {
        return String.format("AuthenticationService{authenticated=%s, cookies=%d}", 
                authenticated, getCookieCount());
    }
    
    /**
     * Builder для создания AuthenticationService.
     */
    public static class Builder {
        
        private HttpClientService httpClient;
        
        public Builder httpClient(HttpClientService httpClient) {
            this.httpClient = httpClient;
            return this;
        }
        
        public AuthenticationService build() {
            if (httpClient == null) {
                httpClient = HttpClientService.builder().build();
            }
            return new AuthenticationService(this);
        }
    }
    
    // Константы для логирования
    private static final String LOG_AUTH_START = "Авторизация...";
    private static final String LOG_AUTH_SUCCESS = "Авторизация успешна.";
    private static final String LOG_AUTH_FAILED = "Ошибка авторизации. Статус: {}";
    private static final String LOG_AUTH_ERROR = "Ошибка авторизации: {}";
    private static final String LOG_SESSION_RECEIVED = "Получена сессия. Cookies: {}";
    private static final String LOG_LOGOUT = "Выход из системы...";
    private static final String LOG_LOGOUT_COMPLETE = "Выход выполнен.";
    private static final String LOG_LOGOUT_NOT_AUTHENTICATED = "Выход: пользователь не авторизован";
}
