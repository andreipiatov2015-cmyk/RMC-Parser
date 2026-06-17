package com.rmc.driver;

import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Обнаружитель Microsoft Edge WebDriver (msedgedriver.exe).
 */
public class DriverDetector {

    private static final Logger logger = AppLogger.getLogger();

    private static final String DRIVER_EXECUTABLE = "msedgedriver.exe";
    private static final String DRIVER_SUBDIR = "drivers" + File.separator + "edge";

    private static final List<Path> DRIVER_SEARCH_PATHS = new ArrayList<>();

    static {
        // Директория приложения drivers/edge/
        String appDir = System.getProperty("user.dir");
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, DRIVER_SUBDIR, DRIVER_EXECUTABLE));
        // Альтернативные расположения
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, "drivers", DRIVER_EXECUTABLE));
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, "driver", DRIVER_EXECUTABLE));
        DRIVER_SEARCH_PATHS.add(Paths.get(appDir, DRIVER_EXECUTABLE));
    }

    private DriverDetector() {
        // Утилитарный класс
    }

    /**
     * Обнаружить установку Microsoft Edge WebDriver.
     *
     * @return DriverInfo с путём, версией и статусом установки
     */
    public static DriverInfo detect() {
        logger.info(Messages.LOG_SEARCHING_DRIVER);

        for (Path searchPath : DRIVER_SEARCH_PATHS) {
            File driverFile = searchPath.toFile();
            logger.debug(Messages.LOG_CHECKING_PATH, searchPath);

            if (driverFile.exists() && driverFile.isFile()) {
                String version = getFileVersion(searchPath.toString());
                if (version != null) {
                    logger.info(Messages.LOG_FOUND_DRIVER);
                    logger.info("Путь: {}", searchPath);
                    logger.info("Версия: {}", version);
                    return new DriverInfo(searchPath.toString(), version, true);
                }
            }
        }

        logger.warn(Messages.LOG_NOT_FOUND);
        return DriverInfo.notInstalled();
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
     * Получить все пути поиска для обнаружения драйвера.
     *
     * @return Список путей для поиска
     */
    public static List<Path> getSearchPaths() {
        return new ArrayList<>(DRIVER_SEARCH_PATHS);
    }
}