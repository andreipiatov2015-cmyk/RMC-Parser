package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Left panel Dashboard with info cards.
 */
public class DashboardPanel extends VBox {
    
    private final InfoCard userCard;
    private final InfoCard serverCard;
    private final InfoCard institutionsCard;
    private final InfoCard programsCard;
    private final InfoCard checkedCard;
    private final InfoCard enrollmentsCard;
    private final InfoCard exportCard;
    
    public DashboardPanel() {
        getStyleClass().add("dashboard-panel");
        setSpacing(12);
        setPadding(new Insets(0));
        setPrefWidth(220);
        
        // Title
        Label title = new Label("📊 Dashboard");
        title.getStyleClass().add("panel-title");
        
        // User info card
        userCard = new InfoCard("Пользователь", "—");
        updateUserCard();
        
        // Server status card
        serverCard = new InfoCard("Сервер", "Офлайн");
        serverCard.setStatus("Офлайн");
        
        // Institutions count
        institutionsCard = new InfoCard("Учреждений найдено", "0");
        
        // Programs count
        programsCard = new InfoCard("Программ найдено", "0");
        
        // Checked count
        checkedCard = new InfoCard("Проверено", "0");
        
        // Enrollments count
        enrollmentsCard = new InfoCard("Зачислений найдено", "0");
        
        // Export status
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
        
        // Listen for auth state changes
        ApplicationState.getInstance().addAuthStateListener(event -> updateUserCard());
    }
    
    private void updateUserCard() {
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            userCard.setValue(state.getUsername());
            userCard.setStatus("Авторизован");
            serverCard.setValue("rmc.ruobr.ru");
            serverCard.setStatus("Онлайн");
        } else {
            userCard.setValue("—");
            userCard.setStatus("Не авторизован");
            serverCard.setValue("—");
            serverCard.setStatus("Офлайн");
        }
    }
    
    public void updateStats(int institutions, int programs, int checked, int enrollments) {
        institutionsCard.setValue(String.valueOf(institutions));
        programsCard.setValue(String.valueOf(programs));
        checkedCard.setValue(String.valueOf(checked));
        enrollmentsCard.setValue(String.valueOf(enrollments));
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
