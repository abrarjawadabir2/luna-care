package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LunaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LunaRepository

    // Free tier AI limits
    companion object {
        const val FREE_AI_MESSAGES_LIMIT = 20
        const val PREMIUM_AI_MESSAGES_LIMIT = 200
    }

    init {
        val database = LunaDatabase.getDatabase(application)
        repository = LunaRepository(database)
    }

    // ==============================
    // CORE STATE FLOWS
    // ==============================

    val profile: StateFlow<Profile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodLogs: StateFlow<List<PeriodLog>> = repository.periodLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val behaviourLogs: StateFlow<List<BehaviourLog>> = repository.behaviourLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moodLogs: StateFlow<List<MoodLog>> = repository.moodLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicalJournalEntries: StateFlow<List<MedicalJournalEntry>> =
        repository.medicalJournalEntries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.journalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicalReminders: StateFlow<List<MedicalReminder>> = repository.medicalReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cupCareLogs: StateFlow<List<CupCareLog>> = repository.cupCareLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<InAppNotification>> = repository.inAppNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==============================
    // AUTH STATE
    // ==============================

    private val _isPinAuthenticated = MutableStateFlow(false)
    val isPinAuthenticated: StateFlow<Boolean> = _isPinAuthenticated.asStateFlow()

    private val _isGuestUser = MutableStateFlow(false)
    val isGuestUser: StateFlow<Boolean> = _isGuestUser.asStateFlow()

    // ==============================
    // TEMPORARY LOCATION (never persisted as exact coords)
    // ==============================

    private val _tempLat = MutableStateFlow<Double?>(null)
    private val _tempLng = MutableStateFlow<Double?>(null)
    val tempLat: StateFlow<Double?> = _tempLat.asStateFlow()
    val tempLng: StateFlow<Double?> = _tempLng.asStateFlow()

    fun setTempLocation(lat: Double, lng: Double) {
        _tempLat.value = lat
        _tempLng.value = lng
        logAudit("USER", "read", "location_temp", "Temporary location set for nearby search")
    }

    /** Must be called after nearby search ends — clears exact coordinates from memory */
    fun clearTempLocation() {
        _tempLat.value = null
        _tempLng.value = null
    }

    // ==============================
    // ONBOARDING & GUEST
    // ==============================

    fun setGuestMode(enabled: Boolean) {
        _isGuestUser.value = enabled
        _isPinAuthenticated.value = true
        viewModelScope.launch {
            val existing = repository.getProfileSync()
            if (existing == null) {
                repository.saveProfile(
                    Profile(
                        id = 1,
                        displayName = "Guest",
                        acceptedDisclaimer = true,
                        userMode = UserMode.EDUCATION_ONLY.name
                    )
                )
            }
        }
    }

    fun onboardUser(
        name: String,
        birthYear: Int?,
        cycleLength: Int,
        periodLength: Int,
        goals: List<String>,
        pin: String = "",
        userMode: String = UserMode.SELF_TRACKING.name,
        genderMode: String = GenderMode.PREFER_NOT_TO_SAY.name,
        pronoun: String = Pronoun.PREFER_NOT_TO_SAY.name,
        customPronoun: String? = null,
        bodyRelevantMode: String = BodyRelevantMode.PREFER_NOT_TO_SAY.name,
        supportRelationship: String? = null,
        consentConfirmed: Boolean = false,
        religion: String? = null,
        country: String? = null,
        region: String? = null,
        city: String? = null,
        selectedConditions: List<String> = emptyList(),
        behaviourFocuses: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val hasPin = pin.isNotEmpty()
            val newProfile = Profile(
                id = 1,
                displayName = name.ifBlank { "Companion" },
                birthYear = birthYear,
                averageCycleLength = cycleLength,
                averagePeriodLength = periodLength,
                goals = goals,
                acceptedDisclaimer = true,
                userMode = userMode,
                genderMode = genderMode,
                pronoun = pronoun,
                customPronoun = customPronoun,
                bodyRelevantMode = bodyRelevantMode,
                supportRelationship = supportRelationship,
                consentConfirmed = consentConfirmed,
                religion = religion,
                country = country,
                region = region,
                city = city,
                selectedConditions = selectedConditions,
                behaviourFocuses = behaviourFocuses,
                locationPrivacyMode = LocationPrivacyMode.OFF.name,
                securityPinEnabled = hasPin,
                securityPin = pin,
                role = UserRole.USER.name
            )
            repository.saveProfile(newProfile)
            _isPinAuthenticated.value = !hasPin
            logAudit("USER", "insert", "profile", "User onboarded")
        }
    }

    // ==============================
    // PIN AUTHENTICATION
    // ==============================

    private var failedPinAttempts = 0
    private var lockoutEndTime: Long = 0

    fun authenticatePin(input: String): Boolean {
        if (System.currentTimeMillis() < lockoutEndTime) {
            logAudit("USER", "login_failed", "pin_auth", "Rate limit active")
            return false
        }

        val currentProfile = profile.value
        return if (currentProfile != null && currentProfile.securityPinEnabled) {
            val matches = currentProfile.securityPin == input
            if (matches) {
                _isPinAuthenticated.value = true
                failedPinAttempts = 0
                logAudit(currentProfile.role, "login", "pin_auth", "PIN authenticated")
            } else {
                failedPinAttempts++
                if (failedPinAttempts >= 5) {
                    lockoutEndTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutes
                }
                logAudit(currentProfile.role, "login_failed", "pin_auth", "Failed PIN attempt")
            }
            matches
        } else {
            _isPinAuthenticated.value = true
            true
        }
    }

    fun lockApp() {
        _isPinAuthenticated.value = false
        logAudit("USER", "logout", "session", "App locked")
    }

    fun logout() {
        viewModelScope.launch {
            _isPinAuthenticated.value = false
            _isGuestUser.value = false
            _tempLat.value = null
            _tempLng.value = null
            logAudit("USER", "logout", "session", "User logged out")
        }
    }

    // ==============================
    // PROFILE UPDATES
    // ==============================

    fun updateCycleSettings(cycleLength: Int, periodLength: Int) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(
                averageCycleLength = cycleLength,
                averagePeriodLength = periodLength
            ))
        }
    }

    fun updateProfileIdentity(
        displayName: String,
        pronoun: String,
        customPronoun: String?,
        genderMode: String,
        religion: String?,
        country: String?,
        region: String?,
        city: String?,
        behaviourFocuses: List<String>
    ) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(
                displayName = displayName,
                pronoun = pronoun,
                customPronoun = customPronoun,
                genderMode = genderMode,
                religion = religion,
                country = country,
                region = region,
                city = city,
                behaviourFocuses = behaviourFocuses
            ))
            logAudit(current.role, "update", "profile", "Identity updated")
        }
    }

    fun updateLocationPrivacyMode(mode: LocationPrivacyMode) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(locationPrivacyMode = mode.name))
            clearTempLocation()
            logAudit(current.role, "update", "profile", "Location privacy mode changed to ${mode.name}")
        }
    }

    fun updateNotificationPreferences(
        periodReminders: Boolean,
        moodReminders: Boolean,
        cupReminders: Boolean,
        selfCareReminders: Boolean,
        reminderTime: String
    ) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(
                periodReminders = periodReminders,
                moodReminders = moodReminders,
                cupReminders = cupReminders,
                selfCareReminders = selfCareReminders,
                reminderTime = reminderTime
            ))
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(isDarkMode = enabled))
        }
    }

    fun saveDashboardLayoutVersion(version: String) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(dashboardLayoutVersion = version))
        }
    }

    // ==============================
    // PERIOD LOGS
    // ==============================

    fun addPeriodLog(
        startDate: String,
        endDate: String?,
        flowLevel: String,
        symptoms: List<String>,
        notes: String?
    ) {
        viewModelScope.launch {
            if (!CycleUtils.isValidDate(startDate)) return@launch
            repository.insertPeriodLog(
                PeriodLog(
                    startDate = startDate,
                    endDate = endDate,
                    flowLevel = flowLevel,
                    symptoms = symptoms,
                    notesEncrypted = SecurityUtils.encrypt(notes)
                )
            )
            logAudit("USER", "insert", "period_logs", null)
        }
    }

    fun deletePeriodLog(id: Int) {
        viewModelScope.launch {
            repository.deletePeriodLog(id)
            logAudit("USER", "delete", "period_logs", id.toString())
        }
    }

    // ==============================
    // BEHAVIOUR CHECK-IN (full)
    // ==============================

    fun addBehaviourLog(
        mood: String,
        stressLevel: Int,
        anxietyLevel: Int,
        sleepHours: Float,
        sleepQuality: Int,
        painLevel: Int,
        energyLevel: Int,
        hydrationLevel: String,
        foodCraving: String?,
        caffeineIntake: String?,
        movement: String?,
        studyWorkPressure: Int,
        relationshipStress: Int,
        socialMediaOverload: Int,
        flowLevel: String?,
        symptoms: List<String>,
        notes: String?,
        crisisFlag: Boolean = false
    ) {
        viewModelScope.launch {
            val flags = mutableListOf<String>()
            if (crisisFlag) flags.add("CRISIS")
            if (painLevel >= 8) flags.add("HIGH_PAIN")
            if (stressLevel >= 8) flags.add("HIGH_STRESS")
            if (sleepHours < 4f) flags.add("LOW_SLEEP")

            repository.insertBehaviourLog(
                BehaviourLog(
                    logDate = CycleUtils.getTodayString(),
                    mood = mood,
                    stressLevel = stressLevel,
                    anxietyLevel = anxietyLevel,
                    sleepHours = sleepHours,
                    sleepQuality = sleepQuality,
                    painLevel = painLevel,
                    energyLevel = energyLevel,
                    hydrationLevel = hydrationLevel,
                    foodCraving = foodCraving,
                    caffeineIntake = caffeineIntake,
                    movement = movement,
                    studyWorkPressure = studyWorkPressure,
                    relationshipStress = relationshipStress,
                    socialMediaOverload = socialMediaOverload,
                    flowLevel = flowLevel,
                    symptoms = symptoms,
                    notesEncrypted = SecurityUtils.encrypt(notes),
                    crisisFlag = crisisFlag,
                    flags = flags
                )
            )
            logAudit("USER", "insert", "behaviour_logs", null)
        }
    }

    fun deleteBehaviourLog(id: Int) {
        viewModelScope.launch {
            repository.deleteBehaviourLog(id)
            logAudit("USER", "delete", "behaviour_logs", id.toString())
        }
    }

    // Legacy mood log (for backward compat)
    fun addMoodLog(mood: String, energy: Int, stress: Int, sleepQuality: Int?, notes: String?) {
        viewModelScope.launch {
            repository.insertMoodLog(
                MoodLog(
                    date = CycleUtils.getTodayString(),
                    mood = mood,
                    energy = energy,
                    stress = stress,
                    sleepQuality = sleepQuality,
                    notes = notes
                )
            )
        }
    }

    fun deleteMoodLog(id: Int) {
        viewModelScope.launch { repository.deleteMoodLog(id) }
    }

    // ==============================
    // MEDICAL JOURNAL
    // ==============================

    fun addMedicalJournalEntry(
        entryDate: String,
        category: String,
        title: String,
        symptoms: List<String>,
        painLevel: Int,
        mood: String?,
        flowLevel: String?,
        medicinesTaken: String?,
        doctorVisit: Boolean,
        nextAppointment: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertMedicalJournalEntry(
                MedicalJournalEntry(
                    entryDate = entryDate,
                    category = category,
                    title = title,
                    symptoms = symptoms,
                    painLevel = painLevel,
                    mood = mood,
                    flowLevel = flowLevel,
                    medicinesTaken = medicinesTaken,
                    doctorVisit = doctorVisit,
                    nextAppointment = nextAppointment,
                    notesEncrypted = SecurityUtils.encrypt(notes),
                    attachmentPath = null
                )
            )
            logAudit("USER", "insert", "medical_journal_entries", null)
        }
    }

    fun deleteMedicalJournalEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteMedicalJournalEntry(id)
            logAudit("USER", "delete", "medical_journal_entries", id.toString())
        }
    }

    // Legacy journal entry
    fun addJournalEntry(title: String, body: String, moodTag: String?, cyclePhase: String?) {
        viewModelScope.launch {
            repository.insertJournalEntry(
                JournalEntry(
                    date = CycleUtils.getTodayString(),
                    title = title,
                    body = body,
                    moodTag = moodTag,
                    cyclePhase = cyclePhase
                )
            )
        }
    }

    fun deleteJournalEntry(id: Int) {
        viewModelScope.launch { repository.deleteJournalEntry(id) }
    }

    // ==============================
    // MEDICINE REMINDERS
    // ==============================

    fun addMedicalReminder(
        title: String,
        reminderType: String,
        reminderTime: String,
        repeatRule: String,
        startDate: String,
        endDate: String?,
        notes: String?,
        reasonNote: String?
    ) {
        viewModelScope.launch {
            repository.insertMedicalReminder(
                MedicalReminder(
                    title = title,
                    reminderType = reminderType,
                    reminderTime = reminderTime,
                    repeatRule = repeatRule,
                    startDate = startDate,
                    endDate = endDate,
                    enabled = true,
                    notesEncrypted = SecurityUtils.encrypt(notes),
                    reasonNote = reasonNote
                )
            )
            logAudit("USER", "insert", "medical_reminders", null)
        }
    }

    fun toggleReminderEnabled(reminder: MedicalReminder) {
        viewModelScope.launch {
            repository.updateMedicalReminder(reminder.copy(enabled = !reminder.enabled))
        }
    }

    fun deleteMedicalReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteMedicalReminder(id)
            logAudit("USER", "delete", "medical_reminders", id.toString())
        }
    }

    // ==============================
    // CUP CARE LOG
    // ==============================

    fun addCupCareLog(
        insertedAt: String?,
        emptiedAt: String?,
        cleanedToday: Boolean,
        discomfortLevel: Int,
        leakageIssue: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertCupCareLog(
                CupCareLog(
                    insertedAt = insertedAt,
                    emptiedAt = emptiedAt,
                    cleanedToday = cleanedToday,
                    discomfortLevel = discomfortLevel,
                    leakageIssue = leakageIssue,
                    notesEncrypted = SecurityUtils.encrypt(notes),
                    logDate = CycleUtils.getTodayString()
                )
            )
            logAudit("USER", "insert", "cup_care_logs", null)
        }
    }

    fun deleteCupCareLog(id: Int) {
        viewModelScope.launch { repository.deleteCupCareLog(id) }
    }

    // ==============================
    // NOTIFICATIONS
    // ==============================

    fun addNotification(title: String, message: String, type: String) {
        viewModelScope.launch {
            repository.insertNotification(
                InAppNotification(
                    title = title,
                    message = message,
                    type = type,
                    isRead = false,
                    createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch { repository.deleteNotification(id) }
    }

    // ==============================
    // BOOKMARKS
    // ==============================

    fun toggleBookmark(slug: String) {
        viewModelScope.launch {
            val list = bookmarks.value
            val exists = list.any { it.articleSlug == slug }
            if (exists) {
                repository.removeBookmark(slug)
            } else {
                repository.addBookmark(slug)
            }
        }
    }

    // ==============================
    // AI TOKEN MANAGEMENT
    // ==============================

    fun isAiLimitReached(): Boolean {
        val p = profile.value ?: return false
        val limit = if (p.isPremium) PREMIUM_AI_MESSAGES_LIMIT else FREE_AI_MESSAGES_LIMIT
        return p.aiMessagesUsedThisMonth >= limit
    }

    fun getRemainingAiMessages(): Int {
        val p = profile.value ?: return 0
        val limit = if (p.isPremium) PREMIUM_AI_MESSAGES_LIMIT else FREE_AI_MESSAGES_LIMIT
        return (limit - p.aiMessagesUsedThisMonth).coerceAtLeast(0)
    }

    fun consumeAiMessage() {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            val currentMonth = java.time.YearMonth.now().toString()
            val monthMatches = current.aiCurrentMonth == currentMonth
            val newUsed = if (monthMatches) current.aiMessagesUsedThisMonth + 1 else 1
            repository.saveProfile(current.copy(
                aiMessagesUsedThisMonth = newUsed,
                aiCurrentMonth = currentMonth
            ))
        }
    }

    // ==============================
    // RBAC CHECKS (local stubs)
    // ==============================

    fun userHasRole(requiredRole: UserRole): Boolean {
        val userRoleStr = profile.value?.role ?: UserRole.USER.name
        val userRole = try { UserRole.valueOf(userRoleStr) } catch (e: Exception) { UserRole.USER }
        return when (requiredRole) {
            UserRole.USER -> true
            UserRole.PREMIUM_USER -> userRole in listOf(
                UserRole.PREMIUM_USER, UserRole.ADMIN, UserRole.SUPER_ADMIN
            )
            UserRole.MODERATOR -> userRole in listOf(
                UserRole.MODERATOR, UserRole.ADMIN, UserRole.SUPER_ADMIN
            )
            UserRole.ADMIN -> userRole in listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)
            UserRole.SUPER_ADMIN -> userRole == UserRole.SUPER_ADMIN
            else -> false
        }
    }

    // ==============================
    // AUDIT LOGGING (internal)
    // ==============================

    private fun logAudit(role: String, action: String, resourceType: String, resourceId: String?) {
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    actorRole = role,
                    action = action,
                    resourceType = resourceType,
                    resourceId = resourceId,
                    ipHash = null,       // No network IP in local-only mode
                    userAgentHash = null,
                    metadata = "{}",
                    createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        }
    }

    // ==============================
    // DATA MANAGEMENT
    // ==============================

    fun clearAllUserData() {
        viewModelScope.launch {
            logAudit("USER", "delete", "all_data", "User requested data deletion")
            repository.clearAllData()
            _isPinAuthenticated.value = false
            _isGuestUser.value = false
            _tempLat.value = null
            _tempLng.value = null
        }
    }
}
