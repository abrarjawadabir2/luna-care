package com.example

import com.example.data.*
import com.example.viewmodel.LunaViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

/**
 * 1-100x realistic user simulation and adversarial bug-hunt tests for LunaCare Android core logic.
 */
class UserSimulation100xTest {

    @Test
    fun `100 iterations of CycleUtils strict calendar date validation reject impossible dates`() {
        val impossibleDates = listOf(
            "2026-02-29", "2026-02-30", "2026-02-31",
            "2026-04-31", "2026-06-31", "2026-09-31", "2026-11-31",
            "2026-00-10", "2026-13-01", "2026-05-00", "2026-5-1", "not-a-date", ""
        )
        for (iteration in 1..100) {
            val candidate = impossibleDates[iteration % impossibleDates.size]
            assertFalse("Iteration $iteration: impossible date '$candidate' must be rejected", CycleUtils.isValidDate(candidate))
        }
        // Valid leap day and normal dates must still pass
        assertTrue(CycleUtils.isValidDate("2024-02-29"))
        assertTrue(CycleUtils.isValidDate("2026-02-28"))
        assertTrue(CycleUtils.isValidDate("2026-12-31"))
    }

    @Test
    fun `100 iterations of CycleUtils getCyclePhase handle future dates and boundary cycle lengths safely`() {
        for (i in 1..100) {
            // Future period start date -> cycleDay <= 0 must NOT misreport Luteal Phase
            val futurePhase = CycleUtils.getCyclePhase(-i, 28, 5)
            assertNotEquals("Iteration $i: negative cycleDay should not be Luteal Phase", "Luteal Phase", futurePhase)
            assertEquals("Upcoming", futurePhase)

            // Zero cycle day
            assertEquals("Upcoming", CycleUtils.getCyclePhase(0, 28, 5))

            // Valid cycle day across varied cycle lengths 21..45 and period lengths 2..10
            val cycleLen = 21 + (i % 25)
            val periodLen = 2 + (i % 9)
            val day = 1 + (i % cycleLen)
            val phase = CycleUtils.getCyclePhase(day, cycleLen, periodLen)
            assertTrue(
                "Iteration $i: phase '$phase' must be valid",
                phase in setOf("Menstrual Phase", "Follicular Phase", "Ovulatory Phase", "Luteal Phase")
            )
        }
    }

    @Test
    fun `100 iterations of SecurityUtils PBKDF2 PIN hashing, verification, and malformed decrypt safety`() {
        for (i in 1..100) {
            val pin = "%04d".format((i * 73) % 10000)
            val wrongPin = "%04d".format(((i * 73) + 1) % 10000)
            val hash = SecurityUtils.hashPin(pin)
            assertTrue("Iteration $i: correct PIN must verify", SecurityUtils.verifyPin(pin, hash))
            assertFalse("Iteration $i: wrong PIN must fail", SecurityUtils.verifyPin(wrongPin, hash))
            assertEquals(4, SecurityUtils.extractPinLength(hash))
        }
        // Malformed encrypted payloads must safely return null, never crash
        assertNull(SecurityUtils.decrypt(""))
        assertNull(SecurityUtils.decrypt("not-base64!!!"))
        assertNull(SecurityUtils.decrypt("AAAA"))
    }

    @Test
    fun `100 iterations of LocationPrivacyManager URL building encode special characters safely`() {
        val queries = listOf(
            "pharmacy & dispensary", "clinic/gynecologist?test=1", "warm tea café",
            "pads + cup", "clinic #2"
        )
        for (i in 1..100) {
            val q = queries[i % queries.size]
            val urlNoCoords = LocationPrivacyManager.buildGoogleMapsSearchUrl(q)
            assertTrue(urlNoCoords.startsWith("https://www.google.com/maps/search/"))
            assertFalse(urlNoCoords.contains(" "))
            assertFalse(urlNoCoords.contains("/@"))

            val urlWithCoords = LocationPrivacyManager.buildGoogleMapsSearchUrl(q, 23.8103, 90.4125)
            assertTrue(urlWithCoords.contains("/@23.8103,90.4125,14z"))
        }
    }

