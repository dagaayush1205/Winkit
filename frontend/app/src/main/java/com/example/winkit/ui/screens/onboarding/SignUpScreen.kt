package com.example.winkit.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.winkit.data.AuthResult
import com.example.winkit.data.SupabaseAuthHelper
import kotlinx.coroutines.launch

// ── SIGN UP STEPS ─────────────────────────────────────────────────────────
private enum class SignUpStep { DETAILS, OTP_VERIFY }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: (workerId: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var step by remember { mutableStateOf(SignUpStep.DETAILS) }
    var partnerId by remember { mutableStateOf("") } // <-- NEW
    var emailError by remember { mutableStateOf(false) } // <-- NEW
    // Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("") }

    // OTP state
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {

        // ── TOP GRADIENT ──────────────────────────────────────────────────
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1A0A3B), Color(0xFF5B2D8E))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(gradientBrush)
                .padding(32.dp)
                .systemBarsPadding()
        ) {
            Column {
                // Logo badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeliveryDining,
                        contentDescription = null,
                        tint = Color(0xFF00E5A0),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (step == SignUpStep.DETAILS) "Create Your\nWinkIT Account" else "Verify\nYour Number",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (step == SignUpStep.DETAILS)
                        "Join thousands of delivery riders getting instant payouts."
                    else
                        "We sent OTP to +91 $phone",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // ── MAIN CARD ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "signup_step"
            ) { currentStep ->
                when (currentStep) {
                    SignUpStep.DETAILS -> SignUpDetailsView(
                        name = name, onNameChange = { name = it },
                        partnerId = partnerId, onPartnerIdChange = { partnerId = it }, // <--- ADD THIS COMMA AT THE END!
                        phone = phone, onPhoneChange = { if (it.length <= 10 && it.all(Char::isDigit)) phone = it },
                        email = email, onEmailChange = { email = it },
                        selectedGender = selectedGender, onGenderChange = { selectedGender = it },
                        selectedPlatform = selectedPlatform, onPlatformChange = { selectedPlatform = it },
                        isLoading = isLoading,
                        onContinue = {
                            // 1. Phone Validation
                            if (phone.length != 10) {
                              Toast.makeText(context, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                              return@SignUpDetailsView
                            }
                            // 2. Email Validation (if they typed one)
                            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                              Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                              return@SignUpDetailsView
                            }
                            // 3. Mandatory Fields Check
                            if (name.isBlank() || partnerId.isBlank() || selectedGender.isBlank() || selectedPlatform.isBlank()) {
                              Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                               return@SignUpDetailsView
                            }
    
                            // If everything is perfect, move to OTP!
                            step = SignUpStep.OTP_VERIFY
                        },
                        onNavigateToLogin = onNavigateToLogin
                    )
                    SignUpStep.OTP_VERIFY -> SignUpOtpView(
                        phone = phone,
                        otpCode = otpCode,
                        onOtpChange = { if (it.length <= 6 && it.all(Char::isDigit)) otpCode = it },
                        isLoading = isLoading,
                        onBack = { step = SignUpStep.DETAILS; otpCode = "" },
                        onVerify = {
                            if (!SupabaseAuthHelper.verifyOtp(otpCode)) {
                                Toast.makeText(context, "Invalid OTP. Use 123456 for demo.", Toast.LENGTH_SHORT).show()
                                return@SignUpOtpView
                            }
                            isLoading = true
                            coroutineScope.launch {
                                val result = SupabaseAuthHelper.signUp(
                                    name = name,
                                    phone = phone,
                                    gender = selectedGender,
                                    email = email,
                                    partnerId = partnerId 
                                )
                                isLoading = false
                                when (result) {
                                    is AuthResult.Success -> onSignUpSuccess(result.workerId)
                                    is AuthResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── DETAILS FORM ──────────────────────────────────────────────────────────
@Composable
private fun SignUpDetailsView(
    name: String, onNameChange: (String) -> Unit,
    partnerId: String, onPartnerIdChange: (String) -> Unit, 
    phone: String, onPhoneChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    selectedGender: String, onGenderChange: (String) -> Unit,
    selectedPlatform: String, onPlatformChange: (String) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // ── Full Name ──────────────────────────────────────────────────────
        SignUpLabel("FULL NAME *")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("e.g. Rahul Kumar", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF5B2D8E)) },
            singleLine = true,
            colors = winkitTextFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))
        
        SignUpLabel("DELIVERY PARTNER ID *")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = partnerId,
            onValueChange = { onPartnerIdChange(it.uppercase()) }, // Force uppercase like ZEP-1001
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("e.g. ZEP-1001 or BKT-8822", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color(0xFF5B2D8E)) },
            singleLine = true,
            colors = winkitTextFieldColors()
        )
        Spacer(modifier = Modifier.height(14.dp))
        // ── Phone ──────────────────────────────────────────────────────────
        SignUpLabel("PHONE NUMBER *")
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.height(56.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🇮🇳 +91", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("98765 43210", color = Color.LightGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = winkitTextFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Email (optional) ───────────────────────────────────────────────
        SignUpLabel("EMAIL (optional)")
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("rahul@email.com", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF5B2D8E)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = winkitTextFieldColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Gender ─────────────────────────────────────────────────────────
        SignUpLabel("GENDER *")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Male", "Female", "Other").forEach { g ->
                GenderChip(
                    label = g,
                    isSelected = selectedGender == g,
                    onClick = { onGenderChange(g) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Platform ───────────────────────────────────────────────────────
        SignUpLabel("YOUR DELIVERY PLATFORM *")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Blinkit" to Color(0xFFF8CB46), "Zepto" to Color(0xFFFF3269)).forEach { (platform, color) ->
                PlatformChip(
                    label = platform,
                    color = color,
                    isSelected = selectedPlatform == platform,
                    onClick = { onPlatformChange(platform) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Continue Button ────────────────────────────────────────────────
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B2D8E))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Already have account ───────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = Color.Gray, fontSize = 13.sp)
            Text(
                "Login",
                color = Color(0xFF5B2D8E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── OTP VERIFY ─────────────────────────────────────────────────────────────
@Composable
private fun SignUpOtpView(
    phone: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {

        // Demo hint banner
        Surface(
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Demo OTP: 123456",
                    color = Color(0xFF1B5E20),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SignUpLabel("ENTER OTP SENT TO +91 $phone")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("● ● ● ● ● ●", color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp, letterSpacing = 10.sp),
            colors = winkitTextFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "← Edit number",
            color = Color(0xFF5B2D8E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onVerify,
            enabled = otpCode.length == 6 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B2D8E),
                disabledContainerColor = Color.LightGray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Verify & Create Account", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── REUSABLE COMPONENTS ───────────────────────────────────────────────────

@Composable
private fun SignUpLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
}

@Composable
private fun GenderChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF5B2D8E) else Color(0xFFF5F5F5),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (isSelected) Color.White else Color.DarkGray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PlatformChip(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else Color(0xFFE0E0E0)
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                label,
                color = if (isSelected) color else Color.DarkGray,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun winkitTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF5B2D8E),
    unfocusedBorderColor = Color(0xFFE0E0E0),
    cursorColor = Color(0xFF5B2D8E)
)
