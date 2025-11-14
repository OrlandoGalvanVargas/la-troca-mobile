package com.troca.latroca.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class FirstTimeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "la_troca_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_FIRST_TIME = "is_first_time"
    }

    fun isFirstTime(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }

    fun markWelcomeShown() {
        prefs.edit { putBoolean(KEY_FIRST_TIME, false) }
    }

}