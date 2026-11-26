package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LunaViewModel @JvmOverloads constructor(
    application: Application,
    testDatabase: LunaDatabase? = null
) : AndroidViewModel(application) {

    private val repository: LunaRepository

    // Free tier AI limits
    companion object {
        const val FREE_AI_MESSAGES_LIMIT = 20
        const val PREMIUM_AI_MESSAGES_LIMIT = 200
        const val MIN_CYCLE_LENGTH = 15
        const val MAX_CYCLE_LENGTH = 60
        const val MIN_PERIOD_LENGTH = 1
        const val MAX_PERIOD_LENGTH = 20

        private val REMINDER_TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        private val RESERVED_USERNAMES = setOf(
            "admin", "administrator", "root", "system", "support", "help",
            "security", "moderator", "staff", "official", "lunacare", "luna_care",
            "luna", "api", "www", "mail", "account", "accounts", "billing",
            "privacy", "terms", "legal", "status", "health", "auth", "login",
            "signup", "register", "settings", "me", "profile", "user", "users",
            "null", "undefined", "anonymous", "guest", "doctor", "nurse",
            "compliance", "emergency", "crisis", "internal", "operator", "moderation"
        )
        private val IMPERSONATION_PREFIXES = listOf(
            "lunacare_admin", "lunacare_support", "lunacare_official", "lunacare_team",
            "lunacare_security", "lunacare_staff", "lunacare_help", "lunacare_system",
            "official_lunacare", "official_", "admin_", "support_", "security_"
        )

        fun isValidCycleSettings(cycleLength: Int, periodLength: Int): Boolean {
            return cycleLength in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH &&
                periodLength in MIN_PERIOD_LENGTH..MAX_PERIOD_LENGTH &&
                periodLength < cycleLength
        }

        fun isValidReminderTime(time: String): Boolean = REMINDER_TIME_REGEX.matches(time.trim())

        fun isAiLimitReachedForProfile(
            profile: Profile?,
            currentMonth: String = java.time.YearMonth.now().toString()
        ): Boolean {
            val p = profile ?: return false
            val limit = if (p.isPremium) PREMIUM_AI_MESSAGES_LIMIT else FREE_AI_MESSAGES_LIMIT
            val usedThisMonth = if (p.aiCurrentMonth == currentMonth) p.aiMessagesUsedThisMonth else 0
            return usedThisMonth >= limit
        }

        fun getRemainingAiMessagesForProfile(
            profile: Profile?,
            currentMonth: String = java.time.YearMonth.now().toString()
        ): Int {
            val p = profile ?: return 0
            val limit = if (p.isPremium) PREMIUM_AI_MESSAGES_LIMIT else FREE_AI_MESSAGES_LIMIT
            val usedThisMonth = if (p.aiCurrentMonth == currentMonth) p.aiMessagesUsedThisMonth else 0
            return (limit - usedThisMonth).coerceAtLeast(0)
        }

        fun validateUsernameRules(raw: String): UsernameValidationResult {
            var cleaned = raw.trim()
            if (cleaned.startsWith("@")) cleaned = cleaned.substring(1)

            if (cleaned.length < 3 || cleaned.length > 30) {
                return UsernameValidationResult(false, message = "Username must contain 3–30 characters.")
            }

            val allowedRegex = Regex("^[a-zA-Z0-9_.-]+$")
            if (!allowedRegex.matches(cleaned)) {
                return UsernameValidationResult(false, message = "Only letters, numbers, periods, hyphens, and underscores are allowed.")
            }

            if (cleaned.startsWith(".") || cleaned.startsWith("_") || cleaned.startsWith("-")) {
                return UsernameValidationResult(false, message = "Username cannot start with a period, hyphen, or underscore.")
            }

            if (cleaned.endsWith(".") || cleaned.endsWith("_") || cleaned.endsWith("-")) {
                return UsernameValidationResult(false, message = "Username cannot end with a period, hyphen, or underscore.")
            }

            if (Regex("[-._]{2,}").containsMatchIn(cleaned)) {
                return UsernameValidationResult(false, message = "Username cannot contain consecutive periods, hyphens, or underscores.")
            }

            val normalized = java.text.Normalizer.normalize(cleaned, java.text.Normalizer.Form.NFKC).lowercase()
            if (RESERVED_USERNAMES.contains(normalized) ||
                IMPERSONATION_PREFIXES.any { normalized.startsWith(it) } ||
                normalized.endsWith("_official")
            ) {
                return UsernameValidationResult(false, message = "Username is not available.")
            }

            return UsernameValidationResult(true, cleaned = cleaned, normalized = normalized)
        }
    }

    init {
        val database = testDatabase ?: LunaDatabase.getDatabase(application)
        repository = LunaRepository(database)
    }

    // ==============================
    // CORE STATE FLOWS
    // ==============================

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

    private val bookmarkMutex = Mutex()

    val profile: StateFlow<Profile?> = repository.profile
        .onEach { _isProfileLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                securityPin = if (hasPin) SecurityUtils.hashPin(pin) else "",
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

    fun isPinLockedOut(): Boolean = System.currentTimeMillis() < lockoutEndTime

    fun authenticatePin(input: String): Boolean {
        if (System.currentTimeMillis() < lockoutEndTime) {
            logAudit("USER", "login_failed", "pin_auth", "Rate limit active")
            return false
        }

        val currentProfile = profile.value ?: return false
        return if (currentProfile.securityPinEnabled) {
            val matches = SecurityUtils.verifyPin(input, currentProfile.securityPin)
            if (matches) {
                _isPinAuthenticated.value = true
                failedPinAttempts = 0
                lockoutEndTime = 0
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

    fun updateCycleSettings(cycleLength: Int, periodLength: Int): Boolean {
        if (cycleLength !in 1..365 || periodLength !in 1..cycleLength) {
            return false
        }
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(
                averageCycleLength = cycleLength,
                averagePeriodLength = periodLength
            ))
        }
        return true
    }

    fun updateUserMode(mode: UserMode, consentConfirmed: Boolean = false): Boolean {
        if (mode == UserMode.SUPPORT_MODE && !consentConfirmed) {
            return false
        }
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(
                current.copy(
                    userMode = mode.name,
                    consentConfirmed = if (mode == UserMode.SUPPORT_MODE) true else current.consentConfirmed
                )
            )
            logAudit(current.role, "update", "profile", "User mode changed to ${mode.name}")
        }
        return true
    }

    // ==============================
    // USERNAME AVAILABILITY STATE & VALIDATION (Req 78, 85, 107, 108)
    // ==============================

    sealed class UsernameAvailabilityState {
        object Idle : UsernameAvailabilityState()
        object Checking : UsernameAvailabilityState()
        data class Available(val username: String) : UsernameAvailabilityState()
        data class Unavailable(val reason: String = "Username is not available.") : UsernameAvailabilityState()
        data class Invalid(val message: String) : UsernameAvailabilityState()
    }

    private val _usernameState = MutableStateFlow<UsernameAvailabilityState>(UsernameAvailabilityState.Idle)
    val usernameState: StateFlow<UsernameAvailabilityState> = _usernameState.asStateFlow()

    data class UsernameValidationResult(
        val isValid: Boolean,
        val cleaned: String = "",
        val normalized: String = "",
        val message: String = ""
    )

    fun validateLocalUsername(raw: String): UsernameValidationResult {
        return validateUsernameRules(raw)
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
        behaviourFocuses: List<String>,
        username: String? = null,
        clearUsername: Boolean = false
    ) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            val cleanedName = displayName.trim().ifBlank { current.displayName.ifBlank { "Companion" } }
            val validatedUsername = username?.trim()?.takeIf { it.isNotBlank() }?.let { candidate ->
                val result = validateUsernameRules(candidate)
                if (result.isValid) result else null
            }
            val updatedUsername = when {
                clearUsername -> null
                validatedUsername != null -> validatedUsername.cleaned
                else -> current.username
            }
            val normalized = when {
                clearUsername -> null
                validatedUsername != null -> validatedUsername.normalized
                else -> current.usernameNormalized
            }
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            repository.saveProfile(current.copy(
                displayName = cleanedName,
                username = updatedUsername,
                usernameNormalized = normalized,
                usernameUpdatedAt = if (updatedUsername != current.username) now else current.usernameUpdatedAt,
                pronoun = pronoun,
                customPronoun = customPronoun?.trim()?.ifBlank { null },
                genderMode = genderMode,
                religion = religion,
                country = country?.trim()?.ifBlank { null },
                region = region?.trim()?.ifBlank { null },
                city = city?.trim()?.ifBlank { null },
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
            if (!CycleUtils.isValidDateRange(startDate, endDate)) return@launch
            repository.insertPeriodLog(
                PeriodLog(
                    startDate = startDate,
                    endDate = endDate?.ifBlank { null },
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
            val safeStress = stressLevel.coerceIn(0, 10)
            val safeAnxiety = anxietyLevel.coerceIn(0, 10)
            val safeSleepHours = sleepHours.coerceIn(0f, 24f)
            val safeSleepQuality = sleepQuality.coerceIn(0, 10)
            val safePain = painLevel.coerceIn(0, 10)
            val safeEnergy = energyLevel.coerceIn(0, 10)

            val flags = mutableListOf<String>()
            if (crisisFlag) flags.add("CRISIS")
            if (safePain >= 8) flags.add("HIGH_PAIN")
            if (safeStress >= 8) flags.add("HIGH_STRESS")
            if (safeSleepHours < 4f) flags.add("LOW_SLEEP")

            repository.insertBehaviourLog(
                BehaviourLog(
                    logDate = CycleUtils.getTodayString(),
                    mood = mood,
                    stressLevel = safeStress,
                    anxietyLevel = safeAnxiety,
                    sleepHours = safeSleepHours,
                    sleepQuality = safeSleepQuality,
                    painLevel = safePain,
                    energyLevel = safeEnergy,
                    hydrationLevel = hydrationLevel,
                    foodCraving = foodCraving,
                    caffeineIntake = caffeineIntake,
                    movement = movement,
                    studyWorkPressure = studyWorkPressure.coerceIn(0, 10),
                    relationshipStress = relationshipStress.coerceIn(0, 10),
                    socialMediaOverload = socialMediaOverload.coerceIn(0, 10),
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
            if (!CycleUtils.isValidDate(entryDate)) return@launch
            val cleanedNextAppt = nextAppointment?.trim()?.ifBlank { null }
            if (cleanedNextAppt != null && !CycleUtils.isValidDate(cleanedNextAppt)) return@launch
            repository.insertMedicalJournalEntry(
                MedicalJournalEntry(
                    entryDate = entryDate,
                    category = category,
                    title = title.trim().ifBlank { "Entry" },
                    symptoms = symptoms,
                    painLevel = painLevel.coerceIn(0, 10),
                    mood = mood,
                    flowLevel = flowLevel,
                    medicinesTaken = medicinesTaken,
                    doctorVisit = doctorVisit,
                    nextAppointment = cleanedNextAppt,
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

    // Legacy journal entry (encrypted at rest; fails closed if Keystore encryption fails)
    fun addJournalEntry(title: String, body: String, moodTag: String?, cyclePhase: String?) {
        val trimmedBody = body.trim()
        if (trimmedBody.isBlank()) return
        viewModelScope.launch {
            val encryptedBody = SecurityUtils.encryptJournalBody(trimmedBody) ?: return@launch
            repository.insertJournalEntry(
                JournalEntry(
                    date = CycleUtils.getTodayString(),
                    title = title.trim().ifBlank { "Reflections" },
                    body = encryptedBody,
                    moodTag = moodTag,
                    cyclePhase = cyclePhase
                )
            )
            logAudit("USER", "insert", "journal_entries", null)
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
            if (title.isBlank()) return@launch
            if (!CycleUtils.isValidDateRange(startDate, endDate)) return@launch
            if (!isValidReminderTime(reminderTime)) return@launch
            repository.insertMedicalReminder(
                MedicalReminder(
                    title = title.trim(),
                    reminderType = reminderType,
                    reminderTime = reminderTime.trim(),
                    repeatRule = repeatRule,
                    startDate = startDate,
                    endDate = endDate?.ifBlank { null },
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
            bookmarkMutex.withLock {
                val list = repository.bookmarks.first()
                val exists = list.any { it.articleSlug == slug }
                if (exists) {
                    repository.removeBookmark(slug)
                } else {
                    repository.addBookmark(slug)
                }
            }
        }
    }

    // ==============================
    // AI TOKEN MANAGEMENT
    // ==============================

    fun isAiLimitReached(): Boolean = isAiLimitReachedForProfile(profile.value)

    fun getRemainingAiMessages(): Int = getRemainingAiMessagesForProfile(profile.value)

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
            failedPinAttempts = 0
            lockoutEndTime = 0
            _isPinAuthenticated.value = false
            _isGuestUser.value = false
            _tempLat.value = null
            _tempLng.value = null
        }
    }
}
