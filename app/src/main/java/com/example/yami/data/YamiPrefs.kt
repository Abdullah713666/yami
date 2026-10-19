package com.example.yami.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.yami.R
import com.example.yami.core.BreakMessage
import com.example.yami.core.CustomTexts
import com.example.yami.core.DayKey
import com.example.yami.core.Limits
import com.example.yami.core.SupportedApp
import org.json.JSONArray
import org.json.JSONObject

class YamiPrefs(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yami_prefs", Context.MODE_PRIVATE)

    var dailyLimit: Int
        get() = prefs.getInt("daily_limit", 30)
        set(value) = prefs.edit().putInt("daily_limit", value.coerceIn(5, 500)).apply()

    var igLimitEnabled: Boolean
        get() = prefs.getBoolean("ig_limit_enabled", false)
        set(value) = prefs.edit().putBoolean("ig_limit_enabled", value).apply()

    var igLimit: Int
        get() = prefs.getInt("ig_limit", 20)
        set(value) = prefs.edit().putInt("ig_limit", value.coerceIn(5, 500)).apply()

    var breakAfter: Int
        get() = prefs.getInt("break_after", 10)
        set(value) = prefs.edit().putInt("break_after", value.coerceIn(0, 100)).apply()

    var overlayEnabled: Boolean
        get() = prefs.getBoolean("overlay_enabled", true)
        set(value) = prefs.edit().putBoolean("overlay_enabled", value).apply()

    var overlaySize: String
        get() = prefs.getString("overlay_size", "M") ?: "M"
        set(value) {
            val clamped = if (value in setOf("S", "M", "L")) value else "M"
            prefs.edit().putString("overlay_size", clamped).apply()
        }

    var overlaySide: String
        get() = prefs.getString("overlay_side", "right") ?: "right"
        set(value) {
            val clamped = if (value in setOf("left", "right")) value else "right"
            prefs.edit().putString("overlay_side", clamped).apply()
        }

    var overlayYFrac: Float
        get() = prefs.getFloat("overlay_y_frac", 0.20f)
        set(value) = prefs.edit().putFloat("overlay_y_frac", value.coerceIn(0f, 1f)).apply()

    fun limits(): Limits = Limits(
        dailyLimit = dailyLimit,
        appLimitEnabled = igLimitEnabled,
        appLimit = igLimit,
        breakAfter = breakAfter
    )

    fun todayTotal(): Int {
        val today = DayKey.of(System.currentTimeMillis())
        return prefs.getInt("total_$today", 0)
    }

    fun todayCount(app: SupportedApp): Int {
        val today = DayKey.of(System.currentTimeMillis())
        return prefs.getInt("count_${today}_${app.key}", 0)
    }

    fun addReel(app: SupportedApp): Int {
        val now = System.currentTimeMillis()
        val today = DayKey.of(now)
        val currentAppCount = prefs.getInt("count_${today}_${app.key}", 0)
        val currentTotal = prefs.getInt("total_$today", 0)
        val newTotal = currentTotal + 1

        val editor = prefs.edit()
        editor.putInt("count_${today}_${app.key}", currentAppCount + 1)
        editor.putInt("total_$today", newTotal)

        val lastCleanup = prefs.getString("last_cleanup_day", null)
        if (lastCleanup != today) {
            val oldestKept = DayKey.daysBack(now, 30).firstOrNull() ?: today
            for (k in prefs.all.keys) {
                if (k.startsWith("count_")) {
                    val parts = k.removePrefix("count_").split("_")
                    if (parts.isNotEmpty()) {
                        val dateKey = parts[0]
                        if (dateKey.length == 10 && dateKey < oldestKept) {
                            editor.remove(k)
                        }
                    }
                } else if (k.startsWith("total_")) {
                    val dateKey = k.removePrefix("total_")
                    if (dateKey.length == 10 && dateKey < oldestKept) {
                        editor.remove(k)
                    }
                }
            }
            editor.putString("last_cleanup_day", today)
        }
        editor.apply()
        return newTotal
    }

    fun resetToday() {
        val today = DayKey.of(System.currentTimeMillis())
        val editor = prefs.edit()
        editor.remove("total_$today")
        for (k in prefs.all.keys) {
            if (k.startsWith("count_${today}_")) {
                editor.remove(k)
            }
        }
        editor.apply()
    }

    fun lastDays(n: Int): List<Pair<String, Int>> {
        val days = DayKey.daysBack(System.currentTimeMillis(), n)
        return days.map { day ->
            day to prefs.getInt("total_$day", 0)
        }
    }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun defaultBreakMessages(): List<BreakMessage> {
        return try {
            val arr = context.resources.getStringArray(R.array.break_messages_default)
            arr.mapIndexed { index, text ->
                BreakMessage(
                    id = "b$index",
                    text = text,
                    enabled = true,
                    builtIn = true
                )
            }
        } catch (e: Exception) {
            Log.e("YamiPrefs", "Error loading default break messages", e)
            emptyList()
        }
    }

    fun breakMessages(): List<BreakMessage> {
        val jsonStr = prefs.getString("break_messages", null) ?: return defaultBreakMessages()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<BreakMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BreakMessage(
                        id = obj.getString("id"),
                        text = obj.getString("text"),
                        enabled = obj.optBoolean("enabled", true),
                        builtIn = obj.optBoolean("builtIn", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("YamiPrefs", "Error parsing break_messages JSON", e)
            defaultBreakMessages()
        }
    }

    fun saveBreakMessages(list: List<BreakMessage>) {
        val cleanedList = mutableListOf<BreakMessage>()
        for (item in list) {
            val cleanedText = CustomTexts.clean(item.text) ?: continue
            cleanedList.add(item.copy(text = cleanedText))
            if (cleanedList.size >= CustomTexts.MAX_ITEMS) break
        }
        val jsonArray = JSONArray()
        for (item in cleanedList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("enabled", item.enabled)
                put("builtIn", item.builtIn)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("break_messages", jsonArray.toString()).apply()
    }

    fun pickBreakMessage(): String {
        val last = prefs.getString("break_last_text", null)
        val picked = CustomTexts.pickBreak(breakMessages(), last = last)
            ?: context.getString(R.string.break_title)
        prefs.edit().putString("break_last_text", picked).apply()
        return picked
    }

    fun customBubbles(): List<String> {
        val jsonStr = prefs.getString("custom_bubbles", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            Log.e("YamiPrefs", "Error parsing custom_bubbles JSON", e)
            emptyList()
        }
    }

    fun saveCustomBubbles(list: List<String>) {
        val cleanedList = mutableListOf<String>()
        for (text in list) {
            val cleanedText = CustomTexts.clean(text) ?: continue
            cleanedList.add(cleanedText)
            if (cleanedList.size >= CustomTexts.MAX_ITEMS) break
        }
        val jsonArray = JSONArray()
        for (text in cleanedList) {
            jsonArray.put(text)
        }
        prefs.edit().putString("custom_bubbles", jsonArray.toString()).apply()
    }

    var soundChoice: String
        get() = prefs.getString("sound_choice", "whistle") ?: "whistle"
        set(value) {
            val valid = if (value in setOf("off", "whistle", "siren", "custom")) value else "whistle"
            prefs.edit().putString("sound_choice", valid).apply()
        }

    var soundCustomUri: String?
        get() = prefs.getString("sound_custom_uri", null)
        set(value) {
            if (value == null) {
                prefs.edit().remove("sound_custom_uri").apply()
            } else {
                prefs.edit().putString("sound_custom_uri", value).apply()
            }
        }

    var soundCustomName: String?
        get() = prefs.getString("sound_custom_name", null)
        set(value) {
            if (value == null) {
                prefs.edit().remove("sound_custom_name").apply()
            } else {
                prefs.edit().putString("sound_custom_name", value).apply()
            }
        }

    var soundOnLimit: Boolean
        get() = prefs.getBoolean("sound_on_limit", true)
        set(value) = prefs.edit().putBoolean("sound_on_limit", value).apply()

    var soundOnBreak: Boolean
        get() = prefs.getBoolean("sound_on_break", true)
        set(value) = prefs.edit().putBoolean("sound_on_break", value).apply()

    var soundEveryOver: Boolean
        get() = prefs.getBoolean("sound_every_over", false)
        set(value) = prefs.edit().putBoolean("sound_every_over", value).apply()
}
