package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.LunaViewModel

// ==========================================
// NAVIGATION TABS BY USER MODE
// ==========================================

enum class SelfTrackingTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Favorite),
    Cycle("Cycle", Icons.Default.CalendarMonth),
    Mood("Wellbeing", Icons.Default.Mood),
    Journal("Journal", Icons.Default.Create),
    Learn("Learn", Icons.Default.School),
    Care("Care", Icons.Default.LocalPharmacy)
}

enum class SupportTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Favorite),
    Support("Support", Icons.Default.VolunteerActivism),
    Learn("Learn", Icons.Default.School),
    Care("Care", Icons.Default.LocalPharmacy),
    Notes("Notes", Icons.Default.StickyNote2),
    Settings("Settings", Icons.Default.Settings)
}

enum class EducationTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Favorite),
    Learn("Learn", Icons.Default.School),
    Care("Care", Icons.Default.LocalPharmacy),
    Mind("Mind", Icons.Default.SelfImprovement),
    Settings("Settings", Icons.Default.Settings)
}

// ==========================================
// ROOT APP COMPOSABLE
// ==========================================

@Composable
fun LunaCareApp(viewModel: LunaViewModel) {
    val profileState by viewModel.profile.collectAsState()
    val isPinAuthenticated by viewModel.isPinAuthenticated.collectAsState()
    val isGuest by viewModel.isGuestUser.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            profileState == null && !isGuest -> {
                // First launch: show opening/onboarding
                OnboardingRootScreen(viewModel = viewModel)
            }
            (profileState?.securityPinEnabled == true) && !isPinAuthenticated && !isGuest -> {
                PinAuthScreen(
                    storedPin = profileState?.securityPin ?: "",
                    onAuthenticated = { viewModel.authenticatePin(it) }
                )
            }
            else -> {
                val profile = profileState ?: Profile(userMode = UserMode.EDUCATION_ONLY.name)
                MainAppLayout(profile = profile, viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// ONBOARDING ROOT — OPENING PAGE + 7 STEPS
// ==========================================

@Composable
fun OnboardingRootScreen(viewModel: LunaViewModel) {
    var showOpeningPage by rememberSaveable { mutableStateOf(true) }
    var onboardingStep by rememberSaveable { mutableStateOf(1) }

    // Collected data
    var userMode by rememberSaveable { mutableStateOf(UserMode.SELF_TRACKING.name) }
    var genderMode by rememberSaveable { mutableStateOf(GenderMode.PREFER_NOT_TO_SAY.name) }
    var pronoun by rememberSaveable { mutableStateOf(Pronoun.PREFER_NOT_TO_SAY.name) }
    var customPronoun by rememberSaveable { mutableStateOf("") }
    var bodyRelevantMode by rememberSaveable { mutableStateOf(BodyRelevantMode.PREFER_NOT_TO_SAY.name) }
    var supportRelationship by rememberSaveable { mutableStateOf<String?>(null) }
    var consentConfirmed by rememberSaveable { mutableStateOf(false) }
    var country by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var religion by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedConditions by rememberSaveable { mutableStateOf(setOf<String>()) }
    var behaviourFocuses by rememberSaveable { mutableStateOf(setOf<String>()) }
    var name by rememberSaveable { mutableStateOf("") }
    var birthYearStr by rememberSaveable { mutableStateOf("") }
    var cycleLength by rememberSaveable { mutableStateOf(28) }
    var periodLength by rememberSaveable { mutableStateOf(5) }
    var pinEnabled by rememberSaveable { mutableStateOf(false) }
    var pinCode by rememberSaveable { mutableStateOf("") }
    var disclaimerAccepted by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(
        targetState = showOpeningPage,
        transitionSpec = {
            slideInHorizontally(tween(400)) { it } togetherWith slideOutHorizontally(tween(400)) { -it }
        },
        label = "opening_to_onboarding"
    ) { isOpening ->
        if (isOpening) {
            OpeningPage(
                onGetStarted = { showOpeningPage = false },
                onGuest = { viewModel.setGuestMode(true) }
            )
        } else {
            OnboardingStepScreen(
                step = onboardingStep,
                totalSteps = 7,
                userMode = userMode, onUserModeChange = { userMode = it },
                genderMode = genderMode, onGenderModeChange = { genderMode = it },
                pronoun = pronoun, onPronounChange = { pronoun = it },
                customPronoun = customPronoun, onCustomPronounChange = { customPronoun = it },
                bodyRelevantMode = bodyRelevantMode, onBodyRelevantModeChange = { bodyRelevantMode = it },
                supportRelationship = supportRelationship, onSupportRelationshipChange = { supportRelationship = it },
                consentConfirmed = consentConfirmed, onConsentChange = { consentConfirmed = it },
                country = country, onCountryChange = { country = it },
                region = region, onRegionChange = { region = it },
                city = city, onCityChange = { city = it },
                religion = religion, onReligionChange = { religion = it },
                selectedConditions = selectedConditions, onConditionsChange = { selectedConditions = it },
                behaviourFocuses = behaviourFocuses, onFocusesChange = { behaviourFocuses = it },
                name = name, onNameChange = { name = it },
                birthYearStr = birthYearStr, onBirthYearChange = { birthYearStr = it },
                cycleLength = cycleLength, onCycleLengthChange = { cycleLength = it },
                periodLength = periodLength, onPeriodLengthChange = { periodLength = it },
                pinEnabled = pinEnabled, onPinEnabledChange = { pinEnabled = it },
                pinCode = pinCode, onPinCodeChange = { pinCode = it },
                disclaimerAccepted = disclaimerAccepted, onDisclaimerChange = { disclaimerAccepted = it },
                onBack = {
                    if (onboardingStep > 1) onboardingStep-- else showOpeningPage = true
                },
                onNext = {
                    if (onboardingStep < 7) {
                        onboardingStep++
                    }
                },
                onComplete = {
                    if (disclaimerAccepted) {
                        val derivedMode = deriveUserMode(userMode, genderMode, bodyRelevantMode)
                        viewModel.onboardUser(
                            name = name.ifBlank { "Companion" },
                            birthYear = birthYearStr.toIntOrNull(),
                            cycleLength = cycleLength,
                            periodLength = periodLength,
                            goals = emptyList(),
                            pin = if (pinEnabled) pinCode else "",
                            userMode = derivedMode.name,
                            genderMode = genderMode,
                            pronoun = pronoun,
                            customPronoun = customPronoun.ifBlank { null },
                            bodyRelevantMode = bodyRelevantMode,
                            supportRelationship = supportRelationship,
                            consentConfirmed = consentConfirmed,
                            religion = religion,
                            country = country.ifBlank { null },
                            region = region.ifBlank { null },
                            city = city.ifBlank { null },
                            selectedConditions = selectedConditions.toList(),
                            behaviourFocuses = behaviourFocuses.toList()
                        )
                    }
                }
            )
        }
    }
}

/** Derives the final user mode from onboarding selections */
fun deriveUserMode(userModeStr: String, genderModeStr: String, bodyRelevantModeStr: String): UserMode {
    return when (userModeStr) {
        UserMode.SUPPORT_MODE.name -> UserMode.SUPPORT_MODE
        UserMode.EDUCATION_ONLY.name -> UserMode.EDUCATION_ONLY
        else -> {
            when (bodyRelevantModeStr) {
                BodyRelevantMode.MENSTRUATES.name -> UserMode.SELF_TRACKING
                BodyRelevantMode.DOES_NOT_MENSTRUATE.name -> UserMode.EDUCATION_ONLY
                else -> UserMode.EDUCATION_ONLY
            }
        }
    }
}

// ==========================================
// OPENING PAGE
// ==========================================

@Composable
fun OpeningPage(onGetStarted: () -> Unit, onGuest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SoftCreamBackground,
                        Color(0xFFF7EEF5)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo + branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MutedRosePrimary.copy(alpha = 0.2f),
                                    LavenderSecondary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MutedRosePrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "LunaCare",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Period care, mental wellness, and gentle support.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Feature highlights
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OpeningFeatureRow(Icons.Default.CalendarMonth, "Track your cycle with privacy")
                OpeningFeatureRow(Icons.Default.Mood, "Daily mental wellness check-ins")
                OpeningFeatureRow(Icons.Default.LocalPharmacy, "Care product discovery")
                OpeningFeatureRow(Icons.Default.School, "Period & menstrual cup education")
                OpeningFeatureRow(Icons.Default.Lock, "All data encrypted on your device")
            }

            // Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = onGuest,
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("guest_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue as Guest", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
                Text(
                    text = "Educational support only. Not medical advice. See full disclaimer inside.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun OpeningFeatureRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

// ==========================================
// 7-STEP ONBOARDING SCREEN
// ==========================================

@Composable
fun OnboardingStepScreen(
    step: Int, totalSteps: Int,
    userMode: String, onUserModeChange: (String) -> Unit,
    genderMode: String, onGenderModeChange: (String) -> Unit,
    pronoun: String, onPronounChange: (String) -> Unit,
    customPronoun: String, onCustomPronounChange: (String) -> Unit,
    bodyRelevantMode: String, onBodyRelevantModeChange: (String) -> Unit,
    supportRelationship: String?, onSupportRelationshipChange: (String?) -> Unit,
    consentConfirmed: Boolean, onConsentChange: (Boolean) -> Unit,
    country: String, onCountryChange: (String) -> Unit,
    region: String, onRegionChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    religion: String?, onReligionChange: (String?) -> Unit,
    selectedConditions: Set<String>, onConditionsChange: (Set<String>) -> Unit,
    behaviourFocuses: Set<String>, onFocusesChange: (Set<String>) -> Unit,
    name: String, onNameChange: (String) -> Unit,
    birthYearStr: String, onBirthYearChange: (String) -> Unit,
    cycleLength: Int, onCycleLengthChange: (Int) -> Unit,
    periodLength: Int, onPeriodLengthChange: (Int) -> Unit,
    pinEnabled: Boolean, onPinEnabledChange: (Boolean) -> Unit,
    pinCode: String, onPinCodeChange: (String) -> Unit,
    disclaimerAccepted: Boolean, onDisclaimerChange: (Boolean) -> Unit,
    onBack: () -> Unit, onNext: () -> Unit, onComplete: () -> Unit
) {
    val canProceed = when (step) {
        1 -> true
        2 -> if (userMode == UserMode.SUPPORT_MODE.name) consentConfirmed else true
        3 -> true
        4 -> true
        5 -> true
        6 -> true
        7 -> disclaimerAccepted && (if (pinEnabled) pinCode.length >= 4 else true)
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "LunaCare",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Progress bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step $step of $totalSteps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Step Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            when (step) {
                1 -> OnboardingStep1UserMode(userMode, onUserModeChange)
                2 -> OnboardingStep2Identity(
                    userMode = userMode,
                    genderMode = genderMode, onGenderModeChange = onGenderModeChange,
                    pronoun = pronoun, onPronounChange = onPronounChange,
                    customPronoun = customPronoun, onCustomPronounChange = onCustomPronounChange,
                    bodyRelevantMode = bodyRelevantMode, onBodyRelevantModeChange = onBodyRelevantModeChange,
                    supportRelationship = supportRelationship, onSupportRelationshipChange = onSupportRelationshipChange,
                    consentConfirmed = consentConfirmed, onConsentChange = onConsentChange
                )
                3 -> OnboardingStep3Region(country, onCountryChange, region, onRegionChange, city, onCityChange)
                4 -> OnboardingStep4Religion(religion, onReligionChange)
                5 -> OnboardingStep5HealthProfile(selectedConditions, onConditionsChange)
                6 -> OnboardingStep6BehaviourFocus(behaviourFocuses, onFocusesChange)
                7 -> OnboardingStep7PinAndDisclaimer(
                    name = name, onNameChange = onNameChange,
                    birthYearStr = birthYearStr, onBirthYearChange = onBirthYearChange,
                    cycleLength = cycleLength, onCycleLengthChange = onCycleLengthChange,
                    periodLength = periodLength, onPeriodLengthChange = onPeriodLengthChange,
                    pinEnabled = pinEnabled, onPinEnabledChange = onPinEnabledChange,
                    pinCode = pinCode, onPinCodeChange = onPinCodeChange,
                    disclaimerAccepted = disclaimerAccepted, onDisclaimerChange = onDisclaimerChange
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Back", color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = { if (step < 7) onNext() else onComplete() },
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp).padding(start = 16.dp)
            ) {
                Text(
                    if (step == 7) "Start LunaCare" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -- Onboarding Step 1: How do you want to use LunaCare? --
@Composable
fun OnboardingStep1UserMode(userMode: String, onUserModeChange: (String) -> Unit) {
    val options = listOf(
        Triple(UserMode.SELF_TRACKING.name, "For myself", "Track cycle, mood, and wellness."),
        Triple(UserMode.SUPPORT_MODE.name, "To support someone else", "Help a partner, family member, or friend."),
        Triple(UserMode.EDUCATION_ONLY.name, "Only to learn", "Read educational content without tracking.")
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "How do you want to use LunaCare?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            "You can change this anytime in settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (mode, title, desc) ->
            val selected = userMode == mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserModeChange(mode) }
                    .testTag("mode_$mode"),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selected,
                        onClick = { onUserModeChange(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// -- Onboarding Step 2: Identity & Consent --
@Composable
fun OnboardingStep2Identity(
    userMode: String,
    genderMode: String, onGenderModeChange: (String) -> Unit,
    pronoun: String, onPronounChange: (String) -> Unit,
    customPronoun: String, onCustomPronounChange: (String) -> Unit,
    bodyRelevantMode: String, onBodyRelevantModeChange: (String) -> Unit,
    supportRelationship: String?, onSupportRelationshipChange: (String?) -> Unit,
    consentConfirmed: Boolean, onConsentChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (userMode == UserMode.SUPPORT_MODE.name) {
            // Support mode — relationship + consent
            Text("Support someone with care", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your relationship to them:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val relationships = listOf("WIFE", "MOTHER", "DAUGHTER", "GIRLFRIEND", "FEMALE_PARTNER", "SISTER", "FRIEND", "OTHER")
                    val labels = listOf("Wife", "Mother", "Daughter", "Girlfriend", "Female Partner", "Sister", "Friend", "Other")
                    relationships.forEachIndexed { i, rel ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSupportRelationshipChange(rel) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = supportRelationship == rel, onClick = { onSupportRelationshipChange(rel) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(labels[i])
                        }
                    }
                }
            }

            // Consent warning
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Support means care, respect, and privacy. Do not track another person's period, mood, symptoms, location, or medical information without clear permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onConsentChange(!consentConfirmed) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consentConfirmed, onCheckedChange = onConsentChange,
                    modifier = Modifier.testTag("consent_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "I understand and will respect privacy and consent.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        } else {
            // Self / Education mode — gender + pronouns + body mode
            Text("Tell us a little about you", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("All optional. Used only to personalise your experience.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            OnboardingOptionGroup(
                label = "Gender (optional)",
                options = mapOf(
                    "FEMALE" to "Female", "MALE" to "Male",
                    "OTHER" to "Other", "PREFER_NOT_TO_SAY" to "Prefer not to say"
                ),
                selected = genderMode, onSelect = onGenderModeChange
            )
            OnboardingOptionGroup(
                label = "Pronouns (optional)",
                options = mapOf(
                    "SHE_HER" to "She/Her", "HE_HIM" to "He/Him",
                    "THEY_THEM" to "They/Them", "PREFER_NOT_TO_SAY" to "Prefer not to say"
                ),
                selected = pronoun, onSelect = onPronounChange
            )
            if (pronoun == "CUSTOM") {
                OutlinedTextField(
                    value = customPronoun, onValueChange = onCustomPronounChange,
                    label = { Text("Your pronouns") }, modifier = Modifier.fillMaxWidth()
                )
            }
            if (userMode == UserMode.SELF_TRACKING.name || userMode == UserMode.EDUCATION_ONLY.name) {
                OnboardingOptionGroup(
                    label = "Do you want cycle tracking features?",
                    options = mapOf(
                        "MENSTRUATES" to "Yes, I menstruate and want tracking",
                        "DOES_NOT_MENSTRUATE" to "No, I don't need tracking",
                        "NOT_SURE" to "I'm not sure",
                        "PREFER_NOT_TO_SAY" to "Prefer not to say"
                    ),
                    selected = bodyRelevantMode, onSelect = onBodyRelevantModeChange
                )
            }
        }
    }
}

@Composable
fun OnboardingOptionGroup(label: String, options: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (key, display) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(key) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == key,
                    onClick = { onSelect(key) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(display, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// -- Onboarding Step 3: Region --
@Composable
fun OnboardingStep3Region(country: String, onCountryChange: (String) -> Unit, region: String, onRegionChange: (String) -> Unit, city: String, onCityChange: (String) -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Where are you located?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Optional. Used only for localised wellness content and nearby search. Never shared or used for profiling.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        OutlinedTextField(value = country, onValueChange = onCountryChange, label = { Text("Country (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = region, onValueChange = onRegionChange, label = { Text("State / Province / Division (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = city, onValueChange = onCityChange, label = { Text("City (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
            Text(
                "Region is used only for: educational wording, nearby search, and cultural content. It is never used for ads, discrimination, or profiling.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -- Onboarding Step 4: Religion --
@Composable
fun OnboardingStep4Religion(religion: String?, onReligionChange: (String?) -> Unit) {
    val options = listOf("ISLAM" to "Islam", "HINDU" to "Hindu", "CHRISTIAN" to "Christian", "BUDDHIST" to "Buddhist", "OTHER" to "Other", "PREFER_NOT_TO_SAY" to "Prefer not to say")
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Faith & culture preference", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Optional. Used only to personalise respectful wellness content.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
            Text(
                "This is entirely optional. Your answer only affects content — it is never shared, used for profiling, or required.",
                modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        options.forEach { (key, display) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onReligionChange(if (religion == key) null else key)
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = religion == key,
                    onClick = { onReligionChange(if (religion == key) null else key) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(display, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// -- Onboarding Step 5: Health Profile --
@Composable
fun OnboardingStep5HealthProfile(selectedConditions: Set<String>, onConditionsChange: (Set<String>) -> Unit) {
    val conditions = listOf(
        "PCOS", "PCOD", "PMOS", "PMS", "Endometriosis awareness",
        "Irregular period", "Heavy bleeding", "Severe cramps", "Anxiety/stress",
        "Low mood", "Menstrual cup discomfort", "Pregnancy concern", "Other", "Prefer not to say"
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Health awareness", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
            Text(
                "Select what you want LunaCare to help you learn or track. These are self-reported awareness labels — the app is not diagnosing you.",
                modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        conditions.forEach { condition ->
            val selected = selectedConditions.contains(condition)
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onConditionsChange(
                        if (selected) selectedConditions - condition else selectedConditions + condition
                    )
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected, onCheckedChange = {
                    onConditionsChange(if (selected) selectedConditions - condition else selectedConditions + condition)
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text(condition, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// -- Onboarding Step 6: Behaviour Focus --
@Composable
fun OnboardingStep6BehaviourFocus(behaviourFocuses: Set<String>, onFocusesChange: (Set<String>) -> Unit) {
    val focuses = listOf(
        "Mood", "Stress", "Anxiety", "Sleep", "Period pain", "PMS",
        "PCOS/PCOD awareness", "PMOS awareness", "Menstrual cup", "Food cravings",
        "Hydration", "Product care", "Medicine reminder", "Doctor visit",
        "Relationship support", "Emergency signs"
    )
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("What should LunaCare focus on?", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Choose as many as you like. You can always change this later.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        focuses.forEach { focus ->
            val selected = behaviourFocuses.contains(focus)
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    onFocusesChange(if (selected) behaviourFocuses - focus else behaviourFocuses + focus)
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected, onCheckedChange = {
                    onFocusesChange(if (selected) behaviourFocuses - focus else behaviourFocuses + focus)
                })
                Spacer(modifier = Modifier.width(8.dp))
                Text(focus, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// -- Onboarding Step 7: Name + Cycle + PIN + Disclaimer --
@Composable
fun OnboardingStep7PinAndDisclaimer(
    name: String, onNameChange: (String) -> Unit,
    birthYearStr: String, onBirthYearChange: (String) -> Unit,
    cycleLength: Int, onCycleLengthChange: (Int) -> Unit,
    periodLength: Int, onPeriodLengthChange: (Int) -> Unit,
    pinEnabled: Boolean, onPinEnabledChange: (Boolean) -> Unit,
    pinCode: String, onPinCodeChange: (String) -> Unit,
    disclaimerAccepted: Boolean, onDisclaimerChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Almost there!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        OutlinedTextField(value = name, onValueChange = onNameChange,
            label = { Text("How shall we address you? (optional)") },
            placeholder = { Text("e.g. Luna") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)

        OutlinedTextField(value = birthYearStr, onValueChange = { if (it.length <= 4) onBirthYearChange(it) },
            label = { Text("Birth year (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Average cycle length: $cycleLength days", fontWeight = FontWeight.Bold)
                Slider(value = cycleLength.toFloat(), onValueChange = { onCycleLengthChange(it.toInt()) }, valueRange = 21f..45f, steps = 24)
                Text("Average period length: $periodLength days", fontWeight = FontWeight.Bold)
                Slider(value = periodLength.toFloat(), onValueChange = { onPeriodLengthChange(it.toInt()) }, valueRange = 3f..10f, steps = 7)
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Lock (optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pinEnabled, onCheckedChange = onPinEnabledChange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Enable PIN lock")
                }
                if (pinEnabled) {
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) onPinCodeChange(it) },
                        label = { Text("4–6 digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Disclaimer
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Important Safety Disclaimer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                Spacer(modifier = Modifier.height(8.dp))
                Text(LunaContent.GLOBAL_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onDisclaimerChange(!disclaimerAccepted) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = disclaimerAccepted, onCheckedChange = onDisclaimerChange,
                modifier = Modifier.testTag("disclaimer_checkbox"))
            Spacer(modifier = Modifier.width(8.dp))
            Text("I understand and accept the educational safety guidelines.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

// ==========================================
// PIN AUTH SCREEN
// ==========================================

@Composable
fun PinAuthScreen(storedPin: String, onAuthenticated: (String) -> Boolean) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 48.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("LunaCare", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Enter your passcode to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp))
            Spacer(modifier = Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(storedPin.length) { i ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                            .background(if (i < pinInput.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","Delete"))
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .clickable(enabled = digit.isNotEmpty()) {
                                    if (digit == "Delete") {
                                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                    } else if (pinInput.length < storedPin.length) {
                                        pinInput += digit
                                        if (pinInput.length == storedPin.length) {
                                            if (!onAuthenticated(pinInput)) {
                                                errorMessage = "Incorrect passcode. Please try again."
                                                pinInput = ""
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (digit == "Delete") {
                                Icon(Icons.Default.Backspace, null, tint = MaterialTheme.colorScheme.onBackground)
                            } else if (digit.isNotEmpty()) {
                                Text(digit, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MAIN APP LAYOUT
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(profile: Profile, viewModel: LunaViewModel) {
    val userMode = try { UserMode.valueOf(profile.userMode) } catch (e: Exception) { UserMode.EDUCATION_ONLY }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    var activeSelfTab by rememberSaveable { mutableStateOf(SelfTrackingTab.Home) }
    var activeSupportTab by rememberSaveable { mutableStateOf(SupportTab.Home) }
    var activeEduTab by rememberSaveable { mutableStateOf(EducationTab.Home) }

    // Deep screens within tabs
    var activeDeepScreen by rememberSaveable { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidePanel(
                profile = profile,
                viewModel = viewModel,
                onNavigateTo = { screen -> activeDeepScreen = screen },
                onClose = { /* handled by drawerState */ }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    profile.displayName.firstOrNull()?.uppercase() ?: "L",
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("LunaCare", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                                if (profile.city != null || profile.country != null) {
                                    Text(
                                        listOfNotNull(profile.city, profile.country).joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* drawerState.open() called by LaunchedEffect pattern */ }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            IconButton(onClick = { activeDeepScreen = "notifications" }) {
                                Icon(Icons.Default.Notifications, "Notifications")
                            }
                        }
                        IconButton(onClick = { activeDeepScreen = "settings" }, modifier = Modifier.testTag("settings_button")) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                when (userMode) {
                    UserMode.SELF_TRACKING -> NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        SelfTrackingTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = activeSelfTab == tab && activeDeepScreen == null,
                                onClick = { activeSelfTab = tab; activeDeepScreen = null },
                                icon = { Icon(tab.icon, tab.title) },
                                label = { Text(tab.title, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                    UserMode.SUPPORT_MODE -> NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        SupportTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = activeSupportTab == tab && activeDeepScreen == null,
                                onClick = { activeSupportTab = tab; activeDeepScreen = null },
                                icon = { Icon(tab.icon, tab.title) },
                                label = { Text(tab.title, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                    UserMode.EDUCATION_ONLY -> NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        EducationTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = activeEduTab == tab && activeDeepScreen == null,
                                onClick = { activeEduTab = tab; activeDeepScreen = null },
                                icon = { Icon(tab.icon, tab.title) },
                                label = { Text(tab.title, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // Deep screen routing
                when (activeDeepScreen) {
                    "notifications" -> NotificationPanelScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "settings" -> SettingsTab(profile = profile, viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "medical_journal" -> MedicalJournalScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "medicine_reminders" -> MedicineRemindersScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "cup_tracker" -> CupCareTrackerScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "health_awareness" -> HealthAwarenessScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "ai_assistant" -> AiAssistantScreen(profile = profile, viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "permission_center" -> PermissionCenterScreen(profile = profile, viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "profile_edit" -> ProfileEditScreen(profile = profile, viewModel = viewModel, onBack = { activeDeepScreen = null })
                    "bookmarks" -> BookmarksScreen(viewModel = viewModel, onBack = { activeDeepScreen = null })
                    else -> {
                        // Tab routing
                        when (userMode) {
                            UserMode.SELF_TRACKING -> when (activeSelfTab) {
                                SelfTrackingTab.Home -> DashboardTab(profile = profile, viewModel = viewModel, onDeepNavigate = { activeDeepScreen = it })
                                SelfTrackingTab.Cycle -> CycleTab(profile = profile, viewModel = viewModel)
                                SelfTrackingTab.Mood -> BehaviourCheckInTab(profile = profile, viewModel = viewModel)
                                SelfTrackingTab.Journal -> JournalTab(profile = profile, viewModel = viewModel)
                                SelfTrackingTab.Learn -> LearningTab(viewModel = viewModel)
                                SelfTrackingTab.Care -> CareTab(profile = profile, viewModel = viewModel)
                            }
                            UserMode.SUPPORT_MODE -> when (activeSupportTab) {
                                SupportTab.Home -> DashboardTab(profile = profile, viewModel = viewModel, onDeepNavigate = { activeDeepScreen = it })
                                SupportTab.Support -> SupportModeTab(profile = profile)
                                SupportTab.Learn -> LearningTab(viewModel = viewModel)
                                SupportTab.Care -> CareTab(profile = profile, viewModel = viewModel)
                                SupportTab.Notes -> JournalTab(profile = profile, viewModel = viewModel)
                                SupportTab.Settings -> SettingsTab(profile = profile, viewModel = viewModel, onBack = {})
                            }
                            UserMode.EDUCATION_ONLY -> when (activeEduTab) {
                                EducationTab.Home -> DashboardTab(profile = profile, viewModel = viewModel, onDeepNavigate = { activeDeepScreen = it })
                                EducationTab.Learn -> LearningTab(viewModel = viewModel)
                                EducationTab.Care -> CareTab(profile = profile, viewModel = viewModel)
                                EducationTab.Mind -> BehaviourCheckInTab(profile = profile, viewModel = viewModel)
                                EducationTab.Settings -> SettingsTab(profile = profile, viewModel = viewModel, onBack = {})
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SIDE PANEL (DRAWER)
// ==========================================

@Composable
fun SidePanel(profile: Profile, viewModel: LunaViewModel, onNavigateTo: (String) -> Unit, onClose: () -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Profile header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            profile.displayName.firstOrNull()?.uppercase() ?: "L",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(profile.displayName.ifBlank { "LunaCare User" }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    val modeLabel = when (profile.userMode) {
                        UserMode.SELF_TRACKING.name -> "Self Tracking"
                        UserMode.SUPPORT_MODE.name -> "Support Mode"
                        else -> "Education Only"
                    }
                    Text(modeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val menuItems = listOf(
                Triple(Icons.Default.Person, "Edit Profile", "profile_edit"),
                Triple(Icons.Default.Security, "Permission Center", "permission_center"),
                Triple(Icons.Default.PrivacyTip, "Privacy Settings", "settings"),
                Triple(Icons.Default.Notifications, "Notification Panel", "notifications"),
                Triple(Icons.Default.Bookmark, "Bookmarks", "bookmarks"),
                Triple(Icons.Default.MedicalServices, "Medical Journal", "medical_journal"),
                Triple(Icons.Default.Alarm, "Medicine Reminders", "medicine_reminders"),
                Triple(Icons.Default.WaterDrop, "Cup Care Tracker", "cup_tracker"),
                Triple(Icons.Default.LocalHospital, "Health Awareness", "health_awareness"),
                Triple(Icons.Default.LocalPharmacy, "Care & Products", "care"),
                Triple(Icons.Default.Psychology, "AI Assistant", "ai_assistant"),
                Triple(Icons.Default.Stars, "Premium & Subscription", "ai_assistant"),
                Triple(Icons.Default.Palette, "Theme / Dark Mode", "settings")
            )
            menuItems.forEach { (icon, label, route) ->
                NavigationDrawerItem(
                    icon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
                    label = { Text(label) },
                    selected = false,
                    onClick = { onNavigateTo(route) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = { viewModel.logout() }
            )

            // Version
            Spacer(modifier = Modifier.height(16.dp))
            Text("LunaCare v2.0.0", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// ==========================================
// DASHBOARD TAB
// ==========================================

@Composable
fun DashboardTab(profile: Profile, viewModel: LunaViewModel, onDeepNavigate: (String) -> Unit) {
    val periodLogs by viewModel.periodLogs.collectAsState()
    val behaviourLogs by viewModel.behaviourLogs.collectAsState()
    val lastLog = periodLogs.maxByOrNull { it.startDate }
    val todayStr = CycleUtils.getTodayString()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Hello, ${profile.displayName.ifBlank { "there" }} 🌸",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Your data is safe and private on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }

        // Cycle Summary Card (only for tracking mode)
        if (profile.userMode == UserMode.SELF_TRACKING.name) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (lastLog == null) {
                            Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Add your first period log to get cycle predictions",
                                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        } else {
                            val cycleDay = CycleUtils.getCurrentCycleDay(lastLog.startDate, todayStr)
                            val predicted = CycleUtils.getPredictedNextPeriod(lastLog.startDate, profile.averageCycleLength)
                            val daysUntil = CycleUtils.getDaysBetween(todayStr, predicted)
                            val phase = CycleUtils.getCyclePhase(cycleDay, profile.averageCycleLength, profile.averagePeriodLength)

                            Text(phase, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.size(140.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface).padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("$cycleDay", fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Text("of cycle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (daysUntil > 0) "$daysUntil days until next predicted period"
                                else "Period predicted today",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(CycleUtils.getPhaseDescription(phase), style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardQuickCard("Log Flow", Icons.Default.WaterDrop, MutedRosePrimary, Modifier.weight(1f)) {}
                DashboardQuickCard("Check-In", Icons.Default.Mood, LavenderSecondary, Modifier.weight(1f)) {}
                DashboardQuickCard("Journal", Icons.Default.Create, WarmCoralTertiary, Modifier.weight(1f)) {}
            }
        }

        // Today's self-care tip
        item {
            val tip = remember { LunaContent.selfCareTips.random() }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SelfImprovement, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Today's Self-Care Reminder", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tip.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(tip.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• ${tip.steps.first()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Religious wellness content (if set)
        if (!profile.religion.isNullOrEmpty() && profile.religion != "PREFER_NOT_TO_SAY") {
            item {
                val content = LunaContent.getWellnessContentForReligion(profile.religion)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Wellness & Faith", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(content.firstOrNull() ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Support mode reminder
        if (profile.userMode == UserMode.SUPPORT_MODE.name) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.VolunteerActivism, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Supporting with Care", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.tertiary)
                            Text("Remember: respect, privacy, and consent come first. Never track another person's health data without their clear permission.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // Shortcuts to deep screens
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardShortcutCard("Medical Journal", Icons.Default.MedicalServices, Modifier.weight(1f)) { onDeepNavigate("medical_journal") }
                DashboardShortcutCard("Reminders", Icons.Default.Alarm, Modifier.weight(1f)) { onDeepNavigate("medicine_reminders") }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardShortcutCard("Cup Tracker", Icons.Default.WaterDrop, Modifier.weight(1f)) { onDeepNavigate("cup_tracker") }
                DashboardShortcutCard("AI Assistant", Icons.Default.Psychology, Modifier.weight(1f)) { onDeepNavigate("ai_assistant") }
            }
        }

        // Crisis / Emergency shortcut
        item {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().clickable { onDeepNavigate("health_awareness") }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmergencyShare, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Urgent Care & Emergency Signs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                        Text("Recognise warning signs and know when to seek help", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Global disclaimer footer
        item {
            Text(
                LunaContent.GLOBAL_DISCLAIMER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DashboardQuickCard(label: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DashboardShortcutCard(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ==========================================
// CYCLE TAB
// ==========================================

@Composable
fun CycleTab(profile: Profile, viewModel: LunaViewModel) {
    val periodLogs by viewModel.periodLogs.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(CycleUtils.getTodayString()) }
    var endDate by remember { mutableStateOf("") }
    var flowLevel by remember { mutableStateOf("Medium") }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    val symptomsList = listOf("Cramps", "Headache", "Back pain", "Breast tenderness", "Acne", "Fatigue", "Bloating", "Nausea", "Mood swings", "Anxiety", "Low mood", "Irritability", "Sleep issues", "Spotting", "Clots")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Period Tracking", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Track logs for personalised cycle insights", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Button(onClick = { startDate = CycleUtils.getTodayString(); endDate = ""; flowLevel = "Medium"; selectedSymptoms = emptySet(); notes = ""; showLogDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_period_log_button")
                ) {
                    Icon(Icons.Default.Add, null); Spacer(modifier = Modifier.width(4.dp)); Text("Add Log")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚠️ Safety Reminder", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Heavy bleeding (soaking through pads/cups hourly) or severe cramps require medical assessment. Predictions are estimates — consult a doctor for medical advice.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (periodLogs.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No logs yet. Tap 'Add Log' to track your first period.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(periodLogs) { log ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Started: ${CycleUtils.formatDisplayDate(log.startDate)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                if (!log.endDate.isNullOrEmpty()) Text("Ended: ${CycleUtils.formatDisplayDate(log.endDate)}", style = MaterialTheme.typography.bodySmall)
                                else Text("Active / Ongoing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deletePeriodLog(log.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Flow: ${log.flowLevel}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        if (log.symptoms.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                log.symptoms.forEach { s ->
                                    Box(modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(s, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Log Period") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date (optional)") }, placeholder = { Text("Leave blank if ongoing") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Flow Level", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Spotting", "Light", "Medium", "Heavy").forEach { level ->
                            val sel = flowLevel == level
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { flowLevel = level }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(level, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Symptoms", fontWeight = FontWeight.Bold)
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(symptomsList) { s ->
                            val checked = selectedSymptoms.contains(s)
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedSymptoms = if (checked) selectedSymptoms - s else selectedSymptoms + s }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { selectedSymptoms = if (checked) selectedSymptoms - s else selectedSymptoms + s })
                                Text(s, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (private)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (CycleUtils.isValidDate(startDate)) {
                        viewModel.addPeriodLog(startDate, endDate.ifBlank { null }, flowLevel, selectedSymptoms.toList(), notes.ifBlank { null })
                        showLogDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showLogDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// BEHAVIOUR CHECK-IN TAB (full 17-field)
// ==========================================

@Composable
fun BehaviourCheckInTab(profile: Profile, viewModel: LunaViewModel) {
    val behaviourLogs by viewModel.behaviourLogs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showCrisisOverlay by remember { mutableStateOf(false) }

    // Dialog state
    var mood by remember { mutableStateOf("Okay") }
    var stressLevel by remember { mutableStateOf(5f) }
    var anxietyLevel by remember { mutableStateOf(5f) }
    var sleepHours by remember { mutableStateOf(7f) }
    var sleepQuality by remember { mutableStateOf(6f) }
    var painLevel by remember { mutableStateOf(2f) }
    var energyLevel by remember { mutableStateOf(6f) }
    var hydration by remember { mutableStateOf("Okay") }
    var notes by remember { mutableStateOf("") }

    val moodOptions = listOf("Great" to "😄", "Good" to "🙂", "Okay" to "😐", "Low" to "😔", "Anxious" to "😟", "Overwhelmed" to "😰", "Very low" to "😢")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Wellbeing Check-In", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Private daily reflection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Button(onClick = { mood = "Okay"; stressLevel = 5f; anxietyLevel = 5f; sleepHours = 7f; sleepQuality = 6f; painLevel = 2f; energyLevel = 6f; hydration = "Okay"; notes = ""; showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_mood_log_button")
                ) {
                    Icon(Icons.Default.Add, null); Spacer(modifier = Modifier.width(4.dp)); Text("Check In")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✨ Wellbeing Insights", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (behaviourLogs.size >= 3) "Patterns from your check-ins appear here. Remember: these are reflections, not diagnoses."
                        else "Log a few check-ins to see gentle patterns. Not diagnostic — just for self-awareness.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (behaviourLogs.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mood, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No check-ins yet. Tap 'Check In' to start your daily reflection.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(behaviourLogs) { log ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = if (log.crisisFlag) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(moodOptions.find { it.first == log.mood }?.second ?: "😐", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(log.mood, fontWeight = FontWeight.Bold)
                                    Text(CycleUtils.formatDisplayDate(log.logDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            IconButton(onClick = { viewModel.deleteBehaviourLog(log.id) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniChip("😴 Sleep ${log.sleepHours}h")
                            MiniChip("😓 Stress ${log.stressLevel}/10")
                            MiniChip("⚡ Energy ${log.energyLevel}/10")
                        }
                        if (log.crisisFlag) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crisis support was shown for this entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Daily Wellbeing Check-In") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("How are you feeling?", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        moodOptions.forEach { (m, emoji) ->
                            val sel = mood == m
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { mood = m }.padding(10.dp)
                            ) {
                                Text(emoji, fontSize = 28.sp)
                                Text(m, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    CheckInSlider("Stress", stressLevel) { stressLevel = it }
                    CheckInSlider("Anxiety", anxietyLevel) { anxietyLevel = it }
                    CheckInSlider("Energy", energyLevel) { energyLevel = it }
                    CheckInSlider("Pain", painLevel) { painLevel = it }
                    Column {
                        Text("Sleep hours: ${sleepHours.toInt()}h", fontWeight = FontWeight.Medium)
                        Slider(value = sleepHours, onValueChange = { sleepHours = it }, valueRange = 0f..12f, steps = 11)
                    }
                    Text("Hydration", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Okay", "Good", "Great").forEach { h ->
                            val sel = hydration == h
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { hydration = h }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(h, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Reflections (optional, private)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val isCrisis = CrisisDetector.detect(notes, mood)
                    viewModel.addBehaviourLog(
                        mood = mood, stressLevel = stressLevel.toInt(), anxietyLevel = anxietyLevel.toInt(),
                        sleepHours = sleepHours, sleepQuality = 5, painLevel = painLevel.toInt(),
                        energyLevel = energyLevel.toInt(), hydrationLevel = hydration,
                        foodCraving = null, caffeineIntake = null, movement = null,
                        studyWorkPressure = 5, relationshipStress = 5, socialMediaOverload = 5,
                        flowLevel = null, symptoms = emptyList(), notes = notes.ifBlank { null },
                        crisisFlag = isCrisis
                    )
                    showDialog = false
                    if (isCrisis) showCrisisOverlay = true
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCrisisOverlay) {
        CrisisOverlay(onDismiss = { showCrisisOverlay = false })
    }
}

@Composable
private fun CheckInSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.toInt()}/10", fontWeight = FontWeight.Medium)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..10f, steps = 9)
    }
}

@Composable
private fun MiniChip(text: String) {
    Box(modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp)
    }
}

// ==========================================
// CRISIS OVERLAY
// ==========================================

@Composable
fun CrisisOverlay(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("You deserve support right now", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                }
                Text("LunaCare cares about you deeply. Because your safety matters most, please reach out to a trusted person, crisis line, or emergency services right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("LunaCare cannot substitute professional crisis support.", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onErrorContainer)
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LunaContent.emergencyResources.take(5).forEach { (flag, type, number) ->
                        Text("$flag $type: $number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Text("See full list under Health Awareness > Emergency Resources", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
                    Text("I'm reaching out for help")
                }
            }
        }
    }
}

// ==========================================
// LEARNING TAB
// ==========================================

@Composable
fun LearningTab(viewModel: LunaViewModel) {
    val bookmarksState by viewModel.bookmarks.collectAsState()
    var selectedSlug by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    if (selectedSlug != null) {
        val article = LunaContent.articles.firstOrNull { it.slug == selectedSlug }
        if (article != null) {
            ArticleDetailView(article = article, isBookmarked = bookmarksState.any { it.articleSlug == article.slug },
                onBookmark = { viewModel.toggleBookmark(article.slug) }, onBack = { selectedSlug = null })
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Resource Library", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Menstrual cup guides, period education, and emotional wellbeing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        item {
            val cats = listOf("All", "Menstrual Cup", "Period & PMS", "Health Awareness", "Emotional Wellbeing", "Bookmarks")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { cat ->
                    FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.primary))
                }
            }
        }

        // Menstrual cup protocol card
        if (selectedCategory == "All" || selectedCategory == "Menstrual Cup") {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔑 Safe Cup Protocol", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Wash hands with unscented soap", "Fold using C-fold or Punch-down", "Relax pelvic floor muscles", "Insert directed slightly backward", "Rotate to verify seal", "Pinch base to break seal before removal", "Empty and clean every 8–12 hours").forEach { step ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(step, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        val filtered = LunaContent.articles.filter { art ->
            when (selectedCategory) {
                "All" -> true
                "Bookmarks" -> bookmarksState.any { it.articleSlug == art.slug }
                else -> art.category == selectedCategory
            }
        }

        if (filtered.isEmpty()) {
            item { Text("No articles in this category.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
        } else {
            items(filtered) { article ->
                val isBookmarked = bookmarksState.any { it.articleSlug == article.slug }
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedSlug = article.slug }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(article.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(article.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(article.content.take(90) + "…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { viewModel.toggleBookmark(article.slug) }) {
                                Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleDetailView(article: EducationArticle, isBookmarked: Boolean, onBookmark: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Row {
                IconButton(onClick = onBookmark) { Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
        Text(article.category, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(article.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        if (article.safetyNote != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = AlertRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(article.safetyNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        Text(article.content, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp))
        HorizontalDivider()
        Text(LunaContent.GLOBAL_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Return to Library") }
    }
}

// ==========================================
// CARE TAB
// ==========================================

@Composable
fun CareTab(profile: Profile, viewModel: LunaViewModel) {
    val context = LocalContext.current
    val privacyMode = try { LocationPrivacyMode.valueOf(profile.locationPrivacyMode) } catch (e: Exception) { LocationPrivacyMode.OFF }
    val tempLat by viewModel.tempLat.collectAsState()
    val tempLng by viewModel.tempLng.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Care & Products", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Discover period care products and find them nearby", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }

        // Location privacy notice
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Location: ${LocationPrivacyManager.getModeDescription(privacyMode)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        if (tempLat != null) {
                            TextButton(onClick = { viewModel.clearTempLocation() }, contentPadding = PaddingValues(0.dp)) {
                                Text("Clear temporary location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        items(LunaContent.careProducts) { product ->
            val isBookmarked = bookmarks.any { it.articleSlug == "product_${product.slug}" }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(product.emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        IconButton(onClick = { viewModel.toggleBookmark("product_${product.slug}") }) {
                            Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(product.whatItIs, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("When it helps: ${product.whenItHelps}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("What to check: ${product.whatToCheck}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    if (product.safetyNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Text("Note: ${product.safetyNote}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val url = LocationPrivacyManager.buildGoogleMapsSearchUrl(product.nearbyQuery, tempLat, tempLng)
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            viewModel.clearTempLocation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Find Nearby")
                    }
                }
            }
        }
    }
}

// ==========================================
// JOURNAL TAB (legacy)
// ==========================================

@Composable
fun JournalTab(profile: Profile, viewModel: LunaViewModel) {
    val entries by viewModel.journalEntries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Wellness Journal", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Private, offline, encrypted reflections", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Button(onClick = { title = ""; body = ""; showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("create_journal_button")) {
                    Icon(Icons.Default.Create, null); Spacer(modifier = Modifier.width(4.dp)); Text("Write")
                }
            }
        }
        item {
            OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        val filtered = entries.filter { it.title.contains(search, true) || it.body.contains(search, true) }
        if (filtered.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No entries yet. Tap 'Write' to start your private journal.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(filtered) { entry ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            IconButton(onClick = { viewModel.deleteJournalEntry(entry.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                        Text(CycleUtils.formatDisplayDate(entry.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        Text(entry.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("Private Journal Entry") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Write your thoughts…") }, modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            },
            confirmButton = { Button(onClick = { if (body.isNotBlank()) { viewModel.addJournalEntry(title.ifBlank { "Reflections" }, body, null, null); showDialog = false } }, enabled = body.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// MEDICAL JOURNAL SCREEN
// ==========================================

@Composable
fun MedicalJournalScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    val entries by viewModel.medicalJournalEntries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var entryDate by remember { mutableStateOf(CycleUtils.getTodayString()) }
    var category by remember { mutableStateOf("General") }
    var entryTitle by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf(setOf<String>()) }
    var painLevel by remember { mutableStateOf(0f) }
    var notes by remember { mutableStateOf("") }
    var doctorVisit by remember { mutableStateOf(false) }
    var nextAppt by remember { mutableStateOf("") }
    val categories = listOf("General", "Symptom", "Doctor Visit", "Medication", "Surgery/Procedure", "Lab Result", "Other")
    val symptomsList = listOf("Cramps", "Bloating", "Headache", "Back pain", "Fatigue", "Nausea", "Spotting", "Clots", "Acne", "Mood changes", "Breast tenderness")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Medical Journal", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
                Button(onClick = { entryDate = CycleUtils.getTodayString(); category = "General"; entryTitle = ""; symptoms = emptySet(); painLevel = 0f; notes = ""; doctorVisit = false; nextAppt = ""; showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null); Spacer(modifier = Modifier.width(4.dp)); Text("Add")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Text(LunaContent.GLOBAL_DISCLAIMER, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))) {
                Text("⚠️ Only take medicine as advised by a qualified healthcare professional or according to the product label.",
                    modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
        }
        if (entries.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No journal entries yet. Record your symptoms, doctor visits, or health observations.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(entries) { entry ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, fontWeight = FontWeight.Bold)
                                Text("${entry.category} · ${CycleUtils.formatDisplayDate(entry.entryDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteMedicalJournalEntry(entry.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                        if (entry.painLevel > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pain: ", style = MaterialTheme.typography.bodySmall)
                                Box(modifier = Modifier.clip(CircleShape).background(if (entry.painLevel >= 7) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("${entry.painLevel}/10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                        if (entry.doctorVisit) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalHospital, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Doctor visit recorded", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("New Journal Entry") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = entryTitle, onValueChange = { entryTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = entryDate, onValueChange = { entryDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Category", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    Column {
                        Text("Pain level: ${painLevel.toInt()}/10", fontWeight = FontWeight.Medium)
                        Slider(value = painLevel, onValueChange = { painLevel = it }, valueRange = 0f..10f, steps = 9)
                    }
                    Text("Symptoms", fontWeight = FontWeight.Bold)
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(150.dp)) {
                        items(symptomsList) { s ->
                            val checked = symptoms.contains(s)
                            Row(modifier = Modifier.clickable { symptoms = if (checked) symptoms - s else symptoms + s }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { symptoms = if (checked) symptoms - s else symptoms + s })
                                Text(s, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = doctorVisit, onCheckedChange = { doctorVisit = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Doctor visit")
                    }
                    if (doctorVisit) {
                        OutlinedTextField(value = nextAppt, onValueChange = { nextAppt = it }, label = { Text("Next appointment (YYYY-MM-DD, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Private notes (encrypted)") }, modifier = Modifier.fillMaxWidth())
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))) {
                        Text("⚠️ Only take medicine as advised by a qualified healthcare professional or according to the product label.", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addMedicalJournalEntry(entryDate, category, entryTitle.ifBlank { "Entry" }, symptoms.toList(), painLevel.toInt(), null, null, null, doctorVisit, nextAppt.ifBlank { null }, notes.ifBlank { null })
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// MEDICINE REMINDERS SCREEN
// ==========================================

@Composable
fun MedicineRemindersScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    val reminders by viewModel.medicalReminders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderType by remember { mutableStateOf("Medication") }
    var reminderTime by remember { mutableStateOf("08:00") }
    var repeatRule by remember { mutableStateOf("Daily") }
    var startDate by remember { mutableStateOf(CycleUtils.getTodayString()) }
    var reasonNote by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Medicine Reminders", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
                Button(onClick = { reminderTitle = ""; reminderType = "Medication"; reminderTime = "08:00"; repeatRule = "Daily"; startDate = CycleUtils.getTodayString(); reasonNote = ""; showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null); Spacer(modifier = Modifier.width(4.dp)); Text("Add")
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Only take medicine as advised by a qualified healthcare professional or according to the product label.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (reminders.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reminders yet. Add your medication or supplement reminders.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(reminders) { reminder ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, null, tint = if (reminder.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(reminder.title, fontWeight = FontWeight.Bold)
                                Text("${reminder.reminderTime} · ${reminder.repeatRule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                if (!reminder.reasonNote.isNullOrBlank()) Text(reminder.reasonNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Row {
                            Switch(checked = reminder.enabled, onCheckedChange = { viewModel.toggleReminderEnabled(reminder) })
                            IconButton(onClick = { viewModel.deleteMedicalReminder(reminder.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("New Reminder") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = reminderTitle, onValueChange = { reminderTitle = it }, label = { Text("Medicine / reminder name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = reasonNote, onValueChange = { reasonNote = it }, label = { Text("Reason / notes (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reminderTime, onValueChange = { reminderTime = it }, label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text("Repeat", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Once", "Daily", "Weekly", "Monthly")) { rule ->
                            FilterChip(selected = repeatRule == rule, onClick = { repeatRule = rule }, label = { Text(rule) })
                        }
                    }
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (reminderTitle.isNotBlank()) {
                        viewModel.addMedicalReminder(reminderTitle, reminderType, reminderTime, repeatRule, startDate, null, null, reasonNote.ifBlank { null })
                        showDialog = false
                    }
                }, enabled = reminderTitle.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// CUP CARE TRACKER
// ==========================================

@Composable
fun CupCareTrackerScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    val logs by viewModel.cupCareLogs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var cleanedToday by remember { mutableStateOf(false) }
    var discomfort by remember { mutableStateOf(0f) }
    var leakage by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var showDiscomfortWarning by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Cup Care Tracker", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
                Button(onClick = { cleanedToday = false; discomfort = 0f; leakage = false; notes = ""; showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null); Text("Log")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔑 Daily Cup Reminders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf("Empty every 8–12 hours", "Rinse with cold water first", "Wash with unscented mild soap", "Clean small holes with soft brush", "Boil between cycles for 5–7 minutes").forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tip, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No cup logs yet. Track insertion, emptying, cleaning, and comfort.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = if (log.discomfortLevel >= 7) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(CycleUtils.formatDisplayDate(log.logDate), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.deleteCupCareLog(log.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            MiniChip(if (log.cleanedToday) "✅ Cleaned" else "❌ Not cleaned")
                            MiniChip("Discomfort: ${log.discomfortLevel}/10")
                            if (log.leakageIssue) MiniChip("⚠️ Leakage")
                        }
                        if (log.discomfortLevel >= 7) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                                Text("High discomfort reported. Do not force use. Remove gently if possible and seek professional advice if pain continues.", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("Log Cup Care") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = cleanedToday, onCheckedChange = { cleanedToday = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cleaned today")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = leakage, onCheckedChange = { leakage = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leakage issue")
                    }
                    Column {
                        Text("Discomfort level: ${discomfort.toInt()}/10", fontWeight = FontWeight.Medium)
                        Slider(value = discomfort, onValueChange = { discomfort = it }, valueRange = 0f..10f, steps = 9)
                        if (discomfort >= 7) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                                Text("Do not force use. Remove gently if possible and seek professional advice if pain continues.", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (private)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCupCareLog(null, null, cleanedToday, discomfort.toInt(), leakage, notes.ifBlank { null })
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// HEALTH AWARENESS SCREEN
// ==========================================

@Composable
fun HealthAwarenessScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    var selectedTopic by remember { mutableStateOf<HealthAwarenessTopic?>(null) }

    if (selectedTopic != null) {
        val topic = selectedTopic!!
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedTopic = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(topic.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.width(40.dp))
            }
            Text("${topic.emoji}  ${topic.summary}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(topic.content, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp))
            if (topic.symptoms.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Common signs people report:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        topic.symptoms.forEach { s ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = MaterialTheme.colorScheme.primary)
                                Text(s, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚠️ When to seek help", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(topic.whenToSeekHelp, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
            HorizontalDivider()
            Text(topic.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Text(LunaContent.REGIONAL_TERMINOLOGY_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            OutlinedButton(onClick = { selectedTopic = null }, modifier = Modifier.fillMaxWidth()) { Text("← Back to Health Awareness") }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column {
                    Text("Health Awareness", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Educational only — not diagnosis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Text(LunaContent.REGIONAL_TERMINOLOGY_NOTE, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(LunaContent.healthAwarenessTopics) { topic ->
            Card(modifier = Modifier.fillMaxWidth().clickable { selectedTopic = topic }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(topic.title, fontWeight = FontWeight.Bold)
                        Text(topic.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        // Emergency resources
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmergencyShare, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Resources", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LunaContent.emergencyResources.forEach { (flag, type, number) ->
                        Text("$flag $type: $number", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// AI ASSISTANT SCREEN
// ==========================================

@Composable
fun AiAssistantScreen(profile: Profile, viewModel: LunaViewModel, onBack: () -> Unit) {
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Pair<Boolean, String>>()) } // isUser, text
    var showPaywall by remember { mutableStateOf(false) }

    val isLimitReached = viewModel.isAiLimitReached()
    val remaining = viewModel.getRemainingAiMessages()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text("AI Wellness Assistant", fontWeight = FontWeight.Bold)
                    Text(if (profile.isPremium) "Premium · $remaining messages left" else "Free · $remaining of ${LunaViewModel.FREE_AI_MESSAGES_LIMIT} left",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )

        // Disclaimer banner
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Educational only. Not medical advice. No diagnosis. No medication dosage. For emergencies, contact emergency services.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Messages
        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (messages.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Ask me anything about period care, menstrual cups, mood, or wellness education.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("I cannot diagnose, prescribe, or replace medical professionals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            }
            items(messages) { (isUser, text) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(if (isUser) 16.dp else 16.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Paywall banner
        if (isLimitReached && !showPaywall) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("You've reached your free AI limit.", fontWeight = FontWeight.Bold)
                    Text("Upgrade for more wellness questions, journaling support, and learning help.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showPaywall = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Upgrade") }
                        TextButton(onClick = {}) { Text("Maybe later") }
                    }
                }
            }
        }

        // Input row
        if (!isLimitReached) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userInput, onValueChange = { userInput = it },
                    placeholder = { Text("Ask a wellness question…") },
                    modifier = Modifier.weight(1f), singleLine = false, maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            val isCrisis = CrisisDetector.detect(userInput)
                            if (isCrisis) {
                                messages = messages + (false to "I notice you may be going through something very difficult. Please reach out to emergency services or a crisis line immediately. I care about your safety. LunaCare cannot provide crisis support — please contact a professional.") + (true to userInput)
                            } else {
                                messages = messages + (true to userInput)
                                messages = messages + (false to "I'm here to help with educational wellness questions about periods, menstrual cups, mood, and self-care. I cannot diagnose, prescribe, or replace a doctor. [Note: AI response endpoint not yet connected — set EXPO_PUBLIC_AI_API_ENDPOINT in your environment.]")
                            }
                            viewModel.consumeAiMessage()
                            userInput = ""
                        }
                    },
                    enabled = userInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showPaywall) {
        Dialog(onDismissRequest = { showPaywall = false }) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Stars, null, tint = WarningAmber, modifier = Modifier.size(48.dp))
                    Text("Upgrade to LunaCare Premium", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                    Text("Get more AI wellness messages, unlimited journaling prompts, and priority learning content.", textAlign = TextAlign.Center)
                    Button(onClick = { showPaywall = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Upgrade — coming soon")
                    }
                    TextButton(onClick = { showPaywall = false }) { Text("Maybe later") }
                }
            }
        }
    }
}

// ==========================================
// PERMISSION CENTER
// ==========================================

@Composable
fun PermissionCenterScreen(profile: Profile, viewModel: LunaViewModel, onBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column {
                    Text("Permission Center", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("You control what LunaCare can access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
        val perms = listOf(
            PermissionInfo(Icons.Default.LocationOn, "Location / GPS", "Used only when you tap 'Find Nearby'.", "Never", "Your device GPS", "Tap 'Find Nearby' → grant in system prompt", "Turn off in Android Settings → Apps → LunaCare → Permissions"),
            PermissionInfo(Icons.Default.CameraAlt, "Camera", "Used only to attach photo to medical journal.", "Never", "Your camera photos (optional)", "Tap the camera icon in medical journal", "Revoke in Android Settings"),
            PermissionInfo(Icons.Default.Mic, "Microphone", "Used only for AI voice input (if enabled).", "Never", "Your voice during AI session only", "Tap microphone icon in AI chat", "Revoke in Android Settings"),
            PermissionInfo(Icons.Default.Notifications, "Notifications", "Used for medicine, mood, period, and cup reminders.", "On-device only", "Notification content you set", "Enable in Reminders settings", "Disable in Android Settings → Notifications")
        )
        items(perms) { perm ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(perm.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(perm.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PermRow("Why needed:", perm.why)
                    PermRow("Stored to server:", perm.stored)
                    PermRow("What is collected:", perm.collected)
                    PermRow("When requested:", perm.whenRequested)
                    PermRow("How to turn off:", perm.howToRevoke)
                }
            }
        }
        // Location privacy mode
        item {
            val privacyMode = try { LocationPrivacyMode.valueOf(profile.locationPrivacyMode) } catch (e: Exception) { LocationPrivacyMode.OFF }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Location Privacy Mode", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LocationPrivacyMode.values().forEach { mode ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.updateLocationPrivacyMode(mode) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = privacyMode == mode, onClick = { viewModel.updateLocationPrivacyMode(mode) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(mode.name.replace("_", " "), fontWeight = FontWeight.Medium)
                                Text(LocationPrivacyManager.getModeDescription(mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PermissionInfo(val icon: ImageVector, val name: String, val why: String, val stored: String, val collected: String, val whenRequested: String, val howToRevoke: String)

@Composable
private fun PermRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

// ==========================================
// NOTIFICATION PANEL
// ==========================================

@Composable
fun NotificationPanelScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Notifications", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) { Text("Mark all read") }
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications yet. Reminders and care suggestions will appear here.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(notifications) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.markNotificationRead(notif.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            when (notif.type) {
                                "period" -> Icons.Default.WaterDrop
                                "mood" -> Icons.Default.Mood
                                "medicine" -> Icons.Default.LocalPharmacy
                                "cup" -> Icons.Default.WaterDrop
                                "ai" -> Icons.Default.Psychology
                                "system" -> Icons.Default.Info
                                else -> Icons.Default.Notifications
                            }, null,
                            tint = if (!notif.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.Normal)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { viewModel.deleteNotification(notif.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// BOOKMARKS SCREEN
// ==========================================

@Composable
fun BookmarksScreen(viewModel: LunaViewModel, onBack: () -> Unit) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Bookmarks", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        if (bookmarks.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No bookmarks yet. Tap the bookmark icon on articles and products to save them here.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            items(bookmarks) { bm ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(bm.articleSlug.replace("-", " ").replace("_", " "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        IconButton(onClick = { viewModel.toggleBookmark(bm.articleSlug) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFILE EDIT
// ==========================================

@Composable
fun ProfileEditScreen(profile: Profile, viewModel: LunaViewModel, onBack: () -> Unit) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var pronoun by remember { mutableStateOf(profile.pronoun) }
    var customPronoun by remember { mutableStateOf(profile.customPronoun ?: "") }
    var genderMode by remember { mutableStateOf(profile.genderMode) }
    var religion by remember { mutableStateOf(profile.religion) }
    var country by remember { mutableStateOf(profile.country ?: "") }
    var region by remember { mutableStateOf(profile.region ?: "") }
    var city by remember { mutableStateOf(profile.city ?: "") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Edit Profile", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
                Button(onClick = {
                    viewModel.updateProfileIdentity(displayName, pronoun, customPronoun.ifBlank { null }, genderMode, religion, country.ifBlank { null }, region.ifBlank { null }, city.ifBlank { null }, profile.behaviourFocuses)
                    onBack()
                }) { Text("Save") }
            }
        }
        item { OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            OnboardingOptionGroup(
                label = "Pronouns",
                options = mapOf("SHE_HER" to "She/Her", "HE_HIM" to "He/Him", "THEY_THEM" to "They/Them", "CUSTOM" to "Custom", "PREFER_NOT_TO_SAY" to "Prefer not to say"),
                selected = pronoun, onSelect = { pronoun = it }
            )
            if (pronoun == "CUSTOM") {
                OutlinedTextField(value = customPronoun, onValueChange = { customPronoun = it }, label = { Text("Your pronouns") }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region / State (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
}

// ==========================================
// SUPPORT MODE TAB
// ==========================================

@Composable
fun SupportModeTab(profile: Profile) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Support Mode", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Learning how to be a caring, respectful supporter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        item {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolunteerActivism, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Supporting with Care", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Ask how you can help — don't assume",
                        "Listen without judgment or advice unless asked",
                        "Respect privacy — never share their health information",
                        "Learn about PMS, PCOS, and period experiences",
                        "Offer practical support: warmth, rest, food",
                        "Never dismiss or minimise their pain",
                        "Encourage professional care if symptoms are severe"
                    ).forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Favorite, null, tint = MutedRosePrimary, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacy Reminder", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Do not track another person's period, mood, symptoms, or location without their clear and ongoing consent. Support is built on trust and respect.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

// ==========================================
// SETTINGS TAB
// ==========================================

@Composable
fun SettingsTab(profile: Profile, viewModel: LunaViewModel, onBack: () -> Unit) {
    var cycleLengthStr by remember { mutableStateOf(profile.averageCycleLength.toString()) }
    var periodLengthStr by remember { mutableStateOf(profile.averagePeriodLength.toString()) }
    var periodReminders by remember { mutableStateOf(profile.periodReminders) }
    var moodReminders by remember { mutableStateOf(profile.moodReminders) }
    var selfCareReminders by remember { mutableStateOf(profile.selfCareReminders) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            if (onBack != {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    Text("Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
            } else {
                Text("Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Dark Mode", fontWeight = FontWeight.Bold)
                        Text("Toggle deep plum dark theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(checked = profile.isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cycle Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(value = cycleLengthStr, onValueChange = { cycleLengthStr = it }, label = { Text("Avg Cycle Length (days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = periodLengthStr, onValueChange = { periodLengthStr = it }, label = { Text("Avg Period Length (days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = { viewModel.updateCycleSettings(cycleLengthStr.toIntOrNull() ?: 28, periodLengthStr.toIntOrNull() ?: 5) }, modifier = Modifier.fillMaxWidth()) { Text("Save Cycle Settings") }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    SettingSwitch("Period predictions", periodReminders) { periodReminders = it; viewModel.updateNotificationPreferences(it, moodReminders, false, selfCareReminders, "20:00") }
                    SettingSwitch("Daily wellbeing check-in", moodReminders) { moodReminders = it; viewModel.updateNotificationPreferences(periodReminders, it, false, selfCareReminders, "20:00") }
                    SettingSwitch("Gentle self-care tips", selfCareReminders) { selfCareReminders = it; viewModel.updateNotificationPreferences(periodReminders, moodReminders, false, it, "20:00") }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data & Privacy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Text(LunaContent.GLOBAL_DISCLAIMER, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.clearAllUserData() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.DeleteForever, null); Spacer(modifier = Modifier.width(8.dp)); Text("Delete All Data & Reset")
                    }
                }
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text("LunaCare v2.0.0 · Educational Support Only", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                Text("Not medical advice • Not a substitute for professional care", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Utility extension
fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()
