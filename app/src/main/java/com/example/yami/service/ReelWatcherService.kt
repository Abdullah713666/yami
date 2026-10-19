package com.example.yami.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.yami.BuildConfig
import com.example.yami.core.Limits
import com.example.yami.core.Messages
import com.example.yami.core.Mood
import com.example.yami.core.CustomTexts
import com.example.yami.core.MoodEvaluator
import com.example.yami.core.ReelSwipeTracker
import com.example.yami.core.SupportedApp
import com.example.yami.data.YamiPrefs
import com.example.yami.overlay.BubbleTexts
import com.example.yami.overlay.OverlayController
import com.example.yami.overlay.OverlayState
import com.example.yami.sound.SoundPlayer
import kotlin.random.Random

class ReelWatcherService : AccessibilityService() {

    private lateinit var prefs: YamiPrefs
    private val tracker = ReelSwipeTracker()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var detector: ReelsScreenDetector
    private lateinit var overlay: OverlayController
    private var previousMood: Mood? = null

    private var soundPlayer: SoundPlayer? = null
    private var customBubbleCount = 0
    private var lastCustomBubble: String? = null
    private val random = Random(System.currentTimeMillis())

    private var isReceiverRegistered = false
    private var isCleanedUp = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                detector.forceFalse()
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "overlay_enabled" || key == "overlay_size") {
            overlay.onPrefsChanged()
            if (key == "overlay_enabled" && prefs.overlayEnabled && detector.inReels) {
                overlay.show(currentState())
            }
        }
    }

    private val settleRunnable = Runnable {
        val now = SystemClock.uptimeMillis()
        val n = tracker.onSettle(now)
        if (BuildConfig.DEBUG) {
            Log.d("YamiDetect", "settle n=$n")
        }
        repeat(n) { countReel() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = YamiPrefs(this)
        overlay = OverlayController(this, prefs)
        soundPlayer = SoundPlayer(this, prefs)
        detector = ReelsScreenDetector(
            app = SupportedApp.INSTAGRAM,
            handler = handler,
            rootProvider = { rootInActiveWindow },
            onChange = { inReels ->
                val now = SystemClock.uptimeMillis()
                if (inReels) {
                    val n = tracker.onEnterReels(now)
                    if (BuildConfig.DEBUG) {
                        Log.d("YamiDetect", "enter n=$n")
                    }
                    overlay.show(currentState())
                    if (n > 0) {
                        countReel()
                    }
                } else {
                    tracker.onExitReels(now)
                    handler.removeCallbacks(settleRunnable)
                    if (BuildConfig.DEBUG) {
                        Log.d("YamiDetect", "exit")
                    }
                    overlay.hide()
                }
            }
        )

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
        isReceiverRegistered = true

        prefs.register(prefListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        detector.onEvent(event)

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && detector.inReels) {
            if (isPagerScroll(event)) {
                val now = SystemClock.uptimeMillis()
                val fromIndex = if (event.fromIndex >= 0) event.fromIndex else -1
                val deltaY = if (Build.VERSION.SDK_INT >= 28) event.scrollDeltaY else null

                val rect = Rect()
                val source = event.source
                val pagerHeight = if (source != null) {
                    source.getBoundsInScreen(rect)
                    val h = rect.height()
                    if (h > 0) h else resources.displayMetrics.heightPixels
                } else {
                    resources.displayMetrics.heightPixels
                }

                val srcName = source?.className ?: event.className ?: "unknown"
                if (BuildConfig.DEBUG) {
                    Log.d("YamiDetect", "scroll idx=$fromIndex dy=$deltaY h=$pagerHeight src=$srcName")
                }

                val landed = tracker.onScroll(now, fromIndex, deltaY, pagerHeight)
                if (BuildConfig.DEBUG && landed > 0) {
                    Log.d("YamiDetect", "landed n=$landed")
                }
                repeat(landed) { countReel() }

                handler.removeCallbacks(settleRunnable)
                handler.postDelayed(settleRunnable, 450L)
            } else {
                val pkg = event.packageName?.toString()
                if (pkg == SupportedApp.INSTAGRAM.packageName) {
                    val source = event.source
                    val srcClass = source?.className ?: event.className ?: "null"
                    val srcId = source?.viewIdResourceName ?: "null"
                    val parentId = source?.parent?.viewIdResourceName ?: "null"
                    if (BuildConfig.DEBUG) {
                        Log.d("YamiDetect", "scroll-ignored src=$srcClass id=$srcId parentId=$parentId")
                    }
                }
            }
        }
    }

    private fun isPagerScroll(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return false
        val pagerId = SupportedApp.INSTAGRAM.reelsPagerId
        if (source.viewIdResourceName == pagerId) return true

        val parent = source.parent
        if (parent != null) {
            if (parent.viewIdResourceName == pagerId) return true
            val grandparent = parent.parent
            if (grandparent != null && grandparent.viewIdResourceName == pagerId) return true
        }
        return false
    }

    private fun currentState(): OverlayState {
        val total = prefs.todayTotal()
        val appCount = prefs.todayCount(SupportedApp.INSTAGRAM)
        val limits = prefs.limits()
        val mood = MoodEvaluator.mood(total, appCount, limits)
        val limitForBadge = getLimitForBadge(total, appCount, limits)
        return OverlayState(total, limitForBadge, mood)
    }

    private fun getLimitForBadge(total: Int, appCount: Int, limits: Limits): Int {
        return if (limits.appLimitEnabled) {
            val dailyRemaining = limits.dailyLimit - total
            val appRemaining = limits.appLimit - appCount
            if (appRemaining < dailyRemaining) limits.appLimit else limits.dailyLimit
        } else {
            limits.dailyLimit
        }
    }

    private fun pickCustomBubble(list: List<String>): String {
        val pool = if (list.size >= 2 && lastCustomBubble != null) {
            val diff = list.filter { it != lastCustomBubble }
            if (diff.isNotEmpty()) diff else list
        } else {
            list
        }
        val picked = pool[random.nextInt(pool.size)]
        lastCustomBubble = picked
        return picked
    }

    private fun countReel() {
        val total = prefs.addReel(SupportedApp.INSTAGRAM)
        val app = prefs.todayCount(SupportedApp.INSTAGRAM)
        val limits = prefs.limits()
        val mood = MoodEvaluator.mood(total, app, limits)
        val reaction = MoodEvaluator.reactionFor(total, mood)
        val overage = MoodEvaluator.overage(total, app, limits)
        val remaining = MoodEvaluator.remaining(total, app, limits)

        val bubble = Messages.bubbleFor(
            mood = mood,
            previousMood = previousMood,
            total = total,
            remaining = remaining,
            overage = overage,
            listSize = { kind -> BubbleTexts.listSize(this, kind) }
        )
        var bubbleText = bubble?.let { BubbleTexts.bubbleText(this, it) }

        if (bubbleText != null && (mood == Mood.ANNOYED || mood == Mood.ANGRY || mood == Mood.FURIOUS || overage == 1)) {
            val custom = prefs.customBubbles()
            if (custom.isNotEmpty()) {
                val line = pickCustomBubble(custom)
                bubbleText = CustomTexts.fillNumber(line, overage)
                customBubbleCount++
            }
        }

        val limitForBadge = getLimitForBadge(total, app, limits)

        overlay.onReel(total, limitForBadge, mood, reaction, bubbleText)

        val breakCard = MoodEvaluator.breakFor(overage, limits.breakAfter)
        val breakShown = breakCard != null
        breakCard?.let {
            overlay.showTouchGrass(total, it.overage, it.countdownSeconds)
        }

        if ((overage == 1 && prefs.soundOnLimit) || (breakShown && prefs.soundOnBreak)) {
            soundPlayer?.play()
        } else if (overage > 1 && prefs.soundEveryOver) {
            soundPlayer?.playChirp()
        }

        previousMood = mood

        if (BuildConfig.DEBUG) {
            Log.d("YamiDetect", "count total=$total mood=$mood reaction=$reaction")
        }
    }

    override fun onInterrupt() {
        // No-op per SPEC
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        if (isCleanedUp) return
        isCleanedUp = true

        soundPlayer?.release()
        soundPlayer = null

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenOffReceiver)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error unregistering receiver", e)
            }
            isReceiverRegistered = false
        }

        if (::prefs.isInitialized) {
            prefs.unregister(prefListener)
        }

        handler.removeCallbacksAndMessages(null)

        if (::overlay.isInitialized) {
            overlay.destroy()
        }
    }
}
