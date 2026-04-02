package com.example.winkit.data

import android.util.Log
import com.example.winkit.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────

data class SupabaseWorker(
    @SerializedName("worker_id")   val worker_id: String,
    @SerializedName("name")        val name: String?,
    @SerializedName("phone")       val phone: String?,
    @SerializedName("trust_score") val trust_score: Int?,
    @SerializedName("status")      val status: String?,
    @SerializedName("email")       val email: String?,
    @SerializedName("gender")      val gender: String?,
    @SerializedName("access")      val access: Boolean?
)

data class SupabaseWorkerInsert(
    @SerializedName("worker_id")      val worker_id: String,
    @SerializedName("name")           val name: String,
    @SerializedName("phone")          val phone: String,
    @SerializedName("aadhar_hash")    val aadhar_hash: String,
    @SerializedName("gender")         val gender: String,
    @SerializedName("primary_h3_hex") val primary_h3_hex: String,
    @SerializedName("trust_score")    val trust_score: Int,
    @SerializedName("status")         val status: String,
    @SerializedName("access")         val access: Boolean,
    @SerializedName("email")          val email: String?
)

data class SupabaseWeeklyPolicy(
    @SerializedName("policy_id")          val policy_id: String,
    @SerializedName("created_at")         val created_at: String,
    @SerializedName("worker_id")          val worker_id: String,
    @SerializedName("week_start_date")    val week_start_date: String,
    @SerializedName("week_end_date")      val week_end_date: String,
    @SerializedName("premium_paid")       val premium_paid: Double?,
    @SerializedName("max_daily_coverage") val max_daily_coverage: Double?,
    @SerializedName("status")             val status: String
)

data class WeeklyPolicyInsert(
    @SerializedName("policy_id")          val policy_id: String,
    @SerializedName("worker_id")          val worker_id: String,
    @SerializedName("week_start_date")    val week_start_date: String,
    @SerializedName("week_end_date")      val week_end_date: String,
    @SerializedName("premium_paid")       val premium_paid: Double,
    @SerializedName("max_daily_coverage") val max_daily_coverage: Double,
    @SerializedName("status")             val status: String
)

data class SupabaseTelemetryRow(
    @SerializedName("worker_id")            val worker_id: String,
    @SerializedName("latitude")             val latitude: Double,
    @SerializedName("longitude")            val longitude: Double,
    @SerializedName("speed_kmh")            val speed_kmh: Float,
    @SerializedName("is_mock_location")     val is_mock_location: Boolean,
    @SerializedName("dev_settings_enabled") val dev_settings_enabled: Boolean,
    @SerializedName("os_signature_valid")   val os_signature_valid: Boolean,
    @SerializedName("fraud_reason")         val fraud_reason: String = "PENDING_VERIFICATION"
)

data class WorkerChargeDto(
    @SerializedName("worker_id")   val worker_id: String? = null,
    @SerializedName("premium")     val premium: Double? = null,
    @SerializedName("hour_bucket") val hour_bucket: String? = null
)

data class SupabaseDailyActivity(
    @SerializedName("log_date")             val log_date: String?,
    @SerializedName("hours_online")         val hours_online: Double?,
    @SerializedName("deliveries_completed") val deliveries_completed: Int?,
    @SerializedName("daily_earnings")       val daily_earnings: Double?
)

data class SupabaseClaim(
    @SerializedName("claim_id")   val claim_id: String,
    @SerializedName("payout_amt") val payout_amt: Double?,
    @SerializedName("status")     val status: String?,
    @SerializedName("created_at") val created_at: String?
)

// ─────────────────────────────────────────────────────────────────────────
// API INTERFACE
// ─────────────────────────────────────────────────────────────────────────

interface SupabaseApiService {

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/Workers")
    suspend fun insertWorker(@Body worker: SupabaseWorkerInsert)

    @GET("rest/v1/Workers")
    suspend fun getWorkerByPhone(
        @Query("phone") phone: String,
        @Query("select") select: String = "worker_id,name,phone,status,trust_score,email,gender,access"
    ): List<SupabaseWorker>

    @GET("rest/v1/Workers")
    suspend fun getWorkerProfile(
        @Query("worker_id") workerId: String = "eq.ZEP-1001"
    ): List<SupabaseWorker>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/Workers")
    suspend fun updateWorker(
        @Query("worker_id") workerId: String,
        @Body workerUpdate: Map<String, String?>
    )

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/weekly_policies")
    suspend fun insertWeeklyPolicy(@Body policy: WeeklyPolicyInsert)

    // 🔥 FIX: Added raw Map endpoint to pass Dates without data class limitations
    @Headers("Prefer: return=minimal")
    @POST("rest/v1/weekly_policies")
    suspend fun insertFirstPolicyRaw(@Body policy: Map<String, @JvmSuppressWildcards Any>)

    @GET("rest/v1/weekly_policies")
    suspend fun getActivePolicies(
        @Query("worker_id") workerId: String,
        @Query("status") status: String,
        @Query("select") select: String = "*"
    ): List<SupabaseWeeklyPolicy>

    @GET("rest/v1/worker_charges")
    suspend fun getWorkerCharges(
        @Query("worker_id") workerId: String,
        @Query("select") select: String = "*"
    ): List<WorkerChargeDto>

    @POST("rest/v1/raw_gps_telemetry")
    suspend fun publishTelemetry(@Body telemetry: SupabaseTelemetryRow)
    
    @GET("rest/v1/weekly_policies")
    suspend fun getPoliciesByWorker(
        @Query("worker_id") workerId: String,
        @Query("select") select: String = "*"
    ): List<SupabaseWeeklyPolicy>
    
    @GET("rest/v1/weekly_policies")
    suspend fun getOfferForRider(
        @Query("worker_id") workerId: String = "eq.ZEP-1001",
        @Query("status")    status:   String = "eq.ACTIVE"
    ): List<SupabaseWeeklyPolicy>

    @GET("rest/v1/worker_daily_activity")
    suspend fun getWorkerActivity(
        @Query("worker_id") workerId: String = "eq.ZEP-1001",
        @Query("order")     order:    String = "log_date.desc"
    ): List<SupabaseDailyActivity>

    @GET("rest/v1/claims_and_payouts")
    suspend fun getWorkerClaims(
        @Query("worker_id") workerId: String = "eq.ZEP-1001",
        @Query("order")     order:    String = "created_at.desc"
    ): List<SupabaseClaim>
}

// ─────────────────────────────────────────────────────────────────────────
// NETWORK MODULE
// ─────────────────────────────────────────────────────────────────────────

object NetworkModule {

    private val baseUrl: String by lazy {
        val clean = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        "$clean/".also { Log.d("NetworkModule", "Supabase base URL: $it") }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("NetworkModule", "→ ${request.method} ${request.url}")
                val response = chain.proceed(request)
                Log.d("NetworkModule", "← ${response.code} ${request.url}")
                response
            }
            .build()
    }

    val api: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApiService::class.java)
    }
}
