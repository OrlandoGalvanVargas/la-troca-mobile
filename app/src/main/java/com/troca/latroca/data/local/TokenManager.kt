package com.troca.latroca.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val ROLE_KEY = "user_role"

    fun saveRole(role: String) {
        sharedPreferences.edit { putString(ROLE_KEY, role) }
    }

    fun getRole(): String? {
        return sharedPreferences.getString(ROLE_KEY, null)
    }

    fun clearAll() {
        sharedPreferences.edit {
            remove(KEY_TOKEN)
                .remove(ROLE_KEY)  // 👈 AGREGAR ESTO
        }
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }

    fun saveToken(token: String) {
        sharedPreferences.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }
}