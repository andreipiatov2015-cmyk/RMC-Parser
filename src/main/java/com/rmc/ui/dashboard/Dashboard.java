package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Left panel with info cards - always visible.
 */
public class Dashboard extends VBox {
    
    private final InfoCard userCard;
    private final InfoCard serverCard;
    private final InfoCard institutionsCard;
    private final InfoCard programsCard;
    private final InfoCard checkedCard;
    private final InfoCard enrollmentsCard;
    private final InfoCard exportCard;
    
    public Dashboard() {
        getStyleClass().add("dashboard");
        setSpacing(8);
        setPadding(new Insets(0, 16, 0, 0));
        setPrefWidth(260);
        
        // Title
        Label title = new Label("📊 Dashboard");
        title.getStyleClass().add("dashboard-title");
        
        // User card
        userCard = new InfoCard("Пользователь", "—");
        
        // Server card
        serverCard = new InfoCard("Сервер", "Офлайн");
        serverCard.setSubtitle("Не подключено");
        
        // Stats cards
        institutionsCard = new InfoCard("Учреждений найдено", "0");
        programsCard = new InfoCard("Программ найдено", "0");
        checkedCard = new InfoCard("Проверено", "0");
        enrollmentsCard = new InfoCard("Зачислений найдено", "0");
        exportCard = new InfoCard("Экспорт", "Не выполнялся");
        
        getChildren().addAll(
            title,
            userCard,
            serverCard,
            institutionsCard,
            programsCard,
            checkedCard,
            enrollmentsCard,
            exportCard
        );
    }
    
    public void onAuthStateChanged() {
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            userCard.setValue(state.getUsername());
            userCard.setSubtitle("Авторизован");
            serverCard.setValue("rmc.ruobr.ru");
            serverCard.setSubtitle("Онлайн");
        } else {
            userCard.setValue("—");
            userCard.setSubtitle("Не авторизован");
            serverCard.setValue("—");
            serverCard.setSubtitle("Офлайн");
        }
    }
    
    public void updateInstitutions(int count) {
        institutionsCard.setValue(String.valueOf(count));
    }
    
    public void updatePrograms(int count) {
        programsCard.setValue(String.valueOf(count));
    }
    
    public void updateChecked(int count) {
        checkedCard.setValue(String.valueOf(count));
    }
    
    public void updateEnrollments(int count) {
        enrollmentsCard.setValue(String.valueOf(count));
    }
    
    public void updateExportStatus(String status) {
        exportCard.setValue(status);
    }
    
    public InfoCard getInstitutionsCard() {
        return institutionsCard;
    }
    
    public InfoCard getProgramsCard() {
        return programsCard;
    }
    
    public InfoCard getCheckedCard() {
        return checkedCard;
    }
    
    public InfoCard getEnrollmentsCard() {
        return enrollmentsCard;
    }
}
