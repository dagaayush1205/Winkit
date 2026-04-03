package com.example.winkit.utils

import android.content.Context

object AuthManager {
    private const val PREFS_NAME = "winkit_auth"
    private const val KEY_WORKER_ID = "worker_id"

    // Call this right after they successfully verify their OTP!
    fun saveWorkerId(context: Context, workerId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WORKER_ID, workerId).apply()
    }

    // Check this in your MainActivity/NavGraph to decide which screen to show first
    fun getWorkerId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WORKER_ID, null)
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
