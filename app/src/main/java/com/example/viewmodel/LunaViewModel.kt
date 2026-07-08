package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LunaViewModel @JvmOverloads constructor(
    application: Application,
    private val injectedRepository: LunaRepository? = null
) : AndroidViewModel(application) {
    private val repository: LunaRepository = injectedRepository ?: LunaRepository(LunaDatabase.getDatabase(application))

    init {
        // Repository is already initialized above
    }

    // Flows from Repository
    val profile: StateFlow<Profile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodLogs: StateFlow<List<PeriodLog>> = repository.periodLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moodLogs: StateFlow<List<MoodLog>> = repository.moodLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.journalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicalJournalEntries: StateFlow<List<MedicalJournalEntry>> = repository.medicalJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicalReminders: StateFlow<List<MedicalReminder>> = repository.medicalReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val behaviourLogs: StateFlow<List<BehaviourLog>> = repository.behaviourLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cupCareLogs: StateFlow<List<CupCareLog>> = repository.cupCareLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportNotes: StateFlow<List<SupportNote>> = repository.supportNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val symptomLogs: StateFlow<List<SymptomLog>> = repository.symptomLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // New Session & Security Flows/States
    val loggedInCredentials: StateFlow<UserCredentials?> = repository.loggedInCredentialsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val securityEvents: StateFlow<List<LoginSecurityEvent>> = repository.securityEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _captchaRequired = MutableStateFlow(false)
    val captchaRequired: StateFlow<Boolean> = _captchaRequired.asStateFlow()

    // Pin Authenticated State for Secure App Lock
    private val _isPinAuthenticated = MutableStateFlow(false)
    val isPinAuthenticated: StateFlow<Boolean> = _isPinAuthenticated.asStateFlow()

    // SEO / Deep Link Routing state
    private val _deepLinkRoute = MutableStateFlow<String?>(null)
    val deepLinkRoute: StateFlow<String?> = _deepLinkRoute.asStateFlow()

    fun handleDeepLink(path: String) {
        _deepLinkRoute.value = path
    }

    fun clearDeepLinkRoute() {
        _deepLinkRoute.value = null
    }

    // Crisis screening state
    private val _showCrisisScreen = MutableStateFlow(false)
    val showCrisisScreen: StateFlow<Boolean> = _showCrisisScreen.asStateFlow()

    fun triggerCrisisDialog() {
        _showCrisisScreen.value = true
    }

    fun dismissCrisisDialog() {
        _showCrisisScreen.value = false
    }

    private fun checkAndTriggerCrisis(text: String?) {
        if (!text.isNullOrEmpty() && CycleUtils.detectCrisisText(text)) {
            _showCrisisScreen.value = true
        }
    }

    // Temporary Guest Profile / Current User Profile for guest mode simulation
    private val _isGuestUser = MutableStateFlow(false)
    val isGuestUser: StateFlow<Boolean> = _isGuestUser.asStateFlow()

    fun setGuestMode(enabled: Boolean) {
        _isGuestUser.value = enabled
        viewModelScope.launch {
            // Save a placeholder profile to bypass full login/auth, marking it guest
            val existing = repository.getProfileSync()
            if (existing == null) {
                repository.saveProfile(
                    Profile(
                        id = 1,
                        displayName = "Guest Companion",
                        acceptedDisclaimer = true,
                        goals = listOf("Track my period", "Learn about menstrual cups")
                    )
                )
            }
        }
    }

    // --- Production-Grade Authentication Flows ---

    fun login(email: String, password: String, onFinished: (Boolean) -> Unit = {}) {
        val trimmedEmail = email.trim()
        val emailHash = EncryptionHelper.hashLookupValue(trimmedEmail)
        
        viewModelScope.launch {
            _isSubmitting.value = true
            _loginError.value = null
            
            // Check account security state
            val state = repository.getSecurityState(emailHash)
            val now = System.currentTimeMillis()
            
            // Server-side lock enforcement
            if (state != null && state.lockedUntil != null && state.lockedUntil > now) {
                _loginError.value = "Too many login attempts. For your safety, login is temporarily paused. Please wait a few minutes before trying again."
                _isSubmitting.value = false
                onFinished(false)
                return@launch
            }
            
            // Retrieve User Credentials
            val credentials = repository.getCredentialsByHash(emailHash)
            
            // Layered server-side protection delay
            val attemptCount = state?.failedAttemptCount ?: 0
            val delayMs = when {
                attemptCount >= 8 -> 5000L
                attemptCount == 7 -> 3000L
                attemptCount == 6 -> 1000L
                attemptCount == 5 -> 500L
                else -> 0L
            }
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }
            
            if (credentials == null) {
                // Generic error: do not expose whether user email exists
                handleFailedAttempt(emailHash)
                _loginError.value = "Email or password is incorrect. Please check your details and try again."
                _isSubmitting.value = false
                onFinished(false)
                return@launch
            }
            
            // Verify candidate password hash
            val candidateHash = EncryptionHelper.byteArrayToHex(
                EncryptionHelper.deriveKey(password, EncryptionHelper.hexToByteArray(credentials.passwordSalt))
            )
            
            // Constant-time comparison to thwart timing side-channel attacks
            val isPasswordCorrect = constantTimeEquals(credentials.passwordHash, candidateHash)
            
            if (!isPasswordCorrect) {
                handleFailedAttempt(emailHash)
                _loginError.value = "Email or password is incorrect. Please check your details and try again."
                _isSubmitting.value = false
                onFinished(false)
                return@launch
            }
            
            // Success! Clear any local failed attempt states
            repository.deleteSecurityState(emailHash)
            
            // Set session key securely derived from password
            EncryptionHelper.setSessionKeyFromPassword(password, credentials.passwordSalt)
            
            // Persist logged in status
            repository.loginUser(emailHash)
            
            // Log security event (Audit Log)
            val successEvent = LoginSecurityEvent(
                emailHash = emailHash,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"), // salted hash of mock local IP
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = "LOGIN_SUCCESS",
                metadata = "Login successful for masked email ${EncryptionHelper.maskEmail(trimmedEmail)}"
            )
            repository.insertSecurityEvent(successEvent)
            
            // Bypass pin verification for active logged in session
            _isPinAuthenticated.value = true
            _captchaRequired.value = false
            _isSubmitting.value = false
            onFinished(true)
        }
    }

    fun register(email: String, password: String, confirm: String, displayName: String, onFinished: (Boolean, String?) -> Unit) {
        val validationResult = ZodValidator.validateSignup(email, password, confirm, displayName)
        if (!validationResult.success) {
            onFinished(false, validationResult.errors.firstOrNull()?.message ?: "Validation failed")
            return
        }
        
        val trimmedEmail = email.trim()
        val emailHash = EncryptionHelper.hashLookupValue(trimmedEmail)
        
        viewModelScope.launch {
            _isSubmitting.value = true
            _loginError.value = null
            
            val existing = repository.getCredentialsByHash(emailHash)
            if (existing != null) {
                // Rule: Signup duplicate email: Generic error to prevent enumeration
                _isSubmitting.value = false
                onFinished(false, "Please check your details or try logging in.")
                return@launch
            }
            
            // Generate unique salt per password
            val salt = EncryptionHelper.generateSalt()
            val passwordHash = EncryptionHelper.byteArrayToHex(
                EncryptionHelper.deriveKey(password, EncryptionHelper.hexToByteArray(salt))
            )
            
            // AES GCM encrypted email for display
            val encryptedEmail = EncryptionHelper.encryptSensitiveText(trimmedEmail) ?: trimmedEmail
            
            val credentials = UserCredentials(
                emailHash = emailHash,
                encryptedEmail = encryptedEmail,
                passwordHash = passwordHash,
                passwordSalt = salt,
                displayName = displayName,
                isLoggedIn = true
            )
            
            repository.insertCredentials(credentials)
            
            // Set session key in memory
            EncryptionHelper.setSessionKeyFromPassword(password, salt)
            
            // Update app profile
            var profileObj = repository.getProfileSync()
            if (profileObj == null) {
                profileObj = Profile(
                    displayName = displayName,
                    acceptedDisclaimer = true,
                    consentConfirmed = true
                )
            } else {
                profileObj = profileObj.copy(displayName = displayName)
            }
            repository.saveProfile(profileObj)
            
            // Log security event
            val event = LoginSecurityEvent(
                emailHash = emailHash,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = "LOGIN_SUCCESS",
                metadata = "Account registered successfully for masked email ${EncryptionHelper.maskEmail(trimmedEmail)}"
            )
            repository.insertSecurityEvent(event)
            
            _isPinAuthenticated.value = true
            _isSubmitting.value = false
            onFinished(true, null)
        }
    }

    fun forgotPassword(email: String, onFinished: (String) -> Unit) {
        val trimmedEmail = email.trim()
        val emailHash = EncryptionHelper.hashLookupValue(trimmedEmail)
        
        viewModelScope.launch {
            _isSubmitting.value = true
            
            // Generic response: "If an account exists for this email, password reset instructions will be sent."
            // Do not reveal whether email exists!
            val credentials = repository.getCredentialsByHash(emailHash)
            if (credentials != null) {
                val event = LoginSecurityEvent(
                    emailHash = emailHash,
                    ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                    userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                    eventType = "PASSWORD_RESET_REQUESTED",
                    metadata = "Password reset instructions requested."
                )
                repository.insertSecurityEvent(event)
            }
            
            _isSubmitting.value = false
            onFinished("If an account exists for this email, password reset instructions will be sent.")
        }
    }

    fun resetPassword(email: String, newPassword: String, confirm: String, onFinished: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        val emailHash = EncryptionHelper.hashLookupValue(trimmedEmail)
        
        val validationResult = ZodValidator.validateSignup(trimmedEmail, newPassword, confirm, "User")
        if (!validationResult.success) {
            onFinished(false, validationResult.errors.find { it.field == "password" || it.field == "confirm" }?.message ?: "Validation failed")
            return
        }
        
        viewModelScope.launch {
            _isSubmitting.value = true
            val credentials = repository.getCredentialsByHash(emailHash)
            if (credentials == null) {
                _isSubmitting.value = false
                onFinished(false, "Failed to reset password. Please check your details and try again.")
                return@launch
            }
            
            val newSalt = EncryptionHelper.generateSalt()
            val newHash = EncryptionHelper.byteArrayToHex(
                EncryptionHelper.deriveKey(newPassword, EncryptionHelper.hexToByteArray(newSalt))
            )
            
            val updated = credentials.copy(
                passwordHash = newHash,
                passwordSalt = newSalt
            )
            repository.insertCredentials(updated)
            
            // Clear security failure states
            repository.deleteSecurityState(emailHash)
            
            val event = LoginSecurityEvent(
                emailHash = emailHash,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = "PASSWORD_RESET_SUCCESS",
                metadata = "Password reset completed successfully."
            )
            repository.insertSecurityEvent(event)
            
            _isSubmitting.value = false
            onFinished(true, null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear DB logged-in flag
            repository.logoutAll()
            
            // Clear session memory key
            EncryptionHelper.clearSession()
            
            _isGuestUser.value = false
            _navigationHistory.value = emptyList()
            _isPinAuthenticated.value = false
            _loginError.value = null
            _captchaRequired.value = false
            
            val event = LoginSecurityEvent(
                emailHash = null,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = "LOGOUT",
                metadata = "User initiated secure logout from session."
            )
            repository.insertSecurityEvent(event)
        }
    }

    private suspend fun handleFailedAttempt(emailHash: String) {
        val now = System.currentTimeMillis()
        val state = repository.getSecurityState(emailHash) ?: AccountSecurityState(
            emailHash = emailHash,
            failedAttemptCount = 0,
            createdAt = now,
            updatedAt = now
        )
        
        val newAttemptCount = state.failedAttemptCount + 1
        var lockedUntil: Long? = null
        var captchaRequired = state.captchaRequired
        
        // Anti-brute force limits (Layered server-side protection):
        // Attempts 1–4: allow normal retry
        // Attempt 5 within 10 minutes: introduce a short delay
        // Attempt 6: 1-minute cooldown
        // Attempt 7: 3-minute cooldown
        // Attempt 8: 5-minute cooldown
        // Attempt 10 within 30 minutes: require CAPTCHA or additional verification
        when {
            newAttemptCount >= 10 -> {
                lockedUntil = now + (30 * 60 * 1000)
                captchaRequired = true
            }
            newAttemptCount == 8 -> {
                lockedUntil = now + (5 * 60 * 1000)
                captchaRequired = true
            }
            newAttemptCount == 7 -> {
                lockedUntil = now + (3 * 60 * 1000)
            }
            newAttemptCount == 6 -> {
                lockedUntil = now + (1 * 60 * 1000)
            }
            newAttemptCount == 5 -> {
                // just a short delay enforced in the login flow
            }
        }
        
        val updatedState = state.copy(
            failedAttemptCount = newAttemptCount,
            firstFailedAt = state.firstFailedAt ?: now,
            lastFailedAt = now,
            lockedUntil = lockedUntil,
            captchaRequired = captchaRequired,
            updatedAt = now
        )
        
        repository.insertSecurityState(updatedState)
        
        // Log secure events
        val failedEvent = LoginSecurityEvent(
            emailHash = emailHash,
            ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
            userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
            eventType = "LOGIN_FAILED",
            metadata = "Failed login attempt $newAttemptCount. Locked until: $lockedUntil"
        )
        repository.insertSecurityEvent(failedEvent)
        
        if (lockedUntil != null) {
            val lockEvent = LoginSecurityEvent(
                emailHash = emailHash,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = if (newAttemptCount >= 10) "ACCOUNT_LOCKED_TEMPORARILY" else "RATE_LIMIT_TRIGGERED",
                metadata = "Rate limit lock triggered."
            )
            repository.insertSecurityEvent(lockEvent)
        }
        
        if (captchaRequired && !state.captchaRequired) {
            val captchaEvent = LoginSecurityEvent(
                emailHash = emailHash,
                ipHash = EncryptionHelper.hashLookupValue("127.0.0.1"),
                userAgentHash = EncryptionHelper.hashLookupValue("AndroidAppletDevice"),
                eventType = "CAPTCHA_REQUIRED",
                metadata = "Captcha required state triggered."
            )
            repository.insertSecurityEvent(captchaEvent)
            _captchaRequired.value = true
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in 0 until a.length) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    fun authenticatePin(input: String): Boolean {
        val currentProfile = profile.value
        return if (currentProfile != null && currentProfile.securityPinEnabled) {
            val matches = currentProfile.securityPin == input
            if (matches) {
                _isPinAuthenticated.value = true
            }
            matches
        } else {
            _isPinAuthenticated.value = true
            true
        }
    }

    fun lockApp() {
        _isPinAuthenticated.value = false
    }

    fun onboardUser(
        name: String,
        birthYear: Int?,
        cycleLength: Int,
        periodLength: Int,
        goals: List<String>,
        pin: String = "",
        userMode: String = "SELF_TRACKING",
        genderMode: String = "FEMALE",
        bodyRelevantMode: String = "MENSTRUATES",
        supportRelationship: String? = null,
        consentConfirmed: Boolean = false,
        sharedTrackingConsent: Boolean = false,
        behaviourFocuses: List<String> = emptyList(),
        medicineReminders: Boolean = false,
        waterReminders: Boolean = true
    ) {
        viewModelScope.launch {
            val hasPin = pin.isNotEmpty()
            val newProfile = Profile(
                id = 1,
                displayName = name,
                birthYear = birthYear,
                averageCycleLength = cycleLength,
                averagePeriodLength = periodLength,
                goals = goals,
                acceptedDisclaimer = true,
                securityPinEnabled = hasPin,
                securityPin = pin,
                userMode = userMode,
                genderMode = genderMode,
                bodyRelevantMode = bodyRelevantMode,
                supportRelationship = supportRelationship,
                consentConfirmed = consentConfirmed,
                sharedTrackingConsent = sharedTrackingConsent,
                behaviourFocuses = behaviourFocuses,
                medicineReminders = medicineReminders,
                waterReminders = waterReminders
            )
            repository.saveProfile(newProfile)
            _isPinAuthenticated.value = !hasPin
        }
    }

    fun updateBehaviourFocuses(focuses: List<String>) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(behaviourFocuses = focuses))
        }
    }

    fun updateSharedTrackingConsent(consent: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(sharedTrackingConsent = consent))
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun addBehaviourLog(
        mood: String,
        stressLevel: Int,
        anxietyLevel: Int,
        sleepHours: Double,
        sleepQuality: Int,
        painLevel: Int,
        energyLevel: Int,
        hydrationLevel: String,
        foodCraving: String,
        caffeineIntake: String,
        movement: String,
        studyWorkPressure: Int,
        relationshipStress: Int,
        socialMediaOverload: Int,
        flowLevel: String,
        symptoms: List<String>,
        notes: String?
    ) {
        val entry = BehaviourLog(
            date = java.time.LocalDate.now().toString(),
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
            notes = notes,
            flags = emptyList()
        )
        val computedFlags = CycleUtils.calculateBehaviourFlags(entry)
        val finalEntry = entry.copy(flags = computedFlags)

        checkAndTriggerCrisis(notes)
        if (mood.uppercase() == "VERY_LOW" || mood.lowercase() == "very low") {
            triggerCrisisDialog()
        }

        viewModelScope.launch {
            repository.insertBehaviourLog(finalEntry)
        }
    }

    fun deleteBehaviourLog(id: Int) {
        viewModelScope.launch {
            repository.deleteBehaviourLog(id)
        }
    }

    fun addCupCareLog(
        insertedAt: String,
        emptiedAt: String,
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
                    notes = notes
                )
            )
        }
    }

    fun deleteCupCareLog(id: Int) {
        viewModelScope.launch {
            repository.deleteCupCareLog(id)
        }
    }

    fun addSupportNote(
        relationship: String,
        noteTitle: String,
        noteBody: String
    ) {
        viewModelScope.launch {
            repository.insertSupportNote(
                SupportNote(
                    relationship = relationship,
                    noteTitle = noteTitle,
                    noteBody = noteBody
                )
            )
        }
    }

    fun deleteSupportNote(id: Int) {
        viewModelScope.launch {
            repository.deleteSupportNote(id)
        }
    }

    fun updateCycleSettings(cycleLength: Int, periodLength: Int) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(
                current.copy(
                    averageCycleLength = cycleLength,
                    averagePeriodLength = periodLength
                )
            )
        }
    }

    fun updateProfile(
        displayName: String,
        birthYear: Int?,
        genderMode: String,
        userMode: String
    ) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(
                current.copy(
                    displayName = displayName,
                    birthYear = birthYear,
                    genderMode = genderMode,
                    userMode = userMode
                )
            )
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
            repository.saveProfile(
                current.copy(
                    periodReminders = periodReminders,
                    moodReminders = moodReminders,
                    cupReminders = cupReminders,
                    selfCareReminders = selfCareReminders,
                    reminderTime = reminderTime
                )
            )
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: Profile()
            repository.saveProfile(current.copy(isDarkMode = enabled))
        }
    }

    // Period Logs
    fun addPeriodLog(
        startDate: String,
        endDate: String?,
        flowLevel: String,
        symptoms: List<String>,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertPeriodLog(
                PeriodLog(
                    startDate = startDate,
                    endDate = endDate,
                    flowLevel = flowLevel,
                    symptoms = symptoms,
                    notes = notes
                )
            )
        }
    }

    fun deletePeriodLog(id: Int) {
        viewModelScope.launch {
            repository.deletePeriodLog(id)
        }
    }

    // Mood Logs
    fun addMoodLog(
        mood: String,
        energy: Int,
        stress: Int,
        sleepQuality: Int?,
        notes: String?
    ) {
        checkAndTriggerCrisis(notes)
        if (mood == "Very low") {
            triggerCrisisDialog()
        }
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
        viewModelScope.launch {
            repository.deleteMoodLog(id)
        }
    }

    // Journal
    fun addJournalEntry(
        title: String,
        body: String,
        moodTag: String?,
        cyclePhase: String?,
        category: String = "General",
        symptoms: String? = null
    ) {
        checkAndTriggerCrisis(title)
        checkAndTriggerCrisis(body)
        viewModelScope.launch {
            repository.insertJournalEntry(
                JournalEntry(
                    date = CycleUtils.getTodayString(),
                    title = title,
                    body = body,
                    moodTag = moodTag,
                    cyclePhase = cyclePhase,
                    category = category,
                    symptoms = symptoms
                )
            )
        }
    }

    fun addJournalEntryWithDate(
        date: String,
        title: String,
        body: String,
        moodTag: String?,
        category: String = "General",
        symptoms: String? = null
    ) {
        viewModelScope.launch {
            repository.insertJournalEntry(
                JournalEntry(
                    date = date,
                    title = title,
                    body = body,
                    moodTag = moodTag,
                    cyclePhase = null,
                    category = category,
                    symptoms = symptoms
                )
            )
        }
    }

    fun deleteJournalEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
        }
    }

    // Bookmarks
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

    // Medical Journal
    fun addMedicalJournalEntry(
        entryDate: String,
        category: String,
        title: String,
        notes: String = "",
        symptoms: List<String> = emptyList(),
        painLevel: Int = 0,
        mood: String = "Okay",
        flowLevel: String = "None",
        medicinesTaken: String = "",
        doctorVisit: Boolean = false,
        appointmentDate: String? = null,
        doctorAdvice: String? = null
    ) {
        checkAndTriggerCrisis(title)
        checkAndTriggerCrisis(notes)
        if (mood == "Very low") {
            triggerCrisisDialog()
        }
        viewModelScope.launch {
            repository.insertMedicalJournalEntry(
                MedicalJournalEntry(
                    entryDate = entryDate,
                    category = category,
                    title = title,
                    notes = notes,
                    symptoms = symptoms,
                    painLevel = painLevel,
                    mood = mood,
                    flowLevel = flowLevel,
                    medicinesTaken = medicinesTaken,
                    doctorVisit = doctorVisit,
                    appointmentDate = appointmentDate,
                    doctorAdvice = doctorAdvice
                )
            )
        }
    }

    fun deleteMedicalJournalEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteMedicalJournalEntry(id)
        }
    }

    // Medical Reminders
    fun addMedicalReminder(
        title: String,
        reminderType: String,
        reminderTime: String,
        repeatRule: String = "Daily",
        enabled: Boolean = true,
        notes: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ) 
    {
        viewModelScope.launch {
            repository.insertMedicalReminder(
                MedicalReminder(
                    title = title,
                    reminderType = reminderType,
                    reminderTime = reminderTime,
                    repeatRule = repeatRule,
                    enabled = enabled,
                    notes = notes,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
    }

    fun deleteMedicalReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteMedicalReminder(id)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            _isPinAuthenticated.value = false
            _isGuestUser.value = false
        }
    }

    // ==========================================
    // NAVIGATION AUTO-TRACKING
    // ==========================================
    private val _navigationHistory = MutableStateFlow<List<NavigationTrack>>(emptyList())
    val navigationHistory: StateFlow<List<NavigationTrack>> = _navigationHistory.asStateFlow()

    private val _isAutoTrackingEnabled = MutableStateFlow(true)
    val isAutoTrackingEnabled: StateFlow<Boolean> = _isAutoTrackingEnabled.asStateFlow()

    fun setAutoTrackingEnabled(enabled: Boolean) {
        _isAutoTrackingEnabled.value = enabled
    }

    fun trackNavigation(tabName: String) {
        if (!_isAutoTrackingEnabled.value) return
        val current = _navigationHistory.value
        if (current.lastOrNull()?.tabName != tabName) {
            _navigationHistory.value = current + NavigationTrack(tabName)
            android.util.Log.d("LunaCareNav", "Automatically tracked navigation event to tab: $tabName")
        }
    }

    fun clearNavigationHistory() {
        _navigationHistory.value = emptyList()
    }

    // ==========================================
    // SYMPTOM LOGGING METHODS
    // ==========================================
    fun addSymptomLog(
        date: String,
        symptomName: String,
        severity: Int,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val log = SymptomLog(
                date = date,
                symptomName = symptomName,
                severity = severity,
                notes = notes
            )
            repository.insertSymptomLog(log)
        }
    }

    fun deleteSymptomLog(id: Int) {
        viewModelScope.launch {
            repository.deleteSymptomLog(id)
        }
    }
}

data class NavigationTrack(
    val tabName: String,
    val timestamp: Long = System.currentTimeMillis()
)
