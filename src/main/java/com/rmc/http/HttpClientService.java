package com.rmc.http;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP клиент для выполнения запросов.
 * 
 * <p>Использует Java 21 HttpClient с поддержкой:</p>
 * <ul>
 *   <li>GET и POST запросы</li>
 *   <li>Автоматическое управление cookies (через SessionManager)</li>
 *   <li>Настраиваемые таймауты</li>
 *   <li>Логирование запросов и ответов</li>
 *   <li>Обработка ошибок</li>
 * </ul>
 * 
 * <p>Все запросы автоматически сохраняют и отправляют cookies.</p>
 */
public class HttpClientService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final SessionManager sessionManager;
    private final Duration requestTimeout;
    private final boolean logRequests;
    private final boolean logResponses;
    
    private HttpClientService(Builder builder) {
        this.sessionManager = builder.sessionManager;
        this.requestTimeout = builder.requestTimeout;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
    }
    
    /**
     * Создать Builder для построения HttpClientService.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Выполнить GET запрос.
     * 
     * @param uri URI запроса
     * @return HttpResponse с ответом
     * @throws HttpException если запрос неуспешен
     */
    public HttpResponse get(String uri) {
        return get(URI.create(uri));
    }
    
    /**
     * Выполнить GET запрос.
     * 
     * @param uri URI запроса
     * @return HttpResponse с ответом
     * @throws HttpException если запрос неуспешен
     */
    public HttpResponse get(URI uri) {
        logger.info(Messages.LOG_HTTP_GET, uri);
        
        Instant start = Instant.now();
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(requestTimeout)
                    .GET()
                    .build();
            
            if (logRequests) {
                logger.debug("GET Request: {}", uri);
            }
            
            HttpResponse<String> response = sessionManager.getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            
            Duration duration = Duration.between(start, Instant.now());
            
            if (logResponses) {
                logger.debug("GET Response: {} - {} in {}ms", 
                        uri, response.statusCode(), duration.toMillis());
            }
            
            HttpResponse result = buildResponse(response, uri, "GET", duration);
            
            logger.info(Messages.LOG_HTTP_STATUS, response.statusCode());
            logger.info(Messages.LOG_HTTP_TIME, duration.toMillis());
            
            return result;
            
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error(Messages.LOG_HTTP_TIMEOUT, uri);
            throw HttpException.timeout(uri, "GET", e);
        } catch (IOException e) {
            logger.error(Messages.LOG_HTTP_CONNECTION_ERROR, uri, e.getMessage());
            throw HttpException.connectionFailed(uri, "GET", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(Messages.LOG_HTTP_INTERRUPTED, uri);
            throw new HttpException("Request interrupted", e, 
                    HttpException.ErrorType.UNKNOWN, uri, "GET", null, null);
        }
    }
    
    /**
     * Выполнить POST запрос.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @return HttpResponse с ответом
     * @throws HttpException если запрос неуспешен
     */
    public HttpResponse post(String uri, String body) {
        return post(URI.create(uri), body);
    }
    
    /**
     * Выполнить POST запрос.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @return HttpResponse с ответом
     * @throws HttpException если запрос неуспешен
     */
    public HttpResponse post(URI uri, String body) {
        return post(uri, body, "application/json");
    }
    
    /**
     * Выполнить POST запрос с указанным content-type.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @param contentType MIME тип контента
     * @return HttpResponse с ответом
     * @throws HttpException если запрос неуспешен
     */
    public HttpResponse post(URI uri, String body, String contentType) {
        logger.info(Messages.LOG_HTTP_POST, uri);
        
        Instant start = Instant.now();
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(requestTimeout)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            
            if (logRequests) {
                logger.debug("POST Request: {} - {}", uri, body.length() > 100 
                        ? body.substring(0, 100) + "..." 
                        : body);
            }
            
            HttpResponse<String> response = sessionManager.getHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            
            Duration duration = Duration.between(start, Instant.now());
            
            if (logResponses) {
                logger.debug("POST Response: {} - {} in {}ms", 
                        uri, response.statusCode(), duration.toMillis());
            }
            
            HttpResponse result = buildResponse(response, uri, "POST", duration);
            
            logger.info(Messages.LOG_HTTP_STATUS, response.statusCode());
            logger.info(Messages.LOG_HTTP_TIME, duration.toMillis());
            
            return result;
            
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error(Messages.LOG_HTTP_TIMEOUT, uri);
            throw HttpException.timeout(uri, "POST", e);
        } catch (IOException e) {
            logger.error(Messages.LOG_HTTP_CONNECTION_ERROR, uri, e.getMessage());
            throw HttpException.connectionFailed(uri, "POST", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(Messages.LOG_HTTP_INTERRUPTED, uri);
            throw new HttpException("Request interrupted", e, 
                    HttpException.ErrorType.UNKNOWN, uri, "POST", null, null);
        }
    }
    
    /**
     * Выполнить GET запрос асинхронно.
     * 
     * @param uri URI запроса
     * @return CompletableFuture с HttpResponse
     */
    public CompletableFuture<HttpResponse> getAsync(String uri) {
        return getAsync(URI.create(uri));
    }
    
    /**
     * Выполнить GET запрос асинхронно.
     * 
     * @param uri URI запроса
     * @return CompletableFuture с HttpResponse
     */
    public CompletableFuture<HttpResponse> getAsync(URI uri) {
        logger.debug("Async GET: {}", uri);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(requestTimeout)
                    .GET()
                    .build();
            
            return sessionManager.getHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        Duration duration = Duration.between(
                                Instant.now().minusMillis(100), // approximate
                                Instant.now());
                        return buildResponse(response, uri, "GET", duration);
                    })
                    .exceptionally(e -> {
                        if (e.getCause() instanceof java.net.http.HttpTimeoutException) {
                            throw new HttpException("Request timed out", e.getCause(),
                                    HttpException.ErrorType.TIMEOUT, uri, "GET", null, null);
                        }
                        throw new HttpException("Request failed", e.getCause(),
                                HttpException.ErrorType.UNKNOWN, uri, "GET", null, null);
                    });
                    
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Выполнить POST запрос асинхронно.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @return CompletableFuture с HttpResponse
     */
    public CompletableFuture<HttpResponse> postAsync(String uri, String body) {
        return postAsync(URI.create(uri), body);
    }
    
    /**
     * Выполнить POST запрос асинхронно.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @return CompletableFuture с HttpResponse
     */
    public CompletableFuture<HttpResponse> postAsync(URI uri, String body) {
        return postAsync(uri, body, "application/json");
    }
    
    /**
     * Выполнить POST запрос асинхронно.
     * 
     * @param uri URI запроса
     * @param body тело запроса
     * @param contentType MIME тип контента
     * @return CompletableFuture с HttpResponse
     */
    public CompletableFuture<HttpResponse> postAsync(URI uri, String body, String contentType) {
        logger.debug("Async POST: {}", uri);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(requestTimeout)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            
            return sessionManager.getHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        Duration duration = Duration.between(
                                Instant.now().minusMillis(100), // approximate
                                Instant.now());
                        return buildResponse(response, uri, "POST", duration);
                    })
                    .exceptionally(e -> {
                        if (e.getCause() instanceof java.net.http.HttpTimeoutException) {
                            throw new HttpException("Request timed out", e.getCause(),
                                    HttpException.ErrorType.TIMEOUT, uri, "POST", null, null);
                        }
                        throw new HttpException("Request failed", e.getCause(),
                                HttpException.ErrorType.UNKNOWN, uri, "POST", null, null);
                    });
                    
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Получить SessionManager для работы с cookies.
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    /**
     * Проверить наличие cookies для URI.
     */
    public boolean hasCookies(String uri) {
        return sessionManager.hasCookies(URI.create(uri));
    }
    
    /**
     * Очистить все cookies.
     */
    public void clearCookies() {
        sessionManager.clearCookies();
        logger.info(Messages.LOG_HTTP_COOKIES_CLEARED);
    }
    
    private HttpResponse buildResponse(HttpResponse<String> response, URI uri, 
                                       String method, Duration duration) {
        Map<String, String> headers = response.headers().map()
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(", ", e.getValue())
                ));
        
        return HttpResponse.builder()
                .statusCode(response.statusCode())
                .body(response.body())
                .headers(headers)
                .requestUri(uri)
                .requestMethod(method)
                .responseTime(duration)
                .build();
    }
    
    @Override
    public String toString() {
        return String.format("HttpClientService{uri=%s, timeout=%s}", 
                sessionManager, requestTimeout);
    }
    
    /**
     * Builder для создания HttpClientService.
     */
    public static class Builder {
        
        private SessionManager sessionManager;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private boolean logRequests = true;
        private boolean logResponses = true;
        
        public Builder sessionManager(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }
        
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }
        
        public Builder requestTimeoutSeconds(int seconds) {
            this.requestTimeout = Duration.ofSeconds(seconds);
            return this;
        }
        
        public Builder logRequests(boolean log) {
            this.logRequests = log;
            return this;
        }
        
        public Builder logResponses(boolean log) {
            this.logResponses = log;
            return this;
        }
        
        public HttpClientService build() {
            if (sessionManager == null) {
                sessionManager = SessionManager.builder().build();
            }
            return new HttpClientService(this);
        }
    }
    
    /**
     * Константы для логирования.
     */
    private static class Messages {
        static final String LOG_HTTP_GET = "HTTP GET: {}";
        static final String LOG_HTTP_POST = "HTTP POST: {}";
        static final String LOG_HTTP_STATUS = "HTTP Status: {}";
        static final String LOG_HTTP_TIME = "Время выполнения: {} мс";
        static final String LOG_HTTP_TIMEOUT = "Таймаут запроса: {}";
        static final String LOG_HTTP_CONNECTION_ERROR = "Ошибка соединения: {} - {}";
        static final String LOG_HTTP_INTERRUPTED = "Запрос прерван: {}";
        static final String LOG_HTTP_COOKIES_CLEARED = "Cookies очищены";
    }
}
