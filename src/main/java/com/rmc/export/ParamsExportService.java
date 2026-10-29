package com.rmc.export;

import com.rmc.config.ServerConfig;
import com.rmc.logging.AppLogger;
import com.rmc.params.ParamsAnalysisResult;
import com.rmc.params.ParamsCard;
import com.rmc.params.ParamsPage;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Экспорт раздела "Параметры РМЦ" в Excel: лист "Сводка" (что есть по
 * каждой территории за год) и по листу на каждую из трёх страниц сайта —
 * одна строка на карточку, показатели карточек — колонками.
 */
public final class ParamsExportService {

    private static final Logger logger = AppLogger.getLogger();
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private ParamsExportService() {
    }

    public static void export(ParamsAnalysisResult result, File file) throws IOException {
        logger.info("Экспорт параметров РМЦ в Excel: {}", file.getAbsolutePath());
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = headerStyle(workbook);
            CellStyle link = linkStyle(workbook);

            writeSummary(workbook, header, result);
            for (ParamsPage page : ParamsPage.values()) {
                writePage(workbook, header, link, page, result.getCards(page));
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
        logger.info("Экспорт завершён: {}", file.getAbsolutePath());
    }

    private static void writeSummary(Workbook workbook, CellStyle header, ParamsAnalysisResult result) {
        Sheet sheet = workbook.createSheet("Сводка");
        Row head = sheet.createRow(0);
        text(head, 0, "Территория", header);
        text(head, 1, "Год", header);
        int col = 2;
        for (ParamsPage page : ParamsPage.values()) {
            text(head, col++, page.getTitle() + " (карточек)", header);
        }
        int rowIdx = 1;
        for (ParamsAnalysisResult.TerritoryYear ty : result.getTerritoryYears()) {
            Row row = sheet.createRow(rowIdx++);
            text(row, 0, ty.getTerritory(), null);
            text(row, 1, ty.getYear(), null);
            int c = 2;
            for (ParamsPage page : ParamsPage.values()) {
                row.createCell(c++).setCellValue(ty.getCards(page).size());
            }
        }
        for (int i = 0; i < 2 + ParamsPage.values().length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void writePage(Workbook workbook, CellStyle header, CellStyle link, ParamsPage page,
                                  List<ParamsCard> cards) {
        Sheet sheet = workbook.createSheet(page.getSheetName());
        CreationHelper helper = workbook.getCreationHelper();

        // Колонки-показатели — из того, что реально встретилось в карточках.
        Set<String> metricColumns = new LinkedHashSet<>();
        for (ParamsCard card : cards) {
            for (ParamsCard.Section section : card.getSections()) {
                for (ParamsCard.Row r : section.rows()) {
                    metricColumns.add(columnKey(section, r));
                }
            }
        }
        List<String> metrics = new ArrayList<>(metricColumns);

        boolean withGroup = page == ParamsPage.CERT_GROUP;
        Row head = sheet.createRow(0);
        int col = 0;
        text(head, col++, "Территория", header);
        text(head, col++, "Год", header);
        if (withGroup) {
            text(head, col++, "Группа сертификата", header);
        }
        text(head, col++, "Статус", header);
        text(head, col++, "Ссылка", header);
        int firstMetric = col;
        for (String metric : metrics) {
            text(head, col++, metric, header);
        }

        int rowIdx = 1;
        for (ParamsCard card : cards) {
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            text(row, c++, card.getTerritory(), null);
            text(row, c++, card.getYear(), null);
            if (withGroup) {
                text(row, c++, card.getGroupName().orElse(""), null);
            }
            text(row, c++, card.getStatusText(), null);
            if (card.getUrl().isPresent()) {
                String url = absolute(card.getUrl().get());
                Cell cell = row.createCell(c++);
                cell.setCellValue(url);
                Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(url);
                cell.setHyperlink(hyperlink);
                cell.setCellStyle(link);
            } else {
                c++;
            }
            for (ParamsCard.Section section : card.getSections()) {
                for (ParamsCard.Row r : section.rows()) {
                    int index = metrics.indexOf(columnKey(section, r));
                    if (index >= 0) {
                        value(row.createCell(firstMetric + index), r.value());
                    }
                }
            }
        }

        for (int i = 0; i < firstMetric + metrics.size(); i++) {
            sheet.autoSizeColumn(i);
            // Подписи показателей длинные — не растягиваем колонки без меры.
            if (sheet.getColumnWidth(i) > 60 * 256) {
                sheet.setColumnWidth(i, 60 * 256);
            }
        }
    }

    private static String columnKey(ParamsCard.Section section, ParamsCard.Row row) {
        return section.title().isEmpty() ? row.label() : section.title() + " / " + row.label();
    }

    /** Число пишем числом ("13 800", "2612,40"), всё остальное ("1 / 1 / 1", "") — текстом. */
    private static void value(Cell cell, String raw) {
        String normalized = raw.replaceAll("[\\s\\u00A0\\u202F]", "").replace(',', '.');
        if (NUMBER.matcher(normalized).matches()) {
            cell.setCellValue(Double.parseDouble(normalized));
        } else {
            cell.setCellValue(raw);
        }
    }

    private static String absolute(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return ServerConfig.BASE_URL + (url.startsWith("/") ? url : "/" + url);
    }

    private static void text(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle linkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);
        return style;
    }
}
