package com.rmc.i18n;

/**
 * Русские локализованные сообщения для приложения RMC Framework.
 * 
 * <p>Класс содержит все пользовательские строки на русском языке.</p>
 */
public class Messages {

    // ==================== Главное окно ====================
    
    public static final String APP_TITLE = "RMC Framework";
    public static final String VERSION_PREFIX = "Версия";
    
    // Главное окно
    public static final String BTN_CHECK_UPDATES = "Проверить обновления";
    public static final String BTN_DEVELOPER_DIAGNOSTICS = "Диагностика разработчика";
    
    // Диалог информации
    public static final String DIALOG_INFO_TITLE = "RMC Framework";
    public static final String DIALOG_INFO_HEADER = "Доступна диагностика разработчика";
    public static final String DIALOG_INFO_CONTENT = "Нажмите 'Диагностика разработчика' для доступа к:\n" +
            "• Консоли живых логов\n" +
            "• Панели состояния системы\n" +
            "• Инструментам управления драйвером\n" +
            "• Диагностическим отчётам";
    
    // ==================== Окно диагностики ====================
    
    // Вкладки
    public static final String TAB_DIAGNOSTICS = "Диагностика";
    public static final String TAB_ENVIRONMENT = "Окружение";
    public static final String TAB_ABOUT = "О программе";
    
    // Статус приложения
    public static final String STATUS_NOT_INITIALIZED = "Статус: Не инициализировано";
    public static final String STATUS_READY = "Статус: Готово";
    public static final String STATUS_NOT_READY = "Статус: Не готово";
    public static final String STATUS_FAILED = "Статус: Ошибка";
    
    // Статусы подсистем
    public static final String STATUS_OK = "ОК";
    public static final String STATUS_ERROR = "ОШИБКА";
    public static final String STATUS_WARNING = "ВНИМАНИЕ";
    
    // Кнопки действий
    public static final String BTN_CHECK_UPDATES_LONG = "Проверить обновления";
    public static final String BTN_CHECK_DRIVER = "Проверить драйвер";
    public static final String BTN_DOWNLOAD_DRIVER = "Скачать драйвер";
    public static final String BTN_VALIDATE_DRIVER = "Проверить драйвер";
    public static final String BTN_RUN_STARTUP = "Запустить последовательность";
    public static final String BTN_CREATE_REPORT = "Создать отчёт";
    public static final String BTN_OPEN_LOG_FOLDER = "Открыть папку логов";
    public static final String BTN_CLEAR_DRIVER = "Очистить драйвер";
    public static final String BTN_CLEAR_LOGS = "Очистить логи";
    public static final String BTN_EXIT = "Выход";
    public static final String BTN_REFRESH_ENV = "Обновить информацию";
    public static final String BTN_COPY_CLIPBOARD = "Копировать в буфер";
    
    // Окно "О программе"
    public static final String ABOUT_TITLE = "RMC Framework";
    public static final String ABOUT_DESCRIPTION = "Система управления Microsoft Edge WebDriver";
    public static final String ABOUT_MODE = "Режим диагностики разработчика";
    
    // ==================== Журнал запуска ====================
    
    public static final String LOG_LIFECYCLE_START = "==================================================\n" +
            "Жизненный цикл приложения - Запуск\n" +
            "==================================================";
    
    public static final String LOG_LIFECYCLE_COMPLETE = "==================================================\n" +
            "Жизненный цикл приложения - Успешно завершён\n" +
            "==================================================";
    
    public static final String LOG_LIFECYCLE_FAILED = "==================================================\n" +
            "Жизненный цикл приложения - Ошибка\n" +
            "==================================================";
    
