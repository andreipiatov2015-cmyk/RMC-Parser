package com.rmc.logging.ui;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Custom Logback appender that sends log messages to a JavaFX TextArea.
 * 
 * <p>This appender collects log messages in a thread-safe list and 
 * displays them in the UI when the JavaFX thread is available.</p>
 */
public class UiLogAppender extends AppenderBase<ILoggingEvent> {

    private static final List<LogMessage> messageBuffer = new CopyOnWriteArrayList<>();
    private static TextArea targetTextArea;
    private static final int MAX_MESSAGES = 1000;

    @Override
    protected void append(ILoggingEvent event) {
        if (targetTextArea == null) {
            // Buffer messages until UI is ready
            addToBuffer(event);
            return;
        }

        LogMessage logMessage = createLogMessage(event);
        
        Platform.runLater(() -> {
            if (targetTextArea != null) {
                appendToTextArea(logMessage);
            }
        });
    }

    private void addToBuffer(ILoggingEvent event) {
        LogMessage logMessage = createLogMessage(event);
        messageBuffer.add(logMessage);
        
        // Trim buffer if too large
        while (messageBuffer.size() > MAX_MESSAGES) {
            messageBuffer.remove(0);
        }
    }

    private LogMessage createLogMessage(ILoggingEvent event) {
        String level = event.getLevel().toString();
        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();
        long timestamp = event.getTimeStamp();
        
        // Get just the class name
        String className = loggerName;
        int lastDot = loggerName.lastIndexOf('.');
        if (lastDot > 0) {
            className = loggerName.substring(lastDot + 1);
        }
        
        return new LogMessage(level, message, className, timestamp);
    }

    private void appendToTextArea(LogMessage logMessage) {
        String formatted = formatMessage(logMessage);
        String currentText = targetTextArea.getText();
        
        if (currentText.isEmpty()) {
            targetTextArea.setText(formatted);
        } else {
            targetTextArea.appendText("\n" + formatted);
        }
        
        // Trim text if too long
        String text = targetTextArea.getText();
        if (text.length() > 50000) {
            targetTextArea.setText(text.substring(text.length() - 40000));
        }
        
        // Auto-scroll to bottom
        targetTextArea.positionCaret(targetTextArea.getLength());
    }

    private String formatMessage(LogMessage logMessage) {
        String timestamp = formatTimestamp(logMessage.getTimestamp());
        String level = padRight(logMessage.getLevel(), 7);
        String className = padRight(logMessage.getClassName(), 20);
        return String.format("%s [%s] %s - %s", 
                timestamp, level, className, logMessage.getMessage());
    }

    private String formatTimestamp(long timestamp) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    private String padRight(String s, int length) {
        if (s == null) s = "";
        if (s.length() >= length) return s.substring(0, length);
        return String.format("%-" + length + "s", s);
    }

    /**
     * Attach a TextArea to receive log messages.
     */
    public static void attach(TextArea textArea) {
        targetTextArea = textArea;
        
        // Flush buffered messages to UI
        Platform.runLater(() -> {
            if (targetTextArea != null && !messageBuffer.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (LogMessage msg : messageBuffer) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(formatMessageStatic(msg));
                }
                targetTextArea.setText(sb.toString());
            }
        });
    }

    private static String formatMessageStatic(LogMessage logMessage) {
        String timestamp = formatTimestampStatic(logMessage.getTimestamp());
        String level = padRightStatic(logMessage.getLevel(), 7);
        String className = padRightStatic(logMessage.getClassName(), 20);
        return String.format("%s [%s] %s - %s", 
                timestamp, level, className, logMessage.getMessage());
    }

    private static String formatTimestampStatic(long timestamp) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    private static String padRightStatic(String s, int length) {
        if (s == null) s = "";
        if (s.length() >= length) return s.substring(0, length);
        return String.format("%-" + length + "s", s);
    }

    /**
     * Clear the log console.
     */
    public static void clear() {
        Platform.runLater(() -> {
            if (targetTextArea != null) {
                targetTextArea.clear();
            }
        });
        messageBuffer.clear();
    }

    /**
     * Detach the TextArea.
     */
    public static void detach() {
        targetTextArea = null;
    }

    /**
     * Simple log message holder.
     */
    public static class LogMessage {
        private final String level;
        private final String message;
        private final String className;
        private final long timestamp;

        public LogMessage(String level, String message, String className, long timestamp) {
            this.level = level;
            this.message = message;
            this.className = className;
            this.timestamp = timestamp;
        }

        public String getLevel() { return level; }
        public String getMessage() { return message; }
        public String getClassName() { return className; }
        public long getTimestamp() { return timestamp; }
    }
}
