package com.example.data

object ZodValidator {
    data class ZodError(val field: String, val message: String)
    data class ZodResult<T>(val success: Boolean, val data: T? = null, val errors: List<ZodError> = emptyList()) {
        fun getFieldError(field: String): String? = errors.find { it.field == field }?.message
    }

    data class ReminderSchema(
        val title: String,
        val reminderType: String,
        val reminderTime: String,
        val repeatRule: String,
        val notes: String?
    )

    fun validateReminder(
        title: String,
        reminderType: String,
        reminderTime: String,
        repeatRule: String,
        notes: String?
    ): ZodResult<ReminderSchema> {
        val errors = mutableListOf<ZodError>()

        // 1. Title validation: minimum 3 chars, max 50 chars
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            errors.add(ZodError("title", "Title schema: Required, cannot be blank"))
        } else if (trimmedTitle.length < 3) {
            errors.add(ZodError("title", "Title schema: Must be at least 3 characters"))
        } else if (trimmedTitle.length > 50) {
            errors.add(ZodError("title", "Title schema: Cannot exceed 50 characters"))
        }

        // 2. Type validation
        val validTypes = listOf("MEDICINE", "DOCTOR_APPOINTMENT", "WATER", "CUP_CLEANING", "PAD_CHANGE", "SELF_CARE")
        if (reminderType.trim().isEmpty()) {
            errors.add(ZodError("reminderType", "Type schema: Required"))
        } else if (!validTypes.contains(reminderType)) {
            errors.add(ZodError("reminderType", "Type schema: Invalid care type category selected"))
        }

        // 3. Time validation (HH:MM format, valid numbers)
        val timeTrim = reminderTime.trim()
        val timeRegex = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")
        if (timeTrim.isEmpty()) {
            errors.add(ZodError("reminderTime", "Time schema: Required"))
        } else if (!timeTrim.matches(timeRegex)) {
            errors.add(ZodError("reminderTime", "Time schema: Valid 24h format mandatory (HH:MM, e.g., 08:30 or 21:00)"))
        }

        // 4. Repeat rule validation
        val trimmedRule = repeatRule.trim()
        if (trimmedRule.isEmpty()) {
            errors.add(ZodError("repeatRule", "Repeat interval schema: Required"))
        } else if (trimmedRule.length < 3) {
            errors.add(ZodError("repeatRule", "Repeat interval schema: Must state valid recurring rule (e.g. Daily, Weekly)"))
        }

        if (errors.isNotEmpty()) {
            return ZodResult(success = false, errors = errors)
        }

        return ZodResult(
            success = true,
            data = ReminderSchema(
                title = trimmedTitle,
                reminderType = reminderType,
                reminderTime = timeTrim,
                repeatRule = trimmedRule,
                notes = notes?.trim()
            )
        )
    }

    data class LoginSchema(val email: String, val password: String)
    data class SignupSchema(val email: String, val password: String, val confirm: String, val displayName: String)

    fun validateLogin(email: String, password: String): ZodResult<LoginSchema> {
        val errors = mutableListOf<ZodError>()
        val emailTrim = email.trim()
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        if (emailTrim.isEmpty()) {
            errors.add(ZodError("email", "Email is required"))
        } else if (!emailTrim.matches(emailRegex)) {
            errors.add(ZodError("email", "Please check your email format"))
        }
        if (password.isEmpty()) {
            errors.add(ZodError("password", "Password is required"))
        }
        return ZodResult(
            success = errors.isEmpty(),
            data = if (errors.isEmpty()) LoginSchema(emailTrim, password) else null,
            errors = errors
        )
    }

    fun validateSignup(email: String, password: String, confirm: String, displayName: String): ZodResult<SignupSchema> {
        val errors = mutableListOf<ZodError>()
        val emailTrim = email.trim()
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
        if (emailTrim.isEmpty()) {
            errors.add(ZodError("email", "Email is required"))
        } else if (!emailTrim.matches(emailRegex)) {
            errors.add(ZodError("email", "Invalid email format"))
        }
        
        val displayNameTrim = displayName.trim()
        if (displayNameTrim.isEmpty()) {
            errors.add(ZodError("displayName", "Display name is required"))
        }

        if (password.length < 12) {
            errors.add(ZodError("password", "Password must be at least 12 characters"))
        } else if (password == emailTrim) {
            errors.add(ZodError("password", "Password must not match your email address"))
        } else {
            val commonWeaks = listOf("password123", "password1234", "1234567890", "qwertyuiop", "lunacare2026", "lunacare123", "administrator", "guestpassword")
            if (commonWeaks.contains(password.lowercase())) {
                errors.add(ZodError("password", "This password is too weak or commonly used. Please try a longer passphrase."))
            }
        }

        if (password != confirm) {
            errors.add(ZodError("confirm", "Passwords do not match"))
        }

        return ZodResult(
            success = errors.isEmpty(),
            data = if (errors.isEmpty()) SignupSchema(emailTrim, password, confirm, displayNameTrim) else null,
            errors = errors
        )
    }
    data class PeriodLogSchema(
        val startDate: String,
        val endDate: String?,
        val flowLevel: String,
        val symptoms: List<String>,
        val notes: String?,
        val painLevel: Int?,
        val productUsed: String?,
        val changedProductFrequency: String?
    )
    
    fun validatePeriodLog(
        startDate: String,
        endDate: String?,
        flowLevel: String,
        symptoms: List<String>,
        notes: String?,
        painLevel: Int?,
        productUsed: String?,
        changedProductFrequency: String?
    ): ZodResult<PeriodLogSchema> {
        val errors = mutableListOf<ZodError>()
        
        // Date regex YYYY-MM-DD
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (!startDate.matches(dateRegex)) {
            errors.add(ZodError("startDate", "Invalid start date format (YYYY-MM-DD)"))
        }
        if (endDate != null && !endDate.matches(dateRegex)) {
            errors.add(ZodError("endDate", "Invalid end date format (YYYY-MM-DD)"))
        }
        
        // Enum validation
        val validFlowLevels = listOf("Spotting", "Light", "Medium", "Heavy", "Very heavy")
        if (!validFlowLevels.contains(flowLevel)) {
            errors.add(ZodError("flowLevel", "Invalid flow level"))
        }
        
        val validSymptoms = listOf("Cramps", "Headache", "Back pain", "Breast tenderness", "Acne", "Fatigue", "Bloating", "Nausea", "Mood swings", "Anxiety", "Low mood", "Irritability", "Sleep issues", "Dizziness", "Fainting", "Severe pain", "Fever", "Bad odor", "Pregnancy concern")
        symptoms.forEach {
            if (!validSymptoms.contains(it)) {
                errors.add(ZodError("symptoms", "Invalid symptom: $it"))
            }
        }
        
        if (painLevel != null && (painLevel < 0 || painLevel > 10)) {
            errors.add(ZodError("painLevel", "Pain level must be between 0 and 10"))
        }
        
        val validProducts = listOf("Pad", "Period panty", "Menstrual cup", "Tampon", "Cloth", "Other", "Prefer not to say")
        if (productUsed != null && !validProducts.contains(productUsed)) {
            errors.add(ZodError("productUsed", "Invalid product used"))
        }
        
        val validFrequencies = listOf("Every 1 hour", "Every 2–3 hours", "Every 4–6 hours", "Less often", "Not sure")
        if (changedProductFrequency != null && !validFrequencies.contains(changedProductFrequency)) {
            errors.add(ZodError("changedProductFrequency", "Invalid frequency"))
        }
        
        if (notes != null && notes.length > 1000) {
            errors.add(ZodError("notes", "Notes too long (max 1000 chars)"))
        }

        if (errors.isNotEmpty()) {
            return ZodResult(success = false, errors = errors)
        }
        
        return ZodResult(
            success = true,
            data = PeriodLogSchema(startDate, endDate, flowLevel, symptoms, notes, painLevel, productUsed, changedProductFrequency)
        )
    }
}
