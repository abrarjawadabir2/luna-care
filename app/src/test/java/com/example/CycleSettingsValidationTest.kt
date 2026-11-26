package com.example

import com.example.data.CycleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Realistic settings scenarios: invalid input must not silently become
 * a different cycle length or show a misleading successful save.
 */
class CycleSettingsValidationTest {
    @Test fun validCycleLengthsArePreserved() {
        assertEquals(28 to 5, CycleUtils.parseCycleSettings("28", "5"))
        assertEquals(43 to 9, CycleUtils.parseCycleSettings(" 43 ", " 9 "))
    }

    @Test fun invalidStringsAreRejected() {
        assertNull(CycleUtils.parseCycleSettings("", "5"))
        assertNull(CycleUtils.parseCycleSettings("hello", "5"))
        assertNull(CycleUtils.parseCycleSettings("28", "invalid"))
        assertNull(CycleUtils.parseCycleSettings("28.5", "5"))
    }

    @Test fun negativeZeroAndInconsistentDurationsAreRejected() {
        assertNull(CycleUtils.parseCycleSettings("0", "5"))
        assertNull(CycleUtils.parseCycleSettings("-28", "5"))
        assertNull(CycleUtils.parseCycleSettings("28", "0"))
        assertNull(CycleUtils.parseCycleSettings("28", "29"))
        assertNull(CycleUtils.parseCycleSettings("366", "5"))
    }
}