    // Шаги инициализации
    public static final String LOG_STEP_CONFIG = "[1/7] Загрузка конфигурации...";
    public static final String LOG_STEP_LOGGING = "[2/7] Инициализация логирования...";
    public static final String LOG_STEP_VERSION = "[3/7] Загрузка модуля версий...";
    public static final String LOG_STEP_UPDATE = "[4/7] Проверка обновлений...";
    public static final String LOG_STEP_DRIVER_DETECT = "[5/7] Обнаружение драйвера...";
    public static final String LOG_STEP_DRIVER_VALIDATE = "[6/7] Проверка драйвера...";
    public static final String LOG_STEP_READY = "[7/7] Приложение готово";
    
    public static final String LOG_CONFIG_LOADED = "Конфигурация загружена - JSON URL: {}";
    public static final String LOG_UPDATE_CHANNEL = "Канал обновлений: {}";
    public static final String LOG_CONFIG_OK = "Конфигурация: ОК";
    public static final String LOG_CONFIG_ERROR = "Не удалось загрузить конфигурацию";
    
    public static final String LOG_LOGGING_OK = "Логирование: ОК";
    public static final String LOG_LOGGING_ERROR = "Не удалось инициализировать логирование";
    
    public static final String LOG_VERSION_LOADED = "Модуль версий загружен - Текущая версия: {}";
    public static final String LOG_VERSION_OK = "Модуль версий: ОК";
    public static final String LOG_VERSION_ERROR = "Не удалось загрузить модуль версий";
    
    public static final String LOG_UPDATE_CHECK_SUCCESS = "Проверка обновлений успешна - HTTP статус: {}";
    public static final String LOG_UPDATE_CHECK_ERROR = "Проверка обновлений завершена с ошибками";
    public static final String LOG_UPDATE_OK = "Проверка обновлений: ОК";
    public static final String LOG_UPDATE_CHECK_FAILED = "Не удалось проверить обновления (некритично)";
    
    public static final String LOG_APP_READY = "Приложение готово к использованию";
    
    // ==================== Действия разработчика ====================
    
    public static final String LOG_DEV_CHECK_UPDATES = "Действие разработчика: Проверка обновлений...";
    public static final String LOG_DEV_CHECK_DRIVER = "Действие разработчика: Проверка драйвера...";
    public static final String LOG_DEV_DOWNLOAD_DRIVER = "Действие разработчика: Загрузка драйвера...";
    public static final String LOG_DEV_VALIDATE_DRIVER = "Действие разработчика: Проверка драйвера...";
    public static final String LOG_DEV_RUN_STARTUP = "Действие разработчика: Запуск последовательности...";
    public static final String LOG_DEV_CREATE_REPORT = "Действие разработчика: Создание диагностического отчёта...";
    public static final String LOG_DEV_OPEN_LOGS = "Действие разработчика: Открытие папки логов...";
    public static final String LOG_DEV_CLEAR_DRIVER = "Действие разработчика: Очистка драйвера...";
    public static final String LOG_DEV_CLEAR_LOGS = "Действие разработчика: Очистка логов...";
    public static final String LOG_DEV_EXIT = "Окно диагностики разработчика закрыто";
    public static final String LOG_DEV_WINDOW_OPEN = "Окно диагностики разработчика открыто";
    
    // ==================== Microsoft Edge ====================
    
    public static final String LOG_EDGE_INSTALLED = "Microsoft Edge: УСТАНОВЛЕН";
    public static final String LOG_EDGE_NOT_INSTALLED = "Microsoft Edge: НЕ УСТАНОВЛЕН";
    public static final String LOG_EDGE_DETECTED = "Обнаружен Microsoft Edge - Версия: {}, Путь: {}";
    
    // ==================== WebDriver ====================
    
    public static final String LOG_DRIVER_INSTALLED = "Edge WebDriver: УСТАНОВЛЕН";
    public static final String LOG_DRIVER_NOT_INSTALLED = "Edge WebDriver: НЕ УСТАНОВЛЕН";
    public static final String LOG_DRIVER_DETECTED = "Обнаружен Edge WebDriver - Версия: {}, Путь: {}";
    public static final String LOG_DRIVER_STATUS = "Статус драйвера: {}";
    
