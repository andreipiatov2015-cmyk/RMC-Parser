package com.rmc.driver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriverStatus enum.
 */
class DriverStatusTest {

    @Test
    void testAllStatusValuesExist() {
        assertNotNull(DriverStatus.NOT_INSTALLED);
        assertNotNull(DriverStatus.MATCH);
        assertNotNull(DriverStatus.OUTDATED);
        assertNotNull(DriverStatus.UNKNOWN);
    }

    @Test
    void testStatusCount() {
        assertEquals(4, DriverStatus.values().length);
    }
}