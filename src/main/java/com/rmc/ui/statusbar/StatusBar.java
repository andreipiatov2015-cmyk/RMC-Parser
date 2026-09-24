package com.rmc.ui.statusbar;

import com.rmc.version.VersionService;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Status bar - always visible footer.
 */
public class StatusBar extends HBox {
    
    private static final long MIN_TAUNT_INTERVAL_MILLIS = 10 * 60 * 1000L;
    private static final long MAX_TAUNT_INTERVAL_MILLIS = 15 * 60 * 1000L;
    private static final double MARQUEE_SPEED_PX_PER_SEC = 70;
    
    private final Label connectionStatus;
    private final Label versionLabel;
    private final Button easterEggButton;
    private final StackPane marqueeArea;
    private final Label marqueeLabel;
    private final Label clockLabel;
    
    private final EasterEggState eggState;
    private Timeline idleTauntTimer;
    private Timeline marqueeAnimation;
    private ScaleTransition enticementPulse;
    private Popup currentBubble;
    
    public StatusBar() {
        getStyleClass().add("status-bar");
        setPadding(new Insets(0, 16, 0, 16));
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(24);
        
        // Connection status
        connectionStatus = new Label("○ Офлайн");
        connectionStatus.getStyleClass().add("status-item");
        
        // Separator
        Label sep1 = createSeparator();
        
        // Version
        versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("status-item");
        
        // Separator
        Label sep2 = createSeparator();
        
        // Пасхалка — маленькая кнопка вместо прежних "Потоков"/"Запросов",
        // которые никогда не были подключены ни к чему реальному.
        eggState = EasterEggState.load();
        easterEggButton = new Button("🥚");
        easterEggButton.getStyleClass().add("easter-egg-button");
        easterEggButton.setOnAction(e -> onEasterEggClicked());
        
        // Бегущая строка для реплик — занимает всё оставшееся место между
        // кнопкой и часами. Реплика "бежит" вдоль нижней панели, не
        // перекрывая ничего в рабочей области, и не остаётся висеть поверх
        // старой — новая всегда сначала останавливает и стирает предыдущую.
        marqueeLabel = new Label();
        marqueeLabel.getStyleClass().add("easter-egg-marquee-text");
        
        marqueeArea = new StackPane(marqueeLabel);
        marqueeArea.getStyleClass().add("easter-egg-marquee");
        marqueeArea.setAlignment(Pos.CENTER_LEFT);
        marqueeArea.setPrefHeight(24);
        marqueeArea.setMinWidth(0);
        HBox.setHgrow(marqueeArea, Priority.ALWAYS);
        StackPane.setAlignment(marqueeLabel, Pos.CENTER_LEFT);
        
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(marqueeArea.widthProperty());
        clip.heightProperty().bind(marqueeArea.heightProperty());
        marqueeArea.setClip(clip);
        
        // Периодические "обиженные" напоминания работают независимо от
        // того, сколько раз уже нажимали — просто когда кнопку долго не
        // трогают. Каждый клик перезапускает отсчёт (см. onEasterEggClicked).
        scheduleIdleTaunt(remainingTauntDelay());
        startEnticementPulse();
        
        // Clock
        clockLabel = new Label();
        clockLabel.getStyleClass().add("status-item");
        
        getChildren().addAll(
            connectionStatus, sep1,
            versionLabel, sep2,
            easterEggButton, marqueeArea, clockLabel
        );
        
        // Start clock
        startClock();
    }
    
