package com.example.yami.core

data class BreakMessage(
    val id: String,
    val text: String,
    val enabled: Boolean,
    val builtIn: Boolean
)

object CustomTexts {
    const val MAX_LEN = 80
    const val MAX_ITEMS = 30

    /** Random enabled message, never equal to `last` when another enabled one exists; null if none enabled. */
    fun pickBreak(
        list: List<BreakMessage>,
        last: String?,
        random: kotlin.random.Random = kotlin.random.Random
    ): String? {
        val enabled = list.filter { it.enabled }
        if (enabled.isEmpty()) return null
        if (last == null) {
            return enabled[random.nextInt(enabled.size)].text
        }
        val different = enabled.filter { it.text != last }
        val pool = if (different.isNotEmpty()) different else enabled
        return pool[random.nextInt(pool.size)].text
    }

    /** Replaces every literal "%d" with the number; no String.format (user text may contain other % signs). */
    fun fillNumber(template: String, n: Int): String {
        return template.replace("%d", n.toString())
    }

    /** Trims, collapses inner whitespace runs to one space, cuts to MAX_LEN; returns null if blank. */
    fun clean(text: String): String? {
        if (text.isBlank()) return null
        val collapsed = text.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return null
        return if (collapsed.length > MAX_LEN) collapsed.substring(0, MAX_LEN) else collapsed
    }
}
