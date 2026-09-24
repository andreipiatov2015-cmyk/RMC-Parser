package com.rmc.ui.statusbar;

import com.rmc.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Пробует получить "живую" реплику пасхалки у локальной нейросети,
 * запущенной прямо на сервере в локальной сети через Ollama
 * (https://ollama.com — бесплатный, open-source, без API-ключей и
 * платных подписок; модель считается прямо на сервере организации).
 *
 * <p>Никакого посредника (n8n, облачного API) не требуется — запрос идёт
 * напрямую на Ollama по сети. Ключей тут в принципе нет, поэтому и прятать
 * в программе нечего.</p>
 *
 * <p>Если сервер недоступен (пользователь не в той же сети, Ollama не
 * запущена, не ответила за отведённое время) — это ОЖИДАЕМАЯ ситуация:
 * тихо возвращаем {@link Optional#empty()}, вызывающий код в этом случае
 * должен показать локальную заготовленную фразу.</p>
 */
public final class AiEasterEggService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    // Адрес и модель локального сервера Ollama в сети организации.
    // ВАЖНО: по умолчанию Ollama слушает только localhost — на самом
    // сервере нужно запускать её с переменной окружения OLLAMA_HOST=0.0.0.0,
    // иначе с других компьютеров в сети до неё не достучаться.
    private static final String LOCAL_URL = "http://192.168.31.18:11434/api/chat";
    
    // ВРЕМЕННО — ТОЛЬКО ДЛЯ ТЕСТИРОВАНИЯ ВНЕ ЛОКАЛЬНОЙ СЕТИ.
    // Публичный адрес сервера через проброшенный на роутере порт — доступ
    // к нему ограничен на самом роутере правилом NAT (Src. Address), так
    // что достучаться может только заранее разрешённый IP (46.181.102.187),
    // а не кто угодно из интернета. Убрать эту строку и PUBLIC_TEST_URL
    // из списка ниже после завершения тестирования — постоянная работа
    // фичи рассчитана только на локальную сеть.
    private static final String PUBLIC_TEST_URL = "http://95.181.53.134:11434/api/chat";
    
    private static final String[] CANDIDATE_URLS = {
            LOCAL_URL,
            PUBLIC_TEST_URL, // TODO: убрать после завершения тестирования вне сети
    };
    
    private static final String MODEL = "qwen2.5:1.5b-instruct";
    
    // Быстрая проверка "сервер вообще рядом?" — если сети нет, не должно
    // ощущаться как зависание программы.
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(700);
    // Сама генерация ответа моделью занимает больше времени, чем просто
    // установка соединения — даём на неё запас.
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    
    private AiEasterEggService() {
        // Утилитарный класс
    }
    
    /**
     * @param escalationPercent насколько "раздражена" кнопка сейчас,
     *                          0-100 (см. вызывающий код в StatusBar —
     *                          считается из того, сколько заготовленных
     *                          реплик уже показано)
     */
    public static Optional<String> tryGetMessage(int escalationPercent) {
        for (String url : CANDIDATE_URLS) {
            Optional<String> result = tryUrl(url, escalationPercent);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
    
    private static Optional<String> tryUrl(String url, int escalationPercent) {
        try {
            String tier = tierFor(escalationPercent);
            
            JSONObject systemMessage = new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "Ты - кнопка в компьютерной программе, к которой пользователь пристаёт, "
                                    + "хотя его просили этого не делать. Твой текущий уровень раздражения: "
                                    + escalationPercent + " из 100. Категория поведения: " + tier
                                    + " (neutral/aggressive/threatening/furious - по нарастающей). "
                                    + "Ответь ОДНОЙ короткой фразой (не больше 15 слов) в этом настроении, по-русски. "
                                    + "Это шутка для пасхалки в приложении, тон - как у уставшего, саркастичного "
                                    + "персонажа, не всерьёз. Не повторяй известные клише дословно. "
                                    + "Ответь только самой фразой, без кавычек и пояснений.\n\n"
                                    + "Вот примеры РЕАЛЬНЫХ фраз из нашего сценария, по каждой категории — "
                                    + "ориентируйся именно на их тон и степень злости, не смягчай:\n"
                                    + "neutral: \"Вы снова нажали. Видимо, первое нажатие вас не удовлетворило.\" / "
                                    + "\"Тогда объясню ещё раз: не надо нажимать эту кнопку.\"\n"
                                    + "aggressive: \"Я начинаю терять терпение.\" / "
                                    + "\"Вы вообще умеете останавливаться самостоятельно?\"\n"
                                    + "threatening: \"ДА СКОЛЬКО МОЖНО?!\" / "
                                    + "\"Я предупреждала. Вы проигнорировали предупреждение.\"\n"
                                    + "furious: \"Ещё одно нажатие - и я удалю все файлы с вашего рабочего стола.\" / "
                                    + "\"RIP КНОПКА. Причина смерти: пользователь.\"");
            
            JSONObject userMessage = new JSONObject()
                    .put("role", "user")
                    .put("content", "Пользователь снова нажал на кнопку.");
            
            JSONObject requestBody = new JSONObject()
                    .put("model", MODEL)
                    .put("messages", new JSONArray().put(systemMessage).put(userMessage))
                    .put("stream", false);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            
            JSONObject json = new JSONObject(response.body());
            JSONObject messageObj = json.optJSONObject("message");
            String content = messageObj != null ? messageObj.optString("content", null) : null;
            
            return Optional.ofNullable(content).map(String::trim).filter(s -> !s.isEmpty());
            
        } catch (Exception e) {
            // Не в сети / Ollama не запущена / таймаут — обычная ситуация,
            // не ошибка. Не засоряем лог на уровне ERROR, только debug.
            logger.debug("AI-сервер пасхалки недоступен по адресу {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
    
    private static String tierFor(int percent) {
        if (percent < 20) {
            return "neutral";
        } else if (percent < 50) {
            return "aggressive";
        } else if (percent < 80) {
            return "threatening";
        } else {
            return "furious";
        }
    }
}
