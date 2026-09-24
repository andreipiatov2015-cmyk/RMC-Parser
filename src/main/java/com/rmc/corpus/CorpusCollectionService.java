package com.rmc.corpus;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.http.HttpResponse;
import com.rmc.logging.AppLogger;
import com.rmc.parser.model.Program;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Собирает текстовый корпус сайта для последующей индексации (RAG).
 *
 * <p>Работает в два независимых прохода:</p>
 * <ul>
 *   <li>{@link #collectFromCards} — быстрый: использует уже разобранные
 *       поля карточек программ (без новых HTTP-запросов);</li>
 *   <li>{@link #collectDetailPages} — медленный фоновый: заходит на
 *       страницу каждой программы и сохраняет её текст целиком.</li>
 * </ul>
 *
 * <p>Оба прохода идемпотентны: если файл для программы уже существует,
 * повторный вызов его не перезапишет — это позволяет останавливать и
 * возобновлять сбор без потери прогресса.</p>
 */
public class CorpusCollectionService {

    private static final Logger logger = AppLogger.getLogger();

    /** Пауза между запросами детальных страниц, чтобы не нагружать сайт. */
    private static final long DETAIL_REQUEST_DELAY_MS = 400;

    /**
     * Обратный вызов для отображения прогресса в UI.
     */
    public interface ProgressListener {
        void onProgress(String message, int done, int total);
    }

    private final HttpClientService httpClient;
    private final String baseUrl;
    private final Path cardsDir;
    private final Path detailsDir;

    private CorpusCollectionService(Builder builder) {
        this.httpClient = builder.httpClient;
        this.baseUrl = builder.baseUrl;
        this.cardsDir = builder.outputRoot.resolve("cards");
        this.detailsDir = builder.outputRoot.resolve("details");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Фаза A — быстрая. Сохраняет по одному текстовому файлу на каждую
     * уже найденную программу, используя поля, которые парсер извлёк
     * из карточки списка (без дополнительных запросов к сайту).
     *
     * @param programs список программ, полученный из {@code ProgramParser}
     *                 (например, из {@code ProgramAnalysisService})
     * @return количество успешно сохранённых файлов
     */
    public int collectFromCards(List<Program> programs) {
        ensureDir(cardsDir);
        int saved = 0;

        for (Program program : programs) {
            Path file = cardsDir.resolve(safeFileName(program.getId()) + ".txt");
            if (Files.exists(file)) {
                continue; // уже собрано раньше
            }

            String text = renderCardText(program);
            try {
                Files.writeString(file, text, StandardCharsets.UTF_8);
                saved++;
            } catch (IOException e) {
                logger.warn("Не удалось сохранить карточку программы {}: {}", program.getId(), e.getMessage());
            }
        }

        logger.info("Фаза A завершена: сохранено {} из {} карточек", saved, programs.size());
        return saved;
    }

    /**
     * Фаза B — фоновая. Заходит на страницу каждой программы и сохраняет
     * весь читаемый текст страницы (описание, расписание, группы и т.д.),
     * которого нет в кратком виде на карточке списка.
     *
     * <p>Рассчитана на запуск в отдельном потоке — может выполняться
     * долго. Проверяет {@code isCancelled} перед каждым запросом.</p>
     *
     * @param programs    список программ (используются только те, у
     *                    которых есть {@code url})
     * @param listener    необязательный слушатель прогресса
     * @param isCancelled проверяется перед обработкой каждой страницы
     * @return количество успешно сохранённых файлов
     */
    public int collectDetailPages(List<Program> programs, ProgressListener listener,
                                   BooleanSupplier isCancelled) {
        ensureDir(detailsDir);
        int saved = 0;
        int total = programs.size();
        int index = 0;

        for (Program program : programs) {
            index++;

            if (isCancelled != null && isCancelled.getAsBoolean()) {
                logger.info("Фаза B остановлена пользователем на {} из {}", index, total);
                break;
            }

            Path file = detailsDir.resolve(safeFileName(program.getId()) + ".txt");
            if (Files.exists(file)) {
                report(listener, "Пропуск (уже есть): " + program.getTitle(), index, total);
                continue;
            }

            String relativeOrAbsoluteUrl = program.getUrl().orElse(null);
            if (relativeOrAbsoluteUrl == null) {
                continue;
            }

            String fullUrl = resolveUrl(relativeOrAbsoluteUrl);
            report(listener, "Загрузка: " + program.getTitle(), index, total);

            try {
                HttpResponse response = httpClient.get(URI.create(fullUrl));
                String pageText = extractReadableText(response.getBody());
                String content = "URL: " + fullUrl + System.lineSeparator()
                        + "Название: " + program.getTitle() + System.lineSeparator()
                        + "---" + System.lineSeparator()
                        + pageText;

                Files.writeString(file, content, StandardCharsets.UTF_8);
                saved++;
            } catch (HttpException e) {
                logger.warn("Не удалось загрузить детальную страницу {}: {}", fullUrl, e.getMessage());
            } catch (IOException e) {
                logger.warn("Не удалось сохранить детальную страницу {}: {}", program.getId(), e.getMessage());
            }

            sleepPolitely();
        }

        logger.info("Фаза B завершена: сохранено {} из {}", saved, total);
        return saved;
    }

    /**
     * Собирает читаемый текст страницы, отбрасывая теги, скрипты и стили.
     * Специально не привязывается к конкретным CSS-классам сайта — чтобы
     * не дублировать хрупкие селекторы {@code ProgramDetailParser}, здесь
     * достаточно общего текста для индексации, а не структурированных полей.
     */
    private String extractReadableText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        Document document = Jsoup.parse(html);
        document.select("script, style, noscript").remove();
        return document.text();
    }

    private String renderCardText(Program program) {
        StringBuilder sb = new StringBuilder();
        sb.append("Название: ").append(program.getTitle()).append(System.lineSeparator());
        program.getDescription().ifPresent(d ->
                sb.append("Описание: ").append(d).append(System.lineSeparator()));
        program.getOrganization().ifPresent(org ->
                sb.append("Организация: ").append(org.getName()).append(System.lineSeparator()));
        program.getDirection().ifPresent(v -> sb.append("Направление: ").append(v).append(System.lineSeparator()));
        program.getActivity().ifPresent(v -> sb.append("Активность: ").append(v).append(System.lineSeparator()));
        program.getAge().ifPresent(v -> sb.append("Возраст: ").append(v).append(System.lineSeparator()));
        program.getHours().ifPresent(v -> sb.append("Часы: ").append(v).append(System.lineSeparator()));
        program.getPrice().ifPresent(v -> sb.append("Цена: ").append(v).append(System.lineSeparator()));
        program.getSchedule().ifPresent(v -> sb.append("Расписание: ").append(v).append(System.lineSeparator()));
        program.getUrl().ifPresent(v -> sb.append("URL: ").append(v).append(System.lineSeparator()));
        return sb.toString();
    }

    private String resolveUrl(String hrefOrUrl) {
        if (hrefOrUrl.startsWith("http://") || hrefOrUrl.startsWith("https://")) {
            return hrefOrUrl;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = hrefOrUrl.startsWith("/") ? hrefOrUrl : "/" + hrefOrUrl;
        return base + path;
    }

    private String safeFileName(String id) {
        String safe = id == null ? "unknown" : id.replaceAll("[^a-zA-Z0-9_-]", "_");
        return safe.isEmpty() ? "unknown" : safe;
    }

    private void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку для корпуса: " + dir, e);
        }
    }

    private void report(ProgressListener listener, String message, int done, int total) {
        logger.info("[{}/{}] {}", done, total, message);
        if (listener != null) {
            listener.onProgress(message, done, total);
        }
    }

    private void sleepPolitely() {
        try {
            Thread.sleep(DETAIL_REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Builder для создания CorpusCollectionService.
     */
    public static class Builder {

        private HttpClientService httpClient;
        private String baseUrl;
        private Path outputRoot = Path.of("data", "raw");

        public Builder httpClient(HttpClientService httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** Корневая папка для корпуса. По умолчанию: data/raw. */
        public Builder outputRoot(Path outputRoot) {
            this.outputRoot = outputRoot;
            return this;
        }

        public CorpusCollectionService build() {
            if (httpClient == null) {
                throw new IllegalStateException("httpClient обязателен");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("baseUrl обязателен");
            }
            return new CorpusCollectionService(this);
        }
    }
}
