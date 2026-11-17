package com.rmc.params;

import com.rmc.config.ServerConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class TerritoryPrices {
    static final String TITLE = "Стоимость 1 человеко-час";
    static final Map<String, String> FIELDS;
    static {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("price_mh_tech", "Техническая");
        fields.put("price_mh_sci", "Естественно-научная");
        fields.put("price_mh_sport", "Физкультурно-спортивная");
        fields.put("price_mh_art", "Художественная");
        fields.put("price_mh_turism", "Туристско-краеведческая");
        fields.put("price_mh_soc", "Социально-гуманитарная");
        FIELDS = Collections.unmodifiableMap(fields);
    }

    static String detailUrl(ParamsCard card) {
        URI base = URI.create(ServerConfig.BASE_URL);
        URI detail = base.resolve("/params/terr/").resolve(card.getUrl().orElseThrow());
        if (!base.getScheme().equals(detail.getScheme()) || !base.getAuthority().equals(detail.getAuthority())
                || !detail.getRawPath().matches("/params/terr/[0-9]+/?")) {
            throw new IllegalArgumentException("Некорректная ссылка на параметры муниципалитета");
        }
        return detail.toString();
    }

    static ParamsCard.Section parse(String html) {
        var document = Jsoup.parse(html);
        if (document.selectFirst("input[type=password]") != null) {
            throw new IllegalStateException("Сессия завершена. Войдите в РМЦ повторно.");
        }
        var rows = new ArrayList<ParamsCard.Row>();
        for (var field : FIELDS.entrySet()) {
            Element input = document.selectFirst("input[name=" + field.getKey() + "]");
            if (input != null) rows.add(new ParamsCard.Row(field.getValue(), input.val().trim()));
        }
        if (rows.isEmpty() && document.selectFirst("input[name=basic_need], input[name=count_cert]") == null) {
            throw new IllegalStateException("На странице не найдена форма параметров муниципалитета");
        }
        return new ParamsCard.Section(TITLE, rows);
    }

    private TerritoryPrices() {}
}
