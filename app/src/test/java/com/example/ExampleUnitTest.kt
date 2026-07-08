package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CycleUtils
import com.example.data.EncryptionHelper
import com.example.data.ZodValidator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, (2 + 2).toLong())
  }

  @Test
  fun testCyclePrediction() {
    val lastPeriodStart = "2026-06-01"
    val cycleLength = 28
    val predicted = CycleUtils.getPredictedNextPeriod(lastPeriodStart, cycleLength)
    assertEquals("2026-06-29", predicted)
  }

  @Test
  fun testCycleDayCalculation() {
    val lastPeriodStart = "2026-06-15"
    val testToday = "2026-06-20"
    val cycleDay = CycleUtils.getCurrentCycleDay(lastPeriodStart, testToday)
    assertEquals(6, cycleDay.toLong())
  }

  @Test
  fun testCyclePhaseSelection() {
    val phase1 = CycleUtils.getCyclePhase(cycleDay = 3, cycleLength = 28, periodLength = 5)
    assertEquals("Menstrual Phase", phase1)

    val phase2 = CycleUtils.getCyclePhase(cycleDay = 10, cycleLength = 28, periodLength = 5)
    assertEquals("Follicular Phase", phase2)

    val phase3 = CycleUtils.getCyclePhase(cycleDay = 14, cycleLength = 28, periodLength = 5)
    assertEquals("Ovulatory Phase", phase3)

    val phase4 = CycleUtils.getCyclePhase(cycleDay = 21, cycleLength = 28, periodLength = 5)
    assertEquals("Luteal Phase", phase4)
  }

  @Test
  fun testEncryptionAndDecryption() {
    val sensitiveText = "My private health journal entry"
    val encrypted = com.example.data.EncryptionHelper.encryptSensitiveText(sensitiveText)
    assertNotNull(encrypted)
    assertNotEquals(sensitiveText, encrypted)

    val decrypted = com.example.data.EncryptionHelper.decryptSensitiveText(encrypted)
    assertEquals(sensitiveText, decrypted)
  }

  @Test
  fun testEmailMaskingAndHashing() {
    val email = "alex.care@gmail.com"
    val masked = com.example.data.EncryptionHelper.maskEmail(email)
    assertEquals("a***@gmail.com", masked)

    val hashed = com.example.data.EncryptionHelper.hashLookupValue(email)
    assertNotNull(hashed)
    assertEquals(64, hashed.length) // SHA-256 hex string is 64 characters long
  }

  @Test
  fun testZodAuthenticationValidation() {
    val weakResult = com.example.data.ZodValidator.validateSignup(
        email = "user@test.com",
        password = "short",
        confirm = "short",
        displayName = "User"
    )
    assertFalse(weakResult.success)
    assertNotNull(weakResult.getFieldError("password"))

    val mismatchResult = com.example.data.ZodValidator.validateSignup(
        email = "user@test.com",
        password = "longsecurepassword123",
        confirm = "differentpwd",
        displayName = "User"
    )
    assertFalse(mismatchResult.success)
    assertEquals("Passwords do not match", mismatchResult.getFieldError("confirm"))

    val successResult = com.example.data.ZodValidator.validateSignup(
        email = "user@test.com",
        password = "highlysecurepassphrase2026",
        confirm = "highlysecurepassphrase2026",
        displayName = "Test User"
    )
    assertTrue(successResult.success)
  }
}
