package com.example.aiquestionassistant

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object NotesScreenKey : NavKey
@Serializable data class NoteEditorKey(val noteId: String? = null) : NavKey
@Serializable data object SettingsScreenKey : NavKey

