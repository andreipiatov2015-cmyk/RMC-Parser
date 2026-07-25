package com.rmc.search.service;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.logging.AppLogger;
import com.rmc.search.SearchRequest;
import org.slf4j.Logger;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

/**
 * Сервис поиска программ.
 * 
 * <p>Выполняет:</p>
 * <ul>
 *   <li>Получение SearchRequest</li>
 *   <li>Выполнение HTTP GET запроса</li>
 *   <li>Получение HTML страницы результатов</li>
 * </ul>
 * 
 * <p>Парсинг HTML пока не выполняется.</p>
 */
public class ProgramSearchService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClientService httpClient;
    
    private ProgramSearchService(Builder builder) {
        this.httpClient = builder.httpClient;
    }
    
    /**
     * Создать Builder для построения сервиса.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Выполнить поиск.
     * 
     * @param request запрос на поиск
     * @return результат поиска
     */
    public SearchResult search(SearchRequest request) {
        String url = request.getFullUrl();
        
        logger.info(LOG_SEARCHING);
        logger.info(LOG_REQUEST_URL, url);
        
        Instant start = Instant.now();
        
        try {
            // Выполняем GET запрос
            com.rmc.http.HttpResponse response = httpClient.get(URI.create(url));
            
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            
            logger.info(LOG_SUCCESS);
            logger.info(LOG_STATUS, response.getStatusCode());
            logger.info(LOG_SIZE, response.getBody().length());
            logger.info(LOG_TIME, durationMs);
            
            return SearchResult.builder()
                    .success(true)
                    .html(response.getBody())
                    .requestUrl(url)
                    .statusCode(response.getStatusCode())
                    .responseTimeMs(durationMs)
                    .build();
                    
        } catch (HttpException e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            
            logger.error(LOG_ERROR, e.getMessage());
            
            if (e.isTimeout()) {
                logger.error(LOG_TIMEOUT, url);
            } else if (e.isDnsError()) {
                logger.error(LOG_DNS_ERROR, e.getMessage());
            } else if (e.isHttpError()) {
                logger.error(LOG_HTTP_ERROR, e.getStatusCode().orElse(0));
            }
            
            return SearchResult.builder()
                    .success(false)
                    .requestUrl(url)
                    .responseTimeMs(durationMs)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Выполнить поиск по URL.
     * 
     * @param url URL для GET запроса
     * @return результат поиска
     */
    public SearchResult search(String url) {
        logger.info(LOG_SEARCHING);
        logger.info(LOG_REQUEST_URL, url);
        
        Instant start = Instant.now();
        
        try {
            com.rmc.http.HttpResponse response = httpClient.get(URI.create(url));
            
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            
            logger.info(LOG_SUCCESS);
            logger.info(LOG_STATUS, response.getStatusCode());
            logger.info(LOG_SIZE, response.getBody().length());
            logger.info(LOG_TIME, durationMs);
            
            return SearchResult.builder()
                    .success(true)
                    .html(response.getBody())
                    .requestUrl(url)
                    .statusCode(response.getStatusCode())
                    .responseTimeMs(durationMs)
                    .build();
                    
        } catch (HttpException e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            
            logger.error(LOG_ERROR, e.getMessage());
            
            return SearchResult.builder()
                    .success(false)
                    .requestUrl(url)
                    .responseTimeMs(durationMs)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Получить HttpClientService.
     */
    public HttpClientService getHttpClient() {
        return httpClient;
    }
    
    @Override
    public String toString() {
        return "ProgramSearchService{}";
    }
    
    /**
     * Builder для создания ProgramSearchService.
     */
    public static class Builder {
        
        private HttpClientService httpClient;
        
        public Builder httpClient(HttpClientService httpClient) {
            this.httpClient = httpClient;
            return this;
        }
        
        public ProgramSearchService build() {
            if (httpClient == null) {
                httpClient = HttpClientService.builder()
                        .requestTimeout(Duration.ofSeconds(30))
                        .build();
            }
            return new ProgramSearchService(this);
        }
    }
    
    // Константы для логирования
    private static final String LOG_SEARCHING = "Выполнение поиска программ...";
    private static final String LOG_REQUEST_URL = "URL запроса: {}";
    private static final String LOG_SUCCESS = "Поиск успешно завершён.";
    private static final String LOG_STATUS = "HTTP статус: {}";
    private static final String LOG_SIZE = "Размер HTML: {} символов";
    private static final String LOG_TIME = "Время выполнения: {} мс";
    private static final String LOG_ERROR = "Ошибка поиска: {}";
    private static final String LOG_TIMEOUT = "Превышен таймаут: {}";
    private static final String LOG_DNS_ERROR = "Ошибка DNS: {}";
    private static final String LOG_HTTP_ERROR = "HTTP ошибка: статус {}";
}
