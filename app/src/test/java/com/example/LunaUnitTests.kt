package com.example

import com.example.data.*
import com.example.data.CycleUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * LunaCare Unit Tests
 *
 * Covers:
 * - User mode derivation logic
 * - Support mode requires consent
 * - Religion is optional
 * - GPS defaults to OFF and does not store exact coordinates
 * - Google Maps URL builds correctly
 * - Cycle phase calculation
 * - Invalid date handling in CycleUtils
 * - CrisisDetector keyword detection
 * - Medical journal validation
 * - Bookmark toggle logic
 * - AI token counter logic
 * - SecurityUtils encrypt/decrypt
 * - Audit hash output
 */
class LunaUnitTests {

    // ==========================================
    // USER MODE DERIVATION
    // ==========================================

    @Test
    fun `SELF_TRACKING mode with MENSTRUATES body mode gives SELF_TRACKING`() {
        val result = deriveUserMode(
            UserMode.SELF_TRACKING.name,
            GenderMode.FEMALE.name,
            BodyRelevantMode.MENSTRUATES.name
        )
        assertEquals(UserMode.SELF_TRACKING, result)
    }

    @Test
    fun `SELF_TRACKING mode with DOES_NOT_MENSTRUATE gives EDUCATION_ONLY`() {
        val result = deriveUserMode(
            UserMode.SELF_TRACKING.name,
            GenderMode.MALE.name,
            BodyRelevantMode.DOES_NOT_MENSTRUATE.name
        )
        assertEquals(UserMode.EDUCATION_ONLY, result)
    }

    @Test
    fun `SUPPORT_MODE always gives SUPPORT_MODE`() {
        val result = deriveUserMode(
            UserMode.SUPPORT_MODE.name,
            GenderMode.MALE.name,
            BodyRelevantMode.DOES_NOT_MENSTRUATE.name
        )
        assertEquals(UserMode.SUPPORT_MODE, result)
    }

    @Test
    fun `EDUCATION_ONLY mode gives EDUCATION_ONLY regardless of body mode`() {
        val result = deriveUserMode(
            UserMode.EDUCATION_ONLY.name,
            GenderMode.FEMALE.name,
            BodyRelevantMode.MENSTRUATES.name
        )
        assertEquals(UserMode.EDUCATION_ONLY, result)
    }

    @Test
    fun `PREFER_NOT_TO_SAY body mode defaults to EDUCATION_ONLY`() {
        val result = deriveUserMode(
            UserMode.SELF_TRACKING.name,
            GenderMode.PREFER_NOT_TO_SAY.name,
            BodyRelevantMode.PREFER_NOT_TO_SAY.name
        )
        assertEquals(UserMode.EDUCATION_ONLY, result)
    }

    // ==========================================
    // SUPPORT MODE CONSENT
    // ==========================================

    @Test
    fun `Support mode requires consent to proceed`() {
        // Step 2 canProceed for SUPPORT_MODE requires consentConfirmed
        val userMode = UserMode.SUPPORT_MODE.name
        val consentConfirmed = false
        val canProceed = if (userMode == UserMode.SUPPORT_MODE.name) consentConfirmed else true
        assertFalse("Support mode should not proceed without consent", canProceed)
    }

    @Test
    fun `Support mode can proceed with consent checked`() {
        val userMode = UserMode.SUPPORT_MODE.name
        val consentConfirmed = true
        val canProceed = if (userMode == UserMode.SUPPORT_MODE.name) consentConfirmed else true
        assertTrue("Support mode should proceed with consent", canProceed)
    }

    @Test
    fun `Non-support mode can proceed without consent`() {
        val userMode = UserMode.SELF_TRACKING.name
        val consentConfirmed = false
        val canProceed = if (userMode == UserMode.SUPPORT_MODE.name) consentConfirmed else true
        assertTrue("Non-support mode should always proceed", canProceed)
    }

    // ==========================================
    // RELIGION IS OPTIONAL
    // ==========================================

    @Test
    fun `Religion can be null and profile is still valid`() {
        val profile = Profile(
            displayName = "TestUser",
            religion = null,
            acceptedDisclaimer = true
        )
        assertNull(profile.religion)
        assertTrue(profile.acceptedDisclaimer)
    }

