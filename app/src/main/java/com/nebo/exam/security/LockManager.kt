package com.nebo.exam.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nebo.exam.utils.Constants

object LockManager {
    private const val PREF_NAME = "nebo_secure_prefs"
    private const val KEY_IS_LOCKED = "is_locked"
    private const val KEY_PENALTY_REMAINING = "penalty_remaining"
    private const val KEY_LAST_ELAPSED = "last_elapsed"
    private const val KEY_VIOLATION_COUNT = "violation_count"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun triggerPenalty(context: Context) {
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_VIOLATION_COUNT, 0) + 1

        val penaltyDurationMs = when (currentCount) {
            1 -> 5 * 60 * 1000L // Pelanggaran 1: 5 Menit
            2 -> 15 * 60 * 1000L // Pelanggaran 2: 15 Menit
            3 -> 30 * 60 * 1000L // Pelanggaran 3: 30 Menit
            else -> 60 * 60 * 1000L // Pelanggaran > 3: 1 Jam (60 Menit)
        }

        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, true)
            .putInt(KEY_VIOLATION_COUNT, currentCount)
            .putLong(KEY_PENALTY_REMAINING, penaltyDurationMs)
            .putLong(KEY_LAST_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }

    fun getViolationCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIOLATION_COUNT, 0)
    }

    fun getRemainingPenaltyTimeMs(context: Context): Long {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_LOCKED, false)) return 0L

        val savedDuration = prefs.getLong(KEY_PENALTY_REMAINING, 0L)
        val savedElapsed = prefs.getLong(KEY_LAST_ELAPSED, SystemClock.elapsedRealtime())
        val currentElapsed = SystemClock.elapsedRealtime()
        val timePassed = currentElapsed - savedElapsed

        return if (timePassed < 0) {
            prefs.edit().putLong(KEY_LAST_ELAPSED, currentElapsed).apply()
            savedDuration
        } else {
            val remaining = savedDuration - timePassed
            if (remaining <= 0) {
                clearLockStateOnly(context)
                0L
            } else {
                prefs.edit()
                    .putLong(KEY_PENALTY_REMAINING, remaining)
                    .putLong(KEY_LAST_ELAPSED, currentElapsed)
                    .apply()
                remaining
            }
        }
    }

    fun isPenaltyActive(context: Context): Boolean = getRemainingPenaltyTimeMs(context) > 0

    private fun clearLockStateOnly(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_PENALTY_REMAINING, 0L)
            .putLong(KEY_LAST_ELAPSED, 0L)
            .apply()
    }

    fun resetPenalty(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun verifySupervisorPin(inputPin: String): Boolean = inputPin == Constants.DEFAULT_SUPERVISOR_PIN
}
