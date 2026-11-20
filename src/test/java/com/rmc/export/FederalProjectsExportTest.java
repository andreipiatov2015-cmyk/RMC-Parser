package com.rmc.export;

import com.rmc.parser.ProgramDetailParser;
import com.rmc.search.service.ProgramAnalysisService;
import com.sun.net.httpserver.HttpServer;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FederalProjectsExportTest {
    @TempDir Path directory;

    @Test
    void exportsMembershipPerProgramWithoutApplyingTheProjectFilter() throws Exception {
        String registry = """
                <form id="modern_filter_form"><select name="federal_project__in" multiple>
                    <option value="0">Нет</option><option value="1">Кванториум 2023</option>
                    <option value="2">Точка роста 2024</option><option value="3">IT-куб 2024</option>
                </select></form>
                <table><tbody>
                """ + row(1, "Кванториум 2023") + row(2, "Точка роста 2024")
                    + row(3, "Нет") + row(4, "") + row(5, "IT-куб 2024") + "</tbody></table>";
        Map<String, String> pages = Map.of(
                "/programs/", registry,
                "/org/1/", "<h1>Учреждение</h1>",
                "/programs/1/", "<h1>Программа 1</h1>",
                "/programs/2/", badge("Точка роста 2024") + badge("IT-куб 2024") + badge("Очная"),
                "/programs/3/", "<div class='menu'>" + badge("Кванториум 2023") + "</div>",
                "/programs/4/", badge("Кванториум 2023"));
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String html = pages.get(exchange.getRequestURI().toString());
            int status = html == null ? 404 : 200;
            byte[] body = (html == null ? "Страница не найдена" : html).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            var service = ProgramAnalysisService.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            var result = service.analyze("", null, () -> false);
            assertTrue(result.isSuccess(), result.getErrorMessage().orElse(""));
            var report = directory.resolve("registry.xlsx").toFile();
            ExportService.exportToExcel(result, report, Set.of());

            try (var workbook = new XSSFWorkbook(report)) {
                var sheet = workbook.getSheet("По программам");
                Map<String, Integer> columns = new HashMap<>();
                for (var cell : sheet.getRow(0)) {
                    columns.put(cell.getStringCellValue(), cell.getColumnIndex());
                }
                int projects = columns.get("Федеральные проекты");
                Map<String, Row> rows = new HashMap<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    var row = sheet.getRow(i);
                    rows.put(row.getCell(columns.get("Программа")).getStringCellValue(), row);
                }
                assertEquals(5, rows.size());
                assertEquals("Кванториум 2023", rows.get("Программа 1").getCell(projects).getStringCellValue());
                assertEquals("Точка роста 2024, IT-куб 2024", rows.get("Программа 2").getCell(projects).getStringCellValue());
                assertEquals(CellType.BLANK, rows.get("Программа 3").getCell(projects).getCellType());
                assertEquals("Кванториум 2023", rows.get("Программа 4").getCell(projects).getStringCellValue());
                Row unavailable = rows.get("Программа 5");
                assertEquals("IT-куб 2024", unavailable.getCell(projects).getStringCellValue());
                assertTrue(unavailable.getCell(columns.get("Примечание")).getStringCellValue().contains("Ошибка"));
            }
            assertEquals(7, requests.get(), "Один список, одно учреждение и пять страниц программ");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void recognizesTheSuppliedLabelWithoutAFilterForm() {
        var detail = ProgramDetailParser.parse("""
                <div class="left ui label">Кванториум 2023</div>
                <div class="ui label">Справочная информация</div>
                """);
        assertEquals(List.of("Кванториум 2023"), detail.getFederalProjects());
    }

    private static String badge(String name) {
        return "<div class='left ui label'>" + name + "</div>";
    }

    private static String row(int id, String project) {
        return "<tr id='obj" + id + "'><td>" + badge(project)
                + "<div class='menu'>Изменить на:" + badge("IT-куб 2024") + "</div></td>"
                + "<td><a href='/programs/" + id + "/'>Программа " + id + "</a></td>"
                + "<td><p class='strong hide_on_desktop'>Учреждение</p>"
                + "<a href='/org/1/'>Учреждение</a></td></tr>";
    }
}
