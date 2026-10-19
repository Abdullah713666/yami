package com.example.yami.core

data class BreakCard(val overage: Int, val countdownSeconds: Int)

object MoodEvaluator {
    fun overage(total: Int, app: Int, l: Limits): Int {
        val totalOver = total - l.dailyLimit
        val appOver = if (l.appLimitEnabled) app - l.appLimit else Int.MIN_VALUE
        return maxOf(totalOver, appOver)
    }

    fun remaining(total: Int, app: Int, l: Limits): Int = -overage(total, app, l)

    fun mood(total: Int, app: Int, l: Limits): Mood {
        val ov = overage(total, app, l)
        val totalRatio = total.toFloat() / l.dailyLimit.toFloat()
        val appRatio = if (l.appLimitEnabled) app.toFloat() / l.appLimit.toFloat() else 0f
        val ratio = maxOf(totalRatio, appRatio)
        val furiousAt = if (l.breakAfter > 5) l.breakAfter else 10

        return when {
            ov < 0 && ratio < 0.8f -> Mood.CALM
            ov < 0 && ratio >= 0.8f -> Mood.CONCERNED
            ov == 0 -> Mood.LIMIT_HIT
            ov in 1..4 -> Mood.ANNOYED
            ov < furiousAt -> Mood.ANGRY
            else -> Mood.FURIOUS
        }
    }

    fun reactionFor(total: Int, mood: Mood): Reaction = when (mood) {
        Mood.CALM, Mood.CONCERNED -> if (total % 10 == 0) Reaction.MILESTONE else Reaction.BOING
        Mood.LIMIT_HIT -> Reaction.LIMIT_REACHED
        Mood.ANNOYED -> Reaction.HEAD_SHAKE
        Mood.ANGRY -> Reaction.ANGRY_SHAKE
        Mood.FURIOUS -> Reaction.FURIOUS_SHAKE
        Mood.BREAK -> Reaction.BOING
    }

    fun breakFor(overage: Int, breakAfter: Int): BreakCard? {
        if (breakAfter <= 0 || overage <= 0 || overage % breakAfter != 0) return null
        val k = overage / breakAfter
        val countdownSeconds = minOf(5 + 3 * (k - 1), 20)
        return BreakCard(overage, countdownSeconds)
    }
}
