package com.rmc.app;

import com.rmc.config.UpdateConfig;
import com.rmc.download.DownloadResult;
import com.rmc.download.DownloadService;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverService;
import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.DriverInfo;
import com.rmc.driver.DriverStatus;
import com.rmc.driver.validation.DriverManifest;
import com.rmc.driver.validation.DriverValidator;
import com.rmc.driver.validation.ValidationResult;
import com.rmc.driver.validation.ValidationStatus;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Управляет жизненным циклом приложения от запуска до завершения.
 * 
 * <p>Последовательность запуска:</p>
 * <pre>
 * Запуск приложения
 *       ↓
 * Загрузка конфигурации
 *       ↓
 * Инициализация логирования (уже выполнена)
 *       ↓
 * Загрузка модуля версий
 *       ↓
 * Проверка обновлений
 *       ↓
 * Обнаружение драйвера
 *       ↓
 * Проверка драйвера
 *       ↓
 * Приложение готово
 * </pre>
 */
public class ApplicationLifecycle {

    private static final Logger logger = AppLogger.getLogger();

    private LifecycleState currentState;
    private LifecycleReport report;

    public ApplicationLifecycle() {
        this.currentState = LifecycleState.NOT_STARTED;
        this.report = new LifecycleReport();
    }

    /**
     * Выполнить полную последовательность запуска.
     *
     * @return LifecycleReport с результатами каждого шага
     */
    public LifecycleReport start() {
        logger.info(Messages.LOG_LIFECYCLE_START);

        try {
            // Шаг 1: Загрузка конфигурации
            step1_LoadConfiguration();

            // Шаг 2: Инициализация логирования (уже выполнена, просто логируем)
            step2_InitializeLogging();

            // Шаг 3: Загрузка модуля версий
            step3_LoadVersionEngine();

            // Шаг 4: Проверка обновлений
            step4_CheckUpdates();

            // Шаг 5: Обнаружение драйвера
            step5_DriverDetection();

            // Шаг 6: Проверка драйвера
            step6_DriverValidation();

            // Шаг 7: Приложение готово
            step7_ApplicationReady();

            logger.info(Messages.LOG_LIFECYCLE_COMPLETE);

        } catch (Exception e) {
            logger.error(Messages.LOG_LIFECYCLE_FAILED, e);
            report.setError(e.getMessage());
            currentState = LifecycleState.FAILED;
        }

        return report;
    }

    private void step1_LoadConfiguration() {
        currentState = LifecycleState.LOADING_CONFIG;
        logger.info(Messages.LOG_STEP_CONFIG);

        try {
            UpdateConfig config = UpdateConfig.load();
            String jsonUrl = config.getJsonUrl();
            String channel = config.getChannel();
            
            // Извлекаем owner/repo из JSON URL
            String owner = "неизвестно";
            String repo = "неизвестно";
            if (jsonUrl != null && jsonUrl.contains("github.com")) {
                String[] parts = jsonUrl.split("github.com/");
                if (parts.length > 1) {
                    String[] repoParts = parts[1].split("/");
                    if (repoParts.length >= 2) {
                        owner = repoParts[0];
                        repo = repoParts[1].replace(".json", "");
                    }
                }
            }

            report.setConfigLoaded(true);
            report.setRepositoryOwner(owner);
            report.setRepositoryName(repo);
            report.setUpdateChannel(channel);

            logger.info(Messages.LOG_CONFIG_LOADED, jsonUrl);
            logger.info(Messages.LOG_UPDATE_CHANNEL, channel);
            logger.info(Messages.LOG_CONFIG_OK);
            currentState = LifecycleState.CONFIG_LOADED;

        } catch (java.io.IOException e) {
            logger.error(Messages.LOG_CONFIG_ERROR, e);
            report.setConfigLoaded(false);
            report.setConfigError(e.getMessage());
            currentState = LifecycleState.CONFIG_LOADED;
        } catch (Exception e) {
            logger.error(Messages.LOG_CONFIG_ERROR, e);
            report.setConfigLoaded(false);
            report.setConfigError(e.getMessage());
            currentState = LifecycleState.CONFIG_LOADED;
        }
    }

    private void step2_InitializeLogging() {
        currentState = LifecycleState.INITIALIZING_LOGGING;
        logger.info(Messages.LOG_STEP_LOGGING);

        // Логирование уже инициализировано на этом этапе
        // Этот шаг просто логирует завершение
        report.setLoggingInitialized(true);
        logger.info(Messages.LOG_LOGGING_OK);
        currentState = LifecycleState.LOGGING_INITIALIZED;
    }

    private void step3_LoadVersionEngine() {
        currentState = LifecycleState.LOADING_VERSION_ENGINE;
        logger.info(Messages.LOG_STEP_VERSION);

        try {
            String version = VersionService.getCurrentVersionString();

            report.setVersionEngineLoaded(true);
            report.setApplicationVersion(version);

            logger.info(Messages.LOG_VERSION_LOADED, version);
            logger.info(Messages.LOG_VERSION_OK);
            currentState = LifecycleState.VERSION_ENGINE_LOADED;

        } catch (Exception e) {
            logger.error(Messages.LOG_VERSION_ERROR, e);
            report.setVersionEngineLoaded(false);
            report.setVersionError(e.getMessage());
            throw e;
        }
    }

