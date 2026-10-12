package com.example.aiquestionassistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "ai_assistant_history"
        private const val KEY_ITEMS = "history_items"
        private const val MAX_ITEMS = 100
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveItem(item: AssistantResult) {
        val list = getItems().toMutableList()
        // Remove duplicate if exists and prepend
        list.removeAll { it.id == item.id }
        list.add(0, item)
        if (list.size > MAX_ITEMS) {
            list.removeAt(list.size - 1)
        }
        persist(list)
    }

    @Synchronized
    fun getItems(): List<AssistantResult> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val list = mutableListOf<AssistantResult>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val actionStr = obj.optString("action", "ASK")
                val actionType = try {
                    AssistantActionType.valueOf(actionStr)
                } catch (e: Exception) {
                    AssistantActionType.ASK
                }

                val keyPointsList = mutableListOf<String>()
                val kpArray = obj.optJSONArray("keyPoints")
                if (kpArray != null) {
                    for (k in 0 until kpArray.length()) {
                        keyPointsList.add(kpArray.getString(k))
                    }
                }

                list.add(
                    AssistantResult(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        isSuccess = obj.optBoolean("isSuccess", true),
                        action = actionType,
                        title = obj.optString("title", ""),
                        resultText = obj.optString("resultText", ""),
                        diagramCode = if (obj.has("diagramCode")) obj.optString("diagramCode") else null,
                        imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                        keyPoints = keyPointsList,
                        error = if (obj.has("error")) obj.optString("error") else null,
                        latencyMs = obj.optLong("latencyMs", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun deleteItem(id: String) {
        val list = getItems().filterNot { it.id == id }
        persist(list)
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove(KEY_ITEMS).apply()
    }

    private fun persist(items: List<AssistantResult>) {
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("isSuccess", item.isSuccess)
                    put("action", item.action.name)
                    put("title", item.title)
                    put("resultText", item.resultText)
                    if (item.diagramCode != null) put("diagramCode", item.diagramCode)
                    if (item.imageUrl != null) put("imageUrl", item.imageUrl)
                    val kp = JSONArray()
                    item.keyPoints.forEach { kp.put(it) }
                    put("keyPoints", kp)
                    if (item.error != null) put("error", item.error)
                    put("latencyMs", item.latencyMs)
                    put("timestamp", item.timestamp)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
