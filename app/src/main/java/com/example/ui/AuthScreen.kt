package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EncryptionHelper
import com.example.viewmodel.LunaViewModel

enum class AuthMode {
    LOGIN, SIGNUP, FORGOT_PASSWORD
}

@Composable
fun AuthScreen(viewModel: LunaViewModel) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    
    // Inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    
    // UI state
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    
    // Status states
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val captchaRequiredState by viewModel.captchaRequired.collectAsState()
    
    // Local CAPTCHA slider verified state
    var captchaVerified by remember { mutableStateOf(false) }
    
    // Reset/Forgot password success message
    var successNotification by remember { mutableStateOf<String?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()

    // Recalculate captchaVerified if captchaRequired state is false
    LaunchedEffect(captchaRequiredState) {
        if (!captchaRequiredState) {
            captchaVerified = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7)) // LunaCare Surface Background
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Vector Header crescent moon icon
            CelestialHeader()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "LunaCare",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Your holistic, secure companion for intimate well-being",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Render specific forms depending on mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when(authMode) {
                            AuthMode.LOGIN -> "Welcome Back"
                            AuthMode.SIGNUP -> "Create Account"
                            AuthMode.FORGOT_PASSWORD -> "Reset Access"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Display security notification alerts
                    if (loginError != null) {
                        SecurityErrorCard(message = loginError ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (inputError != null) {
                        SecurityErrorCard(message = inputError ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (successNotification != null) {
                        SuccessCard(message = successNotification ?: "")
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // --- EMAIL INPUT ---
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            inputError = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (authMode == AuthMode.SIGNUP) {
                        // --- DISPLAY NAME INPUT (Signup only) ---
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { 
                                displayName = it
                                inputError = null
                            },
                            label = { Text("Your Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (authMode != AuthMode.FORGOT_PASSWORD) {
                        // --- PASSWORD INPUT ---
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                inputError = null
                            },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        
                        if (authMode == AuthMode.SIGNUP) {
                            // Password strength meter
                            PasswordStrengthMeter(password = password, email = email)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // --- CONFIRM PASSWORD INPUT ---
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    inputError = null
                                },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                        Icon(
                                            imageVector = if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Slide-to-Verify CAPTCHA if required
                    if (captchaRequiredState && !captchaVerified) {
                        CaptchaSlider(onVerified = { captchaVerified = true })
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // --- ACTION BUTTON ---
                    Button(
                        onClick = {
                            inputError = null
                            successNotification = null
                            
                            // 1. Check captcha if required
                            if (captchaRequiredState && !captchaVerified) {
                                inputError = "Security verification is required. Please slide the slider."
                                return@Button
                            }
                            
                            when (authMode) {
                                AuthMode.LOGIN -> {
                                    if (email.isEmpty() || password.isEmpty()) {
                                        inputError = "Email and password are required."
                                    } else {
                                        viewModel.login(email, password) { success ->
                                            if (!success) {
                                                captchaVerified = false // reset captcha on fail
                                            }
                                        }
                                    }
                                }
                                AuthMode.SIGNUP -> {
                                    viewModel.register(email, password, confirmPassword, displayName) { success, err ->
                                        if (!success) {
                                            inputError = err ?: "Registration failed."
                                            captchaVerified = false
                                        }
                                    }
                                }
                                AuthMode.FORGOT_PASSWORD -> {
                                    if (email.isEmpty()) {
                                        inputError = "Please enter your email."
                                    } else {
                                        viewModel.forgotPassword(email) { msg ->
                                            successNotification = msg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_action_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when(authMode) {
                                    AuthMode.LOGIN -> "Login Securely"
                                    AuthMode.SIGNUP -> "Create My Account"
                                    AuthMode.FORGOT_PASSWORD -> "Send Verification Link"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Switch modes & Forgot password text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (authMode == AuthMode.LOGIN) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { authMode = AuthMode.FORGOT_PASSWORD }
                                    .testTag("forgot_password_link")
                            )
                            
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clickable { authMode = AuthMode.SIGNUP }
                                    .testTag("sign_up_link")
                            )
                        } else if (authMode == AuthMode.SIGNUP) {
                            Text(
                                text = "Already have an account? Login",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { authMode = AuthMode.LOGIN }
                                    .testTag("login_link")
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Back to Login",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { authMode = AuthMode.LOGIN }
                                    .testTag("back_to_login_link")
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Guest mode / Continue offline companion button
            OutlinedButton(
                onClick = { viewModel.setGuestMode(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("guest_mode_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue Offline as Guest",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy Informational Card
            SecurityInfoCard()
        }
    }
}

@Composable
fun CelestialHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val primaryColor = Color(0xFF8A4D4E)
            val accentColor = Color(0xFFD48C8C)
            
            // radial gradient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = size.width / 1.8f
                ),
                radius = size.width / 1.8f
            )
            
            val moonCenter = center
            val moonRadius = size.width / 4f
            
            drawCircle(
                color = primaryColor.copy(alpha = 0.85f),
                radius = moonRadius,
                center = moonCenter
            )
            
            drawCircle(
                color = Color(0xFFFFF8F7), // Overlapping cream circle
                radius = moonRadius,
                center = moonCenter.copy(x = moonCenter.x - (moonRadius * 0.45f), y = moonCenter.y - (moonRadius * 0.15f))
            )
            
            // Small twinkling stars
            drawCircle(
                color = accentColor,
                radius = 3.dp.toPx(),
                center = moonCenter.copy(x = moonCenter.x + (moonRadius * 1.2f), y = moonCenter.y - (moonRadius * 0.4f))
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.6f),
                radius = 2.dp.toPx(),
                center = moonCenter.copy(x = moonCenter.x + (moonRadius * 0.8f), y = moonCenter.y + (moonRadius * 1.1f))
            )
        }
    }
}

@Composable
fun SecurityErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun SuccessCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun PasswordStrengthMeter(password: String, email: String) {
    if (password.isEmpty()) return
    
    val strength = remember(password, email) {
        when {
            password.length < 12 -> "Too Short (Min 12)"
            password == email -> "Matches Email (Weak)"
            listOf("password123", "password1234", "1234567890", "qwertyuiop", "lunacare2026", "lunacare123").contains(password.lowercase()) -> "Common Password (Weak)"
            password.length in 12..15 -> "Okay"
            else -> "Strong Passphrase"
        }
    }
    
    val color = when (strength) {
        "Too Short (Min 12)", "Matches Email (Weak)", "Common Password (Weak)" -> MaterialTheme.colorScheme.error
        "Okay" -> Color(0xFFFBC02D)
        else -> Color(0xFF2E7D32)
    }
    
    val progress = when (strength) {
        "Too Short (Min 12)", "Matches Email (Weak)", "Common Password (Weak)" -> 0.25f
        "Okay" -> 0.6f
        else -> 1f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strength,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun CaptchaSlider(onVerified: () -> Unit) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var verified by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Slide to confirm you are human",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFEBE9)),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Guide Text
                Text(
                    text = if (verified) "Verification Confirmed ✓" else "Slide to verify ➔",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (verified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                // Slide handle simulator
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        if (!verified) {
                            sliderPosition = it
                            if (it >= 0.95f) {
                                verified = true
                                sliderPosition = 1f
                                onVerified()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("captcha_slider"),
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LunaCare Production-Grade Security",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "LunaCare uses secure authentication, rate limits, encrypted private information, and monitored access controls. Your password is never stored in plain text.\n\n" +
                       "• Passwords are strongly hashed using PBKDF2 Hmac-SHA256 with unique salts.\n" +
                       "• Private health data is encrypted on-device with AES-GCM; keys never leave your secure volatile memory session.\n" +
                       "• Progressive anti-brute force protection and CAPTCHA escalation protect your account from automated attacks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = 16.sp
            )
        }
    }
}
