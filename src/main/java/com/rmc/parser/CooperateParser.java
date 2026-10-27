package com.rmc.parser;

import com.rmc.logging.AppLogger;
import com.rmc.parser.model.Cooperate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер HTML страницы результатов раздела "Соглашения" (/cooperates/).
 *
 * <p>Структура таблицы похожа на реестр программ ({@link ProgramParser}),
 * но строки &lt;tr id="objNNNN"&gt; не содержат собственной ссылки на
 * детальную страницу — вместо этого статус заявки и её идентификатор лежат
 * в ячейке с атрибутом {@code data-requestId}.</p>
 */
public class CooperateParser {

    private static final Logger logger = AppLogger.getLogger();

    private static final String LOG_PARSING_START = "Разбор HTML страницы соглашений...";
    private static final String LOG_EMPTY_HTML = "HTML пуст";
    private static final String LOG_ROWS_FOUND = "Найдено строк: {}";
    private static final String LOG_PARSING_COMPLETE = "Разбор завершён. Извлечено соглашений: {}";
    private static final String LOG_PARSING_ERROR = "Ошибка разбора: {}";

    private static final Pattern HOURS_NUMBER = Pattern.compile("(\\d+)");

    private CooperateParser() {
        // Утилитарный класс
    }

    /**
     * Разобрать HTML и извлечь заявки на соглашение.
     *
     * @param html HTML контент
     * @return результат парсинга
     */
    public static ParseResult parse(String html) {
        logger.info(LOG_PARSING_START);

        if (html == null || html.isEmpty()) {
            logger.warn(LOG_EMPTY_HTML);
            return ParseResult.builder()
                    .success(false)
                    .errorMessage("HTML пуст или null")
                    .build();
        }

        try {
            Document document = Jsoup.parse(html);

            List<Cooperate> cooperates = new ArrayList<>();
            Elements rows = document.select("tr[id^=obj]");
            logger.info(LOG_ROWS_FOUND, rows.size());

            for (Element row : rows) {
                Cooperate cooperate = parseRow(row);
                if (cooperate != null) {
                    cooperates.add(cooperate);
                }
            }

            logger.info(LOG_PARSING_COMPLETE, cooperates.size());

            return ParseResult.builder()
                    .success(true)
                    .cooperates(cooperates)
                    .cooperateCount(cooperates.size())
                    .hasNextPage(hasNextPage(document))
                    .nextPageUrl(findNextPageUrl(document))
                    .build();

        } catch (Exception e) {
            logger.error(LOG_PARSING_ERROR, e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private static Cooperate parseRow(Element row) {
        Element statusCell = row.selectFirst("td[data-requestid]");
        String id = statusCell != null
                ? statusCell.attr("data-requestid")
                : extractIdFromRowAttr(row);

        if (id == null || id.isEmpty()) {
            return null;
        }

        Cooperate.Builder builder = Cooperate.builder().id(id);

        for (Element cell : row.select("> td")) {
            String label = cellLabel(cell);

            if (label.startsWith("Дата")) {
                builder.date(cellTextAfterLabel(cell));
            } else if (label.startsWith("Уполномоченный орган")) {
                parsePayerCell(cell, builder);
            } else if (label.startsWith("Период")) {
                builder.period(cellTextAfterLabel(cell));
            } else if (label.startsWith("Количество часов")) {
                parseHoursCell(cell, builder);
            } else if (cell.hasAttr("data-requestid")) {
                parseStatusCell(cell, builder);
            } else if (cell.selectFirst("a[href^=/org/]") != null) {
                parseSchoolCell(cell, builder);
            }
        }

        return builder.build();
    }

    private static String extractIdFromRowAttr(Element row) {
        String rowId = row.id();
        if (rowId != null && rowId.startsWith("obj")) {
            return rowId.substring("obj".length());
        }
        return null;
    }

    /**
     * Подпись ячейки из &lt;p class="strong hide_on_desktop"&gt;, если есть
     * (приём как в {@link ProgramParser}).
     */
    private static String cellLabel(Element cell) {
        Element labelEl = cell.selectFirst("p.strong.hide_on_desktop");
        return labelEl != null ? labelEl.text().trim() : "";
    }

    private static String cellTextAfterLabel(Element cell) {
        Element labelEl = cell.selectFirst("p.strong.hide_on_desktop");
        if (labelEl == null) {
            return cell.text().trim();
        }
        Element copy = cell.clone();
        Element labelCopy = copy.selectFirst("p.strong.hide_on_desktop");
        if (labelCopy != null) {
            labelCopy.remove();
        }
        return copy.text().trim();
    }

    /**
     * Ячейка "Уполномоченный орган" — название и территория плательщика,
     * без собственной ссылки (в отличие от учреждения).
     */
    private static void parsePayerCell(Element cell, Cooperate.Builder builder) {
        Element content = cell.selectFirst("h4.ui.image.header .content");
        if (content == null) {
            return;
        }

        String name = content.ownText().trim();
        if (name.isEmpty()) {
            name = content.text().trim();
        }
        if (!name.isEmpty()) {
            builder.payerName(name);
        }

        Element territory = content.selectFirst(".sub.header");
        if (territory != null) {
            String text = territory.text().trim();
            if (!text.isEmpty()) {
                builder.payerTerritory(text);
            }
        }

        Element link = cell.selectFirst("a[href]");
        if (link != null) {
            builder.payerUrl(link.attr("href"));
        }
    }

    /**
     * Ячейка "Учреждение" — ссылка /org/{id}/, название и две подзаголовка:
     * территория и министерство.
     */
    private static void parseSchoolCell(Element cell, Cooperate.Builder builder) {
        Element link = cell.selectFirst("a[href^=/org/]");
        if (link == null) {
            return;
        }

        builder.schoolUrl(link.attr("href"));

        Element content = link.selectFirst(".content");
        String name;
        if (content != null) {
            name = content.ownText().trim();
            if (name.isEmpty()) {
                name = content.text().trim();
            }
        } else {
            name = link.text().trim();
        }
        if (!name.isEmpty()) {
            builder.schoolName(name);
        }

        if (content != null) {
            Elements subHeaders = content.select(".sub.header");
            if (!subHeaders.isEmpty()) {
                builder.schoolTerritory(subHeaders.get(0).text().trim());
            }
            if (subHeaders.size() > 1) {
                builder.schoolMinistry(subHeaders.get(1).text().trim());
            }
        }
    }

    /**
     * Ячейка "Количество часов" — пустая для новых заявок, либо содержит
     * "Договоров заключено на: N ч." для подтверждённых.
     */
    private static void parseHoursCell(Element cell, Cooperate.Builder builder) {
        String text = cellTextAfterLabel(cell);
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = HOURS_NUMBER.matcher(text);
        if (matcher.find()) {
            builder.confirmedHours(matcher.group(1));
        }
    }

    /**
     * Ячейка "Статус" — определяет {@link Cooperate.Status} по содержимому:
     * кнопки "принять"/"отклонить" → новая заявка; зелёная плашка
     * "подтверждена" → подтверждена; красная плашка "отклонена" → отклонена.
     */
    private static void parseStatusCell(Element cell, Cooperate.Builder builder) {
        Element greenLabel = cell.selectFirst(".ui.green.horizontal.label, p.ui.green.horizontal.label");
        Element redLabel = cell.selectFirst(".ui.red.horizontal.label, p.ui.red.horizontal.label");
        Element confirmButton = cell.selectFirst("button[onclick*=confirmRequestModal]");

        if (greenLabel != null) {
            builder.status(Cooperate.Status.CONFIRMED);
            builder.statusText(greenLabel.text().trim());
        } else if (redLabel != null) {
            builder.status(Cooperate.Status.REJECTED);
            builder.statusText(redLabel.text().trim());
        } else if (confirmButton != null) {
            builder.status(Cooperate.Status.NEW);
            builder.statusText("новая заявка");
        } else {
            builder.status(Cooperate.Status.UNKNOWN);
        }

        Element maxHoursEl = cell.selectFirst("p.ui.small.text");
        if (maxHoursEl != null) {
            Matcher matcher = HOURS_NUMBER.matcher(maxHoursEl.ownText());
            if (matcher.find()) {
                builder.maxHours(matcher.group(1));
            }
        }
    }

    /**
     * Проверка наличия следующей страницы результатов — та же разметка
     * пагинации, что и в реестре программ (см. {@link ProgramParser}).
     */
    private static boolean hasNextPage(Document document) {
        return findNextPageUrl(document) != null;
    }

    private static String findNextPageUrl(Document document) {
        Element pagination = document.selectFirst(".ui.pagination.menu");
        if (pagination == null) {
            return null;
        }

        Elements pageLinks = pagination.select("a.item");
        if (pageLinks.isEmpty()) {
            return null;
        }

        int activeIndex = -1;
        for (int i = 0; i < pageLinks.size(); i++) {
            if (pageLinks.get(i).hasClass("active")) {
                activeIndex = i;
                break;
            }
        }

        if (activeIndex < 0 || activeIndex + 1 >= pageLinks.size()) {
            return null;
        }

        String href = pageLinks.get(activeIndex + 1).attr("href");
        return href.isEmpty() ? null : href;
    }

    /**
     * Результат парсинга.
     */
    public static class ParseResult {

        private final boolean success;
        private final List<Cooperate> cooperates;
        private final int cooperateCount;
        private final boolean hasNextPage;
        private final String nextPageUrl;
        private final String errorMessage;

        private ParseResult(Builder builder) {
            this.success = builder.success;
            this.cooperates = List.copyOf(builder.cooperates);
            this.cooperateCount = builder.cooperateCount;
            this.hasNextPage = builder.hasNextPage;
            this.nextPageUrl = builder.nextPageUrl;
            this.errorMessage = builder.errorMessage;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Cooperate> getCooperates() {
            return cooperates;
        }

        public int getCooperateCount() {
            return cooperateCount;
        }

        public boolean hasNextPage() {
            return hasNextPage;
        }

        public Optional<String> getNextPageUrl() {
            return Optional.ofNullable(nextPageUrl);
        }

        public Optional<String> getErrorMessage() {
            return Optional.ofNullable(errorMessage);
        }

        public static class Builder {

            private boolean success;
            private List<Cooperate> cooperates = new ArrayList<>();
            private int cooperateCount;
            private boolean hasNextPage;
            private String nextPageUrl;
            private String errorMessage;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder cooperates(List<Cooperate> cooperates) {
                this.cooperates = new ArrayList<>(cooperates);
                return this;
            }

            public Builder cooperateCount(int count) {
                this.cooperateCount = count;
                return this;
            }

            public Builder hasNextPage(boolean hasNextPage) {
                this.hasNextPage = hasNextPage;
                return this;
            }

            public Builder nextPageUrl(String nextPageUrl) {
                this.nextPageUrl = nextPageUrl;
                return this;
            }

            public Builder errorMessage(String message) {
                this.errorMessage = message;
                return this;
            }

            public ParseResult build() {
                return new ParseResult(this);
            }
        }
    }
}