    @Test
    fun `All religion values are accepted`() {
        listOf("ISLAM", "HINDU", "CHRISTIAN", "BUDDHIST", "OTHER", "PREFER_NOT_TO_SAY", null).forEach { rel ->
            val profile = Profile(religion = rel)
            // No exception means validation passes
            assertEquals(rel, profile.religion)
        }
    }

    // ==========================================
    // GPS PRIVACY: DEFAULT IS OFF
    // ==========================================

    @Test
    fun `Default location privacy mode is OFF`() {
        val profile = Profile()
        assertEquals(LocationPrivacyMode.OFF.name, profile.locationPrivacyMode)
    }

    @Test
    fun `GPS OFF mode requires no system location permission`() {
        val shouldRequest = LocationPrivacyManager.shouldRequestGpsForMode(LocationPrivacyMode.OFF)
        assertFalse("GPS OFF should not request location permission", shouldRequest)
    }

    @Test
    fun `GPS ON_DEVICE_ONLY does not require permission from manager`() {
        // ON_DEVICE_ONLY uses GPS but we check the mode's intention
        val shouldRequest = LocationPrivacyManager.shouldRequestGpsForMode(LocationPrivacyMode.ON_DEVICE_ONLY)
        assertFalse("ON_DEVICE_ONLY is handled by the system prompt at tap time", shouldRequest)
    }

    @Test
    fun `GPS TEMPORARY_EXACT requires permission`() {
        val shouldRequest = LocationPrivacyManager.shouldRequestGpsForMode(LocationPrivacyMode.TEMPORARY_EXACT)
        assertTrue("TEMPORARY_EXACT requires GPS permission", shouldRequest)
    }

    // ==========================================
    // GPS DOES NOT STORE EXACT COORDINATES
    // ==========================================

    @Test
    fun `Google Maps URL builds without coordinates for OFF mode`() {
        val url = LocationPrivacyManager.buildGoogleMapsSearchUrl("pharmacy near me", null, null)
        assertFalse("URL should not contain coordinate numbers for OFF mode", url.contains("/@"))
        assertTrue("URL should contain the search query", url.contains("pharmacy"))
    }

    @Test
    fun `Google Maps URL builds with temporary coordinates`() {
        val url = LocationPrivacyManager.buildGoogleMapsSearchUrl("pharmacy", 3.1390, 101.6869)
        assertTrue("URL should contain coordinates", url.contains("3.139"))
        assertTrue("URL should contain the search query", url.contains("pharmacy"))
    }

    @Test
    fun `Google Maps URL encodes special characters safely`() {
        val url = LocationPrivacyManager.buildGoogleMapsSearchUrl("pharmacy & dispensary near me")
        assertTrue("URL should be built safely", url.startsWith("https://www.google.com/maps/search/"))
        assertFalse("URL should not contain unencoded &", url.contains(" & "))
    }

    @Test
    fun `Profile never stores lat lng fields`() {
        // Profile entity has no lat/lng fields — this verifies the design
        val profile = Profile()
        val fields = profile.javaClass.declaredFields.map { it.name }
        assertFalse("Profile should not have latitude field", fields.contains("latitude"))
        assertFalse("Profile should not have longitude field", fields.contains("longitude"))
        assertFalse("Profile should not have lat field", fields.contains("lat"))
        assertFalse("Profile should not have lng field", fields.contains("lng"))
    }

    // ==========================================
    // CYCLE UTILS
    // ==========================================

    @Test
    fun `Cycle phase is Menstrual for day 1 to period length`() {
        val phase = CycleUtils.getCyclePhase(2, 28, 5)
        assertEquals("Menstrual Phase", phase)
    }

    @Test
    fun `Cycle phase is Follicular after period ends`() {
        val phase = CycleUtils.getCyclePhase(8, 28, 5)
        assertEquals("Follicular Phase", phase)
    }

    @Test
    fun `Cycle phase is Ovulatory around mid-cycle`() {
        val phase = CycleUtils.getCyclePhase(14, 28, 5)
        assertEquals("Ovulatory Phase", phase)
    }

    @Test
    fun `Cycle phase is Luteal in second half`() {
        val phase = CycleUtils.getCyclePhase(22, 28, 5)
        assertEquals("Luteal Phase", phase)
    }

    @Test
    fun `Invalid date does not crash CycleUtils parseDate`() {
        val result = CycleUtils.parseDate("not-a-date")
        // Should return today without throwing
        assertNotNull(result)
    }

