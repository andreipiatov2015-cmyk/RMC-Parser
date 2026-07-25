package com.rmc.auth.django;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.http.HttpResponse;
import com.rmc.logging.AppLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.HttpCookie;
import java.util.stream.Collectors;

/**
 * Провайдер авторизации Django.
 * 
 * <p>Реализует полную процедуру авторизации Django:
 * <ol>
 *   <li>GET запрос на страницу авторизации</li>
 *   <li>Сохранение cookies (включая csrftoken)</li>
 *   <li>Парсинг HTML для извлечения csrfmiddlewaretoken</li>
 *   <li>POST запрос с данными формы и csrfmiddlewaretoken</li>
 *   <li>Проверка успешности по наличию sessionid</li>
 * </ol>
 * 
 * <p>Не использует Selenium, браузер или WebDriver.</p>
 */
public class DjangoAuthenticationProvider {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClientService httpClient;
    private final String baseUrl;
    private final String loginPath;
    
    // Имена заголовков для имитации браузера
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7";
    
    public DjangoAuthenticationProvider(HttpClientService httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.loginPath = "/login/";
    }
    
    /**
     * Выполнить авторизацию.
     * 
     * @param username имя пользователя
     * @param password пароль
     * @return результат авторизации
     */
    public DjangoAuthResult authenticate(String username, String password) {
        String loginUrl = baseUrl + loginPath;
        
        logger.info(LOG_START);
        logger.info(LOG_GETTING_LOGIN_PAGE, loginUrl);
        
        try {
            // Шаг 1: GET запрос на страницу авторизации
            HttpResponse getResponse = httpClient.get(loginUrl);
            
            logger.info(LOG_HTTP_STATUS, getResponse.getStatusCode());
            
            if (getResponse.getStatusCode() != 200) {
                logger.error(LOG_UNEXPECTED_STATUS, getResponse.getStatusCode());
                return DjangoAuthResult.failure("Неожиданный статус: " + getResponse.getStatusCode());
            }
            
            // Шаг 2: Сохраняем cookies автоматически (HttpClientService делает это)
            Set<String> cookiesBefore = getCookieNames();
            logger.info(LOG_COOKIES_BEFORE, cookiesBefore.size());
            
            // Шаг 3: Парсим HTML для получения csrfmiddlewaretoken
            String html = getResponse.getBody();
            String csrfToken = extractCsrfToken(html);
            
            if (csrfToken == null || csrfToken.isEmpty()) {
                logger.error(LOG_CSRF_TOKEN_MISSING);
                return DjangoAuthResult.failure("CSRF token не найден на странице авторизации");
            }
            
            logger.info(LOG_CSRF_TOKEN_FOUND);
            
            // Шаг 4: Извлекаем все скрытые поля формы
            Map<String, String> formFields = extractFormFields(html);
            formFields.put("username", username);
            formFields.put("password", password);
            formFields.put("csrfmiddlewaretoken", csrfToken);
            
            logger.info(LOG_FORM_FIELDS, formFields.size());
            
            // Шаг 5: Формируем тело POST запроса
            String postBody = buildFormBody(formFields);
            
            // Шаг 6: Выполняем POST с правильными заголовками
            HttpResponse postResponse = performAuthenticatedPost(loginUrl, postBody, csrfToken);
            
            // Шаг 7: Проверяем успешность.
            //
            // ВАЖНО: проверка по наличию "sessionid" среди cookies НЕ РАБОТАЕТ
            // как признак успеха — Django создаёт анонимную сессию уже на GET
            // страницы логина (см. cookiesBefore), а при неудачном входе
            // просто оставляет ту же куку с тем же именем "sessionid"
            // (меняется только значение, которое здесь не сравнивается).
            // Поэтому единственный надёжный признак — наличие блока ошибок
            // (.errorlist) в теле ответа: при неверном логине/пароле Django
            // повторно отрисовывает ту же форму авторизации с этим блоком.
            Set<String> cookiesAfter = getCookieNames();
            Set<String> newCookies = new HashSet<>(cookiesAfter);
            newCookies.removeAll(cookiesBefore);
            
            logger.info(LOG_COOKIES_AFTER, cookiesAfter.size());
            logger.info(LOG_NEW_COOKIES, newCookies);
            
            String responseBody = postResponse.getBody();
            String loginError = extractLoginErrorMessage(responseBody);
            
            if (loginError != null) {
                logger.warn(LOG_AUTH_FAILED);
                logger.warn(LOG_LOGIN_ERROR_FOUND, loginError);
                return DjangoAuthResult.failure(loginError, responseBody);
            }
            
            logger.info(LOG_AUTH_SUCCESS);
            // Возвращаем HttpClientService для сохранения в ApplicationState
            return DjangoAuthResult.success(postResponse.getStatusCode(),
                    new ArrayList<>(newCookies), httpClient);
            
        } catch (HttpException e) {
            logger.error(LOG_HTTP_ERROR, e.getMessage());
            return DjangoAuthResult.failure("HTTP ошибка: " + e.getMessage());
        } catch (Exception e) {
            logger.error(LOG_GENERAL_ERROR, e.getMessage());
            return DjangoAuthResult.failure("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Проверить ответ на страницу логина на наличие блока ошибок Django
     * (например, "Пожалуйста, введите верные имя пользователя и пароль...").
     *
     * @return текст ошибки, если форма авторизации вернулась с ошибками;
     * {@code null}, если ошибок не найдено (вход считается успешным)
     */
    private String extractLoginErrorMessage(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        try {
            Document doc = Jsoup.parse(html);
            
            // Основной признак — список ошибок Django-формы.
            Element errorList = doc.selectFirst(".errorlist");
            if (errorList != null) {
                String text = errorList.text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
            
            // Фолбэк — общий блок ошибок формы, даже если вёрстка errorlist изменится.
            Element errorMessage = doc.selectFirst(".ui.form.error .message.error, .ui.message.error");
            if (errorMessage != null) {
                String text = errorMessage.text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn(LOG_PARSE_ERROR, e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлечь CSRF токен из HTML страницы.
     */
    private String extractCsrfToken(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        try {
            Document doc = Jsoup.parse(html);
            
            // Ищем csrfmiddlewaretoken в input полях
            Element tokenInput = doc.selectFirst("input[name=csrfmiddlewaretoken]");
            if (tokenInput != null) {
                String token = tokenInput.attr("value");
                logger.debug(LOG_CSRF_EXTRACTED, token.length());
                return token;
            }
            
            // Также ищем в meta тегах
            Element csrfMeta = doc.selectFirst("meta[name=csrf-token]");
            if (csrfMeta != null) {
                return csrfMeta.attr("content");
            }
            
            // Ищем в data-* атрибутах
            Element csrfData = doc.selectFirst("[data-csrf-token]");
            if (csrfData != null) {
                return csrfData.attr("data-csrf-token");
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn(LOG_PARSE_ERROR, e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлечь все скрытые поля формы авторизации.
     */
    private Map<String, String> extractFormFields(String html) {
        Map<String, String> fields = new HashMap<>();
        
        try {
            Document doc = Jsoup.parse(html);
            
            // Находим форму авторизации
            Element loginForm = doc.selectFirst("form[action*=login], form[action$=login/]");
            
            if (loginForm == null) {
                // Пробуем найти любую форму с csrfmiddlewaretoken
                loginForm = doc.selectFirst("form:has(input[name=csrfmiddlewaretoken])");
            }
            
            if (loginForm != null) {
                // Извлекаем все скрытые поля
                Elements hiddenInputs = loginForm.select("input[type=hidden]");
                for (Element input : hiddenInputs) {
                    String name = input.attr("name");
                    String value = input.attr("value");
                    if (name != null && !name.isEmpty()) {
                        fields.put(name, value);
                    }
                }
                
                logger.debug(LOG_HIDDEN_FIELDS, fields.size());
            } else {
                logger.warn(LOG_FORM_NOT_FOUND);
            }
            
        } catch (Exception e) {
            logger.warn(LOG_PARSE_ERROR, e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Построить тело POST запроса из полей формы.
     */
    private String buildFormBody(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Выполнить POST запрос с авторизационными данными.
     */
    private HttpResponse performAuthenticatedPost(String url, String body, String csrfToken) {
        URI uri = URI.create(url);
        
        // Заголовки для имитации браузера
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Referer", url);
        headers.put("Origin", baseUrl);
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", ACCEPT);
        headers.put("Accept-Language", ACCEPT_LANGUAGE);
        headers.put("X-CSRFToken", csrfToken);
        
        logger.info(LOG_POST_REQUEST, url);
        
        return httpClient.postWithHeaders(uri, body, headers);
    }
    
    /**
     * Получить имена текущих cookies.
     */
    private Set<String> getCookieNames() {
        try {
            return httpClient.getSessionManager().getAllCookies().stream()
                    .map(HttpCookie::getName)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    // Константы для логирования
    private static final String LOG_START = "==================================================\nDjango авторизация\n==================================================";
    private static final String LOG_GETTING_LOGIN_PAGE = "Получение страницы авторизации: {}";
    private static final String LOG_HTTP_STATUS = "HTTP статус: {}";
    private static final String LOG_UNEXPECTED_STATUS = "Неожиданный статус страницы: {}";
    private static final String LOG_COOKIES_BEFORE = "Cookies до авторизации: {} шт.";
    private static final String LOG_COOKIES_AFTER = "Cookies после авторизации: {} шт.";
    private static final String LOG_NEW_COOKIES = "Новые cookies: {}";
    private static final String LOG_CSRF_TOKEN_FOUND = "CSRF token получен";
    private static final String LOG_CSRF_TOKEN_MISSING = "CSRF token отсутствует. Авторизация невозможна.";
    private static final String LOG_CSRF_EXTRACTED = "CSRF token извлечён, длина: {}";
    private static final String LOG_FORM_FIELDS = "Полей формы: {}";
    private static final String LOG_HIDDEN_FIELDS = "Скрытых полей: {}";
    private static final String LOG_FORM_NOT_FOUND = "Форма авторизации не найдена";
    private static final String LOG_POST_REQUEST = "Отправка POST запроса: {}";
    private static final String LOG_COOKIE_RECEIVED = "Получена cookie: {}";
    private static final String LOG_AUTH_SUCCESS = "Авторизация выполнена успешно. Получена sessionid.";
    private static final String LOG_AUTH_FAILED = "Авторизация неуспешна.";
    private static final String LOG_LOGIN_ERROR_FOUND = "Сообщение об ошибке от сервера: {}";
    private static final String LOG_NO_SESSION = "sessionid не получена.";
    private static final String LOG_HTTP_ERROR = "HTTP ошибка: {}";
    private static final String LOG_GENERAL_ERROR = "Ошибка: {}";
    private static final String LOG_PARSE_ERROR = "Ошибка парсинга: {}";
    private static final String LOG_TIMEOUT = "Таймаут запроса";
    private static final String LOG_CONNECTION_ERROR = "Ошибка соединения: {}";
    private static final String LOG_INTERRUPTED = "Запрос прерван";
    
    /**
     * Результат авторизации Django.
     */
    public static class DjangoAuthResult {
        private final boolean success;
        private final int statusCode;
        private final List<String> receivedCookies;
        private final String errorMessage;
        private final String responseBody;
        private final HttpClientService httpClient;
        
        private DjangoAuthResult(boolean success, int statusCode, List<String> receivedCookies,
                               String errorMessage, String responseBody, HttpClientService httpClient) {
            this.success = success;
            this.statusCode = statusCode;
            this.receivedCookies = receivedCookies;
            this.errorMessage = errorMessage;
            this.responseBody = responseBody;
            this.httpClient = httpClient;
        }
        
        public static DjangoAuthResult success(int statusCode, List<String> cookies, HttpClientService httpClient) {
            return new DjangoAuthResult(true, statusCode, cookies, null, null, httpClient);
        }
        
        public static DjangoAuthResult failure(String errorMessage) {
            return new DjangoAuthResult(false, 0, Collections.emptyList(), errorMessage, null, null);
        }
        
        public static DjangoAuthResult failure(String errorMessage, String responseBody) {
            return new DjangoAuthResult(false, 0, Collections.emptyList(), errorMessage, responseBody, null);
        }
        
        public boolean isSuccess() { return success; }
        public int getStatusCode() { return statusCode; }
        public List<String> getReceivedCookies() { return receivedCookies; }
        public Optional<String> getErrorMessage() { return Optional.ofNullable(errorMessage); }
        public Optional<String> getResponseBody() { return Optional.ofNullable(responseBody); }
        public Optional<HttpClientService> getHttpClient() { return Optional.ofNullable(httpClient); }
    }
}
