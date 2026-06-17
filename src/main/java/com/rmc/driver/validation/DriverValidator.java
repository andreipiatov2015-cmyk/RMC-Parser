package com.rmc.driver.validation;

import com.rmc.driver.system.Architecture;
import com.rmc.driver.system.Platform;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Проверяет установленный Microsoft Edge WebDriver.
 * 
 * <p>Проверяет:
 * <ul>
 *   <li>Драйвер существует по ожидаемому пути</li>
 *   <li>Исполняемый файл существует и доступен для чтения</li>
 *   <li>FileVersion соответствует ожидаемой версии</li>
 *   <li>Платформа и архитектура корректны</li>
 * </ul></p>
 */
public class DriverValidator {

    private static final Logger logger = AppLogger.getLogger();

    private DriverValidator() {
        // Утилитарный класс
    }

    /**
     * Проверить установленный драйвер относительно ожидаемой версии браузера.
     *
     * @param driverPath Путь к исполняемому файлу драйвера
     * @param browserVersion Ожидаемая версия браузера
     * @param expectedDriverVersion Ожидаемая версия драйвера
     * @return ValidationResult с манифестом и статусом
     */
    public static ValidationResult validate(String driverPath, String browserVersion, String expectedDriverVersion) {
        return validate(Paths.get(driverPath), browserVersion, expectedDriverVersion);
    }

    /**
     * Проверить установленный драйвер относительно ожидаемой версии браузера.
     *
     * @param driverPath Путь к исполняемому файлу драйвера
     * @param browserVersion Ожидаемая версия браузера
     * @param expectedDriverVersion Ожидаемая версия драйвера
     * @return ValidationResult с манифестом и статусом
     */
    public static ValidationResult validate(Path driverPath, String browserVersion, String expectedDriverVersion) {
        logger.info(Messages.LOG_VALIDATION_SERVICE);

        // Определяем платформу и архитектуру
        Platform platform = Platform.detect();
        Architecture architecture = Architecture.detect();
        
        logger.info("Путь к драйверу: {}", driverPath);
        logger.info(Messages.LOG_BROWSER_VERSION, browserVersion);
        logger.info(Messages.LOG_EXPECTED_VERSION, expectedDriverVersion);
        logger.info(Messages.LOG_PLATFORM, platform);
        logger.info(Messages.LOG_ARCHITECTURE, architecture);

        // Build base manifest
        DriverManifest.Builder manifestBuilder = DriverManifest.builder()
                .browserVersion(browserVersion)
                .driverVersion(expectedDriverVersion)
                .driverPath(driverPath.toString())
                .platform(platform)
                .architecture(architecture)
                .validationTime(LocalDateTime.now());

        // Проверяем существование драйвера
        if (!Files.exists(driverPath)) {
            logger.info(Messages.LOG_DRIVER_NOT_EXISTS);
            logger.info(Messages.LOG_VALIDATION_NOT_FOUND);
            logger.info(Messages.LOG_VALIDATION_END);
            return ValidationResult.failure(
                    ValidationStatus.MISSING,
                    ValidationStatus.MISSING.getDescription()
            );
        }

        logger.info(Messages.LOG_DRIVER_EXISTS);

        // Проверяем, что это файл, а не директория
        if (!Files.isRegularFile(driverPath)) {
            logger.info("Проверка: ОШИБКА - Путь не является файлом");
            logger.info(Messages.LOG_VALIDATION_END);
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Путь к драйверу не является файлом"
            );
        }

        // Check if executable
        if (!Files.isReadable(driverPath)) {
            logger.info("Проверка: ОШИБКА - Файл не доступен для чтения");
            logger.info(Messages.LOG_VALIDATION_END);
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Файл драйвера не доступен для чтения"
            );
        }

        // Read driver version
        String actualVersion = readDriverVersion(driverPath);
        
        if (actualVersion == null) {
            logger.info(Messages.LOG_DRIVER_VERSION_UNKNOWN);
            logger.info("Проверка: ОШИБКА - Не удалось прочитать версию");
            logger.info(Messages.LOG_VALIDATION_END);
            return ValidationResult.failure(
                    ValidationStatus.UNKNOWN,
                    "Не удалось прочитать версию драйвера"
            );
        }

        logger.info(Messages.LOG_DRIVER_VERSION_FOUND, actualVersion);
        logger.info("Ожидаемая: {}", expectedDriverVersion);

        // Normalize versions for comparison
        String normalizedActual = normalizeVersion(actualVersion);
        String normalizedExpected = normalizeVersion(expectedDriverVersion);

        // Compare versions
        boolean versionsMatch = normalizedActual.equals(normalizedExpected);

        logger.info("Версия совпадает: {}", versionsMatch ? "ДА" : "НЕТ");

        if (!versionsMatch) {
            logger.info(Messages.LOG_VERSION_MISMATCH);
            logger.info("=================================================");
            DriverManifest manifest = manifestBuilder
                    .driverVersion(actualVersion)
                    .status(ValidationStatus.VERSION_MISMATCH)
                    .build();
            return ValidationResult.versionMismatch(manifest, normalizedExpected, normalizedActual);
        }

        // Validate platform
        if (platform != Platform.WINDOWS) {
            logger.info("Проверка платформы: ОШИБКА - Поддерживается только Windows");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Поддерживается только платформа Windows"
            );
        }

        // Validate architecture
        if (!architecture.isSupported()) {
            logger.info("Проверка архитектуры: ОШИБКА - Архитектура не поддерживается");
            logger.info("=================================================");
            return ValidationResult.failure(
                    ValidationStatus.INVALID,
                    "Неподдерживаемая архитектура: " + architecture
            );
        }

        // All checks passed
        logger.info(Messages.LOG_VALIDATION_PASS);
        logger.info("=================================================");

        DriverManifest manifest = manifestBuilder
                .driverVersion(actualVersion)
                .status(ValidationStatus.VALID)
                .build();

        return ValidationResult.success(manifest);
    }

    /**
     * Quick validation - just check if driver exists.
     *
     * @param driverPath Путь для проверки
     * @return true если существует, иначе false
     */
    public static boolean exists(Path driverPath) {
        return Files.exists(driverPath) && Files.isRegularFile(driverPath);
    }

    /**
     * Читает версию файла драйвера через PowerShell.
     */
    private static String readDriverVersion(Path driverPath) {
        try {
            String command = String.format(
                    "powershell -Command \"(Get-Item '%s').VersionInfo.FileVersion\"",
                    driverPath.toAbsolutePath().toString().replace("'", "''")
            );

            Process process = Runtime.getRuntime().exec(command);
            String version = new String(process.getInputStream().readAllBytes()).trim();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes()).trim();
                logger.warn("Не удалось прочитать версию драйвера: {}", error);
                return null;
            }

            if (version.isEmpty()) {
                logger.warn("Получена пустая версия");
                return null;
            }

            return version;

        } catch (IOException | InterruptedException e) {
            logger.warn("Ошибка чтения версии драйвера", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Нормализует строку версии в формат major.minor.build.
     */
    static String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }

        // Обрабатываем шаблоны версий типа "146.0.3856" или "146.0.3856.109"
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return version;
    }
}