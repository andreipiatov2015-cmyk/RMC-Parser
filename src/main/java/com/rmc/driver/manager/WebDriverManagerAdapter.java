package com.rmc.driver.manager;

import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverInfo;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.slf4j.Logger;

/**
 * Адаптер для WebDriverManager - официальная библиотека управления драйверами.
 * 
 * <p>Интеграция основана на официальной документации WebDriverManager 6.x
 * (https://bonigarcia.dev/webdrivermanager/)</p>
 * 
 * <p>Основные принципы:</p>
 * <ul>
 *   <li>Используем WebDriverManager.edgedriver().setup() - официальный API</li>
 *   <li>WebDriverManager самостоятельно определяет версию Edge</li>
 *   <li>Скачивает драйвер с официальных серверов Microsoft</li>
 *   <li>Кэширует драйвер в ~/.cache/selenium</li>
 * </ul>
 */
public class WebDriverManagerAdapter {

    private static final Logger logger = AppLogger.getLogger();
    
    private WebDriverManagerAdapter() {
        // Утилитарный класс
    }
    
    /**
     * Скачать драйвер через WebDriverManager.
     * 
     * <p>Использует официальный API WebDriverManager.edgedriver().setup()
     * без ручного указания версии браузера.</p>
     * 
     * @return Результат операции
     */
    public static DriverDownloadResult downloadDriver() {
        logger.info(Messages.LOG_WDM_HEADER);
        logger.info(Messages.LOG_WDM_CHECK_EDGE);
        logger.info(Messages.LOG_WDM_HEADER);
        
        // Проверяем наличие Microsoft Edge
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (!edgeInfo.isInstalled()) {
            logger.error(Messages.LOG_WDM_NO_EDGE);
            return DriverDownloadResult.edgeNotInstalled();
        }
        
        logger.info(Messages.LOG_WDM_EDGE_FOUND);
        logger.info(Messages.LOG_WDM_EDGE_VERSION, edgeInfo.getVersion());
        
        // Проверяем наличие WebDriver
        DriverInfo driverInfo = DriverDetector.detect();
        if (driverInfo.isInstalled()) {
            logger.info(Messages.LOG_WDM_DRIVER_ALREADY_CURRENT);
            return DriverDownloadResult.alreadyInstalled(driverInfo.getPath());
        }
        
        logger.info(Messages.LOG_WDM_DRIVER_MISSING);
        return downloadDriverWithWDM();
    }
    
    /**
     * Обновить драйвер через WebDriverManager.
     * 
     * @return Результат операции
     */
    public static DriverDownloadResult updateDriver() {
        logger.info(Messages.LOG_WDM_HEADER);
        logger.info(Messages.LOG_WDM_UPDATE_START);
        logger.info(Messages.LOG_WDM_HEADER);
        
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (!edgeInfo.isInstalled()) {
            logger.error(Messages.LOG_WDM_NO_EDGE);
            return DriverDownloadResult.edgeNotInstalled();
        }
        
        logger.info(Messages.LOG_WDM_EDGE_FOUND);
        logger.info(Messages.LOG_WDM_EDGE_VERSION, edgeInfo.getVersion());
        
        return downloadDriverWithWDM();
    }
    
    /**
     * Проверить, установлен ли драйвер.
     */
    public static boolean isDriverInstalled() {
        return DriverDetector.detect().isInstalled();
    }
    
    /**
     * Получить версию установленного драйвера.
     */
    public static String getDriverVersion() {
        DriverInfo info = DriverDetector.detect();
        return info.isInstalled() ? info.getVersion() : null;
    }
    
    /**
     * Получить путь к установленному драйверу.
     */
    public static String getDriverPath() {
        DriverInfo info = DriverDetector.detect();
        return info.isInstalled() ? info.getPath() : null;
    }
    
    /**
     * Убедиться что драйвер готов к использованию.
     */
    public static DriverDownloadResult ensureDriverReady() {
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (!edgeInfo.isInstalled()) {
            logger.error(Messages.LOG_WDM_NO_EDGE);
            return DriverDownloadResult.edgeNotInstalled();
        }
        
        DriverInfo driverInfo = DriverDetector.detect();
        
        if (!driverInfo.isInstalled()) {
            logger.info(Messages.LOG_WDM_DRIVER_MISSING);
            return downloadDriverWithWDM();
        }
        
        logger.info(Messages.LOG_WDM_DRIVER_ALREADY_CURRENT);
        return DriverDownloadResult.success(driverInfo.getPath());
    }
    
