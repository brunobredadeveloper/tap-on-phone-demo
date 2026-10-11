package dev.breda.tap

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceReadinessTest {
    @Test
    fun absentHardwareCannotBeReady() {
        assertEquals(DeviceReadiness.UNSUPPORTED, deviceReadiness(false, true))
    }

    @Test
    fun disabledHardwareRequiresSettings() {
        assertEquals(DeviceReadiness.DISABLED, deviceReadiness(true, false))
    }

    @Test
    fun enabledHardwareIsReady() {
        assertEquals(DeviceReadiness.READY, deviceReadiness(true, true))
    }
}
