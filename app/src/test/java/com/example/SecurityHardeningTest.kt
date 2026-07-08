package com.example

import androidx.room.Room
import com.example.data.*
import com.example.viewmodel.LunaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityHardeningTest {

    private lateinit var db: LunaDatabase
    private lateinit var repository: LunaRepository
    private lateinit var viewModel: LunaViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        
        // Use in-memory database for isolated testing
        db = Room.inMemoryDatabaseBuilder(context, LunaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            
        repository = LunaRepository(db)
        viewModel = LunaViewModel(context, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `password validation requires 12 characters`() {
        val result = ZodValidator.validateSignup(
            email = "test@example.com",
            password = "short",
            confirm = "short",
            displayName = "Test User"
        )
        assertFalse(result.success)
        assertEquals("Password must be at least 12 characters", result.getFieldError("password"))
    }

    @Test
    fun `password validation rejects common weak passwords`() {
        val resultExact = ZodValidator.validateSignup(
            email = "test@example.com",
            password = "password1234",
            confirm = "password1234",
            displayName = "Test User"
        )
        assertFalse(resultExact.success)
        assertTrue(resultExact.getFieldError("password")!!.contains("too weak"))
    }

    @Test
    fun `brute force protection triggers CAPTCHA after 8 failures`() = runTest(testDispatcher) {
        val email = "victim@example.com"
        val emailHash = EncryptionHelper.hashLookupValue(email)
        
        // Simulate 8 failures
        // We use advanceUntilIdle() to ensure each login attempt finishes
        for (i in 1..8) {
            viewModel.login(email, "wrongpassword") { }
            advanceUntilIdle() 
        }
        
        val state = repository.getSecurityState(emailHash)
        assertNotNull("Security state should not be null", state)
        assertEquals(8, state!!.failedAttemptCount)
        assertTrue("CAPTCHA should be required after 8 attempts", state.captchaRequired)
    }

    @Test
    fun `generic error messages do not reveal user existence`() = runTest(testDispatcher) {
        viewModel.login("nonexistent@example.com", "anypassword") { }
        advanceUntilIdle()
        
        val errorMessage = viewModel.loginError.value
        assertEquals("Email or password is incorrect. Please check your details and try again.", errorMessage)
    }
}
