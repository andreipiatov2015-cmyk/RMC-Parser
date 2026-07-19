package com.rmc.parser;

import com.rmc.logging.AppLogger;
import com.rmc.parser.model.Organization;
import com.rmc.parser.model.Program;
import com.rmc.parser.model.Teacher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Парсер HTML страницы результатов поиска программ.
 * 
 * <p>Автоматически извлекает данные из карточек программ:</p>
 * <ul>
 *   <li>ID программы</li>
 *   <li>Название</li>
 *   <li>Описание</li>
 *   <li>Организация</li>
 *   <li>Преподаватели</li>
 *   <li>Направление</li>
 *   <li>Активность</li>
 *   <li>Возраст</li>
 *   <li>Часы</li>
 *   <li>Цена</li>
 *   <li>Расписание</li>
 * </ul>
 * 
 * <p>Использует только CSS селекторы Jsoup.</p>
 */
public class ProgramParser {
    
    private static final Logger logger = AppLogger.getLogger();
    
    // CSS селекторы для поиска карточек программ
    private static final String CARD_SELECTOR = ".program-card, .program-item, .course-card, [class*='program'], [class*='course'], article, .item";
    
    // Селекторы для данных программы
    private static final String TITLE_SELECTOR = "h2, h3, .title, .name, [class*='title'], [class*='name']";
    private static final String DESC_SELECTOR = ".description, .desc, .about, [class*='desc'], p";
    private static final String LINK_SELECTOR = "a[href]";
    private static final String IMAGE_SELECTOR = "img[src]";
    
    // Селекторы для организации
    private static final String ORG_SELECTOR = ".organization, .org, .school, [class*='org'], [class*='school']";
    private static final String ORG_NAME_SELECTOR = ".name, [class*='name'], strong";
    
    // Селекторы для преподавателей
    private static final String TEACHER_SELECTOR = ".teacher, .teachers, .instructor, [class*='teacher'], [class*='instructor']";
    
    // Селекторы для характеристик
    private static final String INFO_SELECTOR = ".info, .details, .meta, [class*='info'], [class*='detail']";
    private static final String AGE_SELECTOR = "[class*='age'], [class*='возраст']";
    private static final String HOURS_SELECTOR = "[class*='hour'], [class*='time'], [class*='длит']";
    private static final String PRICE_SELECTOR = "[class*='price'], [class*='cost'], [class*='руб']";
    private static final String DIRECTION_SELECTOR = "[class*='direction'], [class*='направл']";
    private static final String ACTIVITY_SELECTOR = "[class*='activity'], [class*='активн']";
    private static final String SCHEDULE_SELECTOR = "[class*='schedule'], [class*='расписан']";
    
    private ProgramParser() {
        // Утилитарный класс
    }
    
    /**
     * Разобрать HTML и извлечь программы.
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
            
            // Основной путь: реальная таблица программ на сайте
            // (<tr id="objXXXXX"> со ссылками /programs/{id}/ и /org/{id}/).
            List<Program> programs = parseTableRows(document);
            
            if (!programs.isEmpty()) {
                logger.info(LOG_CARDS_FOUND, programs.size());
                logger.info(LOG_PARSING_COMPLETE, programs.size());
                return ParseResult.builder()
                        .success(true)
                        .programs(programs)
                        .programCount(programs.size())
                        .hasNextPage(hasNextPage(document))
                        .nextPageUrl(findNextPageUrl(document))
                        .build();
            }
            
            // Фолбэк: старая эвристика по карточкам (нужна для страниц с
            // другой вёрсткой, где нет табличной структуры).
            Elements cards = findProgramCards(document);
            logger.info(LOG_CARDS_FOUND, cards.size());
            
            for (Element card : cards) {
                Program program = parseProgramCard(card);
                if (program != null) {
                    programs.add(program);
                }
            }
            
            logger.info(LOG_PARSING_COMPLETE, programs.size());
            
            return ParseResult.builder()
                    .success(true)
                    .programs(programs)
                    .programCount(programs.size())
                    .build();
                    
        } catch (Exception e) {
            logger.error(LOG_PARSING_ERROR, e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Разобрать таблицу программ в её реальном виде на сайте: каждая строка
     * &lt;tr&gt; содержит ссылку на программу (/programs/{id}/) и, внутри
     * одной из ячеек, ссылку на учреждение (/org/{id}/). Ячейки
     * сопоставляются не по номеру колонки (порядок может меняться), а по
     * подписи в &lt;p class="strong hide_on_desktop"&gt;.
     */
    private static List<Program> parseTableRows(Document document) {
        List<Program> programs = new ArrayList<>();
        
        Elements rows = document.select("tr[id]");
        for (Element row : rows) {
            Program program = parseTableRow(row);
            if (program != null) {
                programs.add(program);
            }
        }
        
        return programs;
    }
    
    private static Program parseTableRow(Element row) {
        Element titleLink = row.selectFirst("a[href^=/programs/]");
        if (titleLink == null) {
            return null;
        }
        
        String href = titleLink.attr("href");
        String id = extractIdFromUrl(href, "/programs/");
        String title = titleLink.text().trim();
        
        if (title.isEmpty()) {
            return null;
        }
        
        Program.Builder builder = Program.builder()
                .id(id)
                .title(title)
                .url(href);
        
        for (Element cell : row.select("> td")) {
            String label = cellLabel(cell);
            
            if (label.startsWith("Полное наименование")) {
                builder.description(cellTextAfterLabel(cell));
            } else if (label.startsWith("Учреждение")) {
                Organization org = parseOrganizationCell(cell);
                if (org != null) {
                    builder.organization(org);
                }
            }
        }
        
        return builder.build();
    }
    
    /**
     * Подпись ячейки из &lt;p class="strong hide_on_desktop"&gt;, если есть.
     */
    private static String cellLabel(Element cell) {
        Element labelEl = cell.selectFirst("p.strong.hide_on_desktop");
        return labelEl != null ? labelEl.text().trim() : "";
    }
    
    /**
     * Текст ячейки за вычетом самой подписи-label.
     */
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
    
    private static Organization parseOrganizationCell(Element cell) {
        Element link = cell.selectFirst("a[href^=/org/]");
        if (link == null) {
            return null;
        }
        
        String href = link.attr("href");
        String id = extractIdFromUrl(href, "/org/");
        
        Element contentDiv = link.selectFirst(".content");
        String name;
        if (contentDiv != null) {
            // ownText() берёт только собственный текст .content, минуя
            // вложенные .sub.header (муниципалитет/министерство).
            name = contentDiv.ownText().trim();
            if (name.isEmpty()) {
                name = contentDiv.text().trim();
            }
        } else {
            name = link.text().trim();
        }
        
        if (name.isEmpty()) {
            return null;
        }
        
        return Organization.builder()
                .id(id)
                .url(href)
                .name(name)
                .build();
    }
    
    /**
     * Проверка наличия следующей страницы результатов — по реальной
     * разметке пагинации сайта (.ui.pagination.menu, ссылки a.item,
     * текущая страница помечена классом "active").
     */
    private static boolean hasNextPage(Document document) {
        return findNextPageUrl(document) != null;
    }
    
    /**
     * Следующая страница — это ссылка, идущая сразу после активной
     * (a.item.active) в списке ссылок пагинации. Если активная страница
     * последняя в списке (или пагинации нет вовсе), следующей страницы нет.
     */
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
     * Найти карточки программ на странице.
     */
    private static Elements findProgramCards(Document document) {
        Elements cards = new Elements();
        
        // Пробуем разные селекторы
        for (String selector : CARD_SELECTOR.split(", ")) {
            Elements found = document.select(selector.trim());
            if (!found.isEmpty()) {
                cards = found;
                logger.debug("Found cards with selector: {}", selector);
                break;
            }
        }
        
        // Если ничего не найдено, ищем ссылки на программы
        if (cards.isEmpty()) {
            Elements links = document.select("a[href*='/program/'], a[href*='/course/']");
            for (Element link : links) {
                Element parent = link.parent();
                if (parent != null && !cards.contains(parent)) {
                    cards.add(parent);
                }
            }
        }
        
        return cards;
    }
    
    /**
     * Разобрать одну карточку программы.
     */
    private static Program parseProgramCard(Element card) {
        try {
            Program.Builder builder = Program.builder();
            
            // ID программы
            String id = extractId(card);
            builder.id(id);
            
            // Название
            String title = extractTitle(card);
            builder.title(title);
            
            // Ссылка
            String url = extractUrl(card);
            builder.url(url);
            
            // Описание
            String description = extractDescription(card);
            builder.description(description);
            
            // Изображение
            String imageUrl = extractImageUrl(card);
            builder.imageUrl(imageUrl);
            
            // Организация
            Organization org = extractOrganization(card);
            builder.organization(org);
            
            // Преподаватели
            List<Teacher> teachers = extractTeachers(card);
            builder.teachers(teachers);
            
            // Характеристики
            extractCharacteristics(card, builder);
            
            return builder.build();
            
        } catch (Exception e) {
            logger.warn(LOG_CARD_PARSE_ERROR, e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлечь ID программы.
     */
    private static String extractId(Element card) {
        // Пробуем найти в атрибутах
        String id = card.attr("id");
        if (!id.isEmpty()) {
            return id;
        }
        
        // Пробуем найти в data-атрибутах
        String dataId = card.attr("data-id");
        if (!dataId.isEmpty()) {
            return dataId;
        }
        
        // Пробуем извлечь из URL
        Element link = card.selectFirst(LINK_SELECTOR);
        if (link != null) {
            String href = link.attr("href");
            if (href.contains("/program/")) {
                return extractIdFromUrl(href, "/program/");
            }
            if (href.contains("/course/")) {
                return extractIdFromUrl(href, "/course/");
            }
        }
        
        return null;
    }
    
    private static String extractIdFromUrl(String url, String prefix) {
        int idx = url.indexOf(prefix);
        if (idx >= 0) {
            String rest = url.substring(idx + prefix.length());
            int slashIdx = rest.indexOf('/');
            if (slashIdx > 0) {
                return rest.substring(0, slashIdx);
            }
            int questionIdx = rest.indexOf('?');
            if (questionIdx > 0) {
                return rest.substring(0, questionIdx);
            }
            return rest;
        }
        return null;
    }
    
    /**
     * Извлечь название программы.
     */
    private static String extractTitle(Element card) {
        Element titleEl = card.selectFirst(TITLE_SELECTOR);
        if (titleEl != null) {
            return titleEl.text().trim();
        }
        
        // Пробуем первый заголовок
        Element h = card.selectFirst("h1, h2, h3, h4");
        if (h != null) {
            return h.text().trim();
        }
        
        // Пробуем ссылку
        Element link = card.selectFirst("a");
        if (link != null) {
            return link.text().trim();
        }
        
        return null;
    }
    
    /**
     * Извлечь URL программы.
     */
    private static String extractUrl(Element card) {
        Element link = card.selectFirst("a[href]");
        if (link != null) {
            String href = link.attr("href");
            if (!href.startsWith("http")) {
                // Относительный URL
                return href;
            }
            return href;
        }
        return null;
    }
    
    /**
     * Извлечь описание.
     */
    private static String extractDescription(Element card) {
        Element descEl = card.selectFirst(DESC_SELECTOR);
        if (descEl != null) {
            return descEl.text().trim();
        }
        return null;
    }
    
    /**
     * Извлечь URL изображения.
     */
    private static String extractImageUrl(Element card) {
        Element img = card.selectFirst(IMAGE_SELECTOR);
        if (img != null) {
            String src = img.attr("src");
            if (src.isEmpty()) {
                src = img.attr("data-src");
            }
            if (!src.isEmpty() && !src.startsWith("data:")) {
                return src;
            }
        }
        return null;
    }
    
    /**
     * Извлечь организацию.
     */
    private static Organization extractOrganization(Element card) {
        Element orgEl = card.selectFirst(ORG_SELECTOR);
        if (orgEl == null) {
            return null;
        }
        
        Organization.Builder builder = Organization.builder();
        
        // Название организации
        Element nameEl = orgEl.selectFirst(ORG_NAME_SELECTOR);
        if (nameEl != null) {
            builder.name(nameEl.text().trim());
        } else {
            // Используем весь текст
            String text = orgEl.text().trim();
            if (!text.isEmpty()) {
                builder.name(text);
            }
        }
        
        Organization org = builder.build();
        
        if (org.getName() == null) {
            return null;
        }
        
        return org;
    }
    
    /**
     * Извлечь преподавателей.
     */
    private static List<Teacher> extractTeachers(Element card) {
        List<Teacher> teachers = new ArrayList<>();
        
        Elements teacherEls = card.select(TEACHER_SELECTOR);
        for (Element teacherEl : teacherEls) {
            String name = teacherEl.text().trim();
            if (!name.isEmpty()) {
                teachers.add(Teacher.builder()
                        .name(name)
                        .build());
            }
        }
        
        return teachers;
    }
    
    /**
     * Извлечь характеристики программы.
     */
    private static void extractCharacteristics(Element card, Program.Builder builder) {
        // Ищем возраст
        String age = extractByPattern(card, AGE_SELECTOR, 
                "(возраст|age|лет|год)",
                "(\\d+[-\\s]\\d+\\s*(лет|года)|\\d+\\s*(лет|года)");
        builder.age(age);
        
        // Ищем часы
        String hours = extractByPattern(card, HOURS_SELECTOR,
                "(часов|часа|hours|ч\\.)",
                "(\\d+)\\s*(часов|часа|ч)");
        builder.hours(hours);
        
        // Ищем цену
        String price = extractByPattern(card, PRICE_SELECTOR,
                "(руб|р\\.|price|cost|₽)",
                "(\\d+[\\s\\d]*)\\s*(руб|р\\.)?");
        builder.price(price);
        
        // Ищем направление
        String direction = extractByPattern(card, DIRECTION_SELECTOR,
                "(направление|direction)",
                null);
        builder.direction(direction);
        
        // Ищем активность
        String activity = extractByPattern(card, ACTIVITY_SELECTOR,
                "(активность|activity)",
                null);
        builder.activity(activity);
        
        // Ищем расписание
        String schedule = extractByPattern(card, SCHEDULE_SELECTOR,
                "(расписание|schedule|дни)",
                null);
        builder.schedule(schedule);
    }
    
    /**
     * Извлечь значение по паттерну.
     */
    private static String extractByPattern(Element card, String selector, String... keywords) {
        // Сначала пробуем найти по селектору
        Elements elements = card.select(selector);
        for (Element el : elements) {
            String text = el.text().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        
        // Если не найдено, ищем в тексте
        if (keywords.length > 0) {
            String cardText = card.text();
            
            for (int i = 1; i < keywords.length; i++) {
                String keyword = keywords[i];
                int idx = cardText.toLowerCase().indexOf(keyword.toLowerCase());
                if (idx >= 0) {
                    // Берём несколько символов вокруг
                    int start = Math.max(0, idx - 10);
                    int end = Math.min(cardText.length(), idx + 50);
                    String snippet = cardText.substring(start, end);
                    return snippet.trim();
                }
            }
        }
        
        return null;
    }
    
    // Константы для логирования
    private static final String LOG_PARSING_START = "Разбор HTML страницы результатов...";
    private static final String LOG_EMPTY_HTML = "HTML пуст";
    private static final String LOG_CARDS_FOUND = "Найдено карточек: {}";
    private static final String LOG_PARSING_COMPLETE = "Разбор завершён. Извлечено программ: {}";
    private static final String LOG_PARSING_ERROR = "Ошибка разбора: {}";
    private static final String LOG_CARD_PARSE_ERROR = "Ошибка разбора карточки: {}";
    
    /**
     * Результат парсинга.
     */
    public static class ParseResult {
        
        private final boolean success;
        private final List<Program> programs;
        private final int programCount;
        private final boolean hasNextPage;
        private final String nextPageUrl;
        private final String errorMessage;
        
        private ParseResult(Builder builder) {
            this.success = builder.success;
            this.programs = List.copyOf(builder.programs);
            this.programCount = builder.programCount;
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
        
        public List<Program> getPrograms() {
            return programs;
        }
        
        public int getProgramCount() {
            return programCount;
        }
        
        /**
         * @return true, если на странице есть ссылка на следующую страницу
         * результатов (см. {@link ProgramParser#findNextPageUrl})
         */
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
            private List<Program> programs = new ArrayList<>();
            private int programCount;
            private boolean hasNextPage;
            private String nextPageUrl;
            private String errorMessage;
            
            public Builder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public Builder programs(List<Program> programs) {
                this.programs = new ArrayList<>(programs);
                return this;
            }
            
            public Builder programCount(int count) {
                this.programCount = count;
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
