package com.example.aiquestionassistant

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.aiquestionassistant.ui.main.MainScreen
import com.example.aiquestionassistant.ui.notes.NoteEditorScreen
import com.example.aiquestionassistant.ui.notes.NotesScreen
import com.example.aiquestionassistant.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding())
        }
        entry<NotesScreenKey> {
          NotesScreen(
            onBack = { backStack.removeLastOrNull() },
            onOpenNote = { noteId -> backStack.add(NoteEditorKey(noteId)) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<NoteEditorKey> { key ->
          NoteEditorScreen(
            noteId = key.noteId,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<SettingsScreenKey> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}

