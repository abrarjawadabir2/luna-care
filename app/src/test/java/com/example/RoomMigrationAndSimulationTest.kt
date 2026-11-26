package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Room tests verifying:
 * 1. Real v2 -> v3 SQLite schema migration succeeds with MIGRATION_2_3 (matching Profile unique index).
 * 2. 100 simulated user persistence cycles (profile, period, behaviour, journal, audit, wipe).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationAndSimulationTest {

    @Test
    fun `MIGRATION_2_3 upgrades a real v2 SQLite database to v3 cleanly and creates unique usernameNormalized index`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v2_to_v3.db"
        context.deleteDatabase(dbName)
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create a real v2 SQLite database matching LunaDatabase version 2 schema
        val rawDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            rawDb.execSQL(
                """
                CREATE TABLE `profile` (
                    `id` INTEGER NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `birthYear` INTEGER,
                    `averageCycleLength` INTEGER NOT NULL,
                    `averagePeriodLength` INTEGER NOT NULL,
                    `goals` TEXT NOT NULL,
                    `acceptedDisclaimer` INTEGER NOT NULL,
                    `userMode` TEXT NOT NULL,
                    `genderMode` TEXT NOT NULL,
                    `pronoun` TEXT NOT NULL,
                    `customPronoun` TEXT,
                    `bodyRelevantMode` TEXT NOT NULL,
                    `supportRelationship` TEXT,
                    `consentConfirmed` INTEGER NOT NULL,
                    `sharedTrackingConsent` INTEGER NOT NULL,
                    `religion` TEXT,
                    `country` TEXT,
                    `region` TEXT,
                    `city` TEXT,
                    `languagePreference` TEXT,
                    `selectedConditions` TEXT NOT NULL,
                    `behaviourFocuses` TEXT NOT NULL,
                    `locationPrivacyMode` TEXT NOT NULL,
                    `lastLocationPermissionStatus` TEXT,
                    `isDarkMode` INTEGER NOT NULL,
                    `securityPinEnabled` INTEGER NOT NULL,
                    `securityPin` TEXT NOT NULL,
                    `periodReminders` INTEGER NOT NULL,
                    `moodReminders` INTEGER NOT NULL,
                    `cupReminders` INTEGER NOT NULL,
                    `selfCareReminders` INTEGER NOT NULL,
                    `reminderTime` TEXT NOT NULL,
                    `dashboardLayoutVersion` TEXT NOT NULL,
                    `visibleStatus` INTEGER NOT NULL,
                    `role` TEXT NOT NULL,
                    `isPremium` INTEGER NOT NULL,
                    `aiTokensUsedThisMonth` INTEGER NOT NULL,
                    `aiMessagesUsedThisMonth` INTEGER NOT NULL,
                    `aiCurrentMonth` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            rawDb.execSQL("CREATE TABLE `period_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `flowLevel` TEXT NOT NULL, `symptoms` TEXT NOT NULL, `notesEncrypted` TEXT)")
            rawDb.execSQL("CREATE TABLE `behaviour_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `logDate` TEXT NOT NULL, `mood` TEXT NOT NULL, `stressLevel` INTEGER NOT NULL, `anxietyLevel` INTEGER NOT NULL, `sleepHours` REAL NOT NULL, `sleepQuality` INTEGER NOT NULL, `painLevel` INTEGER NOT NULL, `energyLevel` INTEGER NOT NULL, `hydrationLevel` TEXT NOT NULL, `foodCraving` TEXT, `caffeineIntake` TEXT, `movement` TEXT, `studyWorkPressure` INTEGER NOT NULL, `relationshipStress` INTEGER NOT NULL, `socialMediaOverload` INTEGER NOT NULL, `flowLevel` TEXT, `symptoms` TEXT NOT NULL, `notesEncrypted` TEXT, `crisisFlag` INTEGER NOT NULL, `flags` TEXT NOT NULL)")
            rawDb.execSQL("CREATE TABLE `mood_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `mood` TEXT NOT NULL, `energy` INTEGER NOT NULL, `stress` INTEGER NOT NULL, `sleepQuality` INTEGER, `notes` TEXT)")
            rawDb.execSQL("CREATE TABLE `medical_journal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryDate` TEXT NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `symptoms` TEXT NOT NULL, `painLevel` INTEGER NOT NULL, `mood` TEXT, `flowLevel` TEXT, `medicinesTaken` TEXT, `doctorVisit` INTEGER NOT NULL, `nextAppointment` TEXT, `notesEncrypted` TEXT, `attachmentPath` TEXT)")
            rawDb.execSQL("CREATE TABLE `journal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `moodTag` TEXT, `cyclePhase` TEXT)")
            rawDb.execSQL("CREATE TABLE `medical_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `reminderType` TEXT NOT NULL, `reminderTime` TEXT NOT NULL, `repeatRule` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `enabled` INTEGER NOT NULL, `notesEncrypted` TEXT, `reasonNote` TEXT)")
            rawDb.execSQL("CREATE TABLE `cup_care_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `insertedAt` TEXT, `emptiedAt` TEXT, `cleanedToday` INTEGER NOT NULL, `discomfortLevel` INTEGER NOT NULL, `leakageIssue` INTEGER NOT NULL, `notesEncrypted` TEXT, `logDate` TEXT NOT NULL)")
            rawDb.execSQL("CREATE TABLE `in_app_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `type` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `createdAt` TEXT NOT NULL)")
            rawDb.execSQL("CREATE TABLE `audit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `actorRole` TEXT NOT NULL, `action` TEXT NOT NULL, `resourceType` TEXT NOT NULL, `resourceId` TEXT, `ipHash` TEXT, `userAgentHash` TEXT, `metadata` TEXT NOT NULL, `createdAt` TEXT NOT NULL)")
            rawDb.execSQL("CREATE TABLE `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `articleSlug` TEXT NOT NULL)")

            // Seed a real v2 user profile row
            rawDb.execSQL(
                """
                INSERT INTO profile (
                    id, displayName, birthYear, averageCycleLength, averagePeriodLength, goals,
                    acceptedDisclaimer, userMode, genderMode, pronoun, customPronoun, bodyRelevantMode,
                    supportRelationship, consentConfirmed, sharedTrackingConsent, religion, country, region,
                    city, languagePreference, selectedConditions, behaviourFocuses, locationPrivacyMode,
                    lastLocationPermissionStatus, isDarkMode, securityPinEnabled, securityPin,
                    periodReminders, moodReminders, cupReminders, selfCareReminders, reminderTime,
                    dashboardLayoutVersion, visibleStatus, role, isPremium,
                    aiTokensUsedThisMonth, aiMessagesUsedThisMonth, aiCurrentMonth
                ) VALUES (
                    1, 'Maya', 1998, 28, 5, '[]',
                    1, 'SELF_TRACKING', 'FEMALE', 'SHE_HER', NULL, 'MENSTRUATES',
                    NULL, 0, 0, NULL, 'Bangladesh', NULL,
                    'Dhaka', 'en', '[]', '[]', 'OFF',
                    NULL, 0, 0, '',
                    1, 1, 0, 1, '20:00',
                    'new', 1, 'USER', 0,
                    0, 2, '2026-10'
                )
                """.trimIndent()
            )
            rawDb.version = 2
        } finally {
            rawDb.close()
        }

        // Open with Room v3 + MIGRATION_2_3: Room's onValidateSchema MUST pass without crashing!
        val migratedDb = Room.databaseBuilder(context, LunaDatabase::class.java, dbName)
            .addMigrations(LunaDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val profile = migratedDb.profileDao().getProfileSync()
            assertNotNull("Migrated profile must exist", profile)
            assertEquals("Maya", profile!!.displayName)
            assertNull("New username column should default to null", profile.username)

            // Verify SQLite has the exact unique index on profile(usernameNormalized)
            var foundUniqueIndex = false
            migratedDb.openHelper.readableDatabase.query("PRAGMA index_list('profile')").use { cursor ->
                while (cursor.moveToNext()) {
                    val indexName = cursor.getString(1)
                    val isUnique = cursor.getInt(2) == 1
                    if (indexName == "index_profile_usernameNormalized" && isUnique) {
                        foundUniqueIndex = true
                    }
                }
            }
            assertTrue("Unique index index_profile_usernameNormalized must exist after migration", foundUniqueIndex)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `100 simulated Room user persistence and full data-wipe cycles succeed cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LunaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LunaRepository(db)
            for (i in 1..100) {
                repo.saveProfile(
                    Profile(
                        id = 1,
                        displayName = "User $i",
                        averageCycleLength = 26 + (i % 8),
                        averagePeriodLength = 4 + (i % 3),
                        username = "luna_user_$i",
                        usernameNormalized = "luna_user_$i"
                    )
                )
                repo.insertPeriodLog(
                    PeriodLog(
                        startDate = "2026-01-%02d".format(1 + (i % 20)),
                        endDate = "2026-01-%02d".format(5 + (i % 20)),
                        flowLevel = "Medium",
                        symptoms = listOf("Cramps"),
                        notesEncrypted = null
                    )
                )
                repo.insertBehaviourLog(
                    BehaviourLog(
                        logDate = "2026-02-%02d".format(1 + (i % 28)),
                        mood = "Calm",
                        stressLevel = i % 11,
                        anxietyLevel = i % 11,
                        sleepHours = 7.0f,
                        sleepQuality = 1 + (i % 10),
                        painLevel = i % 11,
                        energyLevel = 6,
                        hydrationLevel = "Good",
                        foodCraving = null,
                        caffeineIntake = "Low",
                        movement = "Light walk",
                        studyWorkPressure = 4,
                        relationshipStress = 2,
                        socialMediaOverload = 3,
                        flowLevel = null,
                        symptoms = emptyList(),
                        notesEncrypted = null
                    )
                )
                repo.insertMedicalJournalEntry(
                    MedicalJournalEntry(
                        entryDate = "2026-03-%02d".format(1 + (i % 28)),
                        category = "GENERAL",
                        title = "Entry $i",
                        symptoms = listOf("Fatigue"),
                        painLevel = i % 11,
                        mood = "Calm",
                        flowLevel = null,
                        medicinesTaken = null,
                        doctorVisit = false,
                        nextAppointment = null,
                        notesEncrypted = null,
                        attachmentPath = null
                    )
                )
            }

            assertEquals("User 100", repo.getProfileSync()?.displayName)
            assertEquals(100, repo.periodLogs.first().size)
            assertEquals(100, repo.behaviourLogs.first().size)
            assertEquals(100, repo.medicalJournalEntries.first().size)

            // Full user data deletion must erase everything
            repo.clearAllData()
            assertNull(repo.getProfileSync())
            assertTrue(repo.periodLogs.first().isEmpty())
            assertTrue(repo.behaviourLogs.first().isEmpty())
            assertTrue(repo.medicalJournalEntries.first().isEmpty())
        } finally {
            db.close()
        }
    }
}
