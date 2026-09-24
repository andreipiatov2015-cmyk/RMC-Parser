package com.rmc.ui.workspace.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;

/**
 * Кольцевая диаграмма (donut): закрашенная дуга показывает долю
 * "зачислено" от общей вместимости — чем меньше процент, тем короче
 * закрашенная часть кольца. По центру — число зачисленных, под ним — %.
 *
 * <p>Используется в двух размерах: крупная для учреждения в целом
 * (столько же по размеру, сколько обычные карточки показателей) и
 * маленькая — рядом с каждой отдельной программой.</p>
 */
public class DonutChart extends StackPane {
    
    public DonutChart(double size, double strokeWidth, int enrolled, int capacity) {
        getStyleClass().add("donut-chart");
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMinSize(size, size);
        
        double radius = (size - strokeWidth) / 2;
        double center = size / 2;
        
        int percent = capacity > 0 ? (int) Math.round((enrolled * 100.0) / capacity) : 0;
        percent = Math.max(0, Math.min(100, percent));
        
        // Фон кольца — полный круг, бледный, задаёт "дорожку".
        Arc track = new Arc(center, center, radius, radius, 90, 360);
        track.setType(ArcType.OPEN);
        track.setFill(null);
        track.getStyleClass().add("donut-track");
        track.setStrokeWidth(strokeWidth);
        track.setStrokeLineCap(StrokeLineCap.BUTT);
        
        // Закрашенная часть — от 12 часов по кругу, длина пропорциональна
        // проценту (отрицательный угол — чтобы идти по часовой стрелке).
        Arc fill = new Arc(center, center, radius, radius, 90, -percent * 3.6);
        fill.setType(ArcType.OPEN);
        fill.setFill(null);
        fill.getStyleClass().add("donut-fill");
        fill.setStrokeWidth(strokeWidth);
        fill.setStrokeLineCap(StrokeLineCap.BUTT);
        
        // ВАЖНО: обычный Pane для самих дуг, а не StackPane — StackPane
        // центрирует каждого ребёнка по ЕГО СОБСТВЕННОЙ bounding box, а у
        // частичной дуги (fill) она несимметрична относительно центра
        // окружности (в отличие от полного круга track) — из-за этого
        // дуги визуально "съезжали" друг относительно друга. Обычный Pane
        // просто рисует детей по их собственным координатам без какого-либо
        // автоцентрирования, поэтому обе дуги остаются точно концентричными.
        Pane arcsPane = new Pane(track, fill);
        arcsPane.setPrefSize(size, size);
        arcsPane.setMaxSize(size, size);
        arcsPane.setMinSize(size, size);
        
        boolean isLarge = size >= 100;
        
        VBox textBox;
        if (isLarge) {
            Label countLabel = new Label(String.valueOf(enrolled));
            countLabel.getStyleClass().add("donut-count");
            
            Label percentLabel = new Label(percent + "%");
            percentLabel.getStyleClass().add("donut-percent");
            
            textBox = new VBox(0, countLabel, percentLabel);
        } else {
            // Для маленьких колец (по каждой программе) — только процент,
            // жирным и покрупнее; число зачисленных и так уже написано
            // текстом рядом (см. InstitutionDetailView).
            Label percentLabel = new Label(percent + "%");
            percentLabel.getStyleClass().add("donut-percent-only-small");
            
            textBox = new VBox(percentLabel);
        }
        textBox.setAlignment(Pos.CENTER);
        
        getChildren().addAll(arcsPane, textBox);
    }
}
