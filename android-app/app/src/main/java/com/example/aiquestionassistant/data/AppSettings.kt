package com.example.aiquestionassistant.data

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    val serverUrl: String = "http://10.0.2.2:3001",
    val directGeminiKey: String = "",
    val directOpenRouterKey: String = "",
    val directAgentRouterKey: String = "",
    val model: String = "gemini-3.5-flash-lite",
    val mode: String = "complete",
    val autoCopy: Boolean = true,
    val hapticFeedback: Boolean = true,
    val bubbleX: Int = 20,
    val bubbleY: Int = 300,
    val bubbleSize: Int = 56,
    val panelExpanded: Boolean = false,
    val ttsSpeechRate: Float = 1.0f,
    val preferredLanguage: String = "Português",
    val defaultImageStyle: String = "educational"
) {
    companion object {
        private const val PREFS_NAME = "ai_question_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_GEMINI_KEY = "gemini_key"
        private const val KEY_OPENROUTER_KEY = "openrouter_key"
        private const val KEY_AGENTROUTER_KEY = "agentrouter_key"
        private const val KEY_MODEL = "model"
        private const val KEY_MODE = "mode"
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_BUBBLE_SIZE = "bubble_size"
        private const val KEY_PANEL_EXPANDED = "panel_expanded"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_PREFERRED_LANG = "preferred_lang"
        private const val KEY_IMAGE_STYLE = "image_style"

        fun load(context: Context): AppSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawModel = prefs.getString(KEY_MODEL, "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
            val sanitizedModel = if (rawModel.contains("1.5") || rawModel.contains("2.0") || rawModel.contains("2.5") || rawModel.contains("3-pro-image")) {
                "gemini-3.5-flash-lite"
            } else if (rawModel.equals("laude-opus-4-8", ignoreCase = true)) {
                "claude-opus-4-8"
            } else rawModel
            return AppSettings(
                serverUrl = prefs.getString(KEY_SERVER_URL, "http://10.0.2.2:3001") ?: "http://10.0.2.2:3001",
                directGeminiKey = prefs.getString(KEY_GEMINI_KEY, "") ?: "",
                directOpenRouterKey = prefs.getString(KEY_OPENROUTER_KEY, "") ?: "",
                directAgentRouterKey = prefs.getString(KEY_AGENTROUTER_KEY, "") ?: "",
                model = sanitizedModel,
                mode = prefs.getString(KEY_MODE, "complete") ?: "complete",
                autoCopy = prefs.getBoolean(KEY_AUTO_COPY, true),
                hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
                bubbleX = prefs.getInt(KEY_BUBBLE_X, 20),
                bubbleY = prefs.getInt(KEY_BUBBLE_Y, 300),
                bubbleSize = prefs.getInt(KEY_BUBBLE_SIZE, 56),
                panelExpanded = prefs.getBoolean(KEY_PANEL_EXPANDED, false),
                ttsSpeechRate = prefs.getFloat(KEY_TTS_SPEED, 1.0f),
                preferredLanguage = prefs.getString(KEY_PREFERRED_LANG, "Português") ?: "Português",
                defaultImageStyle = prefs.getString(KEY_IMAGE_STYLE, "educational") ?: "educational"
            )
        }

        fun save(context: Context, settings: AppSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SERVER_URL, settings.serverUrl)
                .putString(KEY_GEMINI_KEY, settings.directGeminiKey)
                .putString(KEY_OPENROUTER_KEY, settings.directOpenRouterKey)
                .putString(KEY_AGENTROUTER_KEY, settings.directAgentRouterKey)
                .putString(KEY_MODEL, settings.model)
                .putString(KEY_MODE, settings.mode)
                .putBoolean(KEY_AUTO_COPY, settings.autoCopy)
                .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
                .putInt(KEY_BUBBLE_X, settings.bubbleX)
                .putInt(KEY_BUBBLE_Y, settings.bubbleY)
                .putInt(KEY_BUBBLE_SIZE, settings.bubbleSize)
                .putBoolean(KEY_PANEL_EXPANDED, settings.panelExpanded)
                .putFloat(KEY_TTS_SPEED, settings.ttsSpeechRate)
                .putString(KEY_PREFERRED_LANG, settings.preferredLanguage)
                .putString(KEY_IMAGE_STYLE, settings.defaultImageStyle)
                .apply()
        }

        fun saveBubblePosition(context: Context, x: Int, y: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_BUBBLE_X, x)
                .putInt(KEY_BUBBLE_Y, y)
                .apply()
        }

        fun saveBubbleSize(context: Context, size: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_BUBBLE_SIZE, size)
                .apply()
        }
    }
}