    private Label createSeparator() {
        Label sep = new Label("│");
        sep.getStyleClass().add("status-separator");
        return sep;
    }
    
    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        Timeline clock = new Timeline(new KeyFrame(
            Duration.seconds(1),
            e -> {
                clockLabel.setText(LocalDateTime.now().format(formatter));
            }
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        
        // Initial update
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }
    
    public void setConnected(boolean connected) {
        if (connected) {
            connectionStatus.setText("● Онлайн");
            connectionStatus.setStyle("-fx-text-fill: #238636;");
        } else {
            connectionStatus.setText("○ Офлайн");
            connectionStatus.setStyle("-fx-text-fill: #57606A;");
        }
    }
    
    private void onEasterEggClicked() {
        if (eggState.isFinished()) {
            return;
        }
        
        // Сначала (в фоне, коротким запросом) пробуем локальный AI-сервер —
        // если он в сети, используем его ответ и НЕ трогаем локальный
        // прогресс (запас заготовленных фраз бережём именно на случай,
        // когда сервера рядом нет). Если сервер недоступен — используем
        // обычный сценарий, как раньше.
        new Thread(() -> {
            int percent = escalationPercent();
            java.util.Optional<String> aiMessage = AiEasterEggService.tryGetMessage(percent);
            Platform.runLater(() -> {
                if (aiMessage.isPresent()) {
                    showBubble(aiMessage.get());
                } else {
                    String message = eggState.advanceAndGetMessage();
                    showBubble(message);
                }
                // Каждый клик — знак, что за кнопкой следят: не нужно
                // прямо сейчас напоминать "я обиделась", отсчёт начинается заново.
                scheduleIdleTaunt(randomTauntInterval());
            });
        }).start();
    }
    
    /**
     * Лёгкая "дышащая" пульсация кнопки — привлекает взгляд и намекает,
     * что на неё стоит нажать. Останавливается, как только реплики
     * заканчиваются и кнопка становится неактивной.
     */
    private void startEnticementPulse() {
        enticementPulse = new ScaleTransition(Duration.seconds(1.1), easterEggButton);
        enticementPulse.setFromX(1.0);
        enticementPulse.setFromY(1.0);
        enticementPulse.setToX(1.3);
        enticementPulse.setToY(1.3);
        enticementPulse.setAutoReverse(true);
        enticementPulse.setCycleCount(Animation.INDEFINITE);
        enticementPulse.setInterpolator(Interpolator.EASE_BOTH);
        enticementPulse.play();
    }
    
    private void stopEnticementPulse() {
        if (enticementPulse != null) {
            enticementPulse.stop();
            easterEggButton.setScaleX(1.0);
            easterEggButton.setScaleY(1.0);
        }
    }
    
    /**
     * Показать реплику во всплывающем "пузыре" над кнопкой — используется
     * для ответа на клик. Если предыдущий пузырь ещё виден — он сразу
     * прячется, и вместо него показывается новый (а не поверх старого).
     */
    private void showBubble(String message) {
        if (currentBubble != null) {
            currentBubble.hide();
        }
        
        Popup popup = new Popup();
        popup.setAutoHide(true);
        currentBubble = popup;
        
        Label label = new Label(message);
        label.getStyleClass().add("easter-egg-bubble");
        label.setWrapText(true);
        label.setMaxWidth(360);
        popup.getContent().add(label);
        
        var bounds = easterEggButton.localToScreen(easterEggButton.getBoundsInLocal());
        double anchorX = bounds != null ? bounds.getMinX() - 60 : 0;
        double anchorY = bounds != null ? bounds.getMinY() : 0;
        
        // Показываем один раз, чтобы JavaFX посчитал реальный размер
        // "пузыря" с учётом переноса текста, а затем поднимаем его на его
        // же фактическую высоту + отступ — так текст никогда не
        // перекрывает кнопку, независимо от длины конкретной реплики.
        popup.show(easterEggButton, anchorX, anchorY);
        Platform.runLater(() -> {
            double popupHeight = popup.getHeight();
            popup.setY(anchorY - popupHeight - 18);
        });
        
        double seconds = Math.min(8, Math.max(3, 2.5 + message.length() / 20.0));
        PauseTransition hide = new PauseTransition(Duration.seconds(seconds));
        hide.setOnFinished(e -> popup.hide());
        hide.play();
    }
    
    /**
     * Показать реплику бегущей строкой рядом с кнопкой. Если что-то уже
     * бежит — оно немедленно останавливается и стирается, и вместо него
     * сразу же бежит новая реплика (а не поверх старой). Используется
     * только для периодических "обиженных" напоминаний, когда кнопка
     * долго не нажималась.
     */
    private void showMarqueeMessage(String message) {
        stopMarquee();
        
        marqueeLabel.setText(message);
        marqueeLabel.setTranslateX(Math.max(marqueeArea.getWidth(), 1));
        
        // Ждём один pulse, чтобы JavaFX успел посчитать реальную ширину
        // текста — она нужна, чтобы рассчитать полную дистанцию прокрутки.
        Platform.runLater(() -> {
            double textWidth = marqueeLabel.prefWidth(-1);
            double areaWidth = Math.max(marqueeArea.getWidth(), 1);
            double distance = areaWidth + textWidth;
            double durationSeconds = Math.max(4, distance / MARQUEE_SPEED_PX_PER_SEC);
            
            marqueeAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(marqueeLabel.translateXProperty(), areaWidth)),
                    new KeyFrame(Duration.seconds(durationSeconds), new KeyValue(marqueeLabel.translateXProperty(), -textWidth))
            );
            marqueeAnimation.setOnFinished(e -> marqueeLabel.setText(""));
            marqueeAnimation.play();
        });
    }
    
    private void stopMarquee() {
        if (marqueeAnimation != null) {
            marqueeAnimation.stop();
        }
        marqueeLabel.setText("");
    }
    
    /**
     * Сколько осталось до следующего "обиженного" напоминания — если
     * программу перезапустили раньше срока, ждём остаток интервала;
     * если прошло больше положенного (или напоминаний ещё не было),
     * показываем почти сразу.
     */
    private Duration remainingTauntDelay() {
        long last = eggState.getLastTauntEpochMillis();
        if (last <= 0) {
            return Duration.seconds(5);
        }
        long elapsed = System.currentTimeMillis() - last;
        long remaining = MIN_TAUNT_INTERVAL_MILLIS - elapsed;
        return remaining > 0 ? Duration.millis(remaining) : Duration.seconds(5);
    }
    
    private Duration randomTauntInterval() {
        long millis = ThreadLocalRandom.current().nextLong(MIN_TAUNT_INTERVAL_MILLIS, MAX_TAUNT_INTERVAL_MILLIS + 1);
        return Duration.millis(millis);
    }
    
    private void scheduleIdleTaunt(Duration delay) {
        if (idleTauntTimer != null) {
            idleTauntTimer.stop();
        }
        idleTauntTimer = new Timeline(new KeyFrame(delay, e -> {
            new Thread(() -> {
                java.util.Optional<String> aiMessage = AiEasterEggService.tryGetMessage(escalationPercent());
                Platform.runLater(() -> showMarqueeMessage(aiMessage.orElseGet(this::randomIdleTaunt)));
            }).start();
            eggState.recordTaunt(System.currentTimeMillis());
            scheduleIdleTaunt(randomTauntInterval());
        }));
        idleTauntTimer.play();
    }
    
    private String randomIdleTaunt() {
        List<String> taunts = EasterEggMessages.IDLE_TAUNTS;
        return taunts.get(ThreadLocalRandom.current().nextInt(taunts.size()));
    }
    
    /**
     * Насколько "раздражена" кнопка сейчас (0-100) — считается из того,
     * сколько заготовленных реплик по сценарию уже показано. Передаётся
     * в запрос к локальному AI-серверу, чтобы его тон совпадал с тем,
     * что было бы дальше по сценарию.
     */
    private int escalationPercent() {
        int total = EasterEggMessages.SEQUENCE.size();
        if (total <= 0) {
            return 100;
        }
        int percent = (int) Math.round((eggState.getClickIndex() * 100.0) / total);
        return Math.max(0, Math.min(100, percent));
    }
}
