package com.example.winkit.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── RESULT WRAPPER ─────────────────────────────────────────────────────────
sealed class AuthResult {
    data class Success(val workerId: String, val name: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// ── SUPABASE AUTH HELPER ───────────────────────────────────────────────────
object SupabaseAuthHelper {

    private const val DEMO_OTP = "123456"
    private const val DEFAULT_HEX = "8961892a03bffff"

    // ── SIGN UP ────────────────────────────────────────────────────────────
    suspend fun signUp(
        name: String,
        phone: String,
        gender: String,
        email: String,
        partnerId: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val workerId = partnerId

            val newWorker = SupabaseWorkerInsert(
                worker_id = workerId,
                name = name,
                phone = phone,
                aadhar_hash = "DEMO_HASH_${phone.takeLast(4)}",
                gender = gender,
                primary_h3_hex = DEFAULT_HEX,
                trust_score = 100,
                status = "ACTIVE",
                access = true,
                email = email.ifBlank { null }
            )

            NetworkModule.api.insertWorker(newWorker)

            Log.d("Auth", "✅ Signup success: $workerId")
            AuthResult.Success(workerId, name)

        } catch (e: Exception) {
            Log.e("Auth", "❌ Signup failed: ${e.message}", e)
            val msg = when {
                e.message?.contains("duplicate") == true -> "User already exists. Please login."
                e.message?.contains("foreign key") == true -> "Setup error: default zone missing."
                else -> "Signup failed: ${e.message}"
            }
            AuthResult.Error(msg)
        }
    }
suspend fun fileManualClaim(
        workerId: String, lat: Double, lng: Double, hazardType: String, description: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val claim = ManualClaimInsert(workerId, lat, lng, hazardType, description)
            NetworkModule.api.insertManualClaim(claim)
            true
        } catch (e: Exception) {
            Log.e("Claims", "Manual claim failed: ${e.message}")
            false
        }
    }
    // ── LOGIN ──────────────────────────────────────────────────────────────
    suspend fun loginWithPhone(phone: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val results = NetworkModule.api.getWorkerByPhone("eq.$phone")

            if (results.isEmpty()) {
                return@withContext AuthResult.Error("User not found. Please sign up.")
            }

            val worker = results.first()

            if (worker.status?.lowercase() == "suspended") {
                return@withContext AuthResult.Error("Account suspended.")
            }

            AuthResult.Success(worker.worker_id, worker.name ?: "Rider")

        } catch (e: Exception) {
            AuthResult.Error("Login failed: ${e.message}")
        }
    }

    // ── OTP VERIFY ─────────────────────────────────────────────────────────
    fun verifyOtp(enteredOtp: String): Boolean {
        return enteredOtp == DEMO_OTP
    }

    // ── CHECK POLICY ───────────────────────────────────────────────────────
    suspend fun hasActivePolicy(workerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val policies = NetworkModule.api.getActivePolicies(
                workerId = "eq.$workerId",
                status = "eq.ACTIVE"
            )
            policies.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // ── ACTIVATE FIRST POLICY (FIXED RAW PAYLOAD) ─────────────────────────
// ── ACTIVATE FIRST POLICY ──────────────────────────────────────────────
    suspend fun activateFirstPolicy(
        workerId: String,
        premium: Double = 49.0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val policyId = "POL-${System.currentTimeMillis().toString().takeLast(4)}"

            // Calculate Dates
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val startDate = formatter.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val endDate = formatter.format(cal.time)

            // Use the strict Data Class
            val policy = WeeklyPolicyInsert(
                policy_id = policyId,
                worker_id = workerId,
                week_start_date = startDate,
                week_end_date = endDate,
                premium_paid = premium,
                max_daily_coverage = 800.0,
                status = "ACTIVE"
            )

            // Send via standard endpoint
            NetworkModule.api.insertWeeklyPolicy(policy)
            Log.d("Policy", "✅ Activated first policy for $workerId")
            true
        } catch (e: retrofit2.HttpException) {
            // 🔥 THIS WILL TELL US EXACTLY WHY SUPABASE HATES IT
            Log.e("Policy", "❌ Supabase HTTP Error: ${e.response()?.errorBody()?.string()}")
            false
        } catch (e: Exception) {
            Log.e("Policy", "❌ Activation failed: ${e.message}")
            false
        }
    }

    // ── WALLET BALANCE (Universal Ledger) ──────────────────────────────────
    suspend fun getWalletBalance(workerId: String): Double = withContext(Dispatchers.IO) {
        try {
            // 1. Get ALL payouts
            val claims = NetworkModule.api.getWorkerClaims(workerId = "eq.$workerId")
            val totalPayouts = claims
                .filter { it.status == "AUTO_PAID" || it.status == "PAID" }
                .sumOf { it.payout_amt ?: 0.0 }

            // 2. Get ALL premiums
            val policies = NetworkModule.api.getPoliciesByWorker(workerId = "eq.$workerId")
            val totalPremiums = policies.sumOf { it.premium_paid ?: 0.0 }

            totalPayouts - totalPremiums
        } catch (e: Exception) {
            Log.e("Wallet", "Failed to calculate balance: ${e.message}")
            -49.0
        }
    }
}
