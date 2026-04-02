package com.example.winkit.ui.screens.profile

import android.util.Log
import android.widget.Toast
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
            Toast.makeText(context, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { ShiftSafeBottomNav(navController = navController) },
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
                Text(text = "Rider ID: $workerId", color = Color.Gray, fontSize = 14.sp)

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
                            Text("Trust Score", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

                // Editable Fields
                ProfileField("Full Name", name, Icons.Default.Person) { name = it }
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField("Email Address", email, Icons.Default.Email) { email = it }
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField("Phone Number", phone, Icons.Default.Phone) { phone = it }

                Spacer(modifier = Modifier.height(40.dp))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                val updates = mapOf(
                                    "name" to name,
                                    "email" to email,
                                    "phone" to phone
                                )
                                NetworkModule.api.updateWorker(
                                    workerId = "eq.$workerId",
                                    workerUpdate = updates
                                )
                                Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("ProfileUpdate", "Error: ${e.message}")
                                Toast.makeText(context, "Update Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileField(label: String, value: String, icon: ImageVector, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(icon, null, tint = Color(0xFF5B2D8E)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF5B2D8E)
            )
        )
    }
}