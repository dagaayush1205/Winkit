package com.example.winkit.ui.screens.profile

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.winkit.data.NetworkModule
import com.example.winkit.data.SupabaseWorker
import com.example.winkit.ui.components.ShiftSafeBottomNav
import com.example.winkit.utils.Translator // 🔥 Imported Language State
import com.example.winkit.utils.tr // 🔥 Imported Translation Wrapper
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(workerId: String, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var worker by remember { mutableStateOf<SupabaseWorker?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Editable states
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Fetch worker data on load
    LaunchedEffect(workerId) {
        try {
            val response = NetworkModule.api.getWorkerProfile(workerId = "eq.$workerId")
            if (response.isNotEmpty()) {
                val data = response.first()
                worker = data
                name = data.name ?: ""
                email = data.email ?: ""
                phone = data.phone ?: ""
            }
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Fetch error: ${e.message}")
            // 🔥 Notice how we extract the variable from the tr() block!
            Toast.makeText(context, tr("Update Failed") + ": ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF3F4F8)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF5B2D8E))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Profile Picture Area
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5B2D8E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (name.isNotEmpty()) name.take(1).uppercase() else "?",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 🔥 Extracted workerId variable outside the tr() function
                Text(text = tr("Rider ID:") + " $workerId", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                // Trust Score Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF00E5A0))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(tr("Trust Score"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${worker?.trust_score ?: 0}/100", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        val progressVal = (worker?.trust_score ?: 0).toFloat() / 100f
                        CircularProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF00E5A0),
                            strokeWidth = 3.dp,
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Editable Fields (Wrapped in tr)
                ProfileField(tr("Full Name"), name, Icons.Default.Person) { name = it }
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(tr("Email Address"), email, Icons.Default.Email) { email = it }
                Spacer(modifier = Modifier.height(16.dp))
                
                // 🔥 SECURITY: Lock the phone number field
                ProfileField(tr("Phone Number (Verified Identity)"), phone, Icons.Default.Phone, enabled = false) { }

                Spacer(modifier = Modifier.height(40.dp))

                // Save Button
                Button(
                    onClick = {
                        // Form Validation
                        if (name.isBlank()) {
                            Toast.makeText(context, tr("Name cannot be empty"), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(context, tr("Invalid email format"), Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            isSaving = true
                            try {
                                val updates = mapOf("name" to name, "email" to email)
                                NetworkModule.api.updateWorker(workerId = "eq.$workerId", workerUpdate = updates)
                                Toast.makeText(context, tr("Profile Updated Successfully"), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, tr("Update Failed"), Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },                    
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B2D8E)),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(tr("Save Changes"), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // ── LANGUAGE SELECTION CARD ──
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tr("Language"), 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A2E)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        // Top Row: English & Hindi
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { Translator.currentLang = "en" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (Translator.currentLang == "en") Color(0xFF5B2D8E) else Color(0xFFF3F4F8),
                                    contentColor = if (Translator.currentLang == "en") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("English", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { Translator.currentLang = "hi" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (Translator.currentLang == "hi") Color(0xFF00C48C) else Color(0xFFF3F4F8),
                                    contentColor = if (Translator.currentLang == "hi") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("हिंदी", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Row: Kannada & Tamil
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { Translator.currentLang = "kn" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (Translator.currentLang == "kn") Color(0xFFE91E63) else Color(0xFFF3F4F8),
                                    contentColor = if (Translator.currentLang == "kn") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("ಕನ್ನಡ", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { Translator.currentLang = "ta" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (Translator.currentLang == "ta") Color(0xFFFF9800) else Color(0xFFF3F4F8),
                                    contentColor = if (Translator.currentLang == "ta") Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("தமிழ்", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        com.example.winkit.utils.AuthManager.logout(context)
                        // 🔥 Reset language to English on logout so the next user isn't confused
                        Translator.currentLang = "en"
                        
                        navController.navigate("login") { 
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Log Out"), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileField(label: String, value: String, icon: ImageVector, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled, 
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(icon, null, tint = if (enabled) Color(0xFF5B2D8E) else Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF5B2D8E),
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledTextColor = Color.DarkGray
            )
        )
    }
}
