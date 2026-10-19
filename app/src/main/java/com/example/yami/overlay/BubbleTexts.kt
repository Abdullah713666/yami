package com.example.yami.overlay

import android.content.Context
import com.example.yami.R
import com.example.yami.core.Bubble
import com.example.yami.core.MessageKind

fun messageListSize(context: Context, kind: MessageKind): Int = when (kind) {
    MessageKind.CONCERNED -> context.resources.getStringArray(R.array.msg_concerned).size
    MessageKind.LIMIT_HIT -> context.resources.getStringArray(R.array.msg_limit_hit).size
    MessageKind.ANNOYED -> context.resources.getStringArray(R.array.msg_annoyed).size
    MessageKind.ANGRY -> context.resources.getStringArray(R.array.msg_angry).size
    MessageKind.FURIOUS -> context.resources.getStringArray(R.array.msg_furious).size
    MessageKind.TAP_STATS -> 1
}

fun bubbleText(context: Context, bubble: Bubble): String {
    val arr = when (bubble.kind) {
        MessageKind.CONCERNED -> context.resources.getStringArray(R.array.msg_concerned)
        MessageKind.LIMIT_HIT -> context.resources.getStringArray(R.array.msg_limit_hit)
        MessageKind.ANNOYED -> context.resources.getStringArray(R.array.msg_annoyed)
        MessageKind.ANGRY -> context.resources.getStringArray(R.array.msg_angry)
        MessageKind.FURIOUS -> context.resources.getStringArray(R.array.msg_furious)
        MessageKind.TAP_STATS -> return context.getString(R.string.msg_tap_stats)
    }
    val template = arr[bubble.index % arr.size]
    return if (template.contains("%d")) {
        String.format(template, bubble.arg)
    } else {
        template
    }
}

object BubbleTexts {
    fun listSize(context: Context, kind: MessageKind): Int = messageListSize(context, kind)
    fun bubbleText(context: Context, bubble: Bubble): String = com.example.yami.overlay.bubbleText(context, bubble)
}