    private void step4_CheckUpdates() {
        currentState = LifecycleState.CHECKING_UPDATES;
        logger.info(Messages.LOG_STEP_UPDATE);

        try {
            String currentVersion = VersionService.getCurrentVersionString();
            UpdateService updateService = new UpdateService();
            com.rmc.model.UpdateCheckResult updateResult = updateService.checkForUpdates();

            boolean updateAvailable = updateResult != null && updateResult.isSuccess();

            report.setUpdateCheckCompleted(true);
            report.setUpdateAvailable(updateAvailable);

            if (updateAvailable) {
                logger.info(Messages.LOG_UPDATE_CHECK_SUCCESS, updateResult.getHttpStatus());
            } else {
                logger.info(Messages.LOG_UPDATE_CHECK_ERROR);
            }

            logger.info(Messages.LOG_UPDATE_OK);
            currentState = LifecycleState.UPDATES_CHECKED;

        } catch (Exception e) {
            logger.warn(Messages.LOG_UPDATE_CHECK_FAILED, e);
            report.setUpdateCheckCompleted(true);
            report.setUpdateCheckError(e.getMessage());
            currentState = LifecycleState.UPDATES_CHECKED;
        }
    }

    private void step5_DriverDetection() {
        currentState = LifecycleState.DETECTING_DRIVER;
        logger.info(Messages.LOG_STEP_DRIVER_DETECT);

        try {
            // Обнаружение Edge
            EdgeInfo edgeInfo = EdgeDetector.detect();
            report.setEdgeDetected(edgeInfo.isInstalled());
            report.setEdgeVersion(edgeInfo.getVersion());
            report.setEdgePath(edgeInfo.getPath());

            if (!edgeInfo.isInstalled()) {
                logger.info(Messages.LOG_EDGE_NOT_INSTALLED);
            } else {
                logger.info(Messages.LOG_EDGE_DETECTED, edgeInfo.getVersion(), edgeInfo.getPath());
            }

            // Обнаружение драйвера
            DriverInfo driverInfo = DriverDetector.detect();
            report.setDriverDetected(driverInfo.isInstalled());
            report.setDriverVersion(driverInfo.getVersion());
            report.setDriverPath(driverInfo.getPath());

            if (!driverInfo.isInstalled()) {
                logger.info(Messages.LOG_DRIVER_NOT_INSTALLED);
            } else {
                logger.info(Messages.LOG_DRIVER_DETECTED, driverInfo.getVersion(), driverInfo.getPath());
            }

            // Сравнение версий
            DriverStatus status = DriverService.detectAndCompare();
            report.setDriverStatus(status);

            logger.info(Messages.LOG_DRIVER_STATUS, status);
            logger.info("Обнаружение драйвера: ОК");
            currentState = LifecycleState.DRIVER_DETECTED;

        } catch (Exception e) {
            logger.error("Не удалось обнаружить драйвер", e);
            report.setDriverDetectionError(e.getMessage());
            throw e;
        }
    }

    private void step6_DriverValidation() {
        currentState = LifecycleState.VALIDATING_DRIVER;
        logger.info(Messages.LOG_STEP_DRIVER_VALIDATE);

        try {
            if (!report.isEdgeDetected() || !report.isDriverDetected()) {
                logger.info(Messages.LOG_SKIP_VALIDATION);
                report.setValidationCompleted(true);
                currentState = LifecycleState.DRIVER_VALIDATED;
                return;
            }

            Path driverPath = Path.of(report.getDriverPath());
            String browserVersion = report.getEdgeVersion();
            String expectedDriverVersion = report.getDriverVersion();

            ValidationResult validationResult = DriverValidator.validate(
                    driverPath, browserVersion, expectedDriverVersion);

            report.setValidationCompleted(true);
            report.setValidationStatus(validationResult.getStatus());

            if (validationResult.isValid()) {
                logger.info(Messages.LOG_VALIDATION_PASS);
            } else {
                logger.info(Messages.LOG_VALIDATION_FAIL, validationResult.getMessage());
            }

            logger.info("Проверка драйвера: ОК");
            currentState = LifecycleState.DRIVER_VALIDATED;

        } catch (Exception e) {
            logger.error("Не удалось проверить драйвер", e);
            report.setValidationError(e.getMessage());
            currentState = LifecycleState.DRIVER_VALIDATED;
        }
    }

    private void step7_ApplicationReady() {
        currentState = LifecycleState.READY;
        logger.info(Messages.LOG_STEP_READY);

        report.setApplicationReady(true);
        report.setStartTime(LocalDateTime.now());

        logger.info(Messages.LOG_APP_READY);
    }

    /**
     * Get the current lifecycle state.
     */
    public LifecycleState getCurrentState() {
        return currentState;
    }

    /**
     * Get the lifecycle report.
     */
    public LifecycleReport getReport() {
        return report;
    }

    /**
     * Check if the application is ready.
     */
    public boolean isReady() {
        return currentState == LifecycleState.READY;
    }
}
