package com.rmc.driver;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Обнаружитель установки браузера Microsoft Edge.
 */
public class EdgeDetector {

    private static final Logger logger = AppLogger.getLogger();

    private static final String EDGE_EXECUTABLE = "msedge.exe";

    private static final List<Path> EDGE_SEARCH_PATHS = new ArrayList<>();

    static {
        // Стандартные пути установки Windows
        EDGE_SEARCH_PATHS.add(Paths.get("C:\\Program Files\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
        EDGE_SEARCH_PATHS.add(Paths.get("C:\\Program Files (x86)\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
        // Добавляем дополнительные пути при необходимости
        EDGE_SEARCH_PATHS.add(Paths.get(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\Application", EDGE_EXECUTABLE));
    }

    private EdgeDetector() {
        // Утилитарный класс
    }

    /**
     * Обнаружить установку Microsoft Edge.
     *
     * @return EdgeInfo с путём, версией и статусом установки
     */
    public static EdgeInfo detect() {
        logger.info("Поиск Microsoft Edge...");

        for (Path searchPath : EDGE_SEARCH_PATHS) {
            File edgeFile = searchPath.toFile();
            logger.debug("Проверка пути: {}", searchPath);

            if (edgeFile.exists() && edgeFile.isFile()) {
                String version = getFileVersion(searchPath.toString());
                if (version != null) {
                    logger.info("Microsoft Edge найден");
                    logger.info("Путь: {}", searchPath);
                    logger.info("Версия: {}", version);
                    return new EdgeInfo(searchPath.toString(), version, true);
                }
            }
        }

        logger.warn("Microsoft Edge не найден");
        return EdgeInfo.notInstalled();
    }

    /**
     * Получить версию файла через Windows FileVersion API через PowerShell.
     * НЕ выполняет целевое приложение.
     *
     * @param filePath Путь к файлу
     * @return Строка версии или null, если не удалось получить
     */
    private static String getFileVersion(String filePath) {
        try {
            String powershellCommand = String.format(
                "(Get-Item '%s').VersionInfo.FileVersion",
                filePath.replace("'", "''")
            );

            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", 
                "-Command", powershellCommand
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String version = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode == 0 && !version.isEmpty()) {
                return version;
            }
        } catch (Exception e) {
            logger.debug("Не удалось получить версию файла {}: {}", filePath, e.getMessage());
        }
        return null;
    }

    /**
     * Получить все пути поиска для обнаружения Edge.
     *
     * @return Список путей для поиска
     */
    public static List<Path> getSearchPaths() {
        return new ArrayList<>(EDGE_SEARCH_PATHS);
    }
}