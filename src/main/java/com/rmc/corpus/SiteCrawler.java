package com.rmc.corpus;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.http.HttpResponse;
import com.rmc.logging.AppLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Универсальный обход всего сайта (в пределах одного домена), а не только
 * известных разделов ({@code /programs/}).
 *
 * <p>В отличие от {@link CorpusCollectionService}, который использует
 * знание конкретной структуры сайта (карточки программ, пагинация по
 * {@code .ui.pagination.menu}), этот класс ничего заранее не знает о
 * разметке — он просто идёт по ссылкам {@code <a href>}, которые находит
 * на каждой странице, и так добирается до всех разделов, включая те,
 * что не видны через раздел "Программы" (учреждения, статические
 * страницы, что угодно ещё).</p>
 *
 * <p>Обход breadth-first (в ширину) и **по уровням**: сначала страницы,
 * прямо ссылающиеся с главной (глубина 1), затем то, на что ссылаются
 * они (глубина 2), и так далее. {@link Builder#maxDepth} позволяет
 * остановиться на нужном уровне — например, сначала пройти только
 * главное меню (maxDepth=1), посмотреть, что нашлось, и только потом
 * увеличивать глубину. Обход идемпотентен (уже сохранённые страницы не
 * перекачиваются), поэтому повторный запуск с большей глубиной просто
 * доберёт новые страницы, не переделывая старое.</p>
 *
 * <p>{@link Builder#doNotExpand} — разделы, которые нужно увидеть и
 * сохранить, но не разворачивать вглубь (типичный случай — раздел
 * "Программы": там тысячи отдельных страниц программ, и в режиме
 * "изучить структуру сайта" туда лезть рано).</p>
 */
public class SiteCrawler {

    private static final Logger logger = AppLogger.getLogger();

    /** Жёсткий потолок — защита от неожиданно огромного или бесконечного сайта. */
    private static final int DEFAULT_MAX_PAGES = 3000;

    /** Пауза между запросами, чтобы не нагружать сайт. */
    private static final long DEFAULT_REQUEST_DELAY_MS = 350;

    /** Расширения, которые не имеет смысла запрашивать как HTML-страницу. */
    private static final Set<String> SKIP_EXTENSIONS = Set.of(
            ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar",
            ".woff", ".woff2", ".ttf", ".eot", ".mp4", ".mp3", ".avi"
    );

    /** Пути, по которым переходить не нужно — не контент, а действия/служебное. */
    private static final Set<String> SKIP_PATH_SUBSTRINGS = Set.of(
            "/logout", "/выход", "/static/", "/media/uploads/", "/admin/"
    );

    /**
     * Обратный вызов для отображения прогресса в UI/консоли.
     */
    public interface ProgressListener {
        void onProgress(String message, int depth, int visited, int queued, int saved);
    }

    private final HttpClientService httpClient;
    private final String baseUrl;
    private final String hostFilter;
    private final Path outputDir;
    private final int maxPages;
    private final int maxDepth;
    private final long requestDelayMs;
    private final Set<String> doNotExpand;

    private SiteCrawler(Builder builder) {
        this.httpClient = builder.httpClient;
        this.baseUrl = normalizeBase(builder.baseUrl);
        this.hostFilter = extractHost(builder.baseUrl);
        this.outputDir = builder.outputRoot.resolve("site");
        this.maxPages = builder.maxPages;
        this.maxDepth = builder.maxDepth;
        this.requestDelayMs = builder.requestDelayMs;
        this.doNotExpand = builder.doNotExpand;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Запускает обход, начиная с корня сайта.
     *
     * @return количество сохранённых страниц
     */
    public int crawl(ProgressListener listener, BooleanSupplier isCancelled) {
        ensureDir(outputDir);

        Set<String> visited = new LinkedHashSet<>();
        Map<String, Integer> depthOf = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(baseUrl);
        depthOf.put(baseUrl, 0);

        List<String[]> sitemapEntries = new ArrayList<>(); // [depth, url, title]
        int saved = 0;
        int index = 0;

        while (!queue.isEmpty() && visited.size() < maxPages) {
            if (isCancelled != null && isCancelled.getAsBoolean()) {
                logger.info("Обход остановлен пользователем на {} страницах", visited.size());
                break;
            }

            String url = queue.poll();
            int depth = depthOf.getOrDefault(url, 0);

            if (url == null || visited.contains(url)) {
                continue;
            }
            visited.add(url);
            index++;

            Path file = outputDir.resolve(safeFileName(url, index) + ".txt");
            boolean alreadySaved = Files.exists(file);

            report(listener, "Загрузка (уровень " + depth + "): " + url, depth, visited.size(), queue.size(), saved);

            String html;
            try {
                HttpResponse response = httpClient.get(URI.create(url));
                html = response.getBody();
            } catch (HttpException e) {
                logger.warn("Не удалось загрузить {}: {}", url, e.getMessage());
                sleepPolitely();
                continue;
            }

            if (html == null || html.isEmpty()) {
                sleepPolitely();
                continue;
            }

            Document document = Jsoup.parse(html, url);
            String title = document.title();
            sitemapEntries.add(new String[]{String.valueOf(depth), url, title});

            if (!alreadySaved) {
                String text = extractReadableText(document);
                String content = "URL: " + url + System.lineSeparator()
                        + "Заголовок: " + title + System.lineSeparator()
                        + "Уровень: " + depth + System.lineSeparator()
                        + "---" + System.lineSeparator() + text;
                try {
                    Files.writeString(file, content, StandardCharsets.UTF_8);
                    saved++;
                } catch (IOException e) {
                    logger.warn("Не удалось сохранить {}: {}", url, e.getMessage());
                }
            }

            boolean expandThisPage = depth < maxDepth && !matchesAny(url, doNotExpand);

            if (expandThisPage) {
                for (Element link : document.select("a[href]")) {
                    String absUrl = link.absUrl("href");
                    String normalized = normalizeUrl(absUrl);
                    if (normalized != null && !visited.contains(normalized) && !depthOf.containsKey(normalized)
                            && isSameHost(normalized) && !shouldSkip(normalized)) {
                        // Пагинация того же списка (страница 2, 3, ...) — это
                        // продолжение того же раздела, а не переход в новый,
                        // поэтому не тратит уровень глубины: иначе длинный
                        // список из many страниц обрывался бы на второй-третьей
                        // странице просто из-за ограничения maxDepth.
                        int nextDepth = isPaginationSibling(url, normalized) ? depth : depth + 1;
                        depthOf.put(normalized, nextDepth);
                        queue.add(normalized);
                    }
                }
            } else if (matchesAny(url, doNotExpand)) {
                logger.info("Страница сохранена, но не разворачивается вглубь (в списке doNotExpand): {}", url);
            }

            sleepPolitely();
        }

        writeSitemap(sitemapEntries);

        logger.info("Обход завершён: посещено {} страниц, сохранено {}", visited.size(), saved);
        return saved;
    }

    private boolean matchesAny(String url, Set<String> substrings) {
        String lower = url.toLowerCase();
        for (String part : substrings) {
            if (lower.contains(part.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void writeSitemap(List<String[]> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Карта сайта — найдено страниц: ").append(entries.size()).append(System.lineSeparator());
        sb.append("(уровень 0 — главная, уровень 1 — прямые ссылки с неё, и т.д.)").append(System.lineSeparator());
        sb.append("=".repeat(60)).append(System.lineSeparator());
        for (String[] entry : entries) {
            sb.append("[").append(entry[0]).append("] ")
                    .append(entry[2].isEmpty() ? "(без заголовка)" : entry[2])
                    .append(System.lineSeparator())
                    .append("    ").append(entry[1]).append(System.lineSeparator());
        }
        try {
            Files.writeString(outputDir.resolve("_sitemap.txt"), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Не удалось сохранить карту сайта: {}", e.getMessage());
        }
    }

    private String extractReadableText(Document document) {
        document.select("script, style, noscript").remove();
        return document.text();
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            // Убираем фрагмент (#...) — это не отдельная страница для сервера.
            URI withoutFragment = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                    uri.getQuery(), null);
            return withoutFragment.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean isSameHost(String url) {
        try {
            URI uri = new URI(url);
            return hostFilter.equalsIgnoreCase(uri.getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Определяет, является ли candidateUrl "соседней страницей" того же
     * списка, что и currentUrl (страница 2, 3, ... того же раздела) —
     * а не переходом в другой раздел сайта.
     *
     * <p>Намеренно не завязано на конкретное имя параметра ("page",
     * "p" и т.п.), потому что разные разделы сайта (программы,
     * сертификаты, эксперты, отчёты) могут использовать разные схемы
     * пагинации. Вместо этого используется общий признак: тот же путь
     * страницы, тот же набор параметров запроса, и ровно один параметр
     * отличается числовым значением — это и есть типичный отпечаток
     * пагинации, независимо от конкретного названия параметра.</p>
     */
    private boolean isPaginationSibling(String currentUrl, String candidateUrl) {
        try {
            URI current = new URI(currentUrl);
            URI candidate = new URI(candidateUrl);

            if (!java.util.Objects.equals(current.getHost(), candidate.getHost())) {
                return false;
            }
            if (!java.util.Objects.equals(current.getPath(), candidate.getPath())) {
                return false;
            }

            Map<String, String> currentParams = parseQuery(current.getQuery());
            Map<String, String> candidateParams = parseQuery(candidate.getQuery());

            if (!currentParams.keySet().equals(candidateParams.keySet())) {
                return false;
            }

            int differingKeys = 0;
            for (String key : currentParams.keySet()) {
                String currentValue = currentParams.get(key);
                String candidateValue = candidateParams.get(key);
                if (!currentValue.equals(candidateValue)) {
                    differingKeys++;
                    if (!currentValue.matches("\\d+") || !candidateValue.matches("\\d+")) {
                        return false; // отличие не числовое — вряд ли это номер страницы
                    }
                }
            }

            return differingKeys == 1;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            } else if (eq < 0 && !pair.isEmpty()) {
                params.put(pair, "");
            }
        }
        return params;
    }

    private boolean shouldSkip(String url) {
        String lower = url.toLowerCase();
        for (String ext : SKIP_EXTENSIONS) {
            if (lower.contains(ext)) {
                return true;
            }
        }
        for (String part : SKIP_PATH_SUBSTRINGS) {
            if (lower.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private String safeFileName(String url, int index) {
        String slugSource;
        try {
            URI uri = new URI(url);
            String path = uri.getPath() == null ? "" : uri.getPath();
            slugSource = path.replaceAll("^/+|/+$", "").replaceAll("/", "_");
        } catch (URISyntaxException e) {
            slugSource = "page";
        }
        String safe = slugSource.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safe.length() > 60) {
            safe = safe.substring(0, 60);
        }
        if (safe.isEmpty()) {
            safe = "root";
        }
        return String.format("%05d_%s", index, safe);
    }

    private String normalizeBase(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String extractHost(String baseUrl) {
        try {
            return new URI(baseUrl).getHost();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Некорректный baseUrl: " + baseUrl, e);
        }
    }

    private void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку для корпуса: " + dir, e);
        }
    }

    private void report(ProgressListener listener, String message, int depth, int visited, int queued, int saved) {
        logger.info("[уровень {}, {} посещено, {} в очереди, {} сохранено] {}",
                depth, visited, queued, saved, message);
        if (listener != null) {
            listener.onProgress(message, depth, visited, queued, saved);
        }
    }

    private void sleepPolitely() {
        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Builder для создания SiteCrawler.
     */
    public static class Builder {

        private HttpClientService httpClient;
        private String baseUrl;
        private Path outputRoot = Path.of("data", "raw");
        private int maxPages = DEFAULT_MAX_PAGES;
        private int maxDepth = Integer.MAX_VALUE;
        private long requestDelayMs = DEFAULT_REQUEST_DELAY_MS;
        private Set<String> doNotExpand = new LinkedHashSet<>();

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

        /** Максимум страниц за один обход. По умолчанию: 3000. */
        public Builder maxPages(int maxPages) {
            this.maxPages = maxPages;
            return this;
        }

        /**
         * Максимальная глубина обхода от главной страницы (0). Например,
         * maxDepth=1 — обойти только то, на что прямо ссылается главная,
         * не разворачивая найденные страницы дальше. По умолчанию: без
         * ограничения.
         */
        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /** Пауза между запросами в миллисекундах. По умолчанию: 350. */
        public Builder requestDelayMs(long requestDelayMs) {
            this.requestDelayMs = requestDelayMs;
            return this;
        }

        /**
         * Подстроки URL (например, "/programs/"), которые нужно сохранить,
         * но не разворачивать вглубь — используется, чтобы не провалиться
         * в тысячи отдельных страниц программ при изучении структуры сайта.
         */
        public Builder doNotExpand(Set<String> doNotExpand) {
            this.doNotExpand = doNotExpand;
            return this;
        }

        public SiteCrawler build() {
            if (httpClient == null) {
                throw new IllegalStateException("httpClient обязателен");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("baseUrl обязателен");
            }
            return new SiteCrawler(this);
        }
    }
}