    /**
     * Скачать драйвер с использованием WebDriverManager.
     * 
     * <p>Использует официальный API WebDriverManager.edgedriver().setup()
     * согласно документации https://bonigarcia.dev/webdrivermanager/#_driver_management</p>
     */
    private static DriverDownloadResult downloadDriverWithWDM() {
        try {
            logger.info(Messages.LOG_WDM_STARTING_WDM);
            logger.info(Messages.LOG_WDM_WDM_AUTO_DETECT);
            logger.info(Messages.LOG_WDM_STARTING_DOWNLOAD);
            
            // Получаем экземпляр менеджера и вызываем setup()
            // WebDriverManager самостоятельно:
            // 1. Определяет версию Edge
            // 2. Находит подходящий msedgedriver
            // 3. Скачивает драйвер с официальных серверов Microsoft
            // 4. Кэширует в ~/.cache/selenium
            WebDriverManager wdm = WebDriverManager.edgedriver();
            wdm.setup();
            
            logger.info(Messages.LOG_WDM_DOWNLOAD_COMPLETE);
            
            // Получаем путь к скачанному драйверу
            String driverPath = wdm.getDownloadedDriverPath();
            
            if (driverPath != null && !driverPath.isEmpty()) {
                logger.info(Messages.LOG_WDM_DRIVER_SAVED, driverPath);
                
                // Проверяем установленный драйвер
                logger.info(Messages.LOG_WDM_CHECKING_INSTALLED);
                DriverInfo info = DriverDetector.detect();
                
                if (info.isInstalled()) {
                    logger.info(Messages.LOG_WDM_DRIVER_FOUND);
                    logger.info(Messages.LOG_WDM_VERSION_MATCH);
                    logger.info(Messages.LOG_WDM_SUBSYSTEM_READY);
                    logger.info(Messages.LOG_WDM_HEADER);
                    return DriverDownloadResult.success(driverPath);
                }
            }
            
            // Дополнительная проверка через DriverDetector
            logger.info(Messages.LOG_WDM_CHECKING_INSTALLED);
            DriverInfo info = DriverDetector.detect();
            
            if (info.isInstalled()) {
                logger.info(Messages.LOG_WDM_DRIVER_FOUND);
                logger.info(Messages.LOG_WDM_SUBSYSTEM_READY);
                logger.info(Messages.LOG_WDM_HEADER);
                return DriverDownloadResult.success(info.getPath());
            }
            
            logger.info(Messages.LOG_WDM_SUCCESS);
            logger.info(Messages.LOG_WDM_HEADER);
            return DriverDownloadResult.success(driverPath);
            
        } catch (Exception e) {
            logger.error(Messages.LOG_WDM_ERROR_PREFIX + e.getMessage(), e);
            logger.info(Messages.LOG_WDM_HEADER);
            return DriverDownloadResult.error(e.getMessage());
        }
    }
    
    /**
     * Очистить кэш драйверов WebDriverManager.
     */
    public static void clearDriverCache() {
        try {
            WebDriverManager.edgedriver().clearDriverCache();
            logger.info("Кэш WebDriverManager очищен");
        } catch (Exception e) {
            logger.error("Не удалось очистить кэш драйвера", e);
        }
    }
    
    /**
     * Результат операции загрузки/обновления драйвера.
     */
    public static class DriverDownloadResult {
        
        private final boolean success;
        private final String driverPath;
        private final String errorMessage;
        private final boolean edgeNotInstalled;
        
        private DriverDownloadResult(boolean success, String driverPath, String errorMessage, boolean edgeNotInstalled) {
            this.success = success;
            this.driverPath = driverPath;
            this.errorMessage = errorMessage;
            this.edgeNotInstalled = edgeNotInstalled;
        }
        
        public static DriverDownloadResult success(String driverPath) {
            return new DriverDownloadResult(true, driverPath, null, false);
        }
        
        public static DriverDownloadResult error(String message) {
            return new DriverDownloadResult(false, null, message, false);
        }
        
        public static DriverDownloadResult edgeNotInstalled() {
            return new DriverDownloadResult(false, null, Messages.LOG_WDM_NO_EDGE, true);
        }
        
        public static DriverDownloadResult alreadyInstalled(String driverPath) {
            return new DriverDownloadResult(true, driverPath, Messages.LOG_WDM_DRIVER_ALREADY_CURRENT, false);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getDriverPath() {
            return driverPath;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean isEdgeNotInstalled() {
            return edgeNotInstalled;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "DriverDownloadResult{success=true, path=" + driverPath + "}";
            } else {
                return "DriverDownloadResult{success=false, error=" + errorMessage + "}";
            }
        }
    }
}
