package com.example.data

import kotlinx.coroutines.flow.Flow

class LunaRepository(private val db: LunaDatabase) {
    private val profileDao = db.profileDao()
    private val periodLogDao = db.periodLogDao()
    private val behaviourLogDao = db.behaviourLogDao()
    private val moodLogDao = db.moodLogDao()
    private val medicalJournalDao = db.medicalJournalDao()
    private val journalEntryDao = db.journalEntryDao()
    private val medicalReminderDao = db.medicalReminderDao()
    private val cupCareLogDao = db.cupCareLogDao()
    private val inAppNotificationDao = db.inAppNotificationDao()
    private val auditLogDao = db.auditLogDao()
    private val bookmarkDao = db.bookmarkDao()

    // --- Profile ---
    val profile: Flow<Profile?> = profileDao.getProfile()
    suspend fun getProfileSync(): Profile? = profileDao.getProfileSync()
    suspend fun saveProfile(profile: Profile) = profileDao.insertProfile(profile)

    // --- Period Logs ---
    val periodLogs: Flow<List<PeriodLog>> = periodLogDao.getAllPeriodLogs()
    suspend fun insertPeriodLog(log: PeriodLog) = periodLogDao.insertPeriodLog(log)
    suspend fun deletePeriodLog(id: Int) = periodLogDao.deletePeriodLog(id)
    suspend fun getPeriodLogById(id: Int): PeriodLog? = periodLogDao.getPeriodLogById(id)

    // --- Behaviour Logs (new full check-in) ---
    val behaviourLogs: Flow<List<BehaviourLog>> = behaviourLogDao.getAllBehaviourLogs()
    val crisisLogs: Flow<List<BehaviourLog>> = behaviourLogDao.getCrisisLogs()
    suspend fun insertBehaviourLog(log: BehaviourLog) = behaviourLogDao.insertBehaviourLog(log)
    suspend fun deleteBehaviourLog(id: Int) = behaviourLogDao.deleteBehaviourLog(id)
    suspend fun getBehaviourLogByDate(date: String): BehaviourLog? =
        behaviourLogDao.getBehaviourLogByDate(date)

    // --- Mood Logs (legacy) ---
    val moodLogs: Flow<List<MoodLog>> = moodLogDao.getAllMoodLogs()
    suspend fun insertMoodLog(log: MoodLog) = moodLogDao.insertMoodLog(log)
    suspend fun deleteMoodLog(id: Int) = moodLogDao.deleteMoodLog(id)

    // --- Medical Journal ---
    val medicalJournalEntries: Flow<List<MedicalJournalEntry>> = medicalJournalDao.getAllEntries()
    suspend fun insertMedicalJournalEntry(entry: MedicalJournalEntry) =
        medicalJournalDao.insertEntry(entry)
    suspend fun updateMedicalJournalEntry(entry: MedicalJournalEntry) =
        medicalJournalDao.updateEntry(entry)
    suspend fun deleteMedicalJournalEntry(id: Int) = medicalJournalDao.deleteEntry(id)
    suspend fun getMedicalJournalEntryById(id: Int): MedicalJournalEntry? =
        medicalJournalDao.getEntryById(id)

    // --- Legacy Journal Entries ---
    val journalEntries: Flow<List<JournalEntry>> = journalEntryDao.getAllJournalEntries()
    suspend fun insertJournalEntry(entry: JournalEntry) = journalEntryDao.insertJournalEntry(entry)
    suspend fun deleteJournalEntry(id: Int) = journalEntryDao.deleteJournalEntry(id)

    // --- Medicine Reminders ---
    val medicalReminders: Flow<List<MedicalReminder>> = medicalReminderDao.getAllReminders()
    val activeReminders: Flow<List<MedicalReminder>> = medicalReminderDao.getActiveReminders()
    suspend fun insertMedicalReminder(reminder: MedicalReminder) =
        medicalReminderDao.insertReminder(reminder)
    suspend fun updateMedicalReminder(reminder: MedicalReminder) =
        medicalReminderDao.updateReminder(reminder)
    suspend fun deleteMedicalReminder(id: Int) = medicalReminderDao.deleteReminder(id)

    // --- Cup Care Logs ---
    val cupCareLogs: Flow<List<CupCareLog>> = cupCareLogDao.getAllCupCareLogs()
    suspend fun insertCupCareLog(log: CupCareLog) = cupCareLogDao.insertCupCareLog(log)
    suspend fun deleteCupCareLog(id: Int) = cupCareLogDao.deleteCupCareLog(id)
    suspend fun getCupCareLogByDate(date: String): CupCareLog? =
        cupCareLogDao.getCupCareLogByDate(date)

    // --- In-App Notifications ---
    val inAppNotifications: Flow<List<InAppNotification>> =
        inAppNotificationDao.getAllNotifications()
    val unreadNotificationCount: Flow<Int> = inAppNotificationDao.getUnreadCount()
    suspend fun insertNotification(notification: InAppNotification) =
        inAppNotificationDao.insertNotification(notification)
    suspend fun markNotificationRead(id: Int) = inAppNotificationDao.markAsRead(id)
    suspend fun markAllNotificationsRead() = inAppNotificationDao.markAllAsRead()
    suspend fun deleteNotification(id: Int) = inAppNotificationDao.deleteNotification(id)

    // --- Audit Logs (append-only) ---
    val recentAuditLogs: Flow<List<AuditLog>> = auditLogDao.getRecentAuditLogs()
    val highRiskAuditLogs: Flow<List<AuditLog>> = auditLogDao.getHighRiskLogs()
    suspend fun insertAuditLog(log: AuditLog) = auditLogDao.insertAuditLog(log)

    // --- Bookmarks ---
    val bookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    suspend fun addBookmark(slug: String) = bookmarkDao.insertBookmark(Bookmark(articleSlug = slug))
    suspend fun removeBookmark(slug: String) = bookmarkDao.deleteBookmarkBySlug(slug)

    // --- Clear All (for logout/reset) ---
    suspend fun clearAllData() = db.clearAllTables()
}
