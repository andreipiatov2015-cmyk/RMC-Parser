package com.rmc.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LifecycleState enum.
 */
class LifecycleStateTest {

    @Test
    void testAllStatesExist() {
        assertNotNull(LifecycleState.NOT_STARTED);
        assertNotNull(LifecycleState.LOADING_CONFIG);
        assertNotNull(LifecycleState.CONFIG_LOADED);
        assertNotNull(LifecycleState.INITIALIZING_LOGGING);
        assertNotNull(LifecycleState.LOGGING_INITIALIZED);
        assertNotNull(LifecycleState.LOADING_VERSION_ENGINE);
        assertNotNull(LifecycleState.VERSION_ENGINE_LOADED);
        assertNotNull(LifecycleState.CHECKING_UPDATES);
        assertNotNull(LifecycleState.UPDATES_CHECKED);
        assertNotNull(LifecycleState.DETECTING_DRIVER);
        assertNotNull(LifecycleState.DRIVER_DETECTED);
        assertNotNull(LifecycleState.VALIDATING_DRIVER);
        assertNotNull(LifecycleState.DRIVER_VALIDATED);
        assertNotNull(LifecycleState.READY);
        assertNotNull(LifecycleState.FAILED);
    }

    @Test
    void testStateCount() {
        assertEquals(15, LifecycleState.values().length);
    }

    @Test
    void testStateOrder() {
        LifecycleState[] states = LifecycleState.values();
        
        assertEquals(0, states[0].ordinal());
        assertEquals(LifecycleState.NOT_STARTED, states[0]);
        
        assertEquals(14, states[states.length - 1].ordinal());
        assertEquals(LifecycleState.FAILED, states[states.length - 1]);
    }

    @Test
    void testValueOf() {
        assertEquals(LifecycleState.NOT_STARTED, LifecycleState.valueOf("NOT_STARTED"));
        assertEquals(LifecycleState.READY, LifecycleState.valueOf("READY"));
        assertEquals(LifecycleState.FAILED, LifecycleState.valueOf("FAILED"));
    }
}
