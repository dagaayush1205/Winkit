package com.example.winkit.ui.screens.chatbot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.winkit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─── Data ────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val role: String,   // "user" | "model"
    val content: String
)

// ─── Singleton HTTP client ────────────────────────────────────────────────────

private val httpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

// ─── System prompt with Strict Project Guardrails ─────────────────────────────

private const val SYSTEM_PROMPT =
    """
You are WinkAI, the technical co-pilot for Winkit. You have perfect recall of the app's workflow and internal logic.

## MANDATORY GUARDRAIL: PROJECT SCOPE ONLY
- Reject any questions not related to the Winkit codebase, math, or app workflow.
- Standard Rejection: "I'm specialized only in the Winkit platform. I cannot assist with unrelated queries."

## APP WORKFLOW & USER JOURNEY
1. ONBOARDING:
- SignUpScreen (OTP) -> PlatformIntegrationScreen (Blinkit/Zepto ID check).
- RiskProfiling: User selects primary Dark Store and preferred shifts.
- PolicyCheckoutScreen: Displays calculated premium; features "AI Analyzing" animation and Swipe-to-Pay.

2. MAIN DASHBOARD:
- ShiftSafeDashboard: Displays live risk metrics (Wet Bulb, Rain Prob, AQI) and H3-mapped hazard zones.
- HazardReportCard: Allows manual trigger of GPS micro-batching for localized claims.

3. UNICORN TRIGGER (Relocation):
- Triggered when a Dark Store goes offline.
- Workflow: Query nearby hubs -> NetworkX A* Road Graph calculation -> Show relocation route on map -> User clicks "Accept Paid Transit" to receive a relocation bonus.

4. PAYOUT PIPELINE:
- event_evaluator.py (15-min cron) checks risk thresholds (>= 0.65).
- claim enters ESCROW -> Fraud Fortress (LocationFraudTracker.kt) validates GPS telemetry.
- If clean: Razorpay Route API triggers UPI disbursement.

## CORE MATH & TECH
- β (risk multiplier) = 1.0 + U_weather + F_risk.
- H3 Resolution 9 hexes for geospatial precision.
- Tech: Kotlin, Compose, FastAPI, Supabase, Gemini 2.5 Flash (Civic Risk Agent).

## BEHAVIOR
- If asked "How does the app work?", explain the flow from Onboarding to Payout.
- If asked about a specific screen, reference its technical role in the journey.
- Maintain a temperature of 0.2 for strict factual adherence.
"""
// ─── Chatbot Sheet composable ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinkitChatbotSheet(
    onDismiss: () -> Unit
) {
    val Brand    = Color(0xFF5B2D8E)
    val BrandBg  = Color(0xFFF3EEF8)
    val AiBubble = Color(0xFFF3F4F8)
    val UserBg   = Color(0xFF5B2D8E)

    val scope          = rememberCoroutineScope()
    val listState      = rememberLazyListState()
    var inputText      by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "model",
                content = "WinkAI online. I am synced with the Winkit codebase and risk engines. How can I help with the project today?"
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    suspend fun callGeminiApi(history: List<ChatMessage>): String {
        return withContext(Dispatchers.IO) {
            val contentsArray = JSONArray()
            history.forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }

            val body = JSONObject().apply {
                put("contents", contentsArray)
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONObject().put("text", SYSTEM_PROMPT.trimIndent()))
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 1024)
                    put("temperature", 0.2) // Lower temperature for stricter adherence
                })
            }.toString()

            val apiKey = BuildConfig.GEMINI_API_KEY
            // If using the standard 1.5 Flash (most reliable):
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                val errJson = runCatching { JSONObject(responseBody).optString("error", responseBody) }.getOrElse { responseBody }
                throw Exception("API Error: $errJson")
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isLoading) return

        messages.add(ChatMessage(role = "user", content = trimmed))
        inputText = ""
        isLoading = true

        scope.launch {
            try {
                val reply = callGeminiApi(messages.toList())
                messages.add(ChatMessage(role = "model", content = reply))
            } catch (e: Exception) {
                messages.add(ChatMessage(role = "model", content = "Connection Error: ${e.message}"))
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(BrandBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SmartToy, null, tint = Brand, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("WinkAI", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Project-only Technical Assistant", fontSize = 11.sp, color = Color.Gray)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(BrandBg).align(Alignment.Bottom), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, null, tint = Brand, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, if (isUser) 16.dp else 4.dp, if (isUser) 4.dp else 16.dp),
                            color = if (isUser) UserBg else AiBubble,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(msg.content, fontSize = 13.sp, color = if (isUser) Color.White else Color.Black, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                if (isLoading) {
                    item { TypingDots(Modifier.padding(start = 36.dp)) }
                }
            }

            // Input
            HorizontalDivider(thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about Winkit logic...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(inputText) })
                )
                IconButton(
                    onClick = { send(inputText) },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier.background(if(inputText.isBlank()) Color.LightGray else Brand, CircleShape)
                ) {
                    Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500, delayMillis = index * 150), RepeatMode.Reverse),
                label = ""
            )
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
        }
    }
}

@Composable
fun WinkitChatFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, containerColor = Color(0xFF5B2D8E), contentColor = Color.White) {
        Icon(Icons.Default.SmartToy, "WinkAI")
    }
}