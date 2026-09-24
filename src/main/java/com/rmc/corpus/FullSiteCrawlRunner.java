package com.rmc.corpus;

import com.rmc.auth.django.DjangoAuthenticationProvider;
import com.rmc.config.ServerConfig;
import com.rmc.http.HttpClientService;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Автономный запуск обхода сайта по уровням — сначала структура, без
 * провала в тысячи страниц программ. См. {@link SiteCrawler}.
 *
 * <p>Запуск:</p>
 * <pre>
 * java -cp target/classes com.rmc.corpus.FullSiteCrawlRunner &lt;логин&gt; &lt;пароль&gt; [maxDepth] [maxPages]
 * </pre>
 *
 * <p>Раздел программ ({@code /programs/}, {@code /program/}, {@code /course/})
 * по умолчанию сохраняется, если встретится, но не разворачивается —
 * его лучше собирать отдельно через {@link CorpusCollectionRunner},
 * когда структура сайта уже понятна.</p>
 */
public final class FullSiteCrawlRunner {

    private FullSiteCrawlRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Использование: FullSiteCrawlRunner <логин> <пароль> [maxDepth] [maxPages]");
            System.out.println("  maxDepth — сколько уровней ссылок от главной обходить (по умолчанию 1)");
            System.out.println("  maxPages — общий потолок страниц (по умолчанию 500)");
            return;
        }

        String username = args[0];
        String password = args[1];
        int maxDepth = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int maxPages = args.length > 3 ? Integer.parseInt(args[3]) : 500;

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

        System.out.println("Обход сайта: уровней — " + maxDepth + ", максимум страниц — " + maxPages);
        System.out.println("Разделы программ будут сохранены, но не разворачиваются вглубь на этом этапе.");

        Set<String> doNotExpand = new LinkedHashSet<>(Set.of("/programs/", "/program/", "/course/"));

        SiteCrawler crawler = SiteCrawler.builder()
                .httpClient(httpClient)
                .baseUrl(ServerConfig.BASE_URL)
                .maxDepth(maxDepth)
                .maxPages(maxPages)
                .doNotExpand(doNotExpand)
                .build();

        AtomicBoolean cancelled = new AtomicBoolean(false);
        int saved = crawler.crawl(
                (message, depth, visited, queued, savedCount) ->
                        System.out.println("[уровень " + depth + ", посещено: " + visited + ", в очереди: " + queued
                                + ", сохранено: " + savedCount + "] " + message),
                cancelled::get);

        System.out.println("Готово. Сохранено страниц: " + saved);
        System.out.println("Файлы лежат в data/raw/site, карта сайта — в data/raw/site/_sitemap.txt");
        System.out.println("Откройте _sitemap.txt, чтобы увидеть найденную структуру, и решите, какой раздел собирать дальше.");
    }
}

