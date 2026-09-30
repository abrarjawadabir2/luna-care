package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Profile::class,
        PeriodLog::class,
        BehaviourLog::class,
        MoodLog::class,
        MedicalJournalEntry::class,
        JournalEntry::class,
        MedicalReminder::class,
        CupCareLog::class,
        InAppNotification::class,
        AuditLog::class,
        Bookmark::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun periodLogDao(): PeriodLogDao
    abstract fun behaviourLogDao(): BehaviourLogDao
    abstract fun moodLogDao(): MoodLogDao
    abstract fun medicalJournalDao(): MedicalJournalDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun medicalReminderDao(): MedicalReminderDao
    abstract fun cupCareLogDao(): CupCareLogDao
    abstract fun inAppNotificationDao(): InAppNotificationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: LunaDatabase? = null

        // Migration from version 1 → 2: add all new tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Extended columns on profile
                db.execSQL("ALTER TABLE profile ADD COLUMN userMode TEXT NOT NULL DEFAULT 'SELF_TRACKING'")
                db.execSQL("ALTER TABLE profile ADD COLUMN genderMode TEXT NOT NULL DEFAULT 'PREFER_NOT_TO_SAY'")
                db.execSQL("ALTER TABLE profile ADD COLUMN pronoun TEXT NOT NULL DEFAULT 'PREFER_NOT_TO_SAY'")
                db.execSQL("ALTER TABLE profile ADD COLUMN customPronoun TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN bodyRelevantMode TEXT NOT NULL DEFAULT 'PREFER_NOT_TO_SAY'")
                db.execSQL("ALTER TABLE profile ADD COLUMN supportRelationship TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN consentConfirmed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profile ADD COLUMN sharedTrackingConsent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profile ADD COLUMN religion TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN country TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN region TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN city TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN languagePreference TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN selectedConditions TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE profile ADD COLUMN behaviourFocuses TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE profile ADD COLUMN locationPrivacyMode TEXT NOT NULL DEFAULT 'OFF'")
                db.execSQL("ALTER TABLE profile ADD COLUMN lastLocationPermissionStatus TEXT")
                db.execSQL("ALTER TABLE profile ADD COLUMN dashboardLayoutVersion TEXT NOT NULL DEFAULT 'new'")
                db.execSQL("ALTER TABLE profile ADD COLUMN visibleStatus INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE profile ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'")
                db.execSQL("ALTER TABLE profile ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profile ADD COLUMN aiTokensUsedThisMonth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profile ADD COLUMN aiMessagesUsedThisMonth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profile ADD COLUMN aiCurrentMonth TEXT NOT NULL DEFAULT ''")

                // Rename notes → notesEncrypted in period_logs
                db.execSQL("""
                    CREATE TABLE period_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        flowLevel TEXT NOT NULL,
                        symptoms TEXT NOT NULL,
                        notesEncrypted TEXT
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO period_logs_new (id,startDate,endDate,flowLevel,symptoms,notesEncrypted) SELECT id,startDate,endDate,flowLevel,symptoms,notes FROM period_logs")
                db.execSQL("DROP TABLE period_logs")
                db.execSQL("ALTER TABLE period_logs_new RENAME TO period_logs")

                // New: behaviour_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS behaviour_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        logDate TEXT NOT NULL,
                        mood TEXT NOT NULL,
                        stressLevel INTEGER NOT NULL,
                        anxietyLevel INTEGER NOT NULL,
                        sleepHours REAL NOT NULL,
                        sleepQuality INTEGER NOT NULL,
                        painLevel INTEGER NOT NULL,
                        energyLevel INTEGER NOT NULL,
                        hydrationLevel TEXT NOT NULL,
                        foodCraving TEXT,
                        caffeineIntake TEXT,
                        movement TEXT,
                        studyWorkPressure INTEGER NOT NULL,
                        relationshipStress INTEGER NOT NULL,
                        socialMediaOverload INTEGER NOT NULL,
                        flowLevel TEXT,
                        symptoms TEXT NOT NULL,
                        notesEncrypted TEXT,
                        crisisFlag INTEGER NOT NULL DEFAULT 0,
                        flags TEXT NOT NULL DEFAULT '[]'
                    )
                """.trimIndent())

                // New: medical_journal_entries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_journal_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entryDate TEXT NOT NULL,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        symptoms TEXT NOT NULL,
                        painLevel INTEGER NOT NULL,
                        mood TEXT,
                        flowLevel TEXT,
                        medicinesTaken TEXT,
                        doctorVisit INTEGER NOT NULL DEFAULT 0,
                        nextAppointment TEXT,
                        notesEncrypted TEXT,
                        attachmentPath TEXT
                    )
                """.trimIndent())

                // New: medical_reminders
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        reminderType TEXT NOT NULL,
                        reminderTime TEXT NOT NULL,
                        repeatRule TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        notesEncrypted TEXT,
                        reasonNote TEXT
                    )
                """.trimIndent())

                // New: cup_care_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cup_care_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        insertedAt TEXT,
                        emptiedAt TEXT,
                        cleanedToday INTEGER NOT NULL,
                        discomfortLevel INTEGER NOT NULL,
                        leakageIssue INTEGER NOT NULL,
                        notesEncrypted TEXT,
                        logDate TEXT NOT NULL
                    )
                """.trimIndent())

                // New: in_app_notifications
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS in_app_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        type TEXT NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt TEXT NOT NULL
                    )
                """.trimIndent())

                // New: audit_logs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        actorRole TEXT NOT NULL,
                        action TEXT NOT NULL,
                        resourceType TEXT NOT NULL,
                        resourceId TEXT,
                        ipHash TEXT,
                        userAgentHash TEXT,
                        metadata TEXT NOT NULL,
                        createdAt TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): LunaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunaDatabase::class.java,
                    "luna_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
