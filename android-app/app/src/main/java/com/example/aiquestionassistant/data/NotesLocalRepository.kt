package com.example.aiquestionassistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NotesLocalRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "ai_notes_storage"
        private const val KEY_NOTES = "notes_list"
        private const val KEY_CATEGORIES = "categories_list"
        private const val KEY_TAGS = "tags_list"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getNotes(
        query: String = "",
        categoryId: String? = null,
        tag: String? = null,
        color: String? = null,
        status: String = "all", // all, favorites, pinned, archived, trash
        sortBy: String = "updated" // updated, created, title
    ): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        val all = mutableListOf<Note>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(t))
                    }
                }
                all.add(
                    Note(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        userId = if (obj.has("userId")) obj.optString("userId") else null,
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        color = obj.optString("color", "default"),
                        categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getString("categoryId") else null,
                        tags = tags,
                        isPinned = obj.optBoolean("isPinned", false),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Filtros
        var filtered = all.filter { n ->
            if (status == "trash") {
                n.isDeleted
            } else {
                !n.isDeleted && when (status) {
                    "favorites" -> n.isFavorite && !n.isArchived
                    "pinned" -> n.isPinned && !n.isArchived
                    "archived" -> n.isArchived
                    else -> !n.isArchived // all
                }
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
        }

        if (!categoryId.isNullOrBlank()) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        if (!tag.isNullOrBlank()) {
            val tLower = tag.trim().lowercase()
            filtered = filtered.filter { n -> n.tags.any { it.trim().lowercase() == tLower } }
        }

        if (!color.isNullOrBlank()) {
            val cLower = color.trim().lowercase()
            filtered = filtered.filter { n ->
                val noteCol = n.color.trim().lowercase()
                noteCol == cLower || (NoteColor.entries.find { it.name.lowercase() == cLower }?.hex?.lowercase() == noteCol) || (NoteColor.entries.find { it.hex.lowercase() == cLower }?.name?.lowercase() == noteCol)
            }
        }

        // Ordenação
        val sorted = filtered.sortedWith { a, b ->
            if (status != "trash" && a.isPinned != b.isPinned) {
                if (a.isPinned) -1 else 1
            } else {
                when (sortBy) {
                    "title" -> a.title.compareTo(b.title, ignoreCase = true)
                    "created" -> b.createdAt.compareTo(a.createdAt)
                    else -> b.updatedAt.compareTo(a.updatedAt)
                }
            }
        }

        return sorted
    }

    @Synchronized
    fun getNoteById(id: String): Note? {
        val all = getNotes(status = "all") + getNotes(status = "archived") + getNotes(status = "trash")
        return all.find { it.id == id }
    }

    @Synchronized
    fun saveNote(note: Note): Note {
        val raw = prefs.getString(KEY_NOTES, null)
        val all = mutableListOf<Note>()
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagsArr = obj.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) {
                            tags.add(tagsArr.getString(t))
                        }
                    }
                    all.add(
                        Note(
                            id = obj.getString("id"),
                            userId = if (obj.has("userId")) obj.optString("userId") else null,
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            color = obj.optString("color", "default"),
                            categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getString("categoryId") else null,
                            tags = tags,
                            isPinned = obj.optBoolean("isPinned", false),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            isDeleted = obj.optBoolean("isDeleted", false),
                            deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        val idx = all.indexOfFirst { it.id == note.id }
        if (idx >= 0) {
            all[idx] = updatedNote
        } else {
            all.add(0, updatedNote)
        }

        persistNotes(all)
        return updatedNote
    }

    @Synchronized
    fun deleteNote(id: String, permanent: Boolean = false): Boolean {
        val raw = prefs.getString(KEY_NOTES, null) ?: return false
        val all = mutableListOf<Note>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) tags.add(tagsArr.getString(t))
                }
                all.add(
                    Note(
                        id = obj.getString("id"),
                        userId = if (obj.has("userId")) obj.optString("userId") else null,
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        color = obj.optString("color", "default"),
                        categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getString("categoryId") else null,
                        tags = tags,
                        isPinned = obj.optBoolean("isPinned", false),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            return false
        }

        val idx = all.indexOfFirst { it.id == id }
        if (idx < 0) return false

        if (permanent) {
            all.removeAt(idx)
        } else {
            val current = all[idx]
            all[idx] = current.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        persistNotes(all)
        return true
    }

    @Synchronized
    fun restoreNote(id: String): Boolean {
        val note = getNoteById(id) ?: return false
        saveNote(note.copy(isDeleted = false, deletedAt = null))
        return true
    }

    @Synchronized
    fun duplicateNote(id: String): Note? {
        val original = getNoteById(id) ?: return null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = if (original.title.isNotBlank()) "${original.title} (Cópia)" else "Nova Nota (Cópia)",
            isPinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return saveNote(copy)
    }

    // CATEGORIAS
    @Synchronized
    fun getCategories(): List<NoteCategory> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        val list = mutableListOf<NoteCategory>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteCategory(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        color = if (obj.has("color")) obj.optString("color") else null,
                        icon = if (obj.has("icon")) obj.optString("icon") else null,
                        description = if (obj.has("description")) obj.optString("description") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun saveCategory(cat: NoteCategory): NoteCategory {
        val list = getCategories().toMutableList()
        val existingByName = list.find { it.name.trim().equals(cat.name.trim(), ignoreCase = true) }
        if (existingByName != null && cat.id != existingByName.id) {
            return existingByName
        }
        val idx = list.indexOfFirst { it.id == cat.id }
        if (idx >= 0) {
            list[idx] = cat
        } else {
            list.add(cat)
        }
        persistCategories(list)
        return cat
    }

    @Synchronized
    fun deleteCategory(id: String, reassignCategoryId: String? = null): Boolean {
        val list = getCategories().toMutableList()
        val removed = list.removeAll { it.id == id }
        if (!removed) return false
        persistCategories(list)

        // Atualiza notas para não excluir nada
        val allNotes = getNotes(status = "all") + getNotes(status = "archived") + getNotes(status = "trash")
        for (n in allNotes) {
            if (n.categoryId == id) {
                saveNote(n.copy(categoryId = reassignCategoryId))
            }
        }
        return true
    }

    // TAGS
    @Synchronized
    fun getTags(): List<NoteTag> {
        val raw = prefs.getString(KEY_TAGS, null) ?: return emptyList()
        val list = mutableListOf<NoteTag>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteTag(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        color = if (obj.has("color")) obj.optString("color") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun saveTag(name: String, color: String? = null): NoteTag {
        val clean = name.trim()
        val list = getTags().toMutableList()
        val existing = list.find { it.name.equals(clean, ignoreCase = true) }
        if (existing != null) {
            if (color != null) {
                val updated = existing.copy(color = color)
                val idx = list.indexOfFirst { it.id == existing.id }
                list[idx] = updated
                persistTags(list)
                return updated
            }
            return existing
        }
        val newTag = NoteTag(name = clean, color = color)
        list.add(newTag)
        persistTags(list)
        return newTag
    }

    @Synchronized
    fun deleteTag(name: String): Boolean {
        val clean = name.trim().lowercase()
        val list = getTags().toMutableList()
        val removed = list.removeAll { it.name.trim().lowercase() == clean }
        if (!removed) return false
        persistTags(list)

        val allNotes = getNotes(status = "all") + getNotes(status = "archived") + getNotes(status = "trash")
        for (n in allNotes) {
            if (n.tags.any { it.trim().lowercase() == clean }) {
                val updatedTags = n.tags.filter { it.trim().lowercase() != clean }
                saveNote(n.copy(tags = updatedTags))
            }
        }
        return true
    }

    private fun persistNotes(notes: List<Note>) {
        try {
            val array = JSONArray()
            for (n in notes) {
                val obj = JSONObject().apply {
                    put("id", n.id)
                    if (n.userId != null) put("userId", n.userId)
                    put("title", n.title)
                    put("content", n.content)
                    put("color", n.color)
                    if (n.categoryId != null) put("categoryId", n.categoryId)
                    val tagsArr = JSONArray()
                    n.tags.forEach { tagsArr.put(it) }
                    put("tags", tagsArr)
                    put("isPinned", n.isPinned)
                    put("isFavorite", n.isFavorite)
                    put("isArchived", n.isArchived)
                    put("isDeleted", n.isDeleted)
                    if (n.deletedAt != null) put("deletedAt", n.deletedAt)
                    put("createdAt", n.createdAt)
                    put("updatedAt", n.updatedAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_NOTES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistCategories(cats: List<NoteCategory>) {
        try {
            val array = JSONArray()
            for (c in cats) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    if (c.color != null) put("color", c.color)
                    if (c.icon != null) put("icon", c.icon)
                    if (c.description != null) put("description", c.description)
                    put("createdAt", c.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistTags(tags: List<NoteTag>) {
        try {
            val array = JSONArray()
            for (t in tags) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    if (t.color != null) put("color", t.color)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_TAGS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
