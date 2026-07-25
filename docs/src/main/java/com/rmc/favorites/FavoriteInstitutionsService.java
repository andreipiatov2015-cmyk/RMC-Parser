package com.rmc.favorites;

import com.rmc.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Постоянное хранилище "Избранных учреждений" — переживает перезапуск
 * программы и перезагрузку компьютера, как и остальные данные приложения
 * ({@code %LOCALAPPDATA%\RMCFramework}).
 */
public final class FavoriteInstitutionsService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final File FILE;
    
    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        File dir = new File(localAppData + File.separator + "RMCFramework");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FILE = new File(dir, "favorite_institutions.json");
    }
    
    private FavoriteInstitutionsService() {
        // Утилитарный класс
    }
    
    public static List<FavoriteInstitution> loadAll() {
        if (!FILE.exists()) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(FILE.toPath(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            JSONArray array = new JSONArray(content);
            List<FavoriteInstitution> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id", null);
                String name = obj.optString("name", null);
                if (id != null && !id.isEmpty()) {
                    result.add(new FavoriteInstitution(id, name != null ? name : id));
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Не удалось загрузить избранные учреждения: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public static boolean isFavorite(String id) {
        if (id == null) {
            return false;
        }
        return loadAll().stream().anyMatch(f -> id.equals(f.getId()));
    }
    
    public static void add(String id, String name) {
        if (id == null || id.isEmpty()) {
            return;
        }
        List<FavoriteInstitution> all = loadAll();
        all.removeIf(f -> id.equals(f.getId()));
        all.add(new FavoriteInstitution(id, name));
        writeAll(all);
        logger.info("Учреждение добавлено в избранное: {} ({})", name, id);
    }
    
    public static void remove(String id) {
        if (id == null) {
            return;
        }
        List<FavoriteInstitution> all = loadAll();
        boolean removed = all.removeIf(f -> id.equals(f.getId()));
        if (removed) {
            writeAll(all);
            logger.info("Учреждение удалено из избранного: {}", id);
        }
    }
    
    private static void writeAll(List<FavoriteInstitution> favorites) {
        try {
            JSONArray array = new JSONArray();
            for (FavoriteInstitution favorite : favorites) {
                JSONObject obj = new JSONObject();
                obj.put("id", favorite.getId());
                obj.put("name", favorite.getName());
                array.put(obj);
            }
            Files.writeString(FILE.toPath(), array.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Не удалось сохранить избранные учреждения: {}", e.getMessage());
        }
    }
}
