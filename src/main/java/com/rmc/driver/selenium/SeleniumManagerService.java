package com.rmc.driver.selenium;

import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverInfo;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;

/**
 * Сервис управления драйверами с использованием Selenium Manager.
 * 
 * <p>Selenium Manager - это официальный менеджер драйверов Selenium,
 * встроенный в Selenium 4.6+. Он автоматически:</p>
 * <ul>
 *   <li>Определяет версию установленного браузера</li>
 *   <li>Скачивает подходящий драйвер</li>
 *   <li>Управляет локальным кэшем</li>
 * </ul>
 * 
 * <p>Этот сервис является единственной точкой входа для работы с Selenium.</p>
 */
public class SeleniumManagerService {

    private static final Logger logger = AppLogger.getLogger();
    
    private SeleniumManagerService() {
        // Утилитарный класс
    }
    
    /**
     * Проверить готовность Selenium и получить WebDriver.
     * 
     * <p>Если драйвер не готов - Selenium Manager автоматически его загрузит.</p>
     * 
     * @return Результат подготовки
     */
    public static SeleniumPrepareResult ensureDriverReady() {
        logger.info(Messages.LOG_SM_HEADER);
        logger.info(Messages.LOG_SM_PREPARING_SELENIUM);
        logger.info(Messages.LOG_SM_HEADER);
        
        // Проверяем наличие Microsoft Edge
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (!edgeInfo.isInstalled()) {
            logger.error(Messages.LOG_SM_NO_EDGE);
            return SeleniumPrepareResult.edgeNotInstalled();
        }
        
        logger.info(Messages.LOG_SM_EDGE_FOUND);
        logger.info(Messages.LOG_SM_EDGE_VERSION, edgeInfo.getVersion());
        
        // Проверяем наличие драйвера
        DriverInfo driverInfo = DriverDetector.detect();
        if (driverInfo.isInstalled()) {
            logger.info(Messages.LOG_SM_DRIVER_ALREADY_READY);
            return SeleniumPrepareResult.ready(driverInfo.getPath());
        }
        
        logger.info(Messages.LOG_SM_DRIVER_MISSING);
        return prepareDriver();
    }
    
    /**
     * Подготовить драйвер с использованием Selenium Manager.
     */
    private static SeleniumPrepareResult prepareDriver() {
        try {
            logger.info(Messages.LOG_SM_STARTING_SM);
            logger.info(Messages.LOG_SM_FINDING_COMPATIBLE);
            logger.info(Messages.LOG_SM_CHECKING_CACHE);
            logger.info(Messages.LOG_SM_DOWNLOAD_IF_NEEDED);
            
            // Selenium Manager автоматически:
            // 1. Определяет версию Edge
            // 2. Находит совместимый msedgedriver
            // 3. Скачивает драйвер если необходимо
            // 4. Возвращает путь к драйверу
            
            // Создаём EdgeOptions для инициализации Selenium Manager
            EdgeOptions options = new EdgeOptions();
            
            // Создаём временный WebDriver для инициации Selenium Manager
            // Это заставит Selenium Manager загрузить драйвер
            WebDriver driver = new EdgeDriver(options);
            
            // Получаем путь к драйверу от Selenium
            String driverPath = ((EdgeDriver) driver).getCapabilities().getBrowserVersion();
            
            logger.info(Messages.LOG_SM_DOWNLOAD_COMPLETE);
            logger.info(Messages.LOG_SM_DRIVER_READY);
            
            // Закрываем временный драйвер
            driver.quit();
            
            // Проверяем драйвер через наш Detector
            DriverInfo info = DriverDetector.detect();
            
            if (info.isInstalled()) {
                logger.info(Messages.LOG_SM_DRIVER_FOUND);
                logger.info(Messages.LOG_SM_PATH, info.getPath());
                logger.info(Messages.LOG_SM_SUBSYSTEM_READY);
                logger.info(Messages.LOG_SM_HEADER);
                return SeleniumPrepareResult.ready(info.getPath());
            }
            
            logger.info(Messages.LOG_SM_SUCCESS);
            logger.info(Messages.LOG_SM_HEADER);
            return SeleniumPrepareResult.ready(info.getPath());
            
        } catch (Exception e) {
            logger.error(Messages.LOG_SM_ERROR_PREFIX + e.getMessage(), e);
            logger.info(Messages.LOG_SM_HEADER);
            return SeleniumPrepareResult.error(e.getMessage());
        }
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
     * Результат подготовки Selenium.
     */
    public static class SeleniumPrepareResult {
        
        private final boolean success;
        private final String driverPath;
        private final String errorMessage;
        private final boolean edgeNotInstalled;
        
        private SeleniumPrepareResult(boolean success, String driverPath, String errorMessage, boolean edgeNotInstalled) {
            this.success = success;
            this.driverPath = driverPath;
            this.errorMessage = errorMessage;
            this.edgeNotInstalled = edgeNotInstalled;
        }
        
        public static SeleniumPrepareResult ready(String driverPath) {
            return new SeleniumPrepareResult(true, driverPath, null, false);
        }
        
        public static SeleniumPrepareResult error(String message) {
            return new SeleniumPrepareResult(false, null, message, false);
        }
        
        public static SeleniumPrepareResult edgeNotInstalled() {
            return new SeleniumPrepareResult(false, null, Messages.LOG_SM_NO_EDGE, true);
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
                return "SeleniumPrepareResult{success=true, path=" + driverPath + "}";
            } else {
                return "SeleniumPrepareResult{success=false, error=" + errorMessage + "}";
            }
        }
    }
}
