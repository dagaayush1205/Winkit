package com.example.winkit.data


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── RESULT WRAPPERS ────────────────────────────────────────────────────────
sealed class AuthResult {
    data class Success(val workerId: String, val name: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// ── SUPABASE AUTH HELPER ───────────────────────────────────────────────────
object SupabaseAuthHelper {

    private const val DEMO_OTP = "123456"

    // Known hex that exists in h3_zone_states to satisfy the FK constraint
    private const val DEFAULT_HEX = "8961892a03bffff"

    // ── SIGN UP ────────────────────────────────────────────────────────────
    suspend fun signUp(
        name: String,
        phone: String,
        gender: String, // "Male" | "Female" | "Other" — matches public.Gender
        email: String,
        platform: String // "Blinkit" or "Zepto"
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val prefix = if (platform == "Zepto") "ZEP" else "BKT"
            val suffix = phone.takeLast(4)
            val workerId = "$prefix-$suffix"

// ── IMPORTANT: enum value must exactly match your Postgres enums ──
// public.rider_status — use "active" (check your enum with the SQL below)
// public.Gender — "Male" / "Female" / "Other"
//
// To verify, run in Supabase SQL editor:
// SELECT unnest(enum_range(NULL::rider_status));
// SELECT unnest(enum_range(NULL::"Gender"));

            val newWorker = SupabaseWorkerInsert(
                worker_id = workerId,
                name = name,
                phone = phone,
                aadhar_hash = "DEMO_HASH_${phone.takeLast(4)}",
                gender = gender, // "Male", "Female" matches your DB perfectly
                primary_h3_hex = DEFAULT_HEX,
                trust_score = 100, // You might want to start them at 100 instead of 1!
                status = "ACTIVE", // <--- CHANGED THIS TO UPPERCASE
                access = true,
                email = email.ifBlank { null }
            )

            Log.d("SupabaseAuth", "Inserting worker: $workerId | gender=$gender | status=active")
            NetworkModule.api.insertWorker(newWorker)
            Log.d("SupabaseAuth", "✅ Sign up success: $workerId")
            AuthResult.Success(workerId, name)

        } catch (e: Exception) {
            Log.e("SupabaseAuth", "❌ Sign up failed: ${e.message}", e)

            val msg = when {
                e.message?.contains("409") == true ||
                        e.message?.contains("duplicate") == true ||
                        e.message?.contains("unique") == true ->
                    "This phone number is already registered. Please login."

                e.message?.contains("violates foreign key") == true ->
                    "Setup error: default zone not found. Contact support."

                e.message?.contains("invalid input value for enum") == true ->
                    "Signup failed: enum mismatch — check rider_status/Gender values in Supabase. Error: ${e.message}"

                e.message?.contains("scheme") == true ||
                        e.message?.contains("URL") == true ->
                    "Network error: bad Supabase URL. Check local.properties. Error: ${e.message}"

                else -> "Sign up failed: ${e.message}"
            }
            AuthResult.Error(msg)
        }
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────
    suspend fun loginWithPhone(phone: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val results = NetworkModule.api.getWorkerByPhone(phone = "eq.$phone")
            if (results.isEmpty()) {
                return@withContext AuthResult.Error(
                    "Phone number not registered. Please sign up first."
                )
            }
            val worker = results.first()
// Check suspended — handle both capitalisation variants just in case
            if (worker.status?.lowercase() == "suspended") {
                return@withContext AuthResult.Error(
                    "Your account has been suspended. Contact support."
                )
            }
            Log.d("SupabaseAuth", "✅ Login: ${worker.worker_id}")
            AuthResult.Success(worker.worker_id, worker.name ?: "Rider")

        } catch (e: Exception) {
            Log.e("SupabaseAuth", "❌ Login failed: ${e.message}", e)
            AuthResult.Error("Login failed: ${e.message}")
        }
    }

    // ── OTP VERIFY (demo: hardcoded 123456) ───────────────────────────────
    fun verifyOtp(enteredOtp: String): Boolean = enteredOtp == DEMO_OTP
}
