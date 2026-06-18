package com.rmc.ui.dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Results statistics panel - shown below filters.
 */
public class ResultsPanel extends HBox {
    
    private final Label filtersLabel;
    private final Label activeLabel;
    private final Label institutionsLabel;
    private final Label programsLabel;
    private final Label timeLabel;
    
    public ResultsPanel() {
        getStyleClass().add("results-panel");
        setSpacing(24);
        setPadding(new Insets(12, 16, 12, 16));
        setAlignment(Pos.CENTER_LEFT);
        
        filtersLabel = createStat("Загружено фильтров", "0");
        activeLabel = createStat("Активных", "0");
        institutionsLabel = createStat("Учреждений", "0");
        programsLabel = createStat("Программ", "0");
        timeLabel = createStat("Обновлено", "—");
        
        getChildren().addAll(filtersLabel, activeLabel, institutionsLabel, programsLabel, timeLabel);
    }
    
    private Label createStat(String title, String value) {
        Label stat = new Label();
        stat.setText(title + ": " + value);
        stat.getStyleClass().add("result-stat");
        return stat;
    }
    
    public void updateFilters(int total, int active) {
        filtersLabel.setText("Загружено фильтров: " + total);
        activeLabel.setText("Активных: " + active);
    }
    
    public void updateResults(int institutions, int programs) {
        institutionsLabel.setText("Учреждений: " + institutions);
        programsLabel.setText("Программ: " + programs);
    }
    
    public void updateTime(String time) {
        timeLabel.setText("Обновлено: " + time);
    }
}
