package com.rmc;

import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.ui.MainWindow;
import com.rmc.version.VersionService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;

/**
 * Точка входа в приложение RMC Framework.
 */
public class Main extends Application {

    private static final Logger logger = AppLogger.getLogger();

    @Override
    public void start(Stage primaryStage) {
        AppLogger.logStartupInfo();
        logger.info("Запуск RMC Framework {}", VersionService.getCurrentVersionString());

        try {
            primaryStage.setTitle(Messages.APP_TITLE);
            
            java.io.InputStream iconStream = getClass().getResourceAsStream("/images/app-icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                logger.warn("Иконка приложения не найдена: /images/app-icon.png");
            }
            
            // Create main window
            MainWindow mainWindow = new MainWindow();
            
            Scene scene = new Scene(mainWindow, 1200, 800);
            scene.getStylesheets().add("/styles/dashboard.css");
            scene.getStylesheets().add("/styles/dashboard-dark.css");
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Главное окно приложения отображено");
            
            mainWindow.checkForUpdatesOnStartup();

        } catch (Exception e) {
            logger.error("Не удалось запустить приложение", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Приложение закрывается");
    }

    public static void main(String[] args) {
        launch(args);
    }
}