package com.rmc.export;

import com.rmc.logging.AppLogger;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Экспорт результата анализа в Excel (.xlsx).
 *
 * <p>Формирует два листа:</p>
 * <ul>
 *   <li>"Итого" — сводка и суммарные показатели по всем учреждениям;</li>
 *   <li>"По учреждениям" — построчная разбивка. Набор колонок-показателей
 *       не хардкодится, а собирается из того, что реально встретилось у
 *       учреждений — так же, как парсер сам подстраивается под сайт.</li>
 * </ul>
 */
public class ExportService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private ExportService() {
        // Утилитарный класс
    }
    
    public static void exportToExcel(AnalysisResult result, File file) throws IOException {
        logger.info("Экспорт результатов анализа в Excel: {}", file.getAbsolutePath());
        
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            writeSummarySheet(workbook, headerStyle, result);
            writeInstitutionsSheet(workbook, headerStyle, result);
            
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        }
        
        logger.info("Экспорт завершён: {}", file.getAbsolutePath());
    }
    
    private static void writeSummarySheet(Workbook workbook, CellStyle headerStyle, AnalysisResult result) {
        Sheet sheet = workbook.createSheet("Итого");
        int rowIdx = 0;
        
        Row header = sheet.createRow(rowIdx++);
        setCell(header, 0, "Показатель", headerStyle);
        setCell(header, 1, "Значение", headerStyle);
        
        Row programsRow = sheet.createRow(rowIdx++);
        setCell(programsRow, 0, "Найдено программ");
        setCell(programsRow, 1, result.getTotalPrograms());
        
        Row institutionsRow = sheet.createRow(rowIdx++);
        setCell(institutionsRow, 0, "Учреждений обработано");
        setCell(institutionsRow, 1, result.getTotalInstitutions());
        
        for (Map.Entry<String, Integer> entry : result.getTotals().entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            setCell(row, 0, entry.getKey());
            setCell(row, 1, entry.getValue());
        }
        
        autoSizeColumns(sheet, 2);
    }
    
    private static void writeInstitutionsSheet(Workbook workbook, CellStyle headerStyle, AnalysisResult result) {
        Sheet sheet = workbook.createSheet("По учреждениям");
        CreationHelper creationHelper = workbook.getCreationHelper();
        CellStyle linkStyle = createLinkStyle(workbook);
        
        // Набор колонок-показателей собираем из того, что реально есть у
        // учреждений — если у одних показателей больше/меньше, чем у
        // других, таблица всё равно останется согласованной.
        Set<String> statColumns = new LinkedHashSet<>();
        for (InstitutionAnalysis institution : result.getInstitutions()) {
            statColumns.addAll(institution.getStats().keySet());
        }
        List<String> columns = new ArrayList<>(statColumns);
        
        int rowIdx = 0;
        Row header = sheet.createRow(rowIdx++);
        int col = 0;
        setCell(header, col++, "Учреждение", headerStyle);
        setCell(header, col++, "Ссылка", headerStyle);
        for (String statName : columns) {
            setCell(header, col++, statName, headerStyle);
        }
        setCell(header, col, "Примечание", headerStyle);
        
        for (InstitutionAnalysis institution : result.getInstitutions()) {
            Row row = sheet.createRow(rowIdx++);
            col = 0;
            
            String name = institution.getOrganizationName() != null
                    ? institution.getOrganizationName()
                    : institution.getOrganizationId();
            setCell(row, col++, name);
            
            Optional<String> url = institution.getOrganizationUrl();
            if (url.isPresent()) {
                Cell linkCell = row.createCell(col++);
                linkCell.setCellValue(url.get());
                Hyperlink link = creationHelper.createHyperlink(HyperlinkType.URL);
                link.setAddress(url.get());
                linkCell.setHyperlink(link);
                linkCell.setCellStyle(linkStyle);
            } else {
                setCell(row, col++, "");
            }
            
            for (String statName : columns) {
                if (institution.isSuccess()) {
                    Integer value = institution.getStats().get(statName);
                    setCell(row, col++, value != null ? value : 0);
                } else {
                    setCell(row, col++, "");
                }
            }
            
            setCell(row, col, institution.isSuccess()
                    ? ""
                    : "Ошибка: " + institution.getErrorMessage().orElse("не удалось получить данные"));
        }
        
        autoSizeColumns(sheet, columns.size() + 3);
    }
    
    private static CellStyle createLinkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    private static void setCell(Row row, int col, String value) {
        setCell(row, col, value, null, null);
    }
    
    private static void setCell(Row row, int col, int value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
    }
    
    private static void setCell(Row row, int col, String textValue, CellStyle style) {
        setCell(row, col, textValue, style, null);
    }
    
    private static void setCell(Row row, int col, String textValue, CellStyle style, Integer numericValue) {
        Cell cell = row.createCell(col);
        if (numericValue != null) {
            cell.setCellValue(numericValue);
        } else if (textValue != null) {
            cell.setCellValue(textValue);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    
    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
