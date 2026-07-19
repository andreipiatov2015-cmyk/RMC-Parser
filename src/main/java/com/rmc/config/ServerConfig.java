package com.rmc.config;

/**
 * Единая точка правды для адреса сервера РМЦ — раньше был захардкожен
 * отдельно в нескольких местах (AuthView, AccountPickerView,
 * WorkspaceContainer и др.), что легко было забыть поправить везде разом.
 *
 * <p>Пока это просто константа. Если в будущем понадобится смена сервера
 * без пересборки программы, сюда же можно добавить загрузку из файла
 * настроек (например, Properties-файл рядом с логами в
 * {@code %LOCALAPPDATA%\RMCFramework}).</p>
 */
public final class ServerConfig {
    
    public static final String BASE_URL = "https://rmc.ruobr.ru";
    
    private ServerConfig() {
        // Утилитарный класс
    }
}
