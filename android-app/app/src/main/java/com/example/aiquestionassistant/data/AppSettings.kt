package com.example.aiquestionassistant.data

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    val serverUrl: String = "http://10.0.2.2:3001",
    val directGeminiKey: String = "",
    val model: String = "gemini-2.5-flash",
    val mode: String = "complete",
    val autoCopy: Boolean = true,
    val hapticFeedback: Boolean = true
) {
    companion object {
        private const val PREFS_NAME = "ai_question_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_GEMINI_KEY = "gemini_key"
        private const val KEY_MODEL = "model"
        private const val KEY_MODE = "mode"
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_HAPTIC = "haptic"

        fun load(context: Context): AppSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AppSettings(
                serverUrl = prefs.getString(KEY_SERVER_URL, "http://10.0.2.2:3001") ?: "http://10.0.2.2:3001",
                directGeminiKey = prefs.getString(KEY_GEMINI_KEY, "") ?: "",
                model = prefs.getString(KEY_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash",
                mode = prefs.getString(KEY_MODE, "complete") ?: "complete",
                autoCopy = prefs.getBoolean(KEY_AUTO_COPY, true),
                hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true)
            )
        }

        fun save(context: Context, settings: AppSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SERVER_URL, settings.serverUrl)
                .putString(KEY_GEMINI_KEY, settings.directGeminiKey)
                .putString(KEY_MODEL, settings.model)
                .putString(KEY_MODE, settings.mode)
                .putBoolean(KEY_AUTO_COPY, settings.autoCopy)
                .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
                .apply()
        }
    }
}