    // ==================== Проверка обновлений ====================
    
    public static final String LOG_UPDATE_SERVICE = "==================================================\n" +
            "Служба обновлений\n" +
            "==================================================";
    
    public static final String LOG_LOADING_CONFIG = "Загрузка конфигурации...";
    public static final String LOG_JSON_URL = "JSON URL: {}";
    
    public static final String LOG_CONNECTING = "Подключение к: {}";
    public static final String LOG_HTTP_STATUS = "HTTP статус: {}";
    public static final String LOG_RESPONSE_LENGTH = "Размер ответа: {} байт";
    public static final String LOG_DOWNLOAD_TIME = "Время загрузки: {} мс";
    public static final String LOG_CONNECTION_SUCCESS = "Подключение успешно";
    public static final String LOG_JSON_DOWNLOADED = "JSON успешно загружен";
    
    public static final String LOG_UPDATE_CHECK_COMPLETE = "Проверка обновлений завершена";
    public static final String LOG_UPDATE_CHECK_END = "==================================================";
    
    public static final String LOG_UNEXPECTED_STATUS = "Неожиданный HTTP статус: {}";
    public static final String LOG_RESPONSE_BODY = "Тело ответа: {}";
    
    public static final String LOG_TIMEOUT = "Превышен интервал ожидания подключения";
    public static final String LOG_UNKNOWN_HOST = "Неизвестный узел - сеть недоступна";
    public static final String LOG_CONNECTION_ERROR = "Ошибка подключения";
    public static final String LOG_CONFIG_ERROR_PREFIX = "Ошибка конфигурации: ";
    
    // ==================== Служба загрузки ====================
    
    public static final String LOG_DOWNLOAD_SERVICE = "==================================================\n" +
            "Служба загрузки драйверов\n" +
            "==================================================";
    
    public static final String LOG_DETECTING_EDGE = "Определение версии Microsoft Edge...";
    public static final String LOG_EDGE_NOT_FOUND = "Microsoft Edge не найден";
    public static final String LOG_EDGE_FOUND = "Microsoft Edge найден - Версия: {}";
    
    public static final String LOG_RESOLVING_URL = "Разрешение URL для загрузки...";
    public static final String LOG_DOWNLOAD_URL = "URL загрузки: {}";
    public static final String LOG_DOWNLOAD_PATH = "Путь для сохранения: {}";
    
    public static final String LOG_DOWNLOADING = "Загрузка файла...";
    public static final String LOG_DOWNLOAD_COMPLETE = "Загрузка завершена - Размер: {} байт";
    public static final String LOG_DOWNLOAD_FAILED = "Загрузка не удалась";
    
    public static final String LOG_EXTRACTING = "Распаковка архива...";
    public static final String LOG_EXTRACT_COMPLETE = "Распаковка завершена";
    public static final String LOG_EXTRACT_FAILED = "Распаковка не удалась";
    
    public static final String LOG_DRIVER_INSTALLED_PATH = "Драйвер установлен: {}";
    public static final String LOG_DOWNLOAD_SUCCESS = "Загрузка драйвера успешно завершена";
    public static final String LOG_DOWNLOAD_SERVICE_END = "==================================================";
    
    // ==================== Обнаружение драйвера ====================
    
    public static final String LOG_SEARCHING_DRIVER = "Поиск Edge WebDriver...";
    public static final String LOG_CHECKING_PATH = "Проверка пути: {}";
    public static final String LOG_FOUND_DRIVER = "Edge WebDriver найден";
    public static final String LOG_NOT_FOUND = "Edge WebDriver не найден";
    
    // ==================== Проверка драйвера ====================
    
    public static final String LOG_VALIDATION_SERVICE = "==================================================\n" +
            "Служба проверки драйверов\n" +
            "==================================================";
    
