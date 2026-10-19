package com.example.yami.core

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DayKey {
    fun of(timeMs: Long, tz: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = timeMs
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun daysBack(todayMs: Long, n: Int, tz: TimeZone = TimeZone.getDefault()): List<String> {
        if (n <= 0) return emptyList()
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = todayMs
        cal.add(Calendar.DAY_OF_MONTH, -(n - 1))
        val result = ArrayList<String>(n)
        for (i in 0 until n) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            result.add(String.format(Locale.US, "%04d-%02d-%02d", year, month, day))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }
}
