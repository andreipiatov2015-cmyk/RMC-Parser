package com.rmc.ui.newui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

/**
 * Главное окно приложения с вкладками.
 */
public class NewMainWindow extends Application {
    
    private static final Logger logger = AppLogger.getLogger();
    
    @Override
    public void start(Stage primaryStage) {
        logger.info("Запуск нового интерфейса...");
        
        primaryStage.setTitle("RMC Parser - Новый интерфейс");
        
        MainTabController controller = new MainTabController();
        
        Scene scene = new Scene(controller.createTabPane(), 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        logger.info("Новый интерфейс запущен");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
