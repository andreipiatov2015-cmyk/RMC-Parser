package com.rmc.params;

import com.rmc.export.ParamsExportService;
import com.rmc.search.service.SearchResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerritoryPricesTest {
    @TempDir Path directory;

    private String card(int id) {
        return "<div class='mcard' id='tr_" + id + "'><a class='strong' href='/params/terr/" + id
                + "/'>Параметры территории Округ " + id + " для 2026</a><table><tbody>"
                + "<tr><td>Естественно-научная</td><td>88</td></tr></tbody></table></div>";
    }

    @Test
    void followsEveryTerritoryAndExportsDistinctCosts() throws Exception {
        List<String> paths = new ArrayList<>();
        var service = new ParamsAnalysisService(url -> {
            String path = java.net.URI.create(url).getPath();
            paths.add(path);
            String html = switch (path) {
                case "/params/terr/" -> card(262) + card(273);
                case "/params/terr/262/", "/params/terr/273/" -> {
                    var form = new StringBuilder("<form>");
                    int i = path.contains("262") ? 10 : 20;
                    for (String field : TerritoryPrices.FIELDS.keySet()) {
                        form.append("<input name='").append(field).append("' value='").append(++i).append(",5'>");
                    }
                    yield form.append("</form>").toString();
                }
                default -> "<html></html>";
            };
            return SearchResult.builder().success(true).statusCode(200).html(html).build();
        });
        var result = service.analyze(Map.of(), null, () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage().orElse(""));
        assertEquals(5, paths.size());
        var table = ParamsTable.from(result);
        assertEquals(2, table.getRows().size());
        var file = directory.resolve("params.xlsx").toFile();
        ParamsExportService.export(result, file);
        try (var book = new XSSFWorkbook(file)) {
            for (int i = 0; i < 2; i++) {
                int ordinal = 0;
                for (var label : TerritoryPrices.FIELDS.values()) {
                    var column = new ParamsTable.Column(TerritoryPrices.TITLE, label, ParamsPage.TERR);
                    int index = ParamsTable.COLUMNS.indexOf(column);
                    double expected = (i + 1) * 10 + ++ordinal + 0.5;
                    assertEquals(expected, Double.parseDouble(table.getRows().get(i).values().get(index).replace(',', '.')));
                    for (String sheetName : List.of("Сводка", "Муниципалитет")) {
                        Sheet sheet = book.getSheet(sheetName);
                        int col = headerColumn(sheet, TerritoryPrices.TITLE + " / " + label);
                        assertEquals(expected, sheet.getRow(i + 1).getCell(col).getNumericCellValue());
                    }
                }
                var kit = ParamsTable.COLUMNS.stream().filter(c -> c.label().equals("Естественно-научная")
                        && !c.group().equals(TerritoryPrices.TITLE)).findFirst().orElseThrow();
                assertEquals("88", table.getRows().get(i).values().get(ParamsTable.COLUMNS.indexOf(kit)));
            }
        }
    }

    private int headerColumn(Sheet sheet, String name) {
        for (var cell : sheet.getRow(0)) {
            if (name.equals(cell.getStringCellValue())) return cell.getColumnIndex();
        }
        throw new AssertionError("Нет колонки " + name);
    }

    @Test
    void emptyAndZeroAreDistinctAndLoginIsRejected() {
        var section = TerritoryPrices.parse("<input name=price_mh_art value=''><input name=price_mh_sport value=0>");
        assertEquals(List.of(new ParamsCard.Row("Физкультурно-спортивная", "0"),
                new ParamsCard.Row("Художественная", "")), section.rows());
        assertThrows(IllegalStateException.class, () -> TerritoryPrices.parse("<input type=password>"));
    }
}
