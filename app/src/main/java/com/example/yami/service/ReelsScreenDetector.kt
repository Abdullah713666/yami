package com.example.yami.service

import android.os.Handler
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.yami.core.SupportedApp

class ReelsScreenDetector(
    private val app: SupportedApp,
    private val handler: Handler,
    private val rootProvider: () -> AccessibilityNodeInfo?,
    private val onChange: (Boolean) -> Unit
) {
    var inReels: Boolean = false
        private set

    private var lastCheckTime = -400L
    private var hasPendingTrailingCheck = false

    private val checkRunnable = Runnable {
        hasPendingTrailingCheck = false
        lastCheckTime = SystemClock.uptimeMillis()
        performCheck()
    }

    fun onEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null && pkg != app.packageName && pkg != "com.example.yami") {
                // Another package's window appeared. It may be a real app switch, or just a system
                // panel on top of Instagram (volume slider, notification heads-up) that never
                // becomes the active window. Ask which window is active instead of assuming.
                checkNowAndSoon()
                return
            }
        }

        val pkg = event.packageName?.toString()
        if (pkg == "com.example.yami") {
            return
        }

        if (pkg == app.packageName || pkg == null) {
            scheduleCheck()
        }
    }

    private val lateCheckRunnable = Runnable { performCheck() }

    private fun checkNowAndSoon() {
        lastCheckTime = SystemClock.uptimeMillis()
        performCheck()
        handler.removeCallbacks(lateCheckRunnable)
        handler.postDelayed(lateCheckRunnable, 600L)
    }

    private fun scheduleCheck() {
        val now = SystemClock.uptimeMillis()
        if (now - lastCheckTime >= 400L) {
            lastCheckTime = now
            performCheck()
            if (!hasPendingTrailingCheck) {
                hasPendingTrailingCheck = true
                handler.postDelayed(checkRunnable, 400L)
            }
        } else {
            if (!hasPendingTrailingCheck) {
                hasPendingTrailingCheck = true
                val delay = 400L - (now - lastCheckTime)
                handler.postDelayed(checkRunnable, delay)
            }
        }
    }

    private fun performCheck() {
        val root = rootProvider()
        val nextInReels = when {
            root == null || root.packageName?.toString() == "com.example.yami" -> inReels
            root.packageName?.toString() != app.packageName -> false
            else -> {
                val nodes = root.findAccessibilityNodeInfosByViewId(app.reelsPagerId)
                nodes?.any { it.isVisibleToUser } == true
            }
        }

        if (nextInReels != inReels) {
            inReels = nextInReels
            onChange(inReels)
        }
    }

    fun forceFalse() {
        handler.removeCallbacks(checkRunnable)
        handler.removeCallbacks(lateCheckRunnable)
        hasPendingTrailingCheck = false
        if (inReels) {
            inReels = false
            onChange(false)
        }
    }
}
