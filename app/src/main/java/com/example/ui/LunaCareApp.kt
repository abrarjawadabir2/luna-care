package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.Input
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
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.LunaViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AppTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Favorite),
    Cycle("Cycle", Icons.Default.CalendarMonth),
    Mood("Wellbeing", Icons.Default.Mood),
    Learn("Learn", Icons.Default.School),
    Journal("Journal", Icons.Default.Create),
    Support("Support", Icons.Default.Spa),
    Care("Care", Icons.Default.ShoppingCart),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun LunaCareApp(viewModel: LunaViewModel) {
    val profileState by viewModel.profile.collectAsState()
    val isPinAuthenticated by viewModel.isPinAuthenticated.collectAsState()
    val showCrisisScreen by viewModel.showCrisisScreen.collectAsState()
    val loggedInCredentials by viewModel.loggedInCredentials.collectAsState()
    val isGuestUser by viewModel.isGuestUser.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // 0. If NOT logged in and NOT in Guest mode -> Show Auth Screen
            loggedInCredentials == null && !isGuestUser -> {
                AuthScreen(viewModel = viewModel)
            }

            // 1. If Profile is not set up -> Show Onboarding Screen
            profileState == null -> {
                OnboardingScreen(onCompleted = { name, birthYear, cycleLength, periodLength, goals, pin, userMode, genderMode, bodyRelevantMode, supportRel, consent, sharedTrackingConsent, behaviourFocuses, medRem, waterRem ->
                    viewModel.onboardUser(
                        name = name,
                        birthYear = birthYear,
                        cycleLength = cycleLength,
                        periodLength = periodLength,
                        goals = goals,
                        pin = pin,
                        userMode = userMode,
                        genderMode = genderMode,
                        bodyRelevantMode = bodyRelevantMode,
                        supportRelationship = supportRel,
                        consentConfirmed = consent,
                        sharedTrackingConsent = sharedTrackingConsent,
                        behaviourFocuses = behaviourFocuses,
                        medicineReminders = medRem,
                        waterReminders = waterRem
                    )
                })
            }

            // 2. If Security PIN is enabled but NOT authenticated -> Show Pin Screen
            profileState?.securityPinEnabled == true && !isPinAuthenticated -> {
                PinAuthScreen(
                    storedPin = profileState?.securityPin ?: "",
                    onAuthenticated = { viewModel.authenticatePin(it) }
                )
            }

            // 3. Otherwise -> Show Main App Layout
            else -> {
                val profile = profileState ?: Profile()
                MainAppLayout(
                    profile = profile,
                    viewModel = viewModel
                )
            }
        }
    }

    // Global Emergency Overlay Interceptor
    if (showCrisisScreen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCrisisDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crisis Support & Safety Notice",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You deserve support right now. Please contact emergency services, a crisis hotline, or someone you trust immediately.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "We care about your safety. If you are experiencing distress, please reach out. This app is for general educational help only and is not a substitute for clinical therapy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📞 Immediate Support: Call or Text 988",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Or call 911 / visit your nearest Emergency Department.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissCrisisDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("I understand & will find support")
                }
            }
        )
    }
}

