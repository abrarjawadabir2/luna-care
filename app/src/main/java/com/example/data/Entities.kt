package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================
// USER MODE ENUMS (stored as strings in DB)
// ==========================================

enum class UserMode { SELF_TRACKING, SUPPORT_MODE, EDUCATION_ONLY }
enum class GenderMode { FEMALE, MALE, OTHER, PREFER_NOT_TO_SAY }
enum class Pronoun { SHE_HER, HE_HIM, THEY_THEM, CUSTOM, PREFER_NOT_TO_SAY }
enum class BodyRelevantMode { MENSTRUATES, DOES_NOT_MENSTRUATE, NOT_SURE, PREFER_NOT_TO_SAY }
enum class SupportRelationship { WIFE, MOTHER, DAUGHTER, GIRLFRIEND, FEMALE_PARTNER, SISTER, FRIEND, OTHER }
enum class Religion { ISLAM, HINDU, CHRISTIAN, BUDDHIST, OTHER, PREFER_NOT_TO_SAY }
enum class LocationPrivacyMode { OFF, ON_DEVICE_ONLY, APPROXIMATE_REGION, TEMPORARY_EXACT }

enum class BehaviourFocus {
    MOOD, STRESS, ANXIETY, SLEEP, PERIOD_PAIN, PMS, PCOS_PCOD_AWARENESS,
    PMOS_AWARENESS, MENSTRUAL_CUP, FOOD_CRAVINGS, HYDRATION, PRODUCT_CARE,
    MEDICINE_REMINDER, DOCTOR_VISIT, RELATIONSHIP_SUPPORT, EMERGENCY_SIGNS
}

enum class UserRole { USER, PREMIUM_USER, MODERATOR, MEDICAL_CONTENT_REVIEWER, SUPPORT_AGENT, ADMIN, SUPER_ADMIN }

// ==========================================
// PROFILE ENTITY
// ==========================================

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "",
    val birthYear: Int? = null,
    val averageCycleLength: Int = 28,
    val averagePeriodLength: Int = 5,
    val goals: List<String> = emptyList(),
    val acceptedDisclaimer: Boolean = false,

    // User mode & identity
    val userMode: String = UserMode.SELF_TRACKING.name,
    val genderMode: String = GenderMode.PREFER_NOT_TO_SAY.name,
    val pronoun: String = Pronoun.PREFER_NOT_TO_SAY.name,
    val customPronoun: String? = null,
    val bodyRelevantMode: String = BodyRelevantMode.PREFER_NOT_TO_SAY.name,

    // Support mode
    val supportRelationship: String? = null,
    val consentConfirmed: Boolean = false,
    val sharedTrackingConsent: Boolean = false,

    // Region & culture
    val religion: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val languagePreference: String? = null,

    // Health self-reported awareness (NOT diagnosis)
    val selectedConditions: List<String> = emptyList(),

    // Behaviour focus
    val behaviourFocuses: List<String> = emptyList(),

    // Privacy & location
    val locationPrivacyMode: String = LocationPrivacyMode.OFF.name,
    val lastLocationPermissionStatus: String? = null,

    // App preferences
    val isDarkMode: Boolean = false,
    val securityPinEnabled: Boolean = false,
    val securityPin: String = "",
    val periodReminders: Boolean = true,
    val moodReminders: Boolean = true,
    val cupReminders: Boolean = false,
    val selfCareReminders: Boolean = true,
    val reminderTime: String = "20:00",
    val dashboardLayoutVersion: String = "new",
    val visibleStatus: Boolean = true,

    // Role (local stub; in full backend would be verified server-side)
    val role: String = UserRole.USER.name,

    // Premium / AI
    val isPremium: Boolean = false,
    val aiTokensUsedThisMonth: Int = 0,
    val aiMessagesUsedThisMonth: Int = 0,
    val aiCurrentMonth: String = ""
)

// ==========================================
// PERIOD LOG
// ==========================================

@Entity(tableName = "period_logs")
data class PeriodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String,       // YYYY-MM-DD
    val endDate: String?,        // YYYY-MM-DD, nullable
    val flowLevel: String,       // Spotting, Light, Medium, Heavy
    val symptoms: List<String>,
    val notesEncrypted: String?  // AES-256 encrypted
)

