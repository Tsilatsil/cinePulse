package com.cinepulse.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository to manage application settings and preferences.
 * Reference: Feature - Settings menu that makes sense for the application.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _region = MutableStateFlow(prefs.getString("region", "US") ?: "US")
    val region: StateFlow<String> = _region.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        Log.d("SettingsRepository", "Toggling Dark Mode: $enabled")
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setRegion(r: String) {
        Log.d("SettingsRepository", "Setting Region: $r")
        _region.value = r
        prefs.edit().putString("region", r).apply()
    }
}