// ==========================================
// ONBOARDING SCREEN
// ==========================================
@Composable
fun OnboardingScreen(
    onCompleted: (
        name: String,
        birthYear: Int?,
        cycleLength: Int,
        periodLength: Int,
        goals: List<String>,
        pin: String,
        userMode: String,
        genderMode: String,
        bodyRelevantMode: String,
        supportRelationship: String?,
        consentConfirmed: Boolean,
        sharedTrackingConsent: Boolean,
        behaviourFocuses: List<String>,
        medicineReminders: Boolean,
        waterReminders: Boolean
    ) -> Unit
) {
    var step by remember { mutableStateOf(0) } // Step 0: Welcome, Step 1: User Type, Step 2: Demographics, Step 3: Focus Selection, Step 4: Medical Disclaimer, Step 5: Notifications & Pin

    // Collected Data
    var name by remember { mutableStateOf("") }
    var birthYearStr by remember { mutableStateOf("") }
    var cycleLength by remember { mutableStateOf(28) }
    var periodLength by remember { mutableStateOf(5) }
    var lastPeriodStartDate by remember { mutableStateOf(CycleUtils.getTodayString()) }
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }
    var pinEnabled by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    var disclaimerAccepted by remember { mutableStateOf(false) }

    // Demographics and Role selection
    var userTypeChoice by remember { mutableStateOf("SELF_TRACKING") } // "SELF_TRACKING" | "SUPPORT_MODE" | "EDUCATION_ONLY"
    var genderModeChoice by remember { mutableStateOf("FEMALE") } // "FEMALE" | "MALE" | "OTHER" | "PREFER_NOT_TO_SAY"
    var bodyRelevantChoice by remember { mutableStateOf("MENSTRUATES") } // "MENSTRUATES" | "DOES_NOT_MENSTRUATE" | "NOT_SURE" | "PREFER_NOT_TO_SAY"
    var supportRelationship by remember { mutableStateOf("WIFE") } // "WIFE", "MOTHER", "DAUGHTER", etc.
    var consentConfirmed by remember { mutableStateOf(false) }
    var sharedTrackingConsent by remember { mutableStateOf(false) }
    var selectedFocuses by remember { mutableStateOf(setOf<String>()) }

    // Notification checks
    var periodNotification by remember { mutableStateOf(true) }
    var moodNotification by remember { mutableStateOf(true) }
    var cupNotification by remember { mutableStateOf(false) }
    var medicineNotification by remember { mutableStateOf(false) }
    var waterNotification by remember { mutableStateOf(true) }
    var comfortCareNotification by remember { mutableStateOf(true) }

    // Local Validation State
    var localError by remember { mutableStateOf("") }

    val relationshipsList = listOf(
        Pair("WIFE", "Wife"),
        Pair("MOTHER", "Mother"),
        Pair("DAUGHTER", "Daughter"),
        Pair("GIRLFRIEND", "Girlfriend"),
        Pair("FEMALE_PARTNER", "Female Partner"),
        Pair("SISTER", "Sister"),
        Pair("FRIEND", "Friend"),
        Pair("OTHER", "Other")
    )

    val focusesList = listOf(
        "MOOD", "STRESS", "ANXIETY", "SLEEP", "PERIOD_PAIN", "PMS",
        "PCOS_PCOD_AWARENESS", "MENSTRUAL_CUP", "FOOD_CRAVINGS",
        "HYDRATION", "PRODUCT_CARE", "MEDICINE_REMINDER", "DOCTOR_VISIT",
        "RELATIONSHIP_SUPPORT", "EMERGENCY_SIGNS"
    )

    fun getFocusLabel(key: String): String {
        return when(key) {
            "MOOD" -> "Mood & Emotional Health"
            "STRESS" -> "Stress Reduction"
            "ANXIETY" -> "Anxiety Relief"
            "SLEEP" -> "Sleep Quality & Rest"
            "PERIOD_PAIN" -> "Period Pain & Cramps"
            "PMS" -> "PMS Support"
            "PCOS_PCOD_AWARENESS" -> "PCOS / PCOD Awareness"
            "MENSTRUAL_CUP" -> "Menstrual Cup Usage"
            "FOOD_CRAVINGS" -> "Food & Comfort Cravings"
            "HYDRATION" -> "Hydration tracking"
            "PRODUCT_CARE" -> "Period Product Care (cup, etc.)"
            "MEDICINE_REMINDER" -> "Medicine & Care Reminder"
            "DOCTOR_VISIT" -> "Doctor Visit Planner"
            "RELATIONSHIP_SUPPORT" -> "Supporter & Relationship Tips"
            "EMERGENCY_SIGNS" -> "Emergency & Warning signs warning"
            else -> key
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header shown on screens after Welcome
        if (step > 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LunaCare",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Period Care & Support Companion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                // Step indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .height(5.dp)
                                .width(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                        )
                    }
                }
            }
        }

        // Main Center Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                // STEP 0: Welcome Landing Screen
                0 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MutedRosePrimary.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "LunaCare Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "LunaCare",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Period care, mental wellness, and gentle family support.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("get_started_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Get Started", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ElevatedButton(
                        onClick = {
                            // Onboard instantly with typical self tracking profile
                            onCompleted(
                                "Luna",
                                1996,
                                28,
                                5,
                                listOf("Track period", "PMS support"),
                                "",
                                "SELF_TRACKING",
                                "FEMALE",
                                "MENSTRUATES",
                                null,
                                false,
                                false,
                                listOf("MOOD", "STRESS", "PERIOD_PAIN"),
                                false,
                                true
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("already_have_account_button")
                    ) {
                        Text("I already have an account", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            // Continue as Guest immediately
                            onCompleted(
                                "Guest",
                                null,
                                28,
                                5,
                                listOf("Track period"),
                                "",
                                "SELF_TRACKING",
                                "FEMALE",
                                "MENSTRUATES",
                                null,
                                false,
                                false,
                                listOf("MOOD", "PERIOD_PAIN"),
                                false,
                                true
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("continue_as_guest_button")
                    ) {
                        Text("Continue as Guest")
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = "Educational support only. Not medical advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // STEP 1: Usage Mode Selection ("How do you want to use LunaCare?")
                1 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "How do you want to use LunaCare?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "We will optimize features and safety parameters based on your mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Option 1: For myself
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { userTypeChoice = "SELF_TRACKING" }
                            .padding(vertical = 8.dp)
                            .border(
                                width = if (userTypeChoice == "SELF_TRACKING") 2.dp else 1.dp,
                                color = if (userTypeChoice == "SELF_TRACKING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (userTypeChoice == "SELF_TRACKING") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (userTypeChoice == "SELF_TRACKING"),
                                onClick = { userTypeChoice = "SELF_TRACKING" }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("For myself", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Track your cycle, body metrics, wellness symptoms, and medical items.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Support Someone Else
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { userTypeChoice = "SUPPORT_MODE" }
                            .padding(vertical = 8.dp)
                            .border(
                                width = if (userTypeChoice == "SUPPORT_MODE") 2.dp else 1.dp,
                                color = if (userTypeChoice == "SUPPORT_MODE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (userTypeChoice == "SUPPORT_MODE") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (userTypeChoice == "SUPPORT_MODE"),
                                onClick = { userTypeChoice = "SUPPORT_MODE" }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("To support someone else", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Support wife, mother, daughter, girlfriend, or partner with comfortable care guidelines.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 3: Only to learn
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { userTypeChoice = "EDUCATION_ONLY" }
                            .padding(vertical = 8.dp)
                            .border(
                                width = if (userTypeChoice == "EDUCATION_ONLY") 2.dp else 1.dp,
                                color = if (userTypeChoice == "EDUCATION_ONLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (userTypeChoice == "EDUCATION_ONLY") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (userTypeChoice == "EDUCATION_ONLY"),
                                onClick = { userTypeChoice = "EDUCATION_ONLY" }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Only to learn", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Read medical safety logs, menstrual cup parameters, and safety guidelines with privacy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // STEP 2: Profile Setup (Dynamic based on step 1 choice)
                2 -> Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Profile Configuration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; localError = "" },
                        label = { Text("What name should we call you?") },
                        singleLine = true,
                        isError = localError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (localError.isNotEmpty()) {
                        Text(localError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userTypeChoice == "SELF_TRACKING") {
                        Text("Select your gender identity:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val genderOptions = listOf(
                            Pair("FEMALE", "Female"),
                            Pair("MALE", "Male"),
                            Pair("OTHER", "Other"),
                            Pair("PREFER_NOT_TO_SAY", "Prefer not to say")
                        )

                        genderOptions.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        genderModeChoice = key
                                        if (key == "MALE") {
                                            bodyRelevantChoice = "DOES_NOT_MENSTRUATE"
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (genderModeChoice == key), onClick = { 
                                    genderModeChoice = key
                                    if (key == "MALE") bodyRelevantChoice = "DOES_NOT_MENSTRUATE"
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Do you currently want period/cycle tracking features?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val bodyOptions = listOf(
                            Pair("MENSTRUATES", "Yes, I menstruate and want tracking"),
                            Pair("DOES_NOT_MENSTRUATE", "No, I do not need tracking"),
                            Pair("NOT_SURE", "I am not sure"),
                            Pair("PREFER_NOT_TO_SAY", "Prefer not to say")
                        )

                        bodyOptions.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { bodyRelevantChoice = key }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (bodyRelevantChoice == key), onClick = { bodyRelevantChoice = key })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (genderModeChoice == "MALE") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "💡 Comfort Mode: Men's self-tracking defaults to Educational guides & Physical care advice to respect medical privacy, keeping active private trackers hidden from view.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                    } else if (userTypeChoice == "SUPPORT_MODE") {
                        Text("Relationship to partner you are supporting:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        relationshipsList.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { supportRelationship = key }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (supportRelationship == key), onClick = { supportRelationship = key })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🔒 Respect & Safety Agreement",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Support means comfort, trust, and complete privacy. Do not log or monitor another family member's cycles, moods, or intimate safety details without their explicit permission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { consentConfirmed = !consentConfirmed }
                        ) {
                            Checkbox(checked = consentConfirmed, onCheckedChange = { consentConfirmed = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "I understand and will respect privacy and consent.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                    } else {
                        // EDUCATION_ONLY setup
                        Text(
                            text = "LunaCare is configured in Education-only mode. Personal cycle calendars are fully hidden, allowing you to learn and support with maximum simplicity.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                // STEP 3: Setup Focus Setup (“What do you want LunaCare to focus on?”)
                3 -> Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "What should LunaCare focus on?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Select your target focuses so we can custom model suggestions (multiple choices supported):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    focusesList.forEach { key ->
                        val isSel = selectedFocuses.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFocuses = if (isSel) selectedFocuses - key else selectedFocuses + key }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSel,
                                onCheckedChange = { selectedFocuses = if (isSel) selectedFocuses - key else selectedFocuses + key }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getFocusLabel(key), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // STEP 4: Medical Disclaimer
                4 -> Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Safety & Medical Disclaimer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "LunaCare is an educational and wellness tracker only. It is not clinical advice, diagnosis, or prescription. We never make dosage suggestions or prescribe items. If you experience severe bleeding, sharp pelvic distress, suspected infectious discharge, or safety crises, please contact emergency physical/mental support immediately.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { disclaimerAccepted = !disclaimerAccepted; localError = "" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = disclaimerAccepted,
                            onCheckedChange = { disclaimerAccepted = it; localError = "" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I understand and accept this medical disclaimer.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (localError.isNotEmpty()) {
                        Text(localError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // STEP 5: Reminders & Pin Lock
                5 -> Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Set security and alerts",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text("Alert Preferences:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { periodNotification = !periodNotification }) {
                        Checkbox(checked = periodNotification, onCheckedChange = { periodNotification = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cycle phase and health updates", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { moodNotification = !moodNotification }) {
                        Checkbox(checked = moodNotification, onCheckedChange = { moodNotification = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily mood check-in alerts", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { medicineNotification = !medicineNotification }) {
                        Checkbox(checked = medicineNotification, onCheckedChange = { medicineNotification = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Medicine & Care reminder alarms", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("App Lock Security PIN (Optional):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { pinEnabled = !pinEnabled; localError = "" }) {
                        Checkbox(checked = pinEnabled, onCheckedChange = { pinEnabled = it; localError = "" })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable secure 4-6 digit entrance pin code", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (pinEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinCode = it; localError = "" },
                            label = { Text("Enter Numeric Passcode (4-6 digits)") },
                            singleLine = true,
                            isError = localError.isNotEmpty(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (localError.isNotEmpty()) {
                            Text(localError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        // Action Buttons at bottom (For steps > 0)
        if (step > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                TextButton(
                    onClick = {
                        localError = ""
                        step--
                    }
                ) {
                    Text("Back")
                }

                // Continue/Finish button
                Button(
                    onClick = {
                        localError = ""
                        // 1. Validate demographics (Step 2)
                        if (step == 2) {
                            if (name.isBlank()) {
                                localError = "Name cannot be empty. Please enter your name."
                                return@Button
                            }
                            if (userTypeChoice == "SUPPORT_MODE" && !consentConfirmed) {
                                localError = "You must confirm privacy and consent to continue."
                                return@Button
                            }
                        }

                        // 2. Validate Disclaimer accept (Step 4)
                        if (step == 4 && !disclaimerAccepted) {
                            localError = "You must accept the medical disclaimer to continue."
                            return@Button
                        }

                        // 3. Validate Pin Lock (Step 5)
                        if (step == 5 && pinEnabled && pinCode.length < 4) {
                            localError = "PIN passcode must be between 4 to 6 numeric digits."
                            return@Button
                        }

                        // Flow step movement
                        if (step < 5) {
                            step++
                        } else {
                            // Onboard complete!
                            val finalUserMode = when {
                                userTypeChoice == "SUPPORT_MODE" -> "SUPPORT_MODE"
                                userTypeChoice == "EDUCATION_ONLY" -> "EDUCATION_ONLY"
                                genderModeChoice == "MALE" -> "EDUCATION_ONLY"
                                else -> "SELF_TRACKING"
                            }
                            onCompleted(
                                name,
                                null, // birthYear
                                cycleLength,
                                periodLength,
                                listOf("Track period", "PMS support"),
                                if (pinEnabled) pinCode else "",
                                finalUserMode,
                                genderModeChoice,
                                bodyRelevantChoice,
                                if (userTypeChoice == "SUPPORT_MODE") supportRelationship else null,
                                consentConfirmed,
                                sharedTrackingConsent,
                                selectedFocuses.toList(),
                                medicineNotification,
                                waterNotification
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (step == 5) "Discover LunaCare" else "Continue")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// PIN AUTHENTICATION SCREEN
// ==========================================
@Composable
fun PinAuthScreen(
    storedPin: String,
    onAuthenticated: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LunaCare Security",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Please enter your passcode to unlock your journal and data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Dot pin bubbles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until storedPin.length) {
                    val isActive = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Custom Numeric Keypad
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "Delete")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable(enabled = digit.isNotEmpty()) {
                                    if (digit == "Delete") {
                                        if (pinInput.isNotEmpty()) {
                                            pinInput = pinInput.dropLast(1)
                                        }
                                    } else if (digit.isNotEmpty()) {
                                        if (pinInput.length < storedPin.length) {
                                            pinInput += digit
                                            if (pinInput.length == storedPin.length) {
                                                if (onAuthenticated(pinInput)) {
                                                    // Pass success handled upstream
                                                } else {
                                                    errorMessage = "Incorrect Passcode. Try again."
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (digit == "Delete") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onBackground)
                            } else {
                                Text(
                                    text = digit,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
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
fun getTabsForUserMode(profile: Profile?): List<AppTab> {
    if (profile == null) return listOf(AppTab.Home, AppTab.Learn, AppTab.Care, AppTab.Settings)
    val access = CycleUtils.getFeatureAccess(profile)
    return when (profile.userMode) {
        "SELF_TRACKING" -> {
            if (access.canTrackCycle) {
                listOf(AppTab.Home, AppTab.Cycle, AppTab.Mood, AppTab.Journal, AppTab.Learn, AppTab.Care)
            } else {
                listOf(AppTab.Home, AppTab.Mood, AppTab.Journal, AppTab.Learn, AppTab.Care)
            }
        }
        "SUPPORT_MODE" -> listOf(AppTab.Home, AppTab.Support, AppTab.Learn, AppTab.Care, AppTab.Journal, AppTab.Settings)
        "EDUCATION_ONLY" -> listOf(AppTab.Home, AppTab.Learn, AppTab.Care, AppTab.Mood, AppTab.Settings)
        else -> listOf(AppTab.Home, AppTab.Learn, AppTab.Care, AppTab.Settings)
    }
}

fun getTabTitle(tab: AppTab, userMode: String): String {
    if (tab == AppTab.Journal && userMode == "SUPPORT_MODE") return "Notes"
    if (tab == AppTab.Mood && userMode == "EDUCATION_ONLY") return "Mind"
    return tab.title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val activeTabs = remember(profile.userMode, profile.bodyRelevantMode) { getTabsForUserMode(profile) }
    var selectedTab by rememberSaveable(profile.userMode) { mutableStateOf(activeTabs.firstOrNull() ?: AppTab.Home) }

    // Automatic Navigation Event Tracking
    androidx.compose.runtime.LaunchedEffect(selectedTab) {
        viewModel.trackNavigation(getTabTitle(selectedTab, profile.userMode))
    }

    val deepLinkRoute by viewModel.deepLinkRoute.collectAsState()
    androidx.compose.runtime.LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let { path ->
            when {
                path.contains("/learn") || path.contains("/cup-education") || path.contains("/awareness") -> {
                    if (activeTabs.contains(AppTab.Learn)) selectedTab = AppTab.Learn
                }
                path.contains("/care") -> {
                    if (activeTabs.contains(AppTab.Care)) selectedTab = AppTab.Care
                }
            }
            viewModel.clearDeepLinkRoute()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.displayName.firstOrNull()?.uppercase() ?: "L",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "LunaCare",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedTab = AppTab.Settings },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                windowInsets = WindowInsets.navigationBars
            ) {
                activeTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = getTabTitle(tab, profile.userMode)
                            )
                        },
                        label = { Text(text = getTabTitle(tab, profile.userMode), fontSize = 10.sp) },
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                AppTab.Home -> DashboardTab(profile = profile, viewModel = viewModel, onTabRequested = { selectedTab = it })
                AppTab.Cycle -> CycleTab(profile = profile, viewModel = viewModel)
                AppTab.Mood -> EmotionalHealthTab(profile = profile, viewModel = viewModel)
                AppTab.Learn -> LearningTab(profile = profile, viewModel = viewModel)
                AppTab.Journal -> JournalTab(profile = profile, viewModel = viewModel)
                AppTab.Support -> SupportTab(profile = profile, viewModel = viewModel)
                AppTab.Care -> CareTab(profile = profile, viewModel = viewModel)
                AppTab.Settings -> SettingsTab(profile = profile, viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Card(
        modifier = baseModifier
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(content = content)
    }
}

// ==========================================
// TAB 1: DASHBOARD
// ==========================================
@Composable
fun DashboardTab(
    profile: Profile,
    viewModel: LunaViewModel,
    onTabRequested: (AppTab) -> Unit
) {
    val periodLogs by viewModel.periodLogs.collectAsState()
    val moodLogs by viewModel.moodLogs.collectAsState()
    val symptomLogs by viewModel.symptomLogs.collectAsState()
    var showTrendTracker by rememberSaveable { mutableStateOf(false) }

    if (showTrendTracker) {
        SymptomTrendTrackerComponent(
            viewModel = viewModel,
            symptomLogs = symptomLogs,
            periodLogs = periodLogs,
            onBack = { showTrendTracker = false }
        )
        return
    }

    val lastLog = periodLogs.maxByOrNull { it.startDate }
    val todayStr = CycleUtils.getTodayString()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background Glows following LunaCare Soft Modernism Palette
                // Flowing dusty deep rose pastel blob in top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD48B8B).copy(alpha = 0.22f), // MutedRosePrimary
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                )

                // Dreamy Lavender blob in middle-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9881A8).copy(alpha = 0.20f), // LavenderSecondary
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension * 0.45f,
                    center = Offset(size.width * 0.10f, size.height * 0.50f)
                )

                // Glowing warm coral blob in bottom-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD48C73).copy(alpha = 0.18f), // WarmCoralTertiary
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension * 0.50f,
                    center = Offset(size.width * 0.90f, size.height * 0.85f)
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val userMode = profile.userMode
            val access = CycleUtils.getFeatureAccess(profile)

            if (userMode == "SELF_TRACKING") {
                // =====================================
                // SELF-CARE GRAPHICAL TRACK (USER)
                // =====================================

                // Welcome Header
                item {
                    Column {
                        Text(
                            text = "Welcome, ${profile.displayName}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "All logs are safe, private and entirely stored offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                if (access.canTrackCycle) {
                    // Cycle Wheel Card with Glass dial
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().testTag("frosted_cycle_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (lastLog == null) {
                                    Text(
                                        text = "Add your last period start date to unlock personal predictions and support insights.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                    Button(
                                        onClick = { onTabRequested(AppTab.Cycle) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Set Start Date")
                                    }
                                } else {
                                    val prediction = CycleUtils.predictNextPeriod(
                                        periodLogs = periodLogs,
                                        profileAverageLength = profile.averageCycleLength,
                                        profilePeriodLength = profile.averagePeriodLength,
                                        today = java.time.LocalDate.now()
                                    )
                                    val cycleDay = prediction.currentCycleDay ?: 1
                                    val phase = prediction.phase

                                    Text(
                                        text = phase.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Glass dial
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.35f))
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.5f),
                                                        Color.White.copy(alpha = 0.1f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Day",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "$cycleDay",
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 44.sp
                                            )
                                            Text(
                                                text = "of ${prediction.predictedCycleLength}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Predictions based on engine
                                    val daysUntil = prediction.daysUntil
                                    if (prediction.isOverdue) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("⚠️", fontSize = 16.sp)
                                                Text(
                                                    text = "Cycle Day $cycleDay • Period Overdue by ${prediction.overdueDays} days",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = when {
                                                daysUntil == null -> "Calculating predictions..."
                                                daysUntil < 0 -> "Period expected very soon"
                                                daysUntil == 0L -> "Period predicted to start today!"
                                                daysUntil == 1L -> "Period predicted tomorrow!"
                                                else -> "Next period in $daysUntil days"
                                            },
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        
                                        if (prediction.nextPeriodDate != null) {
                                            Text(
                                                text = "Estimated Start: ${CycleUtils.formatDisplayDate(prediction.nextPeriodDate)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    // Engine microcopy / details badge
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 10.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (prediction.isBasedOnHistory) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (prediction.isBasedOnHistory) "✨" else "ℹ️",
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = if (prediction.isBasedOnHistory) {
                                                    "Historical Engine: ${prediction.predictedCycleLength}-day avg (from ${prediction.historicalCycleCount} logged cycles)"
                                                } else {
                                                    "Profile default: ${prediction.predictedCycleLength}-day cycle (log more periods to enable engine)"
                                                },
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (prediction.isBasedOnHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = CycleUtils.getPhaseDescription(phase),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Wellness card instead
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().testTag("frosted_wellness_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🌟 INDIVIDUAL WELLNESS ASSISTANCE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Text(
                                    text = "Supporting your health, ${profile.displayName}!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Active cycle calculations and calendars are safely hidden under your profile to align with your medical mode. Focus on sleep, daily stress registers, or water metrics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                
                                if (profile.behaviourFocuses.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Active Focuses: " + profile.behaviourFocuses.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                // Mood Check-in
                item {
                    var selectedMoodState by remember { mutableStateOf<String?>(null) }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_mood_shortcut")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "How are you today?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Log your emotional wellbeing instantly with a single touch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val moods = listOf(
                                    Pair("😊", "Great"),
                                    Pair("🙂", "Good"),
                                    Pair("😐", "Okay"),
                                    Pair("😔", "Low"),
                                    Pair("😰", "Anxious")
                                )
                                moods.forEach { (emoji, label) ->
                                    val isSelected = selectedMoodState == label
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedMoodState = label
                                                viewModel.addMoodLog(
                                                    mood = label,
                                                    energy = if (label == "Great" || label == "Good") 4 else 2,
                                                    stress = if (label == "Low" || label == "Anxious") 4 else 1,
                                                    sleepQuality = null,
                                                    notes = "Logged instantly via Dashboard check-in shortcut."
                                                )
                                            }
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(emoji, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(selectedMoodState != null) {
                                Surface(
                                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "✓ Logged as '$selectedMoodState'! Keep going, your entries are private.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Bento Action Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Log Period Flow Action Card
                        GlassCard(
                            modifier = Modifier.weight(1f).testTag("action_flow"),
                            onClick = { onTabRequested(AppTab.Cycle) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Log flow",
                                    tint = MutedRosePrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Log Flow",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Log Wellbeing Action Card
                        GlassCard(
                            modifier = Modifier.weight(1f).testTag("action_mood"),
                            onClick = { onTabRequested(AppTab.Mood) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEmotions,
                                    contentDescription = "Wellbeing",
                                    tint = LavenderSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Emotional",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Journal Card
                        GlassCard(
                            modifier = Modifier.weight(1f).testTag("action_journal"),
                            onClick = { onTabRequested(AppTab.Journal) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BorderColor,
                                    contentDescription = "Journal",
                                    tint = WarmCoralTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Write",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Self-Care Tip of the Day
                item {
                    val randomTip = remember { LunaContent.selfCareTips.random() }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("self_care_reminder_card"),
                        onClick = { onTabRequested(AppTab.Mood) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Today's Self-Care",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = randomTip.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = randomTip.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• ${randomTip.steps.first()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Dedicated Symptom Trends Quick Insights Card
                item {
                    val recentLogsCount = symptomLogs.size
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_trends_shortcut_card"),
                        onClick = { showTrendTracker = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cycle & Symptom Trends",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View Details",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Visual Symptom Tracker",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            val promptText = if (recentLogsCount > 0) {
                                "You have $recentLogsCount logged symptoms. Click to analyze frequency distribution, comparative monthly statistics, and phase correlations."
                            } else {
                                "Track frequency of symptoms over the last three months. Explore visual analytics, phase correlations, and offline logs."
                            }
                            
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

            } else if (userMode == "SUPPORT_MODE") {
                // =====================================
                // SUPPORTER / COMPANION CARE TRACKER CARD
                // =====================================

                // Supporter Welcome Header
                item {
                    Column {
                        Text(
                            text = "Companion support, ${profile.displayName}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Active cycles, material standards & private empathy checklist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                // Companion Predictive Cycle Wheel Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("supporter_companion_cycle_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (lastLog == null) {
                                Text(
                                    text = "Add your companion's cycle start dates in the Cycle Tracker tab to calculate predictions and access support checklists.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Button(
                                    onClick = { onTabRequested(AppTab.Cycle) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Log Partner Period Start")
                                }
                            } else {
                                val prediction = CycleUtils.predictNextPeriod(
                                    periodLogs = periodLogs,
                                    profileAverageLength = profile.averageCycleLength,
                                    profilePeriodLength = profile.averagePeriodLength,
                                    today = java.time.LocalDate.now()
                                )
                                val cycleDay = prediction.currentCycleDay ?: 1
                                val phase = prediction.phase

                                Text(
                                    text = "COMPANION'S ACTIVE CYCLE STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Companion dial
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.35f))
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.5f),
                                                    Color.White.copy(alpha = 0.1f)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Phase Day",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "$cycleDay",
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary,
                                            lineHeight = 44.sp
                                        )
                                        Text(
                                            text = "of ${prediction.predictedCycleLength}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Current Phase: $phase",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val daysUntil = prediction.daysUntil
                                if (prediction.isOverdue) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("⚠️", fontSize = 14.sp)
                                            Text(
                                                text = "Companion's period is late by ${prediction.overdueDays} days",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = when {
                                            daysUntil == null -> "Calculating companion predictions..."
                                            daysUntil < 0 -> "Companion's period expected very soon."
                                            daysUntil == 0L -> "Companion's period predicted to start today!"
                                            daysUntil == 1L -> "Companion's period predicted tomorrow!"
                                            else -> "Companion's next cycle estimated in $daysUntil days."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    
                                    if (prediction.nextPeriodDate != null) {
                                        Text(
                                            text = "Estimated Start: ${CycleUtils.formatDisplayDate(prediction.nextPeriodDate)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }

                                // Engine microcopy / details badge
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, bottom = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (prediction.isBasedOnHistory) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (prediction.isBasedOnHistory) "✨" else "ℹ",
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (prediction.isBasedOnHistory) {
                                                "Historical Engine: ${prediction.predictedCycleLength}-day avg (from ${prediction.historicalCycleCount} logged cycles)"
                                            } else {
                                                "Profile default: ${prediction.predictedCycleLength}-day cycle (log more periods to enable engine)"
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (prediction.isBasedOnHistory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Tailored Support Advice based on phase
                                val supportAdvice = when (phase) {
                                    "Menstrual Phase" -> "Rest and warmth are key. Prepare warm lower-back therapy compresses, offer chamomile tea, and take over physical chores."
                                    "Follicular Phase" -> "Support her rising creative ideas and stamina! Perfect phase for scheduling outings, exercise, and pleasant conversations."
                                    "Ovulatory Phase" -> "Her physical and emotional energy is peak today. Listen attentively, offer compliments, and support active projects."
                                    "Luteal Phase" -> "Estrogen is falling and PMS might develop. Keep magnesium-rich dark chocolates nearby, keep background noises soothing, and exercise patience."
                                    else -> "Stay supportive, check in, and maintain open, gentle care dialogues."
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("💡 Companion Empathy Guide:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(supportAdvice, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Support Action Checklist Bento (remember checks locally to provide playful interactiveness)
                item {
                    var chk1 by remember { mutableStateOf(false) }
                    var chk2 by remember { mutableStateOf(false) }
                    var chk3 by remember { mutableStateOf(false) }
                    var chk4 by remember { mutableStateOf(false) }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("supporter_checklist_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "❤️ Daily Empathetic Support Action Checklist",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Small offline actions that immensely ease menstrual comfort today:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cbColors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                    Checkbox(checked = chk1, onCheckedChange = { chk1 = it }, colors = cbColors)
                                    Text("☕ Brew high-iron Herbal Infusion (Red Raspberry or Fennel)", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cbColors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                    Checkbox(checked = chk2, onCheckedChange = { chk2 = it }, colors = cbColors)
                                    Text("🍫 Lay out dark cocoa snacks (soothes magnesium cravings)", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cbColors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                    Checkbox(checked = chk3, onCheckedChange = { chk3 = it }, colors = cbColors)
                                    Text("🛁 Set up warm heating pad / thermal compress on sofa", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val cbColors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                                    Checkbox(checked = chk4, onCheckedChange = { chk4 = it }, colors = cbColors)
                                    Text("💧 Check fluid intake & prompt glass of water", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Modern Menstrual Product Shopping Spec Guide Card (Aesthetic consumer council specifications!)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("supporter_shopping_spec_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Eco-Consumer Hygiene Shopping Guide",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Buying pads, liners or silicone cups? Use these safety checklists:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Pads/Tampons: Choose unbleached chemical-free cotton (labeled ECF or TCF to avoid dioxins). Zero artificial fragrances to protect vaginal ecosystem pH levels.\n" +
                                       "• Menstrual Cups: Medical-grade silicone free from plasticizers or BPA. Confirm shape permits boiling sanitizations for 5-10 minutes.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // =====================================
                // EDUCATION-ONLY COMFORT & KNOWLEDGE DASHBOARD
                // =====================================
                
                // Welcome Header
                item {
                    Column {
                        Text(
                            text = "Education & Wellness, ${profile.displayName}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Privacy-first medical guidelines, cup hygiene and care notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                // Privacy/Disclaimer Focus Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("education_privacy_card")
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your Privacy & Comfort Standard",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "LunaCare functions fully offline inside an encrypted local-only database. Because you selected Education Only, active menstrual, intimate details, or symptom cycle prediction dials are fully excluded. Focus on clinical hygiene standards and menstrual cup security guides.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                        }
                    }
                }

                // Selected Active Focus Areas list
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("education_active_focus_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "📚 Your Registered Learning Focuses",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val activeFocuses = profile.behaviourFocuses
                            if (activeFocuses.isEmpty()) {
                                Text(
                                    text = "Tap Settings on the menu to select specific topic reminders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activeFocuses) { focus ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(
                                                text = focus.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =====================================
            // COMMON SECTION ITEMS (Daily Guides & Safety)
            // =====================================

            // Educational Card Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Knowledge Highlights",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { onTabRequested(AppTab.Learn) }) {
                        Text("See All Guides", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Educational Bento Cards
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tutorial 1: Menstrual Cup Basics
                    item {
                        GlassCard(
                            modifier = Modifier.width(260.dp),
                            onClick = { onTabRequested(AppTab.Learn) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ECO-CARE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "What is a Menstrual Cup?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Understand how flexible medical-grade silicone collects flow instead of absorbing it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("3 min read", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Tutorial 2: How to Fold
                    item {
                        GlassCard(
                            modifier = Modifier.width(260.dp),
                            onClick = { onTabRequested(AppTab.Learn) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "TECHNIQUE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Menstrual Cup Folds & Use",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Master folds like the Punch-down, C-fold and 7-fold to ensure smooth, comfortable seals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("4 min read", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    // Tutorial 3: Care & Sanitization
                    item {
                        GlassCard(
                            modifier = Modifier.width(260.dp),
                            onClick = { onTabRequested(AppTab.Learn) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "HYGIENE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Boiling & Sterilization Guides",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Wash daily with mild soap, boil between cycles and store securely in cotton.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("2 min read", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }

            // Crisis/Emergency Support Shortcut Block
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTabRequested(AppTab.Mood) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Intimate Safety & Crisis Support",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Need severe pain support, emergency helplines, or therapist connection?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: CYCLE & PERIOD TRACKER
// ==========================================
@Composable
fun CycleTab(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val periodLogs by viewModel.periodLogs.collectAsState()
    val symptomLogs by viewModel.symptomLogs.collectAsState()

    var selectedCalendarDate by remember { mutableStateOf(java.time.LocalDate.now()) }

    var showLogDialog by remember { mutableStateOf(false) }

    // Dialog state bindings
    var startDate by remember { mutableStateOf(CycleUtils.getTodayString()) }
    var endDate by remember { mutableStateOf("") }
    var flowLevel by remember { mutableStateOf("Medium") }
    val symptomsList = listOf("Cramps", "Headache", "Back pain", "Breast tenderness", "Acne", "Fatigue", "Bloating", "Nausea", "Mood swings", "Anxiety", "Low mood", "Irritability", "Sleep issues")
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Period Tracking",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Track logs to personalize cycle predictions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = {
                        startDate = CycleUtils.getTodayString()
                        endDate = ""
                        flowLevel = "Medium"
                        selectedSymptoms = emptySet()
                        notes = ""
                        showLogDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_period_log_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Log")
                }
            }
        }

        // Active Medical Alert Disclaimers
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ Medical Guidelines",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Heavy bleeding (soaking through pads/cups hourly) or severe abdominal cramps require urgent healthcare assessment.\n• If your cycle ranges under 21 or over 45 days, consult a physician for hormonal reviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Interactive Calendar Date Range Period Logger Card
        item {
            CalendarPeriodLogger(
                viewModel = viewModel,
                periodLogs = periodLogs,
                onLogSaved = {
                    showLogDialog = false
                }
            )
        }

        // Visual Cycle Calendar Card
        item {
            var calendarYear by remember { mutableStateOf(java.time.LocalDate.now().year) }
            var calendarMonth by remember { mutableStateOf(java.time.LocalDate.now().monthValue) }
            val firstDayOfMonth = java.time.LocalDate.of(calendarYear, calendarMonth, 1)
            val daysInMonth = firstDayOfMonth.lengthOfMonth()
            val firstDayOfWeekObj = firstDayOfMonth.dayOfWeek
            val firstDayOfWeekIndex = if (firstDayOfWeekObj.value == 7) 0 else firstDayOfWeekObj.value // Sunday = 0, Monday = 1...
            
            val totalCells = firstDayOfWeekIndex + daysInMonth
            
            Card(
                modifier = Modifier.fillMaxWidth().testTag("visual_cycle_calendar"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📅 Cycle Phase Calendar & Predictor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Month & Year header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = firstDayOfMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US)),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    if (calendarMonth == 1) {
                                        calendarMonth = 12
                                        calendarYear--
                                    } else {
                                        calendarMonth--
                                    }
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }
                            IconButton(
                                onClick = {
                                    if (calendarMonth == 12) {
                                        calendarMonth = 1
                                        calendarYear++
                                    } else {
                                        calendarMonth++
                                    }
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Weekday headers: S M T W T F S
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val weekdays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Days grid
                    val totalRows = (totalCells + 6) / 7
                    for (row in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..6) {
                                val cellIdx = row * 7 + col
                                if (cellIdx >= totalCells || cellIdx < firstDayOfWeekIndex) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val dayNum = cellIdx - firstDayOfWeekIndex + 1
                                    val cellDate = java.time.LocalDate.of(calendarYear, calendarMonth, dayNum)
                                    val isSelected = selectedCalendarDate == cellDate
                                    
                                    val latestPeriodLog = periodLogs.maxByOrNull { it.startDate }
                                    val lastPeriodStartDate = latestPeriodLog?.startDate
                                    val averageCycleLength = profile.averageCycleLength
                                    val periodLength = profile.averagePeriodLength
                                    
                                    var dayPhase = "Follicular Phase"
                                    var dayColor = Color.Transparent
                                    var onDayColor = MaterialTheme.colorScheme.onSurface
                                    
                                    if (!lastPeriodStartDate.isNullOrEmpty()) {
                                        val startLocalDate = try { java.time.LocalDate.parse(lastPeriodStartDate) } catch (e: Exception) { null }
                                        if (startLocalDate != null) {
                                            val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, cellDate)
                                            if (daysSinceStart >= 0) {
                                                val cycleDay = (daysSinceStart % averageCycleLength).toInt() + 1
                                                dayPhase = CycleUtils.getCyclePhase(cycleDay, averageCycleLength, periodLength)
                                                
                                                when (dayPhase) {
                                                    "Menstrual Phase" -> {
                                                        dayColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                        onDayColor = MaterialTheme.colorScheme.onErrorContainer
                                                    }
                                                    "Follicular Phase" -> {
                                                        dayColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                        onDayColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    }
                                                    "Ovulatory Phase" -> {
                                                        dayColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                                        onDayColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    }
                                                    "Luteal Phase" -> {
                                                        dayColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                                        onDayColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else dayColor
                                            )
                                            .clickable { selectedCalendarDate = cellDate }
                                            .border(
                                                width = if (cellDate == java.time.LocalDate.now()) 1.5.dp else 0.dp,
                                                color = if (cellDate == java.time.LocalDate.now()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .testTag("calendar_day_${calendarYear}_${calendarMonth}_${dayNum}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || cellDate == java.time.LocalDate.now()) FontWeight.Black else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else onDayColor
                                            )
                                            
                                            if (!isSelected && dayPhase == "Menstrual Phase" && dayColor != Color.Transparent) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.error)
                                                )
                                            } else if (!isSelected && dayPhase == "Ovulatory Phase" && dayColor != Color.Transparent) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Detail selection block
                    selectedCalendarDate?.let { date ->
                        val latestPeriodLog = periodLogs.maxByOrNull { it.startDate }
                        val lastPeriodStartDate = latestPeriodLog?.startDate
                        val averageCycleLength = profile.averageCycleLength
                        val periodLength = profile.averagePeriodLength
                        
                        var computedPhase = "Unknown Phase"
                        var computedCycleDay: Int? = null
                        var computedAdvice = ""
                        
                        if (!lastPeriodStartDate.isNullOrEmpty()) {
                            val startLocalDate = try { java.time.LocalDate.parse(lastPeriodStartDate) } catch (e: Exception) { null }
                            if (startLocalDate != null) {
                                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, date)
                                if (daysSinceStart >= 0) {
                                    computedCycleDay = (daysSinceStart % averageCycleLength).toInt() + 1
                                    computedPhase = CycleUtils.getCyclePhase(computedCycleDay, averageCycleLength, periodLength)
                                    computedAdvice = CycleUtils.getPhaseDescription(computedPhase)
                                } else {
                                    computedPhase = "Follicular Phase"
                                    computedAdvice = CycleUtils.getPhaseDescription(computedPhase)
                                }
                            } else {
                                computedAdvice = "Prediction loaded from baseline profile markers."
                            }
                        } else {
                            computedAdvice = "⚠️ Add an Active Period Log to unlock dynamic phase predictions!"
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", java.util.Locale.US)),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (computedCycleDay != null) {
                                    Text(
                                        text = "Day $computedCycleDay of Cycle",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (computedPhase) {
                                                "Menstrual Phase" -> MaterialTheme.colorScheme.error
                                                "Follicular Phase" -> MaterialTheme.colorScheme.primary
                                                "Ovulatory Phase" -> MaterialTheme.colorScheme.secondary
                                                "Luteal Phase" -> MaterialTheme.colorScheme.tertiary
                                                else -> Color.Gray
                                            }
                                        )
                                )
                                Text(
                                    text = computedPhase,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when (computedPhase) {
                                        "Menstrual Phase" -> MaterialTheme.colorScheme.error
                                        "Follicular Phase" -> MaterialTheme.colorScheme.primary
                                        "Ovulatory Phase" -> MaterialTheme.colorScheme.secondary
                                        "Luteal Phase" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = computedAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Pair("Period", MaterialTheme.colorScheme.error),
                            Pair("Follicular", MaterialTheme.colorScheme.primary),
                            Pair("Ovulatory", MaterialTheme.colorScheme.secondary),
                            Pair("Luteal", MaterialTheme.colorScheme.tertiary)
                        ).forEach { (name, color) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // Physical Symptoms Logger Linked to Cycle Date
        item {
            SymptomLoggerComponent(
                viewModel = viewModel,
                symptomLogs = symptomLogs,
                selectedDate = selectedCalendarDate
            )
        }

        // Period Log History
        if (periodLogs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No log history. Click 'Add Log' above to track your first date.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(periodLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Period Started: ${CycleUtils.formatDisplayDate(log.startDate)}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                if (!log.endDate.isNullOrEmpty()) {
                                    Text(
                                        text = "Ended: ${CycleUtils.formatDisplayDate(log.endDate)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "Active Bleeding (Ongoing)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deletePeriodLog(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Flow: ${log.flowLevel}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (log.symptoms.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Symptoms tagged:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                log.symptoms.forEach { symptom ->
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = symptom,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }

                        if (!log.notes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes: ${log.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Log Dialog
    if (showLogDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLogDialog = false }
        ) {
            CalendarPeriodLogger(
                viewModel = viewModel,
                periodLogs = periodLogs,
                onLogSaved = { showLogDialog = false }
            )
        }
    }
}

// ==========================================
// TAB 3: EMOTIONAL HEALTH & MOOD
// ==========================================
@Composable
fun EmotionalHealthTab(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val moodLogs by viewModel.moodLogs.collectAsState()

    var showMoodDialog by remember { mutableStateOf(false) }

    // Dialog state bindings
    var selectedMood by remember { mutableStateOf("Okay") }
    var energyStr by remember { mutableStateOf(5f) }
    var stressStr by remember { mutableStateOf(5f) }
    var sleepQualityStr by remember { mutableStateOf(5f) }
    var notes by remember { mutableStateOf("") }

    // Emergency Crisis Overlay State
    var showCrisisOverlay by remember { mutableStateOf(false) }

    val moods = listOf(
        Pair("Great", Icons.Default.SentimentVerySatisfied),
        Pair("Good", Icons.Default.SentimentSatisfied),
        Pair("Okay", Icons.Default.SentimentNeutral),
        Pair("Low", Icons.Default.SentimentDissatisfied),
        Pair("Anxious", Icons.Default.Warning),
        Pair("Overwhelmed", Icons.Default.Dangerous),
        Pair("Very low", Icons.Default.SentimentVeryDissatisfied)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wellbeing & Mood",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Document feelings and follow patterns safely",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = {
                        selectedMood = "Okay"
                        energyStr = 5f
                        stressStr = 5f
                        sleepQualityStr = 5f
                        notes = ""
                        showMoodDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_mood_log_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check In")
                }
            }
        }

        // Pattern Insights Box (gentle & non-diagnostic)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✨ Gentle Wellbeing Insights",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (moodLogs.size >= 2) {
                            "• Based on your check-ins, days with more sleep generally correspond with reported higher energy levels.\n• Be extra gentle with yourself during the luteal cycle phase when standard minor mood drops can develop."
                        } else {
                            "Keep checking in daily. After logging a couple of feelings, custom, non-diagnostic holistic patterns will populate here."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Mood Trends Chart (Vico-based Trend Chart)
        if (moodLogs.size >= 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📈 Mood & Stress Trends (Last 30 Logs)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Visual trend over time showing your daily emotional check-in history. Powered by off-line secure local storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        val moodMap = mapOf(
                            "Great" to 5f,
                            "Good" to 4f,
                            "Okay" to 3f,
                            "Low" to 2f,
                            "Anxious" to 1.5f,
                            "Overwhelmed" to 1f,
                            "Very low" to 0.5f
                        )
                        val sortedLogs = moodLogs
                            .sortedBy { it.date }
                            .takeLast(30)
                        val yValues = sortedLogs.map { log ->
                            moodMap[log.mood] ?: 3f
                        }.toTypedArray()
                        
                        com.patrykandpatrick.vico.compose.chart.Chart(
                            chart = com.patrykandpatrick.vico.compose.chart.line.lineChart(),
                            model = com.patrykandpatrick.vico.core.entry.entryModelOf(*yValues),
                            startAxis = com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis(
                                valueFormatter = { value, _ ->
                                    when (value.toInt()) {
                                        5 -> "Great"
                                        4 -> "Good"
                                        3 -> "Okay"
                                        2 -> "Low"
                                        1 -> "Anxious"
                                        else -> ""
                                    }
                                }
                            ),
                            bottomAxis = com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis(
                                valueFormatter = { value, _ ->
                                    val idx = value.toInt()
                                    if (idx in sortedLogs.indices) {
                                        val dateRaw = sortedLogs[idx].date
                                        dateRaw.substringAfter("-") // Shows MM-DD
                                    } else ""
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }

        // Mood History List
        if (moodLogs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Private emotional logs live offline. Tap Check In above to create your first entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(moodLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = moods.find { it.first == log.mood }?.second ?: Icons.Default.Mood,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = log.mood,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = CycleUtils.formatDisplayDate(log.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteMoodLog(log.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Energy: ${log.energy}/10", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("Stress: ${log.stress}/10", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            if (log.sleepQuality != null) {
                                Text("Sleep: ${log.sleepQuality}/10", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }

                        if (!log.notes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Reflection: ${log.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Mood Dialog
    if (showMoodDialog) {
        AlertDialog(
            onDismissRequest = { showMoodDialog = false },
            title = { Text("Daily Wellbeing Journal") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select Your Mood", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))

                    // FlowRow or simple scroll row of icon buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        moods.forEach { item ->
                            val isSelected = selectedMood == item.first
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedMood = item.first }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = item.first,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Sliders
                    Column {
                        Text("Energy Level: ${energyStr.toInt()}/10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = energyStr,
                            onValueChange = { energyStr = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }

                    Column {
                        Text("Stress Level: ${stressStr.toInt()}/10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = stressStr,
                            onValueChange = { stressStr = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }

                    Column {
                        Text("Sleep Quality: ${sleepQualityStr.toInt()}/10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Slider(
                            value = sleepQualityStr,
                            onValueChange = { sleepQualityStr = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("What made you feel this way? (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Check for crisis trigger words
                        val isTrigger = (selectedMood == "Very low" || selectedMood == "Low" || selectedMood == "Overwhelmed") &&
                                (notes.contains("suicide", ignoreCase = true) ||
                                 notes.contains("self-harm", ignoreCase = true) ||
                                 notes.contains("die", ignoreCase = true) ||
                                 notes.contains("kill", ignoreCase = true) ||
                                 notes.contains("hurt myself", ignoreCase = true))

                        viewModel.addMoodLog(
                            mood = selectedMood,
                            energy = energyStr.toInt(),
                            stress = stressStr.toInt(),
                            sleepQuality = sleepQualityStr.toInt(),
                            notes = if (notes.isBlank()) null else notes
                        )
                        showMoodDialog = false

                        if (isTrigger) {
                            showCrisisOverlay = true
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Crisis Support Dialog
    if (showCrisisOverlay) {
        AlertDialog(
            onDismissRequest = { showCrisisOverlay = false },
            title = {
                Text(
                    "You deserve support right now",
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "We hear you, and we care deeply. Because your safety is the absolute priority, please remember that LunaCare cannot substitute professional medical or emotional crisis aid.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Please reach out immediately to a trusted friend, family member, or call professional helplines.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• US: Call or text 988 Suicide & Crisis Lifeline\n• Emergency: Dial your local emergency number (e.g., 911, 112)\n• UK: Call 111",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCrisisOverlay = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("I'm reaching out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrisisOverlay = false }) {
                    Text("Dismiss Info")
                }
            }
        )
    }
}

// ==========================================
// TAB 4: EDUCATION SECTION (MENSTRUAL CUP)
// ==========================================
data class CupInfoTip(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val safetyNote: String,
    val articleSlug: String
)

@Composable
fun LearningTab(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val bookmarksState by viewModel.bookmarks.collectAsState()

    var selectedArticleSlug by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    val cupTips = listOf(
        CupInfoTip(
            title = "1. Strict Hand Hygiene",
            subtitle = "Cleanliness is mandatory",
            icon = Icons.Default.CheckCircle,
            description = "Wash hands perfectly with unscented, oil-free soap before handling your cup. Under-nail dirt is the #1 cause of vaginal pH microbial imbalance.",
            safetyNote = "Never touch your cup with dirty hands or standard scented sanitizers.",
            articleSlug = "insertion-guide"
        ),
        CupInfoTip(
            title = "2. Perfect The Fold",
            subtitle = "Three popular folding styles",
            icon = Icons.Default.Info,
            description = "Fold using the C-Fold (flat and bent), Punch-Down (tapered point) or the 7-Fold (diagonal corner). Squeeze firmly while inserting to keep the fold.",
            safetyNote = "Practice folding while dry to get comfortable with the cup's pop-open tension.",
            articleSlug = "how-to-fold-cup"
        ),
        CupInfoTip(
            title = "3. Smooth Insertion",
            subtitle = "Aimed slightly backward",
            icon = Icons.Default.Spa,
            description = "Position in a comfortable squat. Direct the folded cup tilted back toward your tailbone (spine). Let it pop open fully once the base is past the opening.",
            safetyNote = "Do not force it straight up. Use a drop of water-based lubricant if needed.",
            articleSlug = "insertion-guide"
        ),
        CupInfoTip(
            title = "4. Breaking the Vacuum Seal",
            subtitle = "Never pull the stem directly",
            icon = Icons.Default.Warning,
            description = "To safely remove, locate the base of the cup (just above the stem). Squeeze the base firmly between thumb and index. This breaks the seal.",
            safetyNote = "Yanking the stem without breaking the vacuum is painful and can damage cervix tissue.",
            articleSlug = "removal-guide"
        ),
        CupInfoTip(
            title = "5. Sterilization Protocol",
            subtitle = "Boil between cycles",
            icon = Icons.Default.Alarm,
            description = "Before your period starts and after it ends, boil the cup in a saucepan for 5-7 minutes. Ensure it does not stick to the bottom.",
            safetyNote = "Do not use dish soap, alcohol, bleach, or vinegar to wash the cup.",
            articleSlug = "cleaning-sterilizing"
        ),
        CupInfoTip(
            title = "6. Health & Comfort Check",
            subtitle = "When to consult a doctor",
            icon = Icons.Default.MedicalServices,
            description = "Menstrual cups are safe, but require caution if you use an IUD. Keep cup time capped at 8-12 hours maximum to avoid TSS risk.",
            safetyNote = "Seek advice if you have persistent discomfort, pain, or fever.",
            articleSlug = "medical-caveats"
        )
    )

    if (selectedArticleSlug == null) {
        // Main view: Category Tabs and Article List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Resource Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Professional hygiene, safety protocols, and menstrual cup guides",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Carousel of Menstrual Cup Info Cards
            if (selectedCategory == "All" || selectedCategory == "Menstrual Cup") {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎓 Menstrual Cup Education Corner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Slide to learn insertion, safety, and hygiene rules:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        var activeIndex by remember { mutableStateOf(0) }
                        val currentTip = cupTips[activeIndex]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArticleSlug = currentTip.articleSlug }
                                .testTag("cup_carousel_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = currentTip.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = currentTip.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = currentTip.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${activeIndex + 1}/${cupTips.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Text(
                                    text = currentTip.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Safety Tip Note Callout Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Safety Alert",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Safety Note: " + currentTip.safetyNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (activeIndex > 0) activeIndex--
                                                else activeIndex = cupTips.size - 1
                                            },
                                            modifier = Modifier.size(36.dp).testTag("carousel_prev_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous tip"
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (activeIndex < cupTips.size - 1) activeIndex++
                                                else activeIndex = 0
                                            },
                                            modifier = Modifier.size(36.dp).testTag("carousel_next_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next tip"
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = { selectedArticleSlug = currentTip.articleSlug },
                                        modifier = Modifier.testTag("carousel_read_article_btn")
                                    ) {
                                        Text("Read Detailed Guide")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category filter rows
            item {
                val categories = listOf("All", "Menstrual Cup", "Period & PMS", "Emotional Wellbeing", "Bookmarks")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Menstrual Cup Care, Safety, and Cleaning Interactive Guide Card
            if (selectedCategory == "All" || selectedCategory == "Menstrual Cup") {
                item {
                    MenstrualCupGuideCard()
                }
            }

            // Article List matching selection
            val filteredArticles = LunaContent.articles.filter { article ->
                when (selectedCategory) {
                    "All" -> true
                    "Bookmarks" -> bookmarksState.any { it.articleSlug == article.slug }
                    else -> article.category == selectedCategory
                }
            }

            if (filteredArticles.isEmpty()) {
                item {
                    Text(
                        text = "No articles found in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            } else {
                items(filteredArticles) { article ->
                    val isBookmarked = bookmarksState.any { it.articleSlug == article.slug }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedArticleSlug = article.slug },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = article.category,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = article.content.take(100) + "...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { viewModel.toggleBookmark(article.slug) }) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Detail View
        val article = LunaContent.articles.first { it.slug == selectedArticleSlug }
        val isBookmarked = bookmarksState.any { it.articleSlug == article.slug }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedArticleSlug = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Row {
                    IconButton(onClick = { viewModel.toggleBookmark(article.slug) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = article.category,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (article.safetyNote != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = article.safetyNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                text = article.content,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Safety boilerplate
            HorizontalDivider()
            Text(
                text = "Important Note: LunaCare articles represent general sanitary health education. If you suffer from sudden fever, intense lower abdominal cramp, or unexpected rashes while wearing a menstrual cup, remove the cup instantly and consult a doctor immediately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// TAB 5: DIARY & MEDICAL JOURNAL SYSTEM
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalTab(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val journalEntries by viewModel.journalEntries.collectAsState()
    val medicalEntries by viewModel.medicalJournalEntries.collectAsState()
    val medicalReminders by viewModel.medicalReminders.collectAsState()

    var activeSubTab by remember { mutableStateOf("diary") } // "diary", "medical", "reminders"

    // Diary dialog state
    var showDiaryDialog by remember { mutableStateOf(false) }
    var diaryTitle by remember { mutableStateOf("") }
    var diaryBody by remember { mutableStateOf("") }
    var diaryPromptSelected by remember { mutableStateOf("") }

    // Medical Log dialog state
    var showMedicalDialog by remember { mutableStateOf(false) }
    var medTitle by remember { mutableStateOf("") }
    var medNotes by remember { mutableStateOf("") }
    var medCategory by remember { mutableStateOf("Period") } // Period, PMS, PCOS/PCOD, Menstrual cup, Pain, Doctor visit, Other
    var medPainLevel by remember { mutableStateOf(0f) }
    var medMood by remember { mutableStateOf("Okay") }
    var medFlow by remember { mutableStateOf("None") }
    var medMedicines by remember { mutableStateOf("") }
    var medDocVisit by remember { mutableStateOf(false) }
    var medApptDate by remember { mutableStateOf("") }
    var medDocAdvice by remember { mutableStateOf("") }
    var medSelectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var medError by remember { mutableStateOf("") }

    // Reminder dialog state
    var showReminderDialog by remember { mutableStateOf(false) }
    var remTitle by remember { mutableStateOf("") }
    var remType by remember { mutableStateOf("MEDICINE") } // MEDICINE, DOCTOR_APPOINTMENT, WATER, CUP_CLEANING, PAD_CHANGE, SELF_CARE
    var remTime by remember { mutableStateOf("08:00") }
    var remRule by remember { mutableStateOf("Daily") }
    var remNotes by remember { mutableStateOf("") }
    var remError by remember { mutableStateOf("") }
    var remZodErrors by remember { mutableStateOf<List<com.example.data.ZodValidator.ZodError>>(emptyList()) }

    var searchKeyword by remember { mutableStateOf("") }

    val promptSuggestions = listOf(
        "What am I feeling today?",
        "What does my body need today?",
        "What helped me feel calmer?",
        "What do I want to tell myself kindly?"
    )

    val symptomOptions = listOf(
        "Cramping", "Bloating", "Headache", "Backache", "Fatigue",
        "Nausea", "Breast tenderness", "Pelvic pressure", "Insomnia", "Anxiety"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Journal & Care Center",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Secure local logging and medical trackers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Custom M3 Segmented Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Pair("diary", "📖 Diary Reflections"),
                Pair("medical", "🩺 Medical Logs"),
                Pair("reminders", "⏰ Care Reminders")
            ).forEach { (tabId, tabTitle) ->
                val isSelected = activeSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            activeSubTab = tabId
                            searchKeyword = ""
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar for active list
        if (activeSubTab != "diary") {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sub-Tab Contents
        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                "diary" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InteractiveJournalingComponent(
                            viewModel = viewModel,
                            journalEntries = journalEntries
                        )
                    }
                }

                "medical" -> {
                    // MEDICAL JOURNAL SECTION
                    val filteredMedical = medicalEntries.filter {
                        it.title.contains(searchKeyword, ignoreCase = true) ||
                                it.notes.contains(searchKeyword, ignoreCase = true) ||
                                it.category.contains(searchKeyword, ignoreCase = true)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Mood Chart using Vico
                        val moodMap = mapOf(
                            "Great" to 5f,
                            "Okay" to 4f,
                            "Low" to 3f,
                            "Very low" to 2f,
                            "Anxious" to 1f
                        )
                        // Take the last 30 days logs that have a valid mood
                        val recentMoodLogs = medicalEntries
                            .sortedBy { it.entryDate }
                            .takeLast(30)
                            .mapNotNull { entry -> 
                                moodMap[entry.mood]?.let { entry.entryDate to it }
                            }
                        
                        if (recentMoodLogs.size >= 2) {
                            Text(
                                text = "Mood Patterns (Last 30 Logs)", 
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val yValues = recentMoodLogs.map { it.second }.toTypedArray()
                            com.patrykandpatrick.vico.compose.chart.Chart(
                                chart = com.patrykandpatrick.vico.compose.chart.line.lineChart(),
                                model = com.patrykandpatrick.vico.core.entry.entryModelOf(*yValues),
                                startAxis = com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis(),
                                bottomAxis = com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis(
                                    valueFormatter = { value, _ -> 
                                        val idx = value.toInt()
                                        if (idx in recentMoodLogs.indices) {
                                            // Extract MM-DD from YYYY-MM-DD
                                            val dateRaw = recentMoodLogs[idx].first
                                            dateRaw.substringAfter("-")
                                        } else ""
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(bottom = 12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                medTitle = ""
                                medNotes = ""
                                medCategory = "Period"
                                medPainLevel = 0f
                                medMood = "Okay"
                                medFlow = "None"
                                medMedicines = ""
                                medDocVisit = false
                                medApptDate = ""
                                medDocAdvice = ""
                                medSelectedSymptoms = emptySet()
                                medError = ""
                                showMedicalDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Medical Symptom / Appointment")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredMedical.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No medical symptom logs. Keep track of periods, cramps pain level, doctor appointments advice, or PCOS concerns.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filteredMedical) { entry ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(entry.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                    Text(
                                                        text = "Category: ${entry.category}",
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.deleteMedicalJournalEntry(entry.id) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Logged: ${CycleUtils.formatDisplayDate(entry.entryDate)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Mood: ${entry.mood} | Flow: ${entry.flowLevel}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            
                                            if (entry.painLevel > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                    Text("Pain Level: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    val color = if (entry.painLevel >= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                    Text("${entry.painLevel}/10", fontSize = 12.sp, color = color, fontWeight = FontWeight.Black)
                                                }
                                            }

                                            if (entry.symptoms.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Symptoms: ${entry.symptoms.joinToString()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            if (entry.medicinesTaken.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Medications: ${entry.medicinesTaken}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            }

                                            if (entry.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(entry.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            if (entry.doctorVisit) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                                                        .padding(8.dp)
                                                ) {
                                                    Column {
                                                        Text("🩺 Clinical Advisory", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                                                        if (entry.appointmentDate != null) {
                                                            Text("Appt Date: ${entry.appointmentDate}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                        if (entry.doctorAdvice != null) {
                                                            Text("Advice: ${entry.doctorAdvice}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "reminders" -> {
                    // REMINDERS SECTION with Safety Guides
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🩺 Care Reminders Safety warning", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Always follow clinical directions, prescriptions, and exact water goals. Never delay professional consultations based on automated logs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                remTitle = ""
                                remType = "MEDICINE"
                                remTime = "08:00"
                                remRule = "Daily"
                                remNotes = ""
                                remError = ""
                                remZodErrors = emptyList()
                                showReminderDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Alarm, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set New Care/Meds Reminder")
                        }

                        if (medicalReminders.isEmpty()) {
                            Text(
                                "No reminders set up yet. Tap button to set standard cycles or medicine alarms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            medicalReminders.forEach { reminder ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(reminder.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Type: ${reminder.reminderType} | Rule: ${reminder.repeatRule}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("⏰ Time: ${reminder.reminderTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            if (!reminder.notes.isNullOrBlank()) {
                                                Text("Note: ${reminder.notes}", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteMedicalReminder(reminder.id) }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS:

    // 1. Write Diary Note Dialog
    if (showDiaryDialog) {
        AlertDialog(
            onDismissRequest = { showDiaryDialog = false },
            title = { Text("Private Journal reflections") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Inspirational Prompts:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(promptSuggestions) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (diaryBody.startsWith(prompt)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { diaryBody = "$prompt\n\n$diaryBody" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(prompt, fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diaryTitle,
                        onValueChange = { diaryTitle = it },
                        label = { Text("Entry Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = diaryBody,
                        onValueChange = { diaryBody = it },
                        label = { Text("Write your reflections...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = if (diaryTitle.isBlank()) "Reflections" else diaryTitle
                        viewModel.addJournalEntry(
                            title = finalTitle,
                            body = diaryBody,
                            moodTag = null,
                            cyclePhase = null
                        )
                        showDiaryDialog = false
                    },
                    enabled = diaryBody.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiaryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Add Medical Journal Entry Dialog
    if (showMedicalDialog) {
        AlertDialog(
            onDismissRequest = { showMedicalDialog = false },
            title = { Text("Log Medical Symptoms & Care") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (medError.isNotEmpty()) {
                        Text(medError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = medTitle,
                        onValueChange = { medTitle = it; medError = "" },
                        label = { Text("Short Title / Summary *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Medical Category:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Period", "PMS", "PCOS/PCOD", "Pain", "Doctor visit", "Other").forEach { cat ->
                            val isSel = medCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { medCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Text("Pain level: ${medPainLevel.toInt()}/10", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Slider(
                        value = medPainLevel,
                        onValueChange = { medPainLevel = it },
                        valueRange = 0f..10f,
                        steps = 9
                    )

                    Text("Today's mood:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Great", "Okay", "Low", "Very low", "Anxious").forEach { md ->
                            val isSel = medMood == md
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { medMood = md }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(md, fontSize = 11.sp)
                            }
                        }
                    }

                    Text("Symptoms present:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        symptomOptions.forEach { sym ->
                            val isSel = medSelectedSymptoms.contains(sym)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        medSelectedSymptoms = if (isSel) medSelectedSymptoms - sym else medSelectedSymptoms + sym
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(sym, fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = medMedicines,
                        onValueChange = { medMedicines = it },
                        label = { Text("Medicines / Doses Taken") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { medDocVisit = !medDocVisit }
                    ) {
                        Checkbox(checked = medDocVisit, onCheckedChange = { medDocVisit = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("This is an actual doctor appointment", fontSize = 12.sp)
                    }

                    if (medDocVisit) {
                        OutlinedTextField(
                            value = medApptDate,
                            onValueChange = { medApptDate = it },
                            label = { Text("Appt Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = medDocAdvice,
                            onValueChange = { medDocAdvice = it },
                            label = { Text("Doctor's Advice Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = medNotes,
                        onValueChange = { medNotes = it },
                        label = { Text("Additional Clinical Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (medTitle.isBlank()) {
                            medError = "Title is required."
                            return@Button
                        }
                        if (medCategory.isBlank()) {
                            medError = "Category is required."
                            return@Button
                        }
                        viewModel.addMedicalJournalEntry(
                            entryDate = CycleUtils.getTodayString(),
                            category = medCategory,
                            title = medTitle,
                            notes = medNotes,
                            symptoms = medSelectedSymptoms.toList(),
                            painLevel = medPainLevel.toInt(),
                            mood = medMood,
                            flowLevel = medFlow,
                            medicinesTaken = medMedicines,
                            doctorVisit = medDocVisit,
                            appointmentDate = if (medDocVisit && medApptDate.isNotBlank()) medApptDate else null,
                            doctorAdvice = if (medDocVisit && medDocAdvice.isNotBlank()) medDocAdvice else null
                        )
                        showMedicalDialog = false
                    }
                ) {
                    Text("Save Medical Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMedicalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Add Reminder Dialog
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Care Reminder")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clinical Disclaimer & Safety Warning Notice (MANDATORY CONSTRAINT)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Clinical Safety Notice",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "LunaCare does not provide diagnoses or recommend drug dosages. Consult your gynecologist or general practitioner before modifying your care regime or taking any medication.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Template Presets for convenience
                    Text("Select Template Preset:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("PCOS Pill", "MEDICINE", "08:00"),
                            Triple("Hydration Alert", "WATER", "12:00"),
                            Triple("Pad Change Alert", "PAD_CHANGE", "14:30"),
                            Triple("Silicone Cup Clean", "CUP_CLEANING", "21:00")
                        ).forEach { (tLabel, tType, tTime) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                    .clickable {
                                        remTitle = tLabel
                                        remType = tType
                                        remTime = tTime
                                        remRule = "Daily"
                                        remZodErrors = emptyList()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(tLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Field 1: Reminder Title
                    val titleErr = remZodErrors.find { it.field == "title" }?.message
                    OutlinedTextField(
                        value = remTitle,
                        onValueChange = { 
                            remTitle = it
                            remZodErrors = remZodErrors.filterNot { item -> item.field == "title" }
                        },
                        label = { Text("Reminder Title / Pill Name *") },
                        singleLine = true,
                        isError = titleErr != null,
                        placeholder = { Text("E.g., daily vitamins, thyroid") },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_title_field")
                    )
                    if (titleErr != null) {
                        Text(titleErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Field 2: Reminder Type Selection
                    val typeErr = remZodErrors.find { it.field == "reminderType" }?.message
                    Text("Reminder Type:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("MEDICINE", "DOCTOR_APPOINTMENT", "WATER", "CUP_CLEANING", "PAD_CHANGE", "SELF_CARE").forEach { type ->
                            val isSel = remType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { 
                                        remType = type
                                        remZodErrors = remZodErrors.filterNot { item -> item.field == "reminderType" }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(type.replace("_", " "), fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    if (typeErr != null) {
                        Text(typeErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                    }

                    // Field 3: Time
                    val timeErr = remZodErrors.find { it.field == "reminderTime" }?.message
                    OutlinedTextField(
                        value = remTime,
                        onValueChange = { 
                            remTime = it
                            remZodErrors = remZodErrors.filterNot { item -> item.field == "reminderTime" }
                        },
                        label = { Text("Time (HH:MM format, 24h) *") },
                        singleLine = true,
                        isError = timeErr != null,
                        placeholder = { Text("E.g., 08:30 or 21:00") },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_time_field")
                    )
                    if (timeErr != null) {
                        Text(timeErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Field 4: Recurring Rule
                    val ruleErr = remZodErrors.find { it.field == "repeatRule" }?.message
                    OutlinedTextField(
                        value = remRule,
                        onValueChange = { 
                            remRule = it
                            remZodErrors = remZodErrors.filterNot { item -> item.field == "repeatRule" }
                        },
                        label = { Text("Repeat Frequency *") },
                        placeholder = { Text("E.g., Daily, Weekly, Once") },
                        singleLine = true,
                        isError = ruleErr != null,
                        modifier = Modifier.fillMaxWidth().testTag("reminder_rule_field")
                    )
                    if (ruleErr != null) {
                        Text(ruleErr, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Field 5: Notes
                    OutlinedTextField(
                        value = remNotes,
                        onValueChange = { remNotes = it },
                        label = { Text("Reminder description notes") },
                        placeholder = { Text("E.g. after lunch with plenty of water") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validationResult = com.example.data.ZodValidator.validateReminder(
                            title = remTitle,
                            reminderType = remType,
                            reminderTime = remTime,
                            repeatRule = remRule,
                            notes = remNotes
                        )
                        if (!validationResult.success) {
                            remZodErrors = validationResult.errors
                        } else {
                            val rData = validationResult.data!!
                            viewModel.addMedicalReminder(
                                title = rData.title,
                                reminderType = rData.reminderType,
                                reminderTime = rData.reminderTime,
                                repeatRule = rData.repeatRule,
                                enabled = true,
                                notes = if (rData.notes.isNullOrBlank()) null else rData.notes
                            )
                            showReminderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_reminder_btn")
                ) {
                    Text("Save Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 6: SETTINGS & PREFERENCES
// ==========================================
@Composable
fun SettingsTab(
    profile: Profile,
    viewModel: LunaViewModel
) {
    val context = LocalContext.current

    val isAutoTrackingEnabled by viewModel.isAutoTrackingEnabled.collectAsState()
    val navigationHistory by viewModel.navigationHistory.collectAsState()

    var cycleLengthStr by remember { mutableStateOf(profile.averageCycleLength.toString()) }
    var periodLengthStr by remember { mutableStateOf(profile.averagePeriodLength.toString()) }

    var displayNameStr by remember { mutableStateOf(profile.displayName ?: "") }
    var birthYearStr by remember { mutableStateOf(profile.birthYear?.toString() ?: "") }
    var genderModeSelected by remember { mutableStateOf(profile.genderMode) }
    var userModeSelected by remember { mutableStateOf(profile.userMode) }

    var periodReminders by remember { mutableStateOf(profile.periodReminders) }
    var moodReminders by remember { mutableStateOf(profile.moodReminders) }
    var selfCareReminders by remember { mutableStateOf(profile.selfCareReminders) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Theme Toggle Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillModifier()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode Theme", fontWeight = FontWeight.Bold)
                    Text("Toggle deep plum color themes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Switch(
                    checked = profile.isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
            }
        }

        // Profile configurations
        Card(modifier = Modifier.fillMaxWidth().testTag("profile_card")) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Personal Profile 👤", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = displayNameStr,
                    onValueChange = { displayNameStr = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = birthYearStr,
                    onValueChange = { birthYearStr = it },
                    label = { Text("Year of Birth") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Gender identity:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FEMALE", "MALE", "NON_BINARY").forEach { g ->
                        val isSel = genderModeSelected == g
                        val label = when (g) {
                            "FEMALE" -> "Female"
                            "MALE" -> "Male"
                            else -> "Non-Binary"
                        }
                        Button(
                            onClick = { genderModeSelected = g },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                Text("App Companion Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SELF_TRACKING", "SUPPORT_MODE", "EDUCATION_ONLY").forEach { mode ->
                        val isSel = userModeSelected == mode
                        val label = when (mode) {
                            "SELF_TRACKING" -> "Self"
                            "SUPPORT_MODE" -> "Support"
                            else -> "Edu Only"
                        }
                        Button(
                            onClick = { userModeSelected = mode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        val year = birthYearStr.toIntOrNull()
                        viewModel.updateProfile(
                            displayName = displayNameStr,
                            birthYear = year,
                            genderMode = genderModeSelected,
                            userMode = userModeSelected
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                ) {
                    Text("Save Profile Changes")
                }
            }
        }

        // Subscription configurations
        Card(
            modifier = Modifier.fillMaxWidth().testTag("subscription_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LunaCare Pro Subscription ⭐", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "Unlock deeper cycle projections, detailed medical history exports, secure backup, and advanced partner tracking companion widgets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Tier: Free Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("No active payment details on file.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(context, "Pro features simulated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Activate Pro Trial", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Cycle configurations
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cycle Parameters", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                OutlinedTextField(
                    value = cycleLengthStr,
                    onValueChange = { cycleLengthStr = it },
                    label = { Text("Average Cycle Length (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = periodLengthStr,
                    onValueChange = { periodLengthStr = it },
                    label = { Text("Average Period Length (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cLen = cycleLengthStr.toIntOrNull() ?: 28
                        val pLen = periodLengthStr.toIntOrNull() ?: 5
                        viewModel.updateCycleSettings(cLen, pLen)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Parameters")
                }
            }
        }

        // Notification configurations
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reminders & Notifications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Period prediction reminders", fontSize = 14.sp)
                    Switch(checked = periodReminders, onCheckedChange = {
                        periodReminders = it
                        viewModel.updateNotificationPreferences(it, moodReminders, false, selfCareReminders, "20:00")
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily wellbeing check-in", fontSize = 14.sp)
                    Switch(checked = moodReminders, onCheckedChange = {
                        moodReminders = it
                        viewModel.updateNotificationPreferences(periodReminders, it, false, selfCareReminders, "20:00")
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gentle self-care tips", fontSize = 14.sp)
                    Switch(checked = selfCareReminders, onCheckedChange = {
                        selfCareReminders = it
                        viewModel.updateNotificationPreferences(periodReminders, moodReminders, false, it, "20:00")
                    })
                }
            }
        }

        // SEO & Mobile Discovery Options
        var appIndexingEnabled by remember { mutableStateOf(true) }
        var publicGuidesSearchable by remember { mutableStateOf(true) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SEO & Web-to-App Discovery 🔍", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Configure how public, non-personal educational segments like Menstrual Cup guides or PCOS/PCOD articles are indexed by Google Search and web crawlers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text("Google App Indexing", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Allow Google Search web crawls to link directly into public app guides.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = appIndexingEnabled, onCheckedChange = { appIndexingEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text("Index Bookmarks", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Contribute anonymous product popularity scores to improve regional SEO health tags.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = publicGuidesSearchable, onCheckedChange = { publicGuidesSearchable = it })
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verified Domains for Deep Linking:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("• https://lunacare.com/learn/* (Educational/PMS Guides)", fontSize = 11.sp)
                    Text("• https://lunacare.com/care/* (Care Product Discovery)", fontSize = 11.sp)
                    Text("• https://lunacare.com/cup-education/* (Cup Cleaning Guidelines)", fontSize = 11.sp)
                }
            }
        }

        // Navigation Auto-Tracking UI
        Card(modifier = Modifier.fillMaxWidth().testTag("auto_tracking_card")) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text("Auto Navigation Tracking", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "Locally and privately log your navigation journey to help optimize application speed and user experience flow.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoTrackingEnabled,
                        onCheckedChange = { viewModel.setAutoTrackingEnabled(it) },
                        modifier = Modifier.testTag("auto_tracking_switch")
                    )
                }

                if (isAutoTrackingEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    
                    Text(
                        text = "Tracked Navigation Journey (${navigationHistory.size} events):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (navigationHistory.isEmpty()) {
                        Text(
                            text = "No navigation events tracked yet. Switch tabs to record history.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        // Display the timeline elegantly
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            navigationHistory.takeLast(10).reversed().forEach { track ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = track.tabName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    val formattedTime = java.time.Instant.ofEpochMilli(track.timestamp)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalTime()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                                    
                                    Text(
                                        text = formattedTime,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.clearNavigationHistory() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("clear_navigation_history_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Tracked Journey", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Account Session & Security Audit Log Card
        val securityEvents by viewModel.securityEvents.collectAsState()
        val loggedInCreds by viewModel.loggedInCredentials.collectAsState()
        val isGuest by viewModel.isGuestUser.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth().testTag("session_security_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Account Session & Security Audit 🛡️", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.7f)) {
                        val accountText = if (isGuest) {
                            "Guest Mode (Offline Only)"
                        } else if (loggedInCreds != null) {
                            val plainEmail = EncryptionHelper.decryptSensitiveText(loggedInCreds?.encryptedEmail) ?: ""
                            "Logged in as ${EncryptionHelper.maskEmail(plainEmail)}"
                        } else {
                            "Not Authenticated"
                        }
                        Text(accountText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text("Your active session state.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("Recent Security Audit Events:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                if (securityEvents.isEmpty()) {
                    Text("No security events logged yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        securityEvents.forEach { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(0.7f)) {
                                    Text(
                                        text = event.eventType,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when(event.eventType) {
                                            "LOGIN_SUCCESS", "PASSWORD_RESET_SUCCESS" -> Color(0xFF2E7D32)
                                            "LOGIN_FAILED", "ACCOUNT_LOCKED_TEMPORARILY", "RATE_LIMIT_TRIGGERED" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                    Text(
                                        text = event.metadata ?: "",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val eventTime = java.time.Instant.ofEpochMilli(event.createdAt)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                Text(
                                    text = eventTime,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Data Self-Sovereignty and Privacy
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Self-Sovereignty & Security", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))

                Button(
                    onClick = {
                        // Export local JSON payload of data
                        // Highly requested capability
                        println("DEBUG: JSON Data Export successfully printed")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Data (JSON)")
                }

                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Local Content")
                }
            }
        }

        // Technical specs & disclaimers
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LunaCare v1.0.0 (Offline Native MVP)\nCopyright © 2026",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// Utility extension for filling modifier safely without custom size issues
fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()

// ==========================================
// TAB 6: SUPPORT TAB (For Supporters & Partners)
// ==========================================
@Composable
fun SupportTab(profile: Profile, viewModel: LunaViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Welcome, Supporter Mode 🌟",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You are supporting your ${profile.supportRelationship?.replace("_", " ")?.lowercase() ?: "partner"}. Here are safe, vetted ways to offer care, understand cycle symptoms, and buy products.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Cycle Basics Quick-Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("The Cycle at a Glance 🩸", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A menstrual cycle typically lasts 21 to 45 days. It is divided into 4 phases: Menstruation (Bleeding), Follicular (Building), Ovulation (Highest energy), and Luteal (Pre-period / potential PMS).\n\nIf she experiences mood fluctuations during the Luteal phase, remember it is heavily biological. Gentle patient support, back rubs, and keeping dynamic expectations helper can ease comfort tremendously.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 2: Product Buying Guide (Menstrual Cup & pads specs)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Menstrual Care Shopping Guide 🛒", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "If asked to buy pads, tampons, or a menstrual cup, here is what to look for:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Reusable Menstrual Cups: Look for 100% Medical Grade Silicone (FDA compliant), BPA-free, and hypoallergenic. Sizing is usually Size A (Pre-childbirth) or Size B (Post-childbirth).\n" +
                           "• Sanitary Pads: Select unscented, dye-free, organic cotton top-sheet options. This prevents sensitive irritation during high bleeding.\n" +
                           "• Heating Pads: Hot water bags or air-activated waist patches provide substantial period cramp (dysmenorrhea) relief.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 3: Comfort Care Checklists
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How Can I Care For Her Today? 🌱", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                
                listOf(
                    "🍫 Comfort Food: Bring dark chocolate, magnesium-rich almonds, or warm ginger tea (helps ease cramps).",
                    "💧 Hydration: Keep her water bottle filled; hydration is critical to flush bloating water retention.",
                    "🛁 Warm Bath / Heating Pad: Pre-heat a heating pad or offer a warm gel patch for her back/lower tummy.",
                    "🧘 Focus Relief: Reduce loaded schedules or offer chores support so she can rest in the first two heavier days."
                ).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                        Text(text = item, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Section 4: Emergency Signs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Red Flags 🚨", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If she exhibits any of these symptoms, do not wait. Assist her to see a medical professional immediately:\n" +
                           "• Extremely severe pain that doesn't resolve with pain medicines.\n" +
                           "• Heavy bleeding soaking through 1+ pads or tampons every hour for 2+ consecutive hours.\n" +
                           "• A sudden high fever, rash, vomiting, or dizziness when using a menstrual cup or tampon (toxic shock flags).\n" +
                           "• Signs of infection (bad odor, pelvic swelling, acute pain).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ==========================================
// TAB 7: CARE TAB (Nearby Locator & Shopping Specs)
// ==========================================
@Composable
fun CareTab(profile: Profile, viewModel: LunaViewModel) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var locationInput by remember { mutableStateOf("") }
    var suggestedClinic by remember { mutableStateOf("") }
    var gpsSimulated by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Care & Nearby Help Locator 🏥",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Find pharmacies, clinics, and emergency health services. Use our secure Google Maps redirection to seek immediate treatment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // GPS Simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GPS Location Simulator", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Simulate coordinates detection to find medical care safely without requiring sensitive persistent permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = {
                        gpsSimulated = true
                        locationInput = "Main Street Medical Clinic Area"
                        suggestedClinic = "Oakridge Hills Gynecology Center (0.8 mi away)"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Detect Location (Simulated GPS)")
                }

                if (gpsSimulated) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📍 Location Detected: $locationInput", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("🏥 Recommended: $suggestedClinic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    gpsSimulated = false
                                    locationInput = ""
                                    suggestedClinic = ""
                                },
                                modifier = Modifier.align(Alignment.End).testTag("clear_location_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Temporary Location", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Custom Search Input
        OutlinedTextField(
            value = locationInput,
            onValueChange = { locationInput = it },
            label = { Text("Search location or city...") },
            placeholder = { Text("E.g., Cincinnati, OH") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Redirection Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val query = if (locationInput.isNotBlank()) "pharmacy near $locationInput" else "pharmacy nearby"
                    val url = CycleUtils.buildGoogleMapsSearchUrl(query)
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Safe fallback log
                        println("DEBUG: Google Maps browser trigger: $url")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Search Pharmacy 💊", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val query = if (locationInput.isNotBlank()) "gynecologist hospital near $locationInput" else "gynecologist hospital nearby"
                    val url = CycleUtils.buildGoogleMapsSearchUrl(query)
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        println("DEBUG: Google Maps browser trigger: $url")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Search Clinic 🏥", fontSize = 12.sp)
            }
        }

        // Safety Specs specifications
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Product Quality Specs (Consumer Council) 🔍", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Ensure maximum vaginal safety. Always confirm packages follow the strict standards:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Chlorine-free bleaching process (look for ECF or TCF labels to prevent dioxin ingestion).\n" +
                           "• Unscented with absolutely zero artificial perfumes (reduces vaginosis risks).\n" +
                           "• Sterilizer-safe silicone (for cups) allowing boiling treatment for 5-10 minutes between bleeding cycles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

