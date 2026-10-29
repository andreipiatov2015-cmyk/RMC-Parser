package com.rmc.params;

import com.rmc.logging.AppLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Разбор карточек со страниц /params/terr/, /params/certgroup/ и
 * /params/certpotentialbalance/. Разметка у всех одна:
 * {@code <div class="column mcard" id="tr_NNN">} с зелёной угловой
 * отметкой статуса, ссылкой-заголовком и одной или несколькими таблицами
 * "показатель — значение" (у таблицы может быть заголовок в thead).
 */
public final class ParamsCardParser {

    private static final Logger logger = AppLogger.getLogger();

    /** Подпись, если на карточке нет зелёной отметки: она стоит только у действующего (текущего) года, у прошлых лет её нет. */
    public static final String NO_STATUS_MARK_TEXT = "Не действуют";

    private ParamsCardParser() {
    }

    /** Результат разбора одной страницы. */
    public record PageResult(List<ParamsCard> cards, int totalPages) {
    }

    public static PageResult parse(ParamsPage page, String html) {
        if (html == null || html.isEmpty()) {
            return new PageResult(List.of(), 1);
        }
        Document document = Jsoup.parse(html);
        List<ParamsCard> cards = new ArrayList<>();
        // "add_bank" — карточка-кнопка "Добавить", у настоящих карточек id вида tr_NNN
        for (Element card : document.select("div.mcard[id^=tr_]")) {
            ParamsCard parsed = parseCard(page, card);
            if (parsed != null) {
                cards.add(parsed);
            }
        }
        logger.info("{}: разобрано карточек: {}", page.getTitle(), cards.size());
        return new PageResult(cards, Pagination.totalPages(document));
    }

    private static ParamsCard parseCard(ParamsPage page, Element card) {
        Element link = card.selectFirst("a.strong[href]");
        if (link == null) {
            return null;
        }
        String title = link.text().trim();
        String url = link.attr("href");
        String id = card.id().replaceFirst("^tr_", "");

        String territory = title;
        String year = "";
        String group = null;
        Matcher m = page.getTitlePattern().matcher(title);
        if (m.matches()) {
            territory = m.group("terr").trim();
            year = m.group("year");
            if (page == ParamsPage.CERT_GROUP) {
                group = m.group("group").trim();
            }
        } else {
            logger.warn("Не удалось разобрать заголовок карточки: {}", title);
        }

        Element mark = card.selectFirst("p.corner.label");
        boolean active = mark != null;
        String statusText = ParamsCardParser.NO_STATUS_MARK_TEXT;
        if (mark != null) {
            String markTitle = mark.attr("title").trim();
            statusText = markTitle.isEmpty() ? "Действуют" : markTitle;
        }

        List<ParamsCard.Section> sections = new ArrayList<>();
        for (Element table : card.select("table")) {
            Element head = table.selectFirst("thead th");
            String sectionTitle = head != null ? head.text().trim() : "";
            List<ParamsCard.Row> rows = new ArrayList<>();
            for (Element tr : table.select("tbody tr")) {
                Elements cells = tr.select("> td");
                if (cells.size() < 2) {
                    continue;
                }
                rows.add(new ParamsCard.Row(cells.get(0).text().trim(), cells.get(1).text().trim()));
            }
            if (!rows.isEmpty()) {
                sections.add(new ParamsCard.Section(sectionTitle, rows));
            }
        }

        return new ParamsCard(page, id, url, title, territory, year, group, statusText, active, sections);
    }
}
