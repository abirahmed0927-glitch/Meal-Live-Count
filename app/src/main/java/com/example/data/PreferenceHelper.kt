package com.example.data

import android.content.Context

object PreferenceHelper {
    private const val PREFS_NAME = "meal_tracker_prefs"
    private const val KEY_APPS_SCRIPT_URL = "apps_script_url"

    fun getAppsScriptUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APPS_SCRIPT_URL, "") ?: ""
    }

    fun saveAppsScriptUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APPS_SCRIPT_URL, url).apply()
    }
}
