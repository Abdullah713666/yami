package com.example.yami.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Counts reels from the Reels pager's scroll events.
 *
 * Primary path (API 28+, scrollDeltaY known): the deltas add up exactly to the pixels moved, so the
 * tracker keeps the pager's offset since entering Reels and counts every time it lands on a page
 * further than any page reached before. This counts each reel of a fast swipe run immediately
 * (no waiting for the scroll to stop), ignores half swipes that bounce back, ignores Instagram
 * re-indexing its list, and does not re-count reels the user scrolls back to.
 *
 * Fallback (API < 28, no deltas): one count per settled burst, unless the first-visible index
 * went backwards compared to the previous burst.
 */
class ReelSwipeTracker {
    private var lastExitMs: Long? = null

    private var offsetPx = 0L
    private var maxPage = 0
    private var lastPagerHeight = 0

    private var lastSettledIndex = -1
    private var hasPendingBurst = false
    private var burstLastIndex = -1
    private var burstHadDelta = false

    fun onEnterReels(nowMs: Long): Int {
        val count = if (lastExitMs == null || nowMs - lastExitMs!! > 60_000L) 1 else 0
        offsetPx = 0L
        maxPage = 0
        lastSettledIndex = -1
        clearBurst()
        return count
    }

    fun onExitReels(nowMs: Long) {
        lastExitMs = nowMs
        clearBurst()
    }

    /** Returns the number of new reels landed on with this event (usually 0 or 1). */
    fun onScroll(nowMs: Long, pageIndex: Int, deltaY: Int?, pagerHeight: Int): Int {
        hasPendingBurst = true
        burstLastIndex = pageIndex
        if (pagerHeight > 0) lastPagerHeight = pagerHeight
        if (deltaY == null || lastPagerHeight <= 0) return 0

        burstHadDelta = true
        offsetPx += deltaY
        val h = lastPagerHeight
        val page = (offsetPx.toDouble() / h).roundToInt()
        // Only count when the pager actually rests on the page (within 8 % of its height).
        return if (abs(offsetPx - page.toLong() * h) <= 0.08 * h) advanceTo(page) else 0
    }

    /** Call when no scroll event arrived for a while. Returns reels to count. */
    fun onSettle(nowMs: Long): Int {
        if (!hasPendingBurst) return 0
        val count = if (burstHadDelta) {
            if (lastPagerHeight > 0) advanceTo((offsetPx.toDouble() / lastPagerHeight).roundToInt()) else 0
        } else {
            val back = burstLastIndex >= 0 && lastSettledIndex >= 0 && burstLastIndex < lastSettledIndex
            if (burstLastIndex >= 0) lastSettledIndex = burstLastIndex
            if (back) 0 else 1
        }
        clearBurst()
        return count
    }

    private fun advanceTo(page: Int): Int {
        if (page <= maxPage) return 0
        val n = page - maxPage
        maxPage = page
        return n
    }

    private fun clearBurst() {
        hasPendingBurst = false
        burstLastIndex = -1
        burstHadDelta = false
    }
}
