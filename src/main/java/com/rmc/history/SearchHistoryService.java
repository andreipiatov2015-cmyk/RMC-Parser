package com.rmc.history;

import com.rmc.history.model.SearchHistoryEntry;
import com.rmc.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Хранение истории поиска на диске (JSON-файл), между запусками программы.
 *
 * <p>Каталог — тот же, что и у логов ({@code %LOCALAPPDATA%\RMCFramework}),
 * подпапка "history".</p>
 */
public class SearchHistoryService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_ENTRIES = 50;
    
    private static final File HISTORY_FILE;
    
    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        File historyDir = new File(localAppData + File.separator + "RMCFramework" + File.separator + "history");
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }
        HISTORY_FILE = new File(historyDir, "search_history.json");
    }
    
    private SearchHistoryService() {
        // Утилитарный класс
    }
    
    /**
     * Загрузить всю сохранённую историю, от новой записи к старой.
     * При отсутствии или повреждении файла возвращает пустой список —
     * никогда не бросает исключение наружу.
     */
    public static List<SearchHistoryEntry> loadAll() {
        if (!HISTORY_FILE.exists()) {
            return new ArrayList<>();
        }
        
        try {
            String content = Files.readString(HISTORY_FILE.toPath(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            
            JSONArray array = new JSONArray(content);
            List<SearchHistoryEntry> entries = new ArrayList<>();
            
            for (int i = 0; i < array.length(); i++) {
                SearchHistoryEntry entry = fromJson(array.getJSONObject(i));
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
            return entries;
            
        } catch (Exception e) {
            logger.error("Не удалось загрузить историю поиска: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Добавить запись в начало истории и сохранить на диск. Хранится не
     * больше {@link #MAX_ENTRIES} последних записей.
     *
     * @return сохранённая запись (с присвоенным id и текущим временем)
     */
    public static SearchHistoryEntry add(String queryString, List<String> filterSummary,
                                          Integer totalPrograms, Integer totalInstitutions) {
        SearchHistoryEntry entry = SearchHistoryEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .queryString(queryString != null ? queryString : "")
                .filterSummary(filterSummary != null ? filterSummary : List.of())
                .totalPrograms(totalPrograms)
                .totalInstitutions(totalInstitutions)
                .build();
        
        List<SearchHistoryEntry> entries = loadAll();
        entries.add(0, entry);
        
        if (entries.size() > MAX_ENTRIES) {
            entries = entries.subList(0, MAX_ENTRIES);
        }
        
        writeAll(entries);
        return entry;
    }
    
    /**
     * Удалить одну запись по id.
     */
    public static void remove(String id) {
        List<SearchHistoryEntry> entries = loadAll();
        entries.removeIf(e -> e.getId().equals(id));
        writeAll(entries);
    }
    
    /**
     * Полностью очистить историю.
     */
    public static void clear() {
        writeAll(Collections.emptyList());
    }
    
    private static void writeAll(List<SearchHistoryEntry> entries) {
        try {
            JSONArray array = new JSONArray();
            for (SearchHistoryEntry entry : entries) {
                array.put(toJson(entry));
            }
            Files.writeString(HISTORY_FILE.toPath(), array.toString(2), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Не удалось сохранить историю поиска: {}", e.getMessage());
        }
    }
    
    private static JSONObject toJson(SearchHistoryEntry entry) {
        JSONObject json = new JSONObject();
        json.put("id", entry.getId());
        json.put("timestamp", entry.getTimestamp().format(TIMESTAMP_FORMAT));
        json.put("queryString", entry.getQueryString());
        json.put("filterSummary", new JSONArray(entry.getFilterSummary()));
        if (entry.getTotalPrograms() != null) {
            json.put("totalPrograms", entry.getTotalPrograms());
        }
        if (entry.getTotalInstitutions() != null) {
            json.put("totalInstitutions", entry.getTotalInstitutions());
        }
        return json;
    }
    
    private static SearchHistoryEntry fromJson(JSONObject json) {
        try {
            List<String> summary = new ArrayList<>();
            JSONArray summaryArray = json.optJSONArray("filterSummary");
            if (summaryArray != null) {
                for (int i = 0; i < summaryArray.length(); i++) {
                    summary.add(summaryArray.getString(i));
                }
            }
            
            return SearchHistoryEntry.builder()
                    .id(json.optString("id", UUID.randomUUID().toString()))
                    .timestamp(LocalDateTime.parse(json.getString("timestamp"), TIMESTAMP_FORMAT))
                    .queryString(json.optString("queryString", ""))
                    .filterSummary(summary)
                    .totalPrograms(json.has("totalPrograms") ? json.getInt("totalPrograms") : null)
                    .totalInstitutions(json.has("totalInstitutions") ? json.getInt("totalInstitutions") : null)
                    .build();
        } catch (Exception e) {
            logger.warn("Пропущена повреждённая запись истории: {}", e.getMessage());
            return null;
        }
    }
}
