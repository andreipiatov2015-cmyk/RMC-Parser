package com.rmc.corpus;

import com.rmc.auth.django.DjangoAuthenticationProvider;
import com.rmc.config.ServerConfig;
import com.rmc.http.HttpClientService;
import com.rmc.parser.ProgramParser;
import com.rmc.parser.model.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Автономный тестовый запуск сбора корпуса — без UI, из командной строки.
 *
 * <p>Нужен, чтобы проверить, что сбор текста с сайта вообще работает,
 * прежде чем встраивать кнопку в интерфейс. Логика внутри (авторизация,
 * обход страниц) сознательно продублирована в упрощённом виде из
 * {@link com.rmc.search.service.ProgramAnalysisService} — этот класс
 * не изменяет существующие сервисы и безопасен для параллельной
 * разработки остального интерфейса.</p>
 *
 * <p>Запуск:</p>
 * <pre>
 * java -cp target/classes com.rmc.corpus.CorpusCollectionRunner &lt;логин&gt; &lt;пароль&gt;
 * </pre>
 */
public final class CorpusCollectionRunner {

    private static final int MAX_PAGES = 500;

    private CorpusCollectionRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Использование: CorpusCollectionRunner <логин> <пароль> [queryString]");
            return;
        }

        String username = args[0];
        String password = args[1];
        String queryString = args.length > 2 ? args[2] : "";

        HttpClientService httpClient = HttpClientService.builder().build();

        System.out.println("Авторизация на " + ServerConfig.BASE_URL + "...");
        DjangoAuthenticationProvider authProvider =
                new DjangoAuthenticationProvider(httpClient, ServerConfig.BASE_URL);
        DjangoAuthenticationProvider.DjangoAuthResult authResult =
                authProvider.authenticate(username, password);

        if (!authResult.isSuccess()) {
            System.out.println("Авторизация не удалась: " + authResult.getErrorMessage().orElse("неизвестная ошибка"));
            return;
        }
        System.out.println("Авторизация успешна.");

        System.out.println("Сбор списка программ (обход страниц)...");
        List<Program> allPrograms = collectAllPrograms(httpClient, queryString);
        System.out.println("Найдено программ: " + allPrograms.size());

        CorpusCollectionService corpusService = CorpusCollectionService.builder()
                .httpClient(httpClient)
                .baseUrl(ServerConfig.BASE_URL)
                .build();

        System.out.println("Фаза A: сохранение текста карточек...");
        int savedCards = corpusService.collectFromCards(allPrograms);
        System.out.println("Сохранено карточек: " + savedCards);

        System.out.println("Фаза B: обход детальных страниц (может занять долго)...");
        AtomicBoolean cancelled = new AtomicBoolean(false);
        int savedDetails = corpusService.collectDetailPages(allPrograms,
                (message, done, total) -> System.out.println("[" + done + "/" + total + "] " + message),
                cancelled::get);
        System.out.println("Сохранено детальных страниц: " + savedDetails);

        System.out.println("Готово. Файлы лежат в data/raw/cards и data/raw/details");
    }

    /**
     * Упрощённый обход страниц списка программ — только сбор объектов
     * {@link Program}, без побочной агрегации по учреждениям (в отличие
     * от {@code ProgramAnalysisService}, которая параллельно считает
     * статистику и сюда не подходит напрямую).
     */
    private static List<Program> collectAllPrograms(HttpClientService httpClient, String queryString) {
        List<Program> allPrograms = new ArrayList<>();
        String base = ServerConfig.BASE_URL.endsWith("/")
                ? ServerConfig.BASE_URL.substring(0, ServerConfig.BASE_URL.length() - 1)
                : ServerConfig.BASE_URL;

        String nextRelativeUrl = "/programs/" + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
        int pageCount = 0;

        while (nextRelativeUrl != null && pageCount < MAX_PAGES) {
            pageCount++;
            String fullUrl = nextRelativeUrl.startsWith("http")
                    ? nextRelativeUrl
                    : base + (nextRelativeUrl.startsWith("/") ? nextRelativeUrl : "/" + nextRelativeUrl);

            System.out.println("  Страница " + pageCount + ": " + fullUrl);
            var response = httpClient.get(java.net.URI.create(fullUrl));
            ProgramParser.ParseResult parseResult = ProgramParser.parse(response.getBody());

            if (!parseResult.isSuccess()) {
                System.out.println("  Ошибка разбора страницы " + pageCount + ": "
                        + parseResult.getErrorMessage().orElse("неизвестно"));
                break;
            }

            allPrograms.addAll(parseResult.getPrograms());
            nextRelativeUrl = parseResult.hasNextPage() ? parseResult.getNextPageUrl().orElse(null) : null;
        }

        return allPrograms;
    }
}
