package com.rmc;

import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.ui.main.MainApplicationWindow;
import com.rmc.version.VersionService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

/**
 * Точка входа в приложение RMC Framework.
 */
public class Main extends Application {

    private static final Logger logger = AppLogger.getLogger();
    private MainApplicationWindow mainWindow;

    @Override
    public void start(Stage primaryStage) {
        AppLogger.logStartupInfo();
        logger.info("Запуск RMC Framework {}", VersionService.getCurrentVersionString());

        try {
            primaryStage.setTitle(Messages.APP_TITLE);
            
            mainWindow = new MainApplicationWindow();
            
            Scene scene = new Scene(mainWindow, 1000, 700);
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Главное окно приложения отображено");

        } catch (Exception e) {
            logger.error("Не удалось запустить приложение", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Приложение закрывается");
        if (mainWindow != null) {
            mainWindow.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}