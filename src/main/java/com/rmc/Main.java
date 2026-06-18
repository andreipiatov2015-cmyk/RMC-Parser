package com.rmc;

import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.ui.dashboard.DashboardView;
import com.rmc.ui.dashboard.ServiceDrawer;
import com.rmc.version.VersionService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;

/**
 * Точка входа в приложение RMC Framework.
 */
public class Main extends Application {

    private static final Logger logger = AppLogger.getLogger();
    private DashboardView dashboardView;
    private ServiceDrawer serviceDrawer;

    @Override
    public void start(Stage primaryStage) {
        AppLogger.logStartupInfo();
        logger.info("Запуск RMC Framework {}", VersionService.getCurrentVersionString());

        try {
            primaryStage.setTitle(Messages.APP_TITLE);
            
            // Create dashboard view
            dashboardView = new DashboardView();
            
            // Create service drawer
            serviceDrawer = new ServiceDrawer(primaryStage, dashboardView);
            
            // Connect header to service drawer
            dashboardView.getHeaderPane().setServiceDrawer(serviceDrawer);
            
            // StackPane for overlay
            StackPane root = new StackPane();
            root.getChildren().addAll(dashboardView, serviceDrawer);
            
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add("/styles/dashboard.css");
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}