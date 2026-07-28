package com.rmc.auth.account;

import com.rmc.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Постоянное хранилище сохранённых учётных записей на диске.
 *
 * <p>Каталог — тот же принцип, что и у логов/истории поиска
 * ({@code %LOCALAPPDATA%\RMCFramework}), то есть отдельно от папки
 * установки программы — данные переживают обновления и перезагрузки.</p>
 *
 * <p>Пароли хранятся зашифрованными (AES-256-GCM). Ключ шифрования — тоже
 * локальный файл рядом. Это защищает от случайного чтения файла с
 * учётными данными текстовым редактором, но не является полноценной
 * защитой секретов: при целенаправленном доступе к профилю пользователя
 * ключ и зашифрованные данные лежат рядом друг с другом.</p>
 */
public class AccountStorageService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AVATAR_SIZE_PX = 160;
    
    private static final File ACCOUNTS_FILE;
    private static final File KEY_FILE;
    private static final File AVATARS_DIR;
    
    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        File dir = new File(localAppData + File.separator + "RMCFramework" + File.separator + "accounts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        ACCOUNTS_FILE = new File(dir, "accounts.json");
        KEY_FILE = new File(dir, "accounts.key");
        AVATARS_DIR = new File(dir, "avatars");
        if (!AVATARS_DIR.exists()) {
            AVATARS_DIR.mkdirs();
        }
    }
    
    private AccountStorageService() {
        // Утилитарный класс
    }
    
    /**
     * Загрузить все сохранённые учётные записи. При отсутствии или
     * повреждении файла возвращает пустой список — исключение наружу
     * не бросает.
     */
    public static List<SavedAccount> loadAll() {
        if (!ACCOUNTS_FILE.exists()) {
            return new ArrayList<>();
        }
        
        try {
            String content = Files.readString(ACCOUNTS_FILE.toPath(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            
            JSONArray array = new JSONArray(content);
            List<SavedAccount> accounts = new ArrayList<>();
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String username = obj.optString("username", null);
                String encryptedPassword = obj.optString("password", null);
                String displayName = obj.optString("displayName", null);
                if (username != null && !username.isEmpty() && encryptedPassword != null) {
                    accounts.add(new SavedAccount(username, encryptedPassword, displayName));
                }
            }
            
            return accounts;
            
        } catch (Exception e) {
            logger.error("Не удалось загрузить сохранённые учётные записи: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * @return true, если для этого логина уже есть сохранённая запись
     */
    public static boolean isSaved(String username) {
        if (username == null) {
            return false;
        }
        return loadAll().stream().anyMatch(a -> username.equalsIgnoreCase(a.getUsername()));
    }
    
    private static Optional<SavedAccount> find(List<SavedAccount> accounts, String username) {
        return accounts.stream().filter(a -> username.equalsIgnoreCase(a.getUsername())).findFirst();
    }
    
    /**
     * Сохранить (или обновить) учётную запись. Пароль шифруется перед
     * записью на диск. Уже заданное отображаемое имя (если было) сохраняется.
     */
    public static void save(String username, String password) {
        if (username == null || username.isEmpty() || password == null) {
            return;
        }
        
        try {
            List<SavedAccount> accounts = loadAll();
            String existingDisplayName = find(accounts, username)
                    .flatMap(SavedAccount::getDisplayName)
                    .orElse(null);
            accounts.removeIf(a -> username.equalsIgnoreCase(a.getUsername()));
            
            String encrypted = encrypt(password);
            accounts.add(new SavedAccount(username, encrypted, existingDisplayName));
            
            writeAll(accounts);
            logger.info("Учётная запись сохранена: {}", username);
            
        } catch (Exception e) {
            logger.error("Не удалось сохранить учётную запись {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Задать/убрать отображаемое имя для сохранённой учётной записи
     * (настройка внешнего вида — "region_rmc" показывать как "Администратор").
     * Ничего не делает, если такой сохранённой записи нет.
     */
    public static void setDisplayName(String username, String displayName) {
        if (username == null) {
            return;
        }
        try {
            List<SavedAccount> accounts = loadAll();
            Optional<SavedAccount> existing = find(accounts, username);
            if (existing.isEmpty()) {
                return;
            }
            accounts.removeIf(a -> username.equalsIgnoreCase(a.getUsername()));
            accounts.add(new SavedAccount(username, existing.get().getEncryptedPassword(), displayName));
            writeAll(accounts);
            logger.info("Отображаемое имя обновлено для {}: {}", username, displayName);
        } catch (Exception e) {
            logger.error("Не удалось сохранить отображаемое имя для {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Сохранить аватар учётной записи — изображение приводится к единому
     * размеру и формату (PNG {@value #AVATAR_SIZE_PX}x{@value #AVATAR_SIZE_PX})
     * и кладётся в отдельный файл рядом с остальными данными аккаунтов.
     */
    public static boolean saveAvatar(String username, File sourceImage) {
        if (username == null || sourceImage == null || !sourceImage.exists()) {
            return false;
        }
        try {
            BufferedImage original = ImageIO.read(sourceImage);
            if (original == null) {
                logger.error("Не удалось прочитать изображение аватара: {}", sourceImage);
                return false;
            }
            
            // Обрезаем по центру до квадрата, затем масштабируем до
            // фиксированного размера — так аватар одинаково хорошо
            // смотрится в круглой рамке независимо от исходных пропорций.
            int side = Math.min(original.getWidth(), original.getHeight());
            int x = (original.getWidth() - side) / 2;
            int y = (original.getHeight() - side) / 2;
            BufferedImage cropped = original.getSubimage(x, y, side, side);
            
            BufferedImage resized = new BufferedImage(AVATAR_SIZE_PX, AVATAR_SIZE_PX, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cropped, 0, 0, AVATAR_SIZE_PX, AVATAR_SIZE_PX, null);
            g.dispose();
            
            File target = avatarFile(username);
            ImageIO.write(resized, "png", target);
            logger.info("Аватар сохранён для {}: {}", username, target);
            return true;
        } catch (Exception e) {
            logger.error("Не удалось сохранить аватар для {}: {}", username, e.getMessage());
            return false;
        }
    }
    
    /**
     * @return файл аватара учётной записи, если он был задан ранее
     */
    public static Optional<File> getAvatarFile(String username) {
        if (username == null) {
            return Optional.empty();
        }
        File file = avatarFile(username);
        return file.exists() ? Optional.of(file) : Optional.empty();
    }
    
    public static void removeAvatar(String username) {
        if (username == null) {
            return;
        }
        File file = avatarFile(username);
        if (file.exists() && !file.delete()) {
            logger.warn("Не удалось удалить файл аватара: {}", file);
        }
    }
    
    private static File avatarFile(String username) {
        // Логин уже проверяется сервером/используется как ID Django-аккаунта,
        // но на всякий случай убираем символы, недопустимые в имени файла.
        String safeName = username.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return new File(AVATARS_DIR, safeName + ".png");
    }
    
    /**
     * Удалить сохранённую учётную запись (например, при выходе с опцией
     * "забыть меня", если такая появится) — пока используется для замены
     * записи с новым паролем через {@link #save}.
     */
    public static void remove(String username) {
        if (username == null) {
            return;
        }
        try {
            List<SavedAccount> accounts = loadAll();
            boolean removed = accounts.removeIf(a -> username.equalsIgnoreCase(a.getUsername()));
            if (removed) {
                writeAll(accounts);
                removeAvatar(username);
                logger.info("Учётная запись удалена: {}", username);
            }
        } catch (Exception e) {
            logger.error("Не удалось удалить учётную запись {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Расшифровать пароль сохранённой учётной записи.
     *
     * @return пароль в открытом виде, либо {@link Optional#empty()}, если
     * расшифровать не удалось (например, ключ был утерян/повреждён)
     */
    public static Optional<String> decryptPassword(SavedAccount account) {
        try {
            return Optional.of(decrypt(account.getEncryptedPassword()));
        } catch (Exception e) {
            logger.error("Не удалось расшифровать пароль для {}: {}", account.getUsername(), e.getMessage());
            return Optional.empty();
        }
    }
    
    private static void writeAll(List<SavedAccount> accounts) throws Exception {
        JSONArray array = new JSONArray();
        for (SavedAccount account : accounts) {
            JSONObject obj = new JSONObject();
            obj.put("username", account.getUsername());
            obj.put("password", account.getEncryptedPassword());
            account.getDisplayName().ifPresent(name -> obj.put("displayName", name));
            array.put(obj);
        }
        Files.writeString(ACCOUNTS_FILE.toPath(), array.toString(2), StandardCharsets.UTF_8);
    }
    
    // ===== Шифрование =====
    
    private static String encrypt(String plaintext) throws Exception {
        SecretKey key = getOrCreateKey();
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    private static String decrypt(String base64) throws Exception {
        SecretKey key = getOrCreateKey();
        
        byte[] combined = Base64.getDecoder().decode(base64);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);
        
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    private static SecretKey getOrCreateKey() throws Exception {
        if (KEY_FILE.exists()) {
            byte[] keyBytes = Base64.getDecoder().decode(Files.readString(KEY_FILE.toPath(), StandardCharsets.UTF_8).trim());
            return new SecretKeySpec(keyBytes, "AES");
        }
        
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        
        Files.writeString(KEY_FILE.toPath(), Base64.getEncoder().encodeToString(key.getEncoded()), StandardCharsets.UTF_8);
        
        return key;
    }
}
