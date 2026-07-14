package com.example

import androidx.room.Room
import com.example.data.*
import com.example.security.HashUtils
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
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityHardeningTest {

    private lateinit var db: LunaDatabase
    private lateinit var repository: LunaRepository
    private lateinit var viewModel: LunaViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        
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
        val emailHash = HashUtils.hashForLookup(HashUtils.normalizeEmail(email), HashUtils.HashPurpose.EMAIL_LOOKUP)
        
        // Simulate 8 failures
        for (i in 1..8) {
            val deferred = CompletableDeferred<Boolean>()
            viewModel.login(email, "wrongpassword") { 
                deferred.complete(it)
            }
            deferred.await() 
            
            // Fast-forward time / remove lock so the next attempt can proceed
            val currentState = repository.getSecurityState(emailHash)
            if (currentState != null && currentState.lockedUntil != null) {
                repository.insertSecurityState(currentState.copy(lockedUntil = null))
            }
        }
        
        val state = repository.getSecurityState(emailHash)
        assertNotNull("Security state should not be null", state)
        assertEquals(8, state!!.failedAttemptCount)
        assertTrue("CAPTCHA should be required after 8 attempts", state.captchaRequired)
    }

    @Test
    fun `generic error messages do not reveal user existence`() = runTest(testDispatcher) {
        val deferred = CompletableDeferred<Boolean>()
        viewModel.login("nonexistent@example.com", "anypassword") { 
            deferred.complete(it)
        }
        deferred.await()
        
        val errorMessage = viewModel.loginError.value
        assertEquals("Email or password is incorrect. Please check your details and try again.", errorMessage)
    }
}
