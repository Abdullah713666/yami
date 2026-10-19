package com.example.yami.core

enum class MessageKind {
    CONCERNED,
    LIMIT_HIT,
    ANNOYED,
    ANGRY,
    FURIOUS,
    TAP_STATS
}

data class Bubble(val kind: MessageKind, val index: Int, val arg: Int)

object Messages {
    fun bubbleFor(
        mood: Mood,
        previousMood: Mood?,
        total: Int,
        remaining: Int,
        overage: Int,
        listSize: (MessageKind) -> Int
    ): Bubble? {
        val kind = when (mood) {
            Mood.CALM, Mood.BREAK -> return null
            Mood.CONCERNED -> {
                val shouldShow = remaining in setOf(5, 3, 1) || previousMood == null || previousMood == Mood.CALM
                if (!shouldShow) return null
                MessageKind.CONCERNED
            }
            Mood.LIMIT_HIT -> MessageKind.LIMIT_HIT
            Mood.ANNOYED -> MessageKind.ANNOYED
            Mood.ANGRY -> MessageKind.ANGRY
            Mood.FURIOUS -> MessageKind.FURIOUS
        }

        val size = listSize(kind)
        val index = if (size > 0) ((total % size) + size) % size else 0
        val arg = when (kind) {
            MessageKind.CONCERNED -> remaining
            MessageKind.LIMIT_HIT -> 0
            MessageKind.ANNOYED, MessageKind.ANGRY, MessageKind.FURIOUS -> overage
            MessageKind.TAP_STATS -> 0
        }
        return Bubble(kind, index, arg)
    }
}