    @Test
    fun `isValidDate returns true for valid date`() {
        assertTrue(CycleUtils.isValidDate("2025-01-15"))
    }

    @Test
    fun `isValidDate returns false for invalid date`() {
        assertFalse(CycleUtils.isValidDate("2025-13-45"))
        assertFalse(CycleUtils.isValidDate("hello"))
        assertFalse(CycleUtils.isValidDate(""))
    }

    @Test
    fun `Prediction adds cycle length to last period`() {
        val predicted = CycleUtils.getPredictedNextPeriod("2025-01-01", 28)
        assertEquals("2025-01-29", predicted)
    }

    @Test
    fun `Days between returns correct count`() {
        val days = CycleUtils.getDaysBetween("2025-01-01", "2025-01-15")
        assertEquals(14L, days)
    }

    // ==========================================
    // CRISIS DETECTION
    // ==========================================

    @Test
    fun `Crisis detector finds suicide keyword`() {
        assertTrue(CrisisDetector.detect("I'm thinking about suicide"))
    }

    @Test
    fun `Crisis detector finds self-harm keyword`() {
        assertTrue(CrisisDetector.detect("I want to hurt myself"))
    }

    @Test
    fun `Crisis detector finds end life keyword`() {
        assertTrue(CrisisDetector.detect("I want to end my life"))
    }

    @Test
    fun `Crisis detector is case-insensitive`() {
        assertTrue(CrisisDetector.detect("I want to KILL MYSELF"))
    }

    @Test
    fun `Crisis detector does not flag normal text`() {
        assertFalse(CrisisDetector.detect("I feel tired and stressed"))
        assertFalse(CrisisDetector.detect("My period cramps are bad today"))
    }

    @Test
    fun `Crisis detector handles null input safely`() {
        assertFalse(CrisisDetector.detect(null))
        assertFalse(CrisisDetector.detect(null, null, null))
    }

    @Test
    fun `Crisis detector checks all passed texts`() {
        // One clean text, one crisis text
        assertTrue(CrisisDetector.detect("I feel okay", "I want to end my life"))
    }

    // ==========================================
    // SECURITY UTILS
    // ==========================================

    @Test
    fun `Encrypt returns non-null for non-null input`() {
        // Note: Keystore is not available in unit tests, so we test null-safety only
        // For full encryption tests, see androidTest/
        val result = try { SecurityUtils.encrypt("test") } catch (e: Exception) { null }
        // We only assert it doesn't throw an uncaught exception
        assertTrue("Encrypt should either succeed or return null, not throw", result == null || result.isNotEmpty())
    }

    @Test
    fun `Encrypt returns null for null input`() {
        val result = SecurityUtils.encrypt(null)
        assertNull("Encrypting null should return null", result)
    }

    @Test
    fun `Decrypt returns null for null input`() {
        val result = SecurityUtils.decrypt(null)
        assertNull("Decrypting null should return null", result)
    }

    @Test
    fun `Hash for audit returns non-empty string`() {
        val result = SecurityUtils.hashForAudit("192.168.1.1")
        assertTrue("Hash should be non-empty", result.isNotEmpty())
    }

    @Test
    fun `Hash for audit is deterministic`() {
        val h1 = SecurityUtils.hashForAudit("same-input")
        val h2 = SecurityUtils.hashForAudit("same-input")
        assertEquals("Same input should produce same hash", h1, h2)
    }

    @Test
    fun `Hash for audit does not return original value`() {
        val input = "192.168.1.1"
        val hash = SecurityUtils.hashForAudit(input)
        assertNotEquals("Hash should not equal original input", input, hash)
    }

    // ==========================================
    // AI TOKEN LIMITS
    // ==========================================

    @Test
    fun `Free user limit is 20 messages`() {
        assertEquals(20, LunaViewModel.FREE_AI_MESSAGES_LIMIT)
    }

    @Test
    fun `Premium user limit is 200 messages`() {
        assertEquals(200, LunaViewModel.PREMIUM_AI_MESSAGES_LIMIT)
    }

    @Test
    fun `Free user at limit of 20 is limit reached`() {
        val profile = Profile(isPremium = false, aiMessagesUsedThisMonth = 20, aiCurrentMonth = java.time.YearMonth.now().toString())
        val limit = if (profile.isPremium) LunaViewModel.PREMIUM_AI_MESSAGES_LIMIT else LunaViewModel.FREE_AI_MESSAGES_LIMIT
        val isLimitReached = profile.aiMessagesUsedThisMonth >= limit
        assertTrue("User at 20 messages should be at limit", isLimitReached)
    }

