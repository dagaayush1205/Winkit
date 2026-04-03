package com.example.winkit.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import com.example.winkit.utils.tr

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (workerId: String) -> Unit,
    onNavigateToSignUp: () -> Unit = {}
) {
    var isOtpSent by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    // workerId resolved after Supabase phone lookup
    var resolvedWorkerId by remember { mutableStateOf("") }
    var resolvedName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {

        // ── TOP GRADIENT ──────────────────────────────────────────────────
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A2A59), Color(0xFF006C7A))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(gradientBrush)
                .padding(32.dp)
                .systemBarsPadding()
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "WinkIT\nInstant Payout in a WINK",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = tr("Enter your registered number to get started with your active delivery coverage"),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // ── BOTTOM CARD ───────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AnimatedContent(
                targetState = isOtpSent,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "login_transition"
            ) { otpSent ->
                if (!otpSent) {
                    LoginPhoneView(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { if (it.length <= 10 && it.all(Char::isDigit)) phoneNumber = it },
                        isLoading = isLoading,
                        onSendOtp = {
                            if (phoneNumber.length == 10) {
                                isLoading = true
                                coroutineScope.launch {
                                    // Look up the phone in Supabase Workers table
                                    val result = SupabaseAuthHelper.loginWithPhone(phoneNumber)
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            resolvedWorkerId = result.workerId
                                            resolvedName = result.name
                                            isOtpSent = true
                                        }
                                        is AuthResult.Error -> {
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        onNavigateToSignUp = onNavigateToSignUp,
                        onTermsClick = { showTermsDialog = true }
                    )
                } else {
                    LoginOtpView(
                        phoneNumber = phoneNumber,
                        otpCode = otpCode,
                        onOtpChange = { if (it.length <= 6 && it.all(Char::isDigit)) otpCode = it },
                        isLoading = isLoading,
                        onEditPhone = { isOtpSent = false; otpCode = "" },
                        onVerify = {
                            if (otpCode.length == 6) {
                                if (SupabaseAuthHelper.verifyOtp(otpCode)) {
                                    com.example.winkit.utils.AuthManager.saveWorkerId(context, resolvedWorkerId) 
                                    onLoginSuccess(resolvedWorkerId)
                                } else {
                                    Toast.makeText(context, tr("Invalid OTP. Use 123456 for demo."), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
        if (showTermsDialog) {
            TermsDialog(onAccept = { showTermsDialog = false })
        }
    }
}

// ── PHONE INPUT VIEW ──────────────────────────────────────────────────────
@Composable
private fun LoginPhoneView(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onSendOtp: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onTermsClick: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {

        Text("PHONE NUMBER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.height(56.dp).wrapContentWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🇮🇳 +91", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                placeholder = { Text("98765 43210", color = Color.LightGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF0A2A59)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSendOtp,
            enabled = phoneNumber.length == 10 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A2A59),
                disabledContainerColor = Color.LightGray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(tr("Send OTP"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Sign Up Link ───────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text(tr("New here? "), color = Color.DarkGray, fontSize = 13.sp)
                Text(
                    tr("Sign Up"),
                    color = Color(0xFF0A2A59),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

          Text(
                text = tr("Terms of Service  •  Privacy Policy"), 
                color = Color(0xFF0A2A59), // Made it blue so it looks clickable
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onTermsClick() }
            )
        }
    }
}

// ── OTP INPUT VIEW ────────────────────────────────────────────────────────
@Composable
private fun LoginOtpView(
    phoneNumber: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onEditPhone: () -> Unit,
    onVerify: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {

        // Demo hint
        Surface(
            color = Color(0xFFFFF8E1),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Demo OTP: 123456", color = Color(0xFF5D4037), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("ENTER OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text(tr("Enter 6-digit OTP"), color = Color.LightGray) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF0A2A59)
            ),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                letterSpacing = 8.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Sent to +91 $phoneNumber ", color = Color.Gray, fontSize = 12.sp)
            Text(
                tr("Edit"),
                color = Color(0xFF0A2A59),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onEditPhone() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            enabled = otpCode.length == 6 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A2A59),
                disabledContainerColor = Color.LightGray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(tr("Verify & Login"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TermsDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAccept,
        containerColor = Color.White,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0A2A59))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Terms & Privacy Policy"), fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E), fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text(tr("1. User Agreement"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("By logging in, you agree to WinkIT's platform terms, including GPS telemetry tracking for parametric insurance verification."), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(tr("2. Data Privacy"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("We securely store your phone number and location data exclusively for processing automated risk payouts and fraud prevention."), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(tr("3. Financial Consent"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("You authorize WinkIT to process premium deductions and route automated claim payouts directly to your linked wallet."), fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A2A59)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(tr("I Accept & Continue"), fontWeight = FontWeight.Bold)
            }
        }
    )
}
