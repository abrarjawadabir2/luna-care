package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Profile::class,
        PeriodLog::class,
        MoodLog::class,
        JournalEntry::class,
        Bookmark::class,
        MedicalJournalEntry::class,
        MedicalReminder::class,
        BehaviourLog::class,
        CupCareLog::class,
        SupportNote::class,
        SymptomLog::class,
        UserCredentials::class,
        LoginSecurityEvent::class,
        AccountSecurityState::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun periodLogDao(): PeriodLogDao
    abstract fun moodLogDao(): MoodLogDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun medicalJournalEntryDao(): MedicalJournalEntryDao
    abstract fun medicalReminderDao(): MedicalReminderDao
    abstract fun behaviourLogDao(): BehaviourLogDao
    abstract fun cupCareLogDao(): CupCareLogDao
    abstract fun supportNoteDao(): SupportNoteDao
    abstract fun symptomLogDao(): SymptomLogDao
    abstract fun userCredentialsDao(): UserCredentialsDao
    abstract fun loginSecurityEventDao(): LoginSecurityEventDao
    abstract fun accountSecurityStateDao(): AccountSecurityStateDao

    companion object {
        @Volatile
        private var INSTANCE: LunaDatabase? = null

        fun getDatabase(context: Context): LunaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunaDatabase::class.java,
                    "luna_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
