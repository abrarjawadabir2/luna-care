package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// PROFILE DAO
// ==========================================

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)
}

// ==========================================
// PERIOD LOG DAO
// ==========================================

@Dao
interface PeriodLogDao {
    @Query("SELECT * FROM period_logs ORDER BY startDate DESC")
    fun getAllPeriodLogs(): Flow<List<PeriodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodLog(log: PeriodLog)

    @Query("DELETE FROM period_logs WHERE id = :id")
    suspend fun deletePeriodLog(id: Int)

    @Query("SELECT * FROM period_logs WHERE id = :id LIMIT 1")
    suspend fun getPeriodLogById(id: Int): PeriodLog?
}

// ==========================================
// BEHAVIOUR LOG DAO (full check-in)
// ==========================================

@Dao
interface BehaviourLogDao {
    @Query("SELECT * FROM behaviour_logs ORDER BY logDate DESC")
    fun getAllBehaviourLogs(): Flow<List<BehaviourLog>>

    @Query("SELECT * FROM behaviour_logs WHERE logDate = :date LIMIT 1")
    suspend fun getBehaviourLogByDate(date: String): BehaviourLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviourLog(log: BehaviourLog)

    @Query("DELETE FROM behaviour_logs WHERE id = :id")
    suspend fun deleteBehaviourLog(id: Int)

    @Query("SELECT * FROM behaviour_logs WHERE crisisFlag = 1 ORDER BY logDate DESC")
    fun getCrisisLogs(): Flow<List<BehaviourLog>>
}

// ==========================================
// MOOD LOG DAO (legacy - keep for backward compat)
// ==========================================

@Dao
interface MoodLogDao {
    @Query("SELECT * FROM mood_logs ORDER BY date DESC")
    fun getAllMoodLogs(): Flow<List<MoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(log: MoodLog)

    @Query("DELETE FROM mood_logs WHERE id = :id")
    suspend fun deleteMoodLog(id: Int)
}

// ==========================================
// MEDICAL JOURNAL DAO
// ==========================================

@Dao
interface MedicalJournalDao {
    @Query("SELECT * FROM medical_journal_entries ORDER BY entryDate DESC")
    fun getAllEntries(): Flow<List<MedicalJournalEntry>>

    @Query("SELECT * FROM medical_journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Int): MedicalJournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MedicalJournalEntry)

    @Update
    suspend fun updateEntry(entry: MedicalJournalEntry)

    @Query("DELETE FROM medical_journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Int)

    @Query("SELECT * FROM medical_journal_entries WHERE category = :category ORDER BY entryDate DESC")
    fun getEntriesByCategory(category: String): Flow<List<MedicalJournalEntry>>
}

// ==========================================
// LEGACY JOURNAL ENTRY DAO
// ==========================================

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntry(id: Int)
}

// ==========================================
// MEDICINE REMINDER DAO
// ==========================================

@Dao
interface MedicalReminderDao {
    @Query("SELECT * FROM medical_reminders ORDER BY reminderTime ASC")
    fun getAllReminders(): Flow<List<MedicalReminder>>

    @Query("SELECT * FROM medical_reminders WHERE enabled = 1 ORDER BY reminderTime ASC")
    fun getActiveReminders(): Flow<List<MedicalReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicalReminder)

    @Update
    suspend fun updateReminder(reminder: MedicalReminder)

    @Query("DELETE FROM medical_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Int)
}

// ==========================================
// CUP CARE LOG DAO
// ==========================================

@Dao
interface CupCareLogDao {
    @Query("SELECT * FROM cup_care_logs ORDER BY logDate DESC")
    fun getAllCupCareLogs(): Flow<List<CupCareLog>>

    @Query("SELECT * FROM cup_care_logs WHERE logDate = :date LIMIT 1")
    suspend fun getCupCareLogByDate(date: String): CupCareLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCupCareLog(log: CupCareLog)

    @Query("DELETE FROM cup_care_logs WHERE id = :id")
    suspend fun deleteCupCareLog(id: Int)
}

// ==========================================
// IN-APP NOTIFICATION DAO
// ==========================================

@Dao
interface InAppNotificationDao {
    @Query("SELECT * FROM in_app_notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<InAppNotification>>

    @Query("SELECT COUNT(*) FROM in_app_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: InAppNotification)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE in_app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM in_app_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)
}

// ==========================================
// AUDIT LOG DAO (append-only; no delete/update)
// ==========================================

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 500")
    fun getRecentAuditLogs(): Flow<List<AuditLog>>

    @Insert
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs WHERE action IN ('delete', 'export') ORDER BY createdAt DESC LIMIT 100")
    fun getHighRiskLogs(): Flow<List<AuditLog>>
}

// ==========================================
// BOOKMARK DAO
// ==========================================

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE articleSlug = :slug")
    suspend fun deleteBookmarkBySlug(slug: String)
}
