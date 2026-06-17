package com.rmc.app;

/**
 * Represents the current state of the application lifecycle.
 */
public enum LifecycleState {
    NOT_STARTED,
    LOADING_CONFIG,
    CONFIG_LOADED,
    INITIALIZING_LOGGING,
    LOGGING_INITIALIZED,
    LOADING_VERSION_ENGINE,
    VERSION_ENGINE_LOADED,
    CHECKING_UPDATES,
    UPDATES_CHECKED,
    DETECTING_DRIVER,
    DRIVER_DETECTED,
    VALIDATING_DRIVER,
    DRIVER_VALIDATED,
    READY,
    FAILED
}
