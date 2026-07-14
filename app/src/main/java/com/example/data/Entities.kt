package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "",
    val birthYear: Int? = null,
    val averageCycleLength: Int = 28,
    val averagePeriodLength: Int = 5,
    val goals: List<String> = emptyList(),
    val acceptedDisclaimer: Boolean = false,
    val isDarkMode: Boolean = false,
    val securityPinEnabled: Boolean = false,
    val securityPin: String = "",
    val periodReminders: Boolean = true,
    val moodReminders: Boolean = true,
    val cupReminders: Boolean = false,
    val selfCareReminders: Boolean = true,
    val reminderTime: String = "20:00",
    val userMode: String = "SELF_TRACKING", // SELF_TRACKING, SUPPORT_MODE, EDUCATION_ONLY
    val genderMode: String = "FEMALE", // FEMALE, MALE, OTHER, PREFER_NOT_TO_SAY
    val bodyRelevantMode: String = "MENSTRUATES", // MENSTRUATES, DOES_NOT_MENSTRUATE, NOT_SURE, PREFER_NOT_TO_SAY
    val supportRelationship: String? = null, // WIFE, MOTHER, etc.
    val consentConfirmed: Boolean = false,
    val sharedTrackingConsent: Boolean = false,
    val behaviourFocuses: List<String> = emptyList(),
    val medicineReminders: Boolean = false,
    val waterReminders: Boolean = true
)

@Entity(tableName = "behaviour_logs")
data class BehaviourLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val mood: String, 
    val stressLevel: Int, // 1-10
    val anxietyLevel: Int, // 1-10
    val sleepHours: Double,
    val sleepQuality: Int, // 1-10
    val painLevel: Int, // 0-10
    val energyLevel: Int, // 1-10
    val hydrationLevel: String, 
    val foodCraving: String,
    val caffeineIntake: String,
    val movement: String,
    val studyWorkPressure: Int, // 1-10
    val relationshipStress: Int, // 1-10
    val socialMediaOverload: Int, // 1-10
    val flowLevel: String, // None, Light, Medium, Heavy
    val symptoms: List<String>,
    val notes: String?,
    val flags: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cup_care_logs")
data class CupCareLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val insertedAt: String, // e.g. "08:00"
    val emptiedAt: String, // e.g. "16:00"
    val cleanedToday: Boolean,
    val discomfortLevel: Int, // 0-10
    val leakageIssue: Boolean,
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_notes")
data class SupportNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val relationship: String,
    val noteTitle: String,
    val noteBody: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "period_logs")
data class PeriodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String, // YYYY-MM-DD
    val endDate: String?,  // YYYY-MM-DD, nullable
    val flowLevel: String, // Spotting, Light, Medium, Heavy, Very heavy
    val symptoms: List<String>,
    val notes: String?,
    val painLevel: Int? = null,
    val productUsed: String? = null,
    val changedProductFrequency: String? = null
)

@Entity(tableName = "mood_logs")
data class MoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val mood: String, // Great, Good, Okay, Low, Very low, Anxious, Angry, Overwhelmed
    val energy: Int,  // 1-10
    val stress: Int,  // 1-10
    val sleepQuality: Int?, // 1-10
    val notes: String?
)

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val title: String,
    val body: String,
    val moodTag: String?,
    val cyclePhase: String?,
    val category: String = "General",
    val symptoms: String? = null
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val articleSlug: String
)

@Entity(tableName = "medical_journal_entries")
data class MedicalJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryDate: String, // YYYY-MM-DD
    val category: String, // Period, PMS, PCOS/PCOD, Menstrual cup, Medicine, Doctor visit, Mental health, Pain, Other
    val title: String,
    val notes: String = "",
    val symptoms: List<String> = emptyList(),
    val painLevel: Int = 0, // 0 to 10
    val mood: String = "Okay",
    val flowLevel: String = "None", // None, Spotting, Light, Medium, Heavy
    val medicinesTaken: String = "",
    val doctorVisit: Boolean = false,
    val appointmentDate: String? = null,
    val doctorAdvice: String? = null
)

@Entity(tableName = "medical_reminders")
data class MedicalReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reminderType: String, // MEDICINE, DOCTOR_APPOINTMENT, WATER, CUP_CLEANING, PAD_CHANGE, SELF_CARE
    val reminderTime: String, // HH:MM
    val repeatRule: String = "Daily", // Daily, Weekly, Once
    val enabled: Boolean = true,
    val notes: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Entity(tableName = "symptom_logs")
data class SymptomLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val symptomName: String, // Cramps, Headache, Fatigue, Bloating, Backache, Mood swings, Acne, Breast tenderness, Nausea, Insomnia, etc.
    val severity: Int, // 1-5 or 1-10 (let's say 1-5 for standard severity)
    val notes: String? = null
)

@Entity(tableName = "user_credentials")
data class UserCredentials(
    @PrimaryKey val emailHash: String, // SHA-256 lookup
    val encryptedEmail: String,        // AES-GCM encrypted
    val passwordHash: String,          // PBKDF2 Hmac SHA-256 hash
    val passwordSalt: String,          // Unique salt per user
    val displayName: String,
    val role: String = "user", // user, premium_user, moderator, medical_content_reviewer, support_agent, admin, super_admin
    val securityPinEnabled: Boolean = false,
    val securityPin: String = "",
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "login_security_events")
data class LoginSecurityEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailHash: String?,
    val ipHash: String?,
    val userAgentHash: String?,
    val eventType: String, // LOGIN_FAILED, LOGIN_SUCCESS, RATE_LIMIT_TRIGGERED, CAPTCHA_REQUIRED, PASSWORD_RESET_REQUESTED, PASSWORD_RESET_SUCCESS, ACCOUNT_LOCKED_TEMPORARILY
    val metadata: String?, // serialized JSON or normal string
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "account_security_state")
data class AccountSecurityState(
    @PrimaryKey val emailHash: String, // Key is emailHash
    val userId: String? = null,
    val failedAttemptCount: Int = 0,
    val firstFailedAt: Long? = null,
    val lastFailedAt: Long? = null,
    val lockedUntil: Long? = null,
    val captchaRequired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