// ==========================================
// BEHAVIOUR / MOOD LOG
// ==========================================

@Entity(tableName = "behaviour_logs")
data class BehaviourLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val logDate: String,            // YYYY-MM-DD
    val mood: String,               // Great, Good, Okay, Low, Anxious, Overwhelmed, Very low
    val stressLevel: Int,           // 0-10
    val anxietyLevel: Int,          // 0-10
    val sleepHours: Float,          // 0-12
    val sleepQuality: Int,          // 0-10
    val painLevel: Int,             // 0-10
    val energyLevel: Int,           // 0-10
    val hydrationLevel: String,     // Low, Okay, Good, Great
    val foodCraving: String?,
    val caffeineIntake: String?,    // None, Low, Medium, High
    val movement: String?,          // None, Light walk, Yoga, Moderate, Intense
    val studyWorkPressure: Int,     // 0-10
    val relationshipStress: Int,    // 0-10
    val socialMediaOverload: Int,   // 0-10
    val flowLevel: String?,
    val symptoms: List<String>,
    val notesEncrypted: String?,    // AES-256 encrypted
    val crisisFlag: Boolean = false,
    val flags: List<String> = emptyList()
)

// Legacy alias for backward compat with existing ViewModel calls
@Entity(tableName = "mood_logs")
data class MoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val mood: String,
    val energy: Int,
    val stress: Int,
    val sleepQuality: Int?,
    val notes: String?
)

// ==========================================
// MEDICAL JOURNAL
// ==========================================

@Entity(tableName = "medical_journal_entries")
data class MedicalJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryDate: String,          // YYYY-MM-DD
    val category: String,           // e.g. "Symptom", "Doctor Visit", "Medication", "General"
    val title: String,
    val symptoms: List<String>,
    val painLevel: Int,             // 0-10
    val mood: String?,
    val flowLevel: String?,
    val medicinesTaken: String?,
    val doctorVisit: Boolean = false,
    val nextAppointment: String?,   // YYYY-MM-DD
    val notesEncrypted: String?,    // AES-256 encrypted
    val attachmentPath: String?     // local file path only
)

// Legacy journal entry for backward compat
@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val title: String,
    val body: String,
    val moodTag: String?,
    val cyclePhase: String?
)

// ==========================================
// MEDICINE REMINDERS
// ==========================================

@Entity(tableName = "medical_reminders")
data class MedicalReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reminderType: String,       // e.g. "Medication", "Supplement", "Doctor Appointment"
    val reminderTime: String,       // HH:mm
    val repeatRule: String,         // "Daily", "Weekly", "Once", "Custom"
    val startDate: String,          // YYYY-MM-DD
    val endDate: String?,           // YYYY-MM-DD
    val enabled: Boolean = true,
    val notesEncrypted: String?,    // AES-256 encrypted
    val reasonNote: String?
)

// ==========================================
// MENSTRUAL CUP CARE LOG
// ==========================================

@Entity(tableName = "cup_care_logs")
data class CupCareLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val insertedAt: String?,        // ISO datetime
    val emptiedAt: String?,         // ISO datetime
    val cleanedToday: Boolean,
    val discomfortLevel: Int,       // 0-10; if >=7 show safety warning
    val leakageIssue: Boolean,
    val notesEncrypted: String?,    // AES-256 encrypted
    val logDate: String             // YYYY-MM-DD
)

// ==========================================
// IN-APP NOTIFICATIONS
// ==========================================

@Entity(tableName = "in_app_notifications")
data class InAppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String,               // period, mood, medicine, cup, hydration, ai, system
    val isRead: Boolean = false,
    val createdAt: String          // ISO datetime
)

// ==========================================
// AUDIT LOG (append-only, local)
// ==========================================

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actorRole: String,          // USER, ADMIN, etc.
    val action: String,             // read, insert, update, delete, export, login, logout
    val resourceType: String,       // profile, period_log, behaviour_log, etc.
    val resourceId: String?,
    val ipHash: String?,            // SHA-256 hashed; never raw IP
    val userAgentHash: String?,     // SHA-256 hashed
    val metadata: String,           // JSON string, no decrypted content
    val createdAt: String          // ISO datetime
)

// ==========================================
// BOOKMARK (existing, unchanged)
// ==========================================

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val articleSlug: String
)