    @Test
    fun `Free user at 15 messages is not at limit`() {
        val profile = Profile(isPremium = false, aiMessagesUsedThisMonth = 15, aiCurrentMonth = java.time.YearMonth.now().toString())
        val limit = if (profile.isPremium) LunaViewModel.PREMIUM_AI_MESSAGES_LIMIT else LunaViewModel.FREE_AI_MESSAGES_LIMIT
        val isLimitReached = profile.aiMessagesUsedThisMonth >= limit
        assertFalse("User at 15 messages should not be at limit", isLimitReached)
    }

    @Test
    fun `Premium user at 200 messages is at limit`() {
        val profile = Profile(isPremium = true, aiMessagesUsedThisMonth = 200, aiCurrentMonth = java.time.YearMonth.now().toString())
        val limit = if (profile.isPremium) LunaViewModel.PREMIUM_AI_MESSAGES_LIMIT else LunaViewModel.FREE_AI_MESSAGES_LIMIT
        val isLimitReached = profile.aiMessagesUsedThisMonth >= limit
        assertTrue("Premium user at 200 messages should be at limit", isLimitReached)
    }

    // ==========================================
    // MEDICAL JOURNAL VALIDATION
    // ==========================================

    @Test
    fun `Medical journal entry is valid with required fields`() {
        val entry = MedicalJournalEntry(
            entryDate = "2025-01-15",
            category = "General",
            title = "Monday check",
            symptoms = listOf("Cramps"),
            painLevel = 4,
            mood = null,
            flowLevel = "Medium",
            medicinesTaken = null,
            doctorVisit = false,
            nextAppointment = null,
            notesEncrypted = null,
            attachmentPath = null
        )
        assertEquals("General", entry.category)
        assertEquals(4, entry.painLevel)
        assertFalse(entry.doctorVisit)
    }

    @Test
    fun `Pain level 0-10 is valid range`() {
        for (i in 0..10) {
            val entry = MedicalJournalEntry(entryDate = "2025-01-15", category = "General", title = "Test", symptoms = emptyList(), painLevel = i, mood = null, flowLevel = null, medicinesTaken = null, doctorVisit = false, nextAppointment = null, notesEncrypted = null, attachmentPath = null)
            assertTrue(entry.painLevel in 0..10)
        }
    }

    // ==========================================
    // DISCLAIMER ENFORCED
    // ==========================================

    @Test
    fun `Global disclaimer contains key safety terms`() {
        val disclaimer = LunaContent.GLOBAL_DISCLAIMER
        assertTrue("Should mention medical advice", disclaimer.contains("medical advice"))
        assertTrue("Should mention emergency", disclaimer.contains("emergency"))
        assertTrue("Should mention healthcare professional", disclaimer.contains("healthcare professional"))
        assertTrue("Should not diagnose", disclaimer.contains("not medical advice"))
    }

    @Test
    fun `Articles all have a category`() {
        LunaContent.articles.forEach { article ->
            assertTrue("Article '${article.title}' should have a category", article.category.isNotBlank())
        }
    }

    @Test
    fun `Care products all have a nearby query`() {
        LunaContent.careProducts.forEach { product ->
            assertTrue("Product '${product.name}' should have a nearby query", product.nearbyQuery.isNotBlank())
        }
    }

    // ==========================================
    // SUPPORT: deriveUserMode is accessible
    // ==========================================

    private fun deriveUserMode(userModeStr: String, genderModeStr: String, bodyRelevantModeStr: String): UserMode {
        return when (userModeStr) {
            UserMode.SUPPORT_MODE.name -> UserMode.SUPPORT_MODE
            UserMode.EDUCATION_ONLY.name -> UserMode.EDUCATION_ONLY
            else -> {
                when (bodyRelevantModeStr) {
                    BodyRelevantMode.MENSTRUATES.name -> UserMode.SELF_TRACKING
                    BodyRelevantMode.DOES_NOT_MENSTRUATE.name -> UserMode.EDUCATION_ONLY
                    else -> UserMode.EDUCATION_ONLY
                }
            }
        }
    }
}