    public static final String LOG_VALIDATING = "Проверка драйвера: {}";
    public static final String LOG_BROWSER_VERSION = "Версия браузера: {}";
    public static final String LOG_EXPECTED_VERSION = "Ожидаемая версия: {}";
    public static final String LOG_PLATFORM = "Платформа: {}";
    public static final String LOG_ARCHITECTURE = "Архитектура: {}";
    
    public static final String LOG_DRIVER_EXISTS = "Драйвер существует: ДА";
    public static final String LOG_DRIVER_NOT_EXISTS = "Драйвер существует: НЕТ";
    
    public static final String LOG_DRIVER_VERSION_FOUND = "Версия драйвера: {}";
    public static final String LOG_DRIVER_VERSION_UNKNOWN = "Версия драйвера: НЕИЗВЕСТНО";
    
    public static final String LOG_VERSION_MATCH = "Версия совпадает: ДА";
    public static final String LOG_VERSION_MISMATCH = "Версия совпадает: НЕТ";
    
    public static final String LOG_VALIDATION_PASS = "Проверка: УСПЕШНО";
    public static final String LOG_VALIDATION_FAIL = "Проверка: ОШИБКА - {}";
    public static final String LOG_VALIDATION_NOT_FOUND = "Проверка: ОШИБКА - Драйвер не найден";
    public static final String LOG_VALIDATION_END = "==================================================";
    
    // ==================== Selenium Manager ====================
    // Selenium Manager - официальный менеджер драйверов Selenium 4.6+
    
    public static final String LOG_SM_HEADER = "==============================";
    public static final String LOG_SM_PREPARING_SELENIUM = "Подготовка Selenium";
    public static final String LOG_SM_EDGE_FOUND = "Microsoft Edge найден";
    public static final String LOG_SM_EDGE_VERSION = "Версия браузера:\n{}";
    public static final String LOG_SM_DRIVER_MISSING = "Драйвер не найден.";
    public static final String LOG_SM_DRIVER_ALREADY_READY = "Драйвер уже готов к использованию.";
    public static final String LOG_SM_STARTING_SM = "Запуск Selenium Manager...";
    public static final String LOG_SM_FINDING_COMPATIBLE = "Определение совместимого драйвера...";
    public static final String LOG_SM_CHECKING_CACHE = "Проверка локального кэша...";
    public static final String LOG_SM_DOWNLOAD_IF_NEEDED = "При необходимости выполняется загрузка...";
    public static final String LOG_SM_DOWNLOAD_COMPLETE = "Драйвер загружен.";
    public static final String LOG_SM_DRIVER_READY = "Драйвер готов.";
    public static final String LOG_SM_DRIVER_FOUND = "Драйвер найден.";
    public static final String LOG_SM_PATH = "Путь:\n{}";
    public static final String LOG_SM_SUBSYSTEM_READY = "Подсистема готова.";
    public static final String LOG_SM_SUCCESS = "Подготовка успешно завершена";
    public static final String LOG_SM_NO_EDGE = "Microsoft Edge не установлен. Установите Microsoft Edge для продолжения.";
    public static final String LOG_SM_ERROR_PREFIX = "Ошибка Selenium Manager: ";
    
    // ==================== Диагностический отчёт ====================
    
    public static final String LOG_REPORT_COPIED = "Отчёт скопирован в буфер обмена";
    
    // ==================== Прочее ====================
    
    public static final String LOG_LATEST_VERSION = "Последняя версия: {}";
    public static final String LOG_NO_UPDATE_INFO = "Информация об обновлении недоступна";
    public static final String LOG_UPDATE_CHECK_ERROR_LOG = "Не удалось проверить обновления";
    
    public static final String LOG_EDGE_STATUS = "Edge: {} ({})";
    public static final String LOG_DRIVER_STATUS_DETAIL = "Драйвер: {} ({})";
    
