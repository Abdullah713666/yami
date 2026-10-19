package com.example.yami.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CoreLogicTest {

    @Test
    fun testMoodEvaluator() {
        val limitsDefault = Limits(dailyLimit = 30, appLimitEnabled = false, appLimit = 20, breakAfter = 10)
        assertEquals(Mood.CALM, MoodEvaluator.mood(0, 0, limitsDefault))
        assertEquals(Mood.CALM, MoodEvaluator.mood(23, 0, limitsDefault))
        assertEquals(Mood.CONCERNED, MoodEvaluator.mood(24, 0, limitsDefault))
        assertEquals(Mood.CONCERNED, MoodEvaluator.mood(29, 0, limitsDefault))
        assertEquals(Mood.LIMIT_HIT, MoodEvaluator.mood(30, 0, limitsDefault))
        assertEquals(Mood.ANNOYED, MoodEvaluator.mood(31, 0, limitsDefault))
        assertEquals(Mood.ANNOYED, MoodEvaluator.mood(34, 0, limitsDefault))
        assertEquals(Mood.ANGRY, MoodEvaluator.mood(35, 0, limitsDefault))
        assertEquals(Mood.ANGRY, MoodEvaluator.mood(39, 0, limitsDefault))
        assertEquals(Mood.FURIOUS, MoodEvaluator.mood(40, 0, limitsDefault))
        assertEquals(Mood.FURIOUS, MoodEvaluator.mood(80, 0, limitsDefault))

        val limitsApp = Limits(dailyLimit = 30, appLimitEnabled = true, appLimit = 20, breakAfter = 10)
        assertEquals(Mood.LIMIT_HIT, MoodEvaluator.mood(20, 20, limitsApp))
        assertEquals(Mood.ANNOYED, MoodEvaluator.mood(22, 22, limitsApp))
        assertEquals(Mood.CONCERNED, MoodEvaluator.mood(10, 16, limitsApp))

        val limitsBreak0 = Limits(dailyLimit = 30, appLimitEnabled = false, appLimit = 20, breakAfter = 0)
        assertEquals(Mood.ANGRY, MoodEvaluator.mood(39, 0, limitsBreak0))
        assertEquals(Mood.FURIOUS, MoodEvaluator.mood(40, 0, limitsBreak0))
    }

    @Test
    fun testReactions() {
        assertEquals(Reaction.MILESTONE, MoodEvaluator.reactionFor(20, Mood.CALM))
        assertEquals(Reaction.BOING, MoodEvaluator.reactionFor(21, Mood.CALM))
        assertEquals(Reaction.MILESTONE, MoodEvaluator.reactionFor(30, Mood.CONCERNED))
        assertEquals(Reaction.HEAD_SHAKE, MoodEvaluator.reactionFor(31, Mood.ANNOYED))
        assertEquals(Reaction.LIMIT_REACHED, MoodEvaluator.reactionFor(30, Mood.LIMIT_HIT))
    }

    @Test
    fun testBreakCard() {
        assertEquals(BreakCard(10, 5), MoodEvaluator.breakFor(10, 10))
        assertEquals(BreakCard(20, 8), MoodEvaluator.breakFor(20, 10))
        assertEquals(BreakCard(30, 11), MoodEvaluator.breakFor(30, 10))
        assertEquals(BreakCard(70, 20), MoodEvaluator.breakFor(70, 10))
        assertNull(MoodEvaluator.breakFor(15, 10))
        assertNull(MoodEvaluator.breakFor(10, 0))
        assertNull(MoodEvaluator.breakFor(0, 10))
        assertNull(MoodEvaluator.breakFor(-3, 10))
    }

    @Test
    fun testMessages() {
        val dummyListSize: (MessageKind) -> Int = { 4 }

        // CALM -> null
        assertNull(Messages.bubbleFor(Mood.CALM, null, 10, 10, 0, dummyListSize))

        // CONCERNED remaining 4, previous CONCERNED -> null
        assertNull(Messages.bubbleFor(Mood.CONCERNED, Mood.CONCERNED, 26, 4, -4, dummyListSize))

        // remaining 5 -> bubble arg 5
        val b5 = Messages.bubbleFor(Mood.CONCERNED, Mood.CONCERNED, 25, 5, -5, dummyListSize)
        assertEquals(Bubble(MessageKind.CONCERNED, 25 % 4, 5), b5)

        // remaining 4, previous CALM -> bubble
        val b4 = Messages.bubbleFor(Mood.CONCERNED, Mood.CALM, 26, 4, -4, dummyListSize)
        assertEquals(Bubble(MessageKind.CONCERNED, 26 % 4, 4), b4)

        // ANGRY total 37 overage 7 with listSize 4 -> Bubble(ANGRY, 1, 7)
        val bAngry = Messages.bubbleFor(Mood.ANGRY, Mood.ANNOYED, 37, -7, 7, dummyListSize)
        assertEquals(Bubble(MessageKind.ANGRY, 1, 7), bAngry)
    }

    @Test
    fun testReelSwipeTrackerEnterRule() {
        val tracker = ReelSwipeTracker()
        assertEquals(1, tracker.onEnterReels(0))
        tracker.onExitReels(1000)
        assertEquals(0, tracker.onEnterReels(31_000))
        tracker.onExitReels(32_000)
        assertEquals(1, tracker.onEnterReels(93_000))
    }

    /** Replays a burst; returns reels counted by onScroll events plus the final onSettle. */
    private fun ReelSwipeTracker.replay(start: Long, idx: Int, deltas: List<Int>, h: Int = 1426): Int {
        var n = 0
        deltas.forEachIndexed { i, dy -> n += onScroll(start + i * 100L, idx, dy, h) }
        return n + onSettle(start + deltas.size * 100L + 450)
    }

    @Test
    fun testFastSwipesCountEveryReel() {
        // Recorded on the phone: 6 fast swipes, 5 landed, the 6th bounced back. Expect 5.
        val t = ReelSwipeTracker()
        t.onEnterReels(0)
        val deltas = listOf(1012, 414, 929, 494, 3, 1118, 308, 1001, 425, 968, 455, 3, 515, -408, -96, -11)
        var n = 0
        val perEvent = mutableListOf<Int>()
        deltas.forEach { dy -> val c = t.onScroll(1000, 30, dy, 1426); perEvent += c; n += c }
        // Counted while scrolling, the moment each reel landed (not only after the burst).
        assertEquals(listOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0), perEvent)
        assertEquals(0, t.onSettle(5000))
        assertEquals(5, n)
    }

    @Test
    fun testSlowSwipesHalfSwipesAndGoingBack() {
        val t = ReelSwipeTracker()
        t.onEnterReels(0)
        assertEquals(1, t.replay(1_000, 0, listOf(700, 726)))          // one full swipe
        assertEquals(0, t.replay(3_000, 1, listOf(400, -400)))         // half swipe, bounced back
        assertEquals(0, t.replay(5_000, 1, listOf(-1426)))             // back to reel 1
        assertEquals(0, t.replay(7_000, 0, listOf(1426)))              // forward again: already seen
        assertEquals(1, t.replay(9_000, 1, listOf(1426)))              // a new reel
        // Settle in a slightly off position still rounds to the nearest page.
        assertEquals(1, t.replay(11_000, 2, listOf(1300)))
        assertEquals(0, t.onSettle(20_000))                            // no burst pending
    }

    @Test
    fun testReindexWithoutSwipeIsNotAReel() {
        // Seen on device: Instagram re-indexes 3 -> 4 while the list moves slightly backwards.
        val t = ReelSwipeTracker()
        t.onEnterReels(0)
        assertEquals(0, t.replay(500, 3, listOf(0)))
        var n = t.onScroll(4_000, 3, -96, 1426) + t.onScroll(4_100, 3, -16, 1426) + t.onScroll(4_500, 4, 0, 1426)
        assertEquals(0, n + t.onSettle(4_950))
        // The real swipe that follows still counts
        n = t.onScroll(8_000, 4, 360, 1426) + t.onScroll(8_300, 4, 355, 1426) + t.onScroll(8_400, 5, 823, 1426)
        assertEquals(1, n + t.onSettle(8_850))
    }

    @Test
    fun testReEnterResetsPosition() {
        val t = ReelSwipeTracker()
        t.onEnterReels(0)
        assertEquals(2, t.replay(1_000, 0, listOf(1426, 1426)))
        t.onScroll(4_000, 2, 1426, 1426)
        t.onExitReels(4_100)                         // pending burst dropped, offset forgotten on enter
        assertEquals(0, t.onSettle(4_600))
        assertEquals(1, t.onEnterReels(100_000))
        assertEquals(1, t.replay(101_000, 0, listOf(1426)))
    }

    @Test
    fun testFallbackWithoutDeltas() {
        val t = ReelSwipeTracker()
        t.onEnterReels(0)
        t.onScroll(1_000, 4, null, 1426)
        assertEquals(1, t.onSettle(1_500))
        t.onScroll(2_000, 5, null, 1426)
        assertEquals(1, t.onSettle(2_500))
        t.onScroll(3_000, 4, null, 1426)          // index went back
        assertEquals(0, t.onSettle(3_500))
        t.onScroll(4_000, -1, null, 1426)         // nothing known: count it
        assertEquals(1, t.onSettle(4_500))
    }

    @Test
    fun testDayKey() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("1970-01-01", DayKey.of(0L, utc))

        val cal = Calendar.getInstance(utc)
        cal.set(2024, Calendar.MARCH, 1, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fixedNoon = cal.timeInMillis

        val days = DayKey.daysBack(fixedNoon, 3, utc)
        assertEquals(listOf("2024-02-28", "2024-02-29", "2024-03-01"), days)
    }
}
