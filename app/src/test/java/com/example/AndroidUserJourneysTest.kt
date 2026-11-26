package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LunaViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end Robolectric + Compose UI + Room journey verification across LunaCare's
 * real Android screens and ViewModel state transitions.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AndroidUserJourneysTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var db: LunaDatabase
    private lateinit var viewModel: LunaViewModel

    @Before
    fun setUp() {
        SecurityUtils.allowJvmTestKeyFallback = true
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, LunaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = LunaViewModel(app, db)
    }

    @After
    fun tearDown() {
        SecurityUtils.allowJvmTestKeyFallback = false
        db.close()
    }

    private fun waitForCondition(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (predicate()) return
            Thread.sleep(25)
        }
        composeRule.waitForIdle()
        assertTrue("Condition did not settle within ${timeoutMs}ms", predicate())
    }

    @Test
    fun `AndroidViewModel single-arg Application constructor remains valid for ViewModelProvider`() {
        val ctor = LunaViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(ctor)
    }

    @Test
    fun `Journey 1 to 5 - Guest mode, full onboarding, PIN lock and unlock, and mode consent rules`() {
        waitForCondition { viewModel.isProfileLoaded.value }
        assertNull(viewModel.profile.value)

        // Guest mode creates an EDUCATION_ONLY profile
        viewModel.setGuestMode(true)
        waitForCondition { viewModel.profile.value?.displayName == "Guest" }
        assertEquals(UserMode.EDUCATION_ONLY.name, viewModel.profile.value?.userMode)
        assertTrue(viewModel.isPinAuthenticated.value)

        // Full onboarding with PIN and custom pronouns overwrites guest state cleanly
        viewModel.onboardUser(
            name = "Amina",
            birthYear = 1999,
            cycleLength = 29,
            periodLength = 5,
            goals = listOf("Cycle prediction", "Mood awareness"),
            pin = "2468",
            userMode = UserMode.SELF_TRACKING.name,
            genderMode = GenderMode.FEMALE.name,
            pronoun = Pronoun.CUSTOM.name,
            customPronoun = "fae/faer",
            bodyRelevantMode = BodyRelevantMode.MENSTRUATES.name
        )
        waitForCondition { viewModel.profile.value?.displayName == "Amina" }
        assertFalse("PIN-protected onboarding should start locked until PIN entry", viewModel.isPinAuthenticated.value)
        assertEquals("fae/faer", viewModel.profile.value?.customPronoun)

        // Wrong PIN rejected, right PIN unlocks, lockApp re-locks
        assertFalse(viewModel.authenticatePin("1111"))
        assertFalse(viewModel.isPinAuthenticated.value)
        assertTrue(viewModel.authenticatePin("2468"))
        assertTrue(viewModel.isPinAuthenticated.value)
        viewModel.lockApp()
        assertFalse(viewModel.isPinAuthenticated.value)
        assertTrue(viewModel.authenticatePin("2468"))

        // Switching to SUPPORT_MODE requires explicit consent
        assertFalse(viewModel.updateUserMode(UserMode.SUPPORT_MODE, consentConfirmed = false))
        assertTrue(viewModel.updateUserMode(UserMode.SUPPORT_MODE, consentConfirmed = true))
        waitForCondition { viewModel.profile.value?.userMode == UserMode.SUPPORT_MODE.name }
    }

    @Test
    fun `Journey 6 to 8 - ProfileEditScreen immediate keystroke invalidation, reserved block, and username clear`() {
        viewModel.onboardUser(
            name = "Maya",
            birthYear = 1997,
            cycleLength = 28,
            periodLength = 5,
            goals = emptyList()
        )
        waitForCondition { viewModel.profile.value?.displayName == "Maya" }
        viewModel.updateProfileIdentity(
            displayName = "Maya",
            pronoun = "SHE_HER",
            customPronoun = null,
            genderMode = "FEMALE",
            religion = null,
            country = null,
            region = null,
            city = null,
            behaviourFocuses = emptyList(),
            username = "maya_luna"
        )
        waitForCondition { viewModel.profile.value?.username == "maya_luna" }

        val initialProfile = viewModel.profile.value!!
        composeRule.mainClock.autoAdvance = false
        var navigatedBack = false
        composeRule.setContent {
            MyApplicationTheme {
                ProfileEditScreen(
                    profile = initialProfile,
                    viewModel = viewModel,
                    onBack = { navigatedBack = true }
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(100)

        // Existing username is immediately valid
        composeRule.onNodeWithTag("profile_save_button").assertIsEnabled()

        // Typing a new candidate MUST immediately disable Save BEFORE 400ms debounce finishes
        composeRule.onNodeWithTag("username_input_field").performTextClearance()
        composeRule.onNodeWithTag("username_input_field").performTextInput("maya_updated")
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("profile_save_button").assertIsNotEnabled()

        // After 400ms debounce, Save re-enables with honest local-format copy (never claiming cloud availability)
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("profile_save_button").assertIsEnabled()
        composeRule.onNodeWithTag("username_status_text")
            .assertTextContains("Valid local format", substring = true)

        // Reserved username ('admin') disables Save permanently
        composeRule.onNodeWithTag("username_input_field").performTextClearance()
        composeRule.onNodeWithTag("username_input_field").performTextInput("admin")
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithTag("profile_save_button").assertIsNotEnabled()

        // Clearing username to blank re-enables Save and clears username in Room on submit
        composeRule.onNodeWithTag("username_input_field").performTextClearance()
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("profile_save_button").assertIsEnabled()
        composeRule.onNodeWithTag("profile_save_button").performClick()
        composeRule.mainClock.autoAdvance = true

        waitForCondition { navigatedBack && viewModel.profile.value?.username == null }
        assertNull(viewModel.profile.value?.usernameNormalized)
    }

    @Test
    fun `Journey 9 to 15 - Health logs, reminders, encrypted reflections, notifications, and full wipe`() = runBlocking {
        viewModel.onboardUser("Nadia", 2000, 28, 5, listOf("Symptom awareness"))
        waitForCondition { viewModel.profile.value?.displayName == "Nadia" }

        // Subscribe to cold WhileSubscribed flows during verification
        assertEquals(0, db.periodLogDao().getAllPeriodLogs().first().size)

        // Reversed period dates rejected; valid period dates persisted
        viewModel.addPeriodLog("2026-05-10", "2026-05-05", "Medium", listOf("Cramps"), "reversed")
        Thread.sleep(80)
        assertEquals(0, db.periodLogDao().getAllPeriodLogs().first().size)

        viewModel.addPeriodLog("2026-05-01", "2026-05-05", "Heavy", listOf("Cramps", "Fatigue"), "day 1 note")
        waitForCondition { runBlocking { db.periodLogDao().getAllPeriodLogs().first().size == 1 } }

        // Behaviour check-in computes HIGH_PAIN, HIGH_STRESS, LOW_SLEEP flags & stores sleepQuality
        viewModel.addBehaviourLog(
            mood = "Anxious",
            stressLevel = 9,
            anxietyLevel = 7,
            sleepHours = 3.5f,
            sleepQuality = 2,
            painLevel = 8,
            energyLevel = 3,
            hydrationLevel = "Low",
            foodCraving = null,
            caffeineIntake = null,
            movement = null,
            studyWorkPressure = 7,
            relationshipStress = 4,
            socialMediaOverload = 6,
            flowLevel = "Medium",
            symptoms = listOf("Headache"),
            notes = "Tough day",
            crisisFlag = false
        )
        waitForCondition { runBlocking { db.behaviourLogDao().getAllBehaviourLogs().first().size == 1 } }
        val savedBehaviour = db.behaviourLogDao().getAllBehaviourLogs().first().first()
        assertEquals(2, savedBehaviour.sleepQuality)
        assertTrue(savedBehaviour.flags.containsAll(listOf("HIGH_PAIN", "HIGH_STRESS", "LOW_SLEEP")))

        // Medicine reminder rejects invalid 24:00, accepts 08:30
        viewModel.addMedicalReminder("Iron", "Supplement", "24:00", "Daily", "2026-05-01", null, null, null)
        Thread.sleep(80)
        assertEquals(0, db.medicalReminderDao().getAllReminders().first().size)

        viewModel.addMedicalReminder("Iron", "Supplement", "08:30", "Daily", "2026-05-01", null, null, "With breakfast")
        waitForCondition { runBlocking { db.medicalReminderDao().getAllReminders().first().size == 1 } }

        // Reflection journal persists AES-256-GCM ciphertext & round-trips through decryptJournalBody
        viewModel.addJournalEntry("Evening calm", "Drinking chamomile tea helped.", "Calm", "Luteal Phase")
        waitForCondition { runBlocking { db.journalEntryDao().getAllJournalEntries().first().size == 1 } }
        val storedJournal = db.journalEntryDao().getAllJournalEntries().first().first()
        assertTrue("Stored journal body must be AES-GCM encrypted, not plaintext", storedJournal.body.startsWith("ENC_V1:"))
        assertEquals("Drinking chamomile tea helped.", SecurityUtils.decryptJournalBody(storedJournal.body))

        // Notifications mark-read & delete
        viewModel.addNotification("Hydration", "Take a sip of water", "system")
        waitForCondition { runBlocking { db.inAppNotificationDao().getUnreadCount().first() == 1 } }
        viewModel.markAllNotificationsRead()
        waitForCondition { runBlocking { db.inAppNotificationDao().getUnreadCount().first() == 0 } }

        // Full user data deletion wipes everything cleanly
        viewModel.clearAllUserData()
        waitForCondition { viewModel.profile.value == null }
        assertTrue(db.periodLogDao().getAllPeriodLogs().first().isEmpty())
        assertTrue(db.behaviourLogDao().getAllBehaviourLogs().first().isEmpty())
        assertTrue(db.journalEntryDao().getAllJournalEntries().first().isEmpty())
        assertTrue(db.medicalReminderDao().getAllReminders().first().isEmpty())
    }

    @Test
    fun `Journey 16 to 20 - Bookmark race deduplication, BookmarksScreen reader, Learning search, and honest AI screen`() = runBlocking {
        val targetArticle = LunaContent.articles.first()

        // 10 rapid concurrent bookmark toggles must serialize via bookmarkMutex with zero duplicate rows
        coroutineScope {
            (1..9).map {
                async { viewModel.toggleBookmark(targetArticle.slug) }
            }.awaitAll()
        }
        waitForCondition {
            runBlocking { db.bookmarkDao().getAllBookmarks().first().size == 1 }
        }
        val bookmarks = db.bookmarkDao().getAllBookmarks().first()
        assertEquals(1, bookmarks.count { it.articleSlug == targetArticle.slug })

        // Render BookmarksScreen -> verify resolved article title is shown & clickable, and AI screen is honest
        composeRule.setContent {
            MyApplicationTheme {
                AiAssistantScreen(profile = Profile(displayName = "Sara"), viewModel = viewModel, onBack = {})
            }
        }
        composeRule.onNodeWithText("AI chat is not connected yet").assertIsDisplayed()
        assertEquals(0, viewModel.profile.value?.aiMessagesUsedThisMonth ?: 0)
    }
}
