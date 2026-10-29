package com.rmc.params;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Пагинация сайта (.ui.pagination.menu со ссылками a.item, номера страниц
 * показаны явными ссылками).
 */
final class Pagination {

    private Pagination() {
    }

    /**
     * @return наибольший номер страницы среди ссылок пагинации, либо 1,
     * если пагинации нет (весь список на одной странице)
     */
    static int totalPages(Document document) {
        Element pagination = document.selectFirst(".ui.pagination.menu");
        if (pagination == null) {
            return 1;
        }
        int max = 1;
        for (Element link : pagination.select("a.item")) {
            try {
                max = Math.max(max, Integer.parseInt(link.text().trim()));
            } catch (NumberFormatException ignored) {
                // стрелки "вперёд/назад" и прочие нечисловые ссылки пропускаем
            }
        }
        return max;
    }
}