    public static final String LOG_DOWNLOAD_RESULT = "Результат загрузки: {}";
    public static final String LOG_DOWNLOAD_SUCCESS_PATH = "Загрузка успешна: {}";
    public static final String LOG_DOWNLOAD_FAILED_MSG = "Загрузка не удалась: {}";
    
    public static final String LOG_VALIDATION_RESULT = "Результат проверки: {}";
    public static final String LOG_VALIDATION_PASS_DETAIL = "Проверка УСПЕШНА: {}";
    public static final String LOG_VALIDATION_FAIL_DETAIL = "Проверка ОШИБКА: {}";
    
    public static final String LOG_CANNOT_VALIDATE = "Невозможно проверить: Edge или драйвер не установлены";
    public static final String LOG_SKIP_VALIDATION = "Пропуск проверки - Edge или драйвер не обнаружены";
    
    public static final String LOG_STARTUP_COMPLETE = "Последовательность запуска завершена";
    public static final String LOG_STARTUP_FAILED = "Последовательность запуска не удалась";
    
    public static final String LOG_NO_DRIVER_TO_CLEAR = "Нет драйвера для очистки";
    public static final String LOG_DRIVER_CLEARED = "Драйвер успешно очищен";
    public static final String LOG_CLEAR_DRIVER_FAILED = "Не удалось очистить драйвер";
    
    public static final String LOG_LOGS_CLEARED = "Логи очищены";
    
    // ==================== Информация об окружении ====================
    
    public static final String ENV_JAVA = "=== Среда выполнения Java ===";
    public static final String ENV_OS = "=== Операционная система ===";
    public static final String ENV_USER = "=== Среда пользователя ===";
    public static final String ENV_APPLICATION = "=== Приложение ===";
    public static final String ENV_EDGE = "=== Microsoft Edge ===";
    public static final String ENV_DRIVER = "=== Edge WebDriver ===";
    public static final String ENV_LOGGING = "=== Логирование ===";
    public static final String ENV_NETWORK = "=== Сеть ===";
    
    public static final String ENV_INSTALLED_YES = "Установлен: Да";
    public static final String ENV_INSTALLED_NO = "Установлен: Нет";
    public static final String ENV_NOT_INSTALLED = "Не установлен";
    public static final String ENV_VERSION = "Версия: {}";
    public static final String ENV_PATH = "Путь: {}";
    public static final String ENV_STATUS = "Статус: {}";
    
    public static final String ENV_GITHUB_REACHABLE = "GitHub API: Доступен";
    public static final String ENV_GITHUB_NOT_REACHABLE = "GitHub API: Недоступен";
    public static final String ENV_TEMP_DIR = "Временная директория: {}";
    public static final String ENV_CONFIG_ERROR = "Конфигурация: Ошибка загрузки";
    
    // ==================== Диагностический отчёт ====================
    
    public static final String REPORT_TITLE = "=== Диагностический отчёт RMC Framework ===";
    public static final String REPORT_GENERATED = "Создано: {}";
    public static final String REPORT_VERSION = "Текущая версия: {}";
    public static final String REPORT_JSON_URL = "JSON URL: {}";
    public static final String REPORT_CHANNEL = "Канал обновлений: {}";
    public static final String REPORT_CONFIG_ERROR = "Конфигурация: Ошибка";
    public static final String REPORT_SUMMARY = "=== Результат последнего запуска ===";
    public static final String REPORT_SYS_INFO = "=== Информация о системе ===";
    
    // ==================== Статусы ====================
    
    public static final String STATUS_CONFIG = "Конфиг";
    public static final String STATUS_LOGGING = "Логи";
    public static final String STATUS_VERSION = "Версия";
    public static final String STATUS_UPDATE = "Обновление";
    public static final String STATUS_DRIVER = "Драйвер";
    public static final String STATUS_VALIDATION = "Проверка";
    public static final String STATUS_INTERNET = "Интернет";
    
    private Messages() {
        // Приватный конструктор - класс только для констант
    }
}
