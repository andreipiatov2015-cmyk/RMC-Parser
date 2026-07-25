package com.rmc.auth.account;

import java.util.Optional;

/**
 * Сохранённая учётная запись: логин, зашифрованный пароль и необязательные
 * настройки внешнего вида (отображаемое имя; аватар хранится отдельным
 * файлом, см. {@link AccountStorageService#getAvatarFile(String)}).
 *
 * <p>Пароль хранится не в открытом виде, а зашифрованным (AES-GCM,
 * ключ — локальный файл рядом, см. {@link AccountStorageService}). Это
 * защита "от случайного чтения файла", а не полноценное хранение секретов —
 * при физическом доступе к профилю пользователя ключ и зашифрованные данные
 * лежат рядом и теоретически могут быть сопоставлены.</p>
 */
public class SavedAccount {
    
    private final String username;
    private final String encryptedPassword;
    private final String displayName;
    
    public SavedAccount(String username, String encryptedPassword) {
        this(username, encryptedPassword, null);
    }
    
    public SavedAccount(String username, String encryptedPassword, String displayName) {
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.displayName = displayName;
    }
    
    public String getUsername() {
        return username;
    }
    
    /**
     * @return пароль в зашифрованном виде (base64: IV + шифротекст).
     * Для получения пароля в открытом виде используйте
     * {@link AccountStorageService#decryptPassword(SavedAccount)}.
     */
    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    /**
     * @return "красивое" имя, заданное пользователем в настройках
     * внешнего вида (например, "Администратор" вместо "region_rmc"),
     * если оно задано.
     */
    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName).filter(s -> !s.isBlank());
    }
}