    @Test
    fun `AI monthly quota resets accurately on month rollover across 100 simulated months`() {
        val currentMonth = YearMonth.now().toString()
        val prevMonth = YearMonth.now().minusMonths(1).toString()

        for (i in 1..100) {
            val isPremium = i % 5 == 0
            val limit = if (isPremium) LunaViewModel.PREMIUM_AI_MESSAGES_LIMIT else LunaViewModel.FREE_AI_MESSAGES_LIMIT

            // Exhausted LAST month -> MUST reset to 0 used / full quota remaining in current month
            val lastMonthProfile = Profile(
                isPremium = isPremium,
                aiMessagesUsedThisMonth = limit,
                aiCurrentMonth = prevMonth
            )
            assertFalse(
                "Iteration $i: exhausted last month must NOT lock out user in current month",
                LunaViewModel.isAiLimitReachedForProfile(lastMonthProfile, currentMonth)
            )
            assertEquals(
                "Iteration $i: remaining messages must reset to full limit on new month",
                limit,
                LunaViewModel.getRemainingAiMessagesForProfile(lastMonthProfile, currentMonth)
            )

            // Exhausted THIS month -> MUST report limit reached & 0 remaining
            val thisMonthExhausted = lastMonthProfile.copy(aiCurrentMonth = currentMonth)
            assertTrue(LunaViewModel.isAiLimitReachedForProfile(thisMonthExhausted, currentMonth))
            assertEquals(0, LunaViewModel.getRemainingAiMessagesForProfile(thisMonthExhausted, currentMonth))
        }
    }

    @Test
    fun `Cycle settings validation rejects impossible ranges across 100 inputs`() {
        for (i in 1..100) {
            val cycle = 15 + (i % 46) // 15..60
            val period = 1 + (i % minOf(20, cycle - 1)) // strictly < cycle
            assertTrue("Valid ($cycle, $period) must pass", LunaViewModel.isValidCycleSettings(cycle, period))
        }
        // Invalid cases
        assertFalse(LunaViewModel.isValidCycleSettings(0, 5))
        assertFalse(LunaViewModel.isValidCycleSettings(-28, 5))
        assertFalse(LunaViewModel.isValidCycleSettings(14, 5))
        assertFalse(LunaViewModel.isValidCycleSettings(61, 5))
        assertFalse(LunaViewModel.isValidCycleSettings(28, 0))
        assertFalse(LunaViewModel.isValidCycleSettings(28, 21))
        assertFalse(LunaViewModel.isValidCycleSettings(15, 15)) // period == cycle
        assertFalse(LunaViewModel.isValidCycleSettings(15, 18)) // period > cycle
    }

    @Test
    fun `Date range and reminder time validation reject reversed ranges and impossible times across 100 inputs`() {
        for (i in 1..100) {
            val day = 1 + (i % 20)
            val start = "2026-05-%02d".format(day)
            val validEnd = "2026-05-%02d".format(day + 4)
            val reversedEnd = "2026-04-%02d".format(day)
            assertTrue("Iteration $i: valid range must pass", CycleUtils.isValidDateRange(start, validEnd))
            assertTrue("Iteration $i: same-day range must pass", CycleUtils.isValidDateRange(start, start))
            assertFalse("Iteration $i: reversed range must fail", CycleUtils.isValidDateRange(start, reversedEnd))

            val hour = i % 24
            val minute = (i * 7) % 60
            assertTrue(LunaViewModel.isValidReminderTime("%02d:%02d".format(hour, minute)))
        }
        assertFalse(LunaViewModel.isValidReminderTime("24:00"))
        assertFalse(LunaViewModel.isValidReminderTime("12:60"))
        assertFalse(LunaViewModel.isValidReminderTime("9:00"))
        assertFalse(LunaViewModel.isValidReminderTime("not-a-time"))
    }

    @Test
    fun `Username validation blocks impersonation prefixes and invalid formats across 100 attempts`() {
        val badNames = listOf(
            "admin", "administrator", "lunacare", "lunacare_support", "official_lunacare",
            "admin_123", "support_team", "ab", "a".repeat(31), ".leading", "trailing.",
            "double..dot", "bad space", "user@name", "doctor", "emergency"
        )
        for (i in 1..100) {
            val candidate = badNames[i % badNames.size]
            val result = LunaViewModel.validateUsernameRules(candidate)
            assertFalse("Iteration $i: '$candidate' must be rejected", result.isValid)
        }
        for (i in 1..100) {
            val good = "luna.user_$i"
            val result = LunaViewModel.validateUsernameRules(good)
            assertTrue("Iteration $i: '$good' must be valid", result.isValid)
            assertEquals(good.lowercase(), result.normalized)
        }
    }
}
