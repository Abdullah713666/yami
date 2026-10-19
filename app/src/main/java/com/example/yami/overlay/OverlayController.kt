package com.example.yami.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.example.yami.R
import com.example.yami.character.ShahidView
import com.example.yami.core.Mood
import com.example.yami.core.Reaction
import com.example.yami.data.YamiPrefs
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

data class OverlayState(
    val total: Int,
    val limitForBadge: Int?,
    val mood: Mood
)

class OverlayController(
    private val service: AccessibilityService,
    private val prefs: YamiPrefs
) {
    private enum class PhysicsState {
        IDLE, FLYING, DIZZY, DUSTING, WALKING, CLIMBING
    }

    private val windowManager = service.getSystemService(WindowManager::class.java)

    private val shahidView = ShahidView(service).apply {
        showBadge = true
    }
    private val yamiParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
    }
    private var isYamiAttached = false

    private val bubbleView = BubbleView(service)
    private val bubbleParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }
    private var isBubbleAttached = false

    private var touchGrassRoot: View? = null
    private var touchGrassCard: TouchGrassCard? = null
    private var isTouchGrassAttached = false

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())

    // Animation & Physics
    private var snapAnimator: ValueAnimator? = null
    private var flightAnimator: TimeAnimator? = null
    private var walkAnimator: ValueAnimator? = null
    private var climbAnimator: ValueAnimator? = null
    private var recoveryRunnable: Runnable? = null
    private var physicsState = PhysicsState.IDLE

    // Patrol
    private var patrolRunnable: Runnable? = null
    private var patrolAnimator: ValueAnimator? = null
    private var isPatrolling = false

    // Touch handling state
    private var downRawX = 0f
    private var downRawY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var downTime = 0L
    private var isDragging = false
    private var homeY = 0 // Body top before drag

    private var velocityTracker: VelocityTracker? = null
    private var shaken = false
    private val shakeFlipTimes = ArrayList<Long>()
    private var lastShakeSign = 0

    private var currentMood: Mood = Mood.CALM

    init {
        setupTouchListener()
        bubbleView.onHidden = {
            removeBubbleWindow()
            checkAndSchedulePatrol()
        }
    }

    private data class ScreenMetrics(
        val width: Int,
        val height: Int,
        val statusBarHeight: Int,
        val navBarHeight: Int
    )

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getScreenMetrics(): ScreenMetrics {
        val width: Int
        val height: Int
        val statusBar: Int
        val navBar: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            width = bounds.width()
            height = bounds.height()
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            statusBar = insets.top
            navBar = insets.bottom
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels

            val res = service.resources
            val sbId = res.getIdentifier("status_bar_height", "dimen", "android")
            statusBar = if (sbId > 0) res.getDimensionPixelSize(sbId) else 0

            val nbId = res.getIdentifier("navigation_bar_height", "dimen", "android")
            navBar = if (nbId > 0) res.getDimensionPixelSize(nbId) else 0
        }

        return ScreenMetrics(width, height, statusBar, navBar)
    }

    private fun getSpritePx(): Int {
        return max(1, yamiParams.width / ShahidView.STAGE_SIZE)
    }

    private fun calculateDimensions(metrics: ScreenMetrics) {
        val density = service.resources.displayMetrics.density
        val pixelDp = when (prefs.overlaySize) {
            "S" -> 2.5f
            "L" -> 3.5f
            else -> 3.0f
        }
        shahidView.pixelDp = pixelDp
        shahidView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val windowSize = shahidView.measuredWidth
        yamiParams.width = windowSize
        yamiParams.height = windowSize

        val px = max(1, windowSize / ShahidView.STAGE_SIZE)
        bubbleView.pixelPx = px

        val dp4 = (4f * density).roundToInt()
        val dp8 = (8f * density).roundToInt()
        val isLeft = prefs.overlaySide == "left"

        // Resting x: left side -> bodyLeft = 4dp (params.x = 4dp - 10*px);
        // right side -> bodyRight = screenW - 4dp (params.x = screenW - 4dp - 34*px)
        yamiParams.x = if (isLeft) dp4 - 10 * px else metrics.width - dp4 - 34 * px

        // Resting y: bodyTop = overlayYFrac * screenH clamped within [statusBar + 8dp, screenH - navBar - 8dp - 32*px]
        val bodyH = 32 * px
        val minY = metrics.statusBarHeight + dp8
        val maxY = metrics.height - metrics.navBarHeight - dp8 - bodyH
        val targetBodyTop = (prefs.overlayYFrac * metrics.height).roundToInt().coerceIn(minY, maxY.coerceAtLeast(minY))
        yamiParams.y = targetBodyTop - 8 * px

        shahidView.faceRight = isLeft
    }

    fun show(state: OverlayState) {
        if (!prefs.overlayEnabled) return

        val metrics = getScreenMetrics()
        calculateDimensions(metrics)
        currentMood = state.mood

        shahidView.setMood(state.mood, animate = false)
        shahidView.setCount(state.total, state.limitForBadge)

        if (!isYamiAttached) {
            try {
                windowManager.addView(shahidView, yamiParams)
                isYamiAttached = true
                shahidView.appear()
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error adding ShahidView", e)
            }
        }
        checkAndSchedulePatrol()
    }

    fun hide() {
        stopAllAnimations()
        hideBubbleNow()
        if (isYamiAttached) {
            shahidView.disappear {
                if (isYamiAttached) {
                    try {
                        windowManager.removeView(shahidView)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error removing ShahidView", e)
                    }
                    isYamiAttached = false
                }
            }
        }
    }

    fun onReel(total: Int, limit: Int?, mood: Mood, reaction: Reaction, bubbleText: String?) {
        if (isYamiAttached) {
            val previousMood = currentMood
            currentMood = mood
            shahidView.setCount(total, limit)
            shahidView.setMood(mood, animate = true)

            // Cancel patrol if mood changed away from CALM or if playing reaction
            if (previousMood == Mood.CALM && mood != Mood.CALM) {
                cancelPatrol()
            }

            if (physicsState == PhysicsState.IDLE) {
                cancelPatrol()
                shahidView.playReaction(reaction)

                if (bubbleText != null) {
                    val isAngry = mood == Mood.ANGRY || mood == Mood.FURIOUS
                    showBubble(bubbleText, isAngry)
                }
                checkAndSchedulePatrol()
            }
        }
    }

    private fun showBubble(text: String, angry: Boolean) {
        if (!isYamiAttached) return

        val density = service.resources.displayMetrics.density
        val dp4 = (4f * density).roundToInt()
        val isLeft = prefs.overlaySide == "left"
        val px = getSpritePx()
        bubbleView.pixelPx = px

        bubbleView.tail = if (isLeft) BubbleView.Tail.START else BubbleView.Tail.END
        bubbleView.show(text, angry)

        bubbleView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val bubbleW = bubbleView.measuredWidth
        val bubbleH = bubbleView.measuredHeight

        val bodyLeft = yamiParams.x + 10 * px
        val bodyRight = yamiParams.x + 34 * px
        val bodyTop = yamiParams.y + 8 * px
        val bodyHeight = 32 * px

        // Shahid on the right -> bubble right edge = bodyLeft + 4dp, tail END
        // on the left -> bubble left edge = bodyRight - 4dp, tail START
        bubbleParams.x = if (isLeft) {
            bodyRight - dp4
        } else {
            bodyLeft + dp4 - bubbleW
        }
        // bubble centre y = bodyTop + 0.28 * bodyHeight
        bubbleParams.y = (bodyTop + 0.28f * bodyHeight - bubbleH / 2f).roundToInt()

        if (!isBubbleAttached) {
            try {
                windowManager.addView(bubbleView, bubbleParams)
                isBubbleAttached = true
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error adding bubble view", e)
            }
        } else {
            try {
                windowManager.updateViewLayout(bubbleView, bubbleParams)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error updating bubble view", e)
            }
        }
    }

    private fun showBubbleFromResource(arrayRes: Int, angry: Boolean) {
        val msgs = service.resources.getStringArray(arrayRes)
        if (msgs.isNotEmpty()) {
            val text = msgs[random.nextInt(msgs.size)]
            showBubble(text, angry)
        }
    }

    private fun hideBubbleNow() {
        bubbleView.hideNow()
        removeBubbleWindow()
    }

    private fun removeBubbleWindow() {
        if (isBubbleAttached) {
            try {
                windowManager.removeView(bubbleView)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error removing bubble view", e)
            }
            isBubbleAttached = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val density = service.resources.displayMetrics.density
        val slopPx = 6f * density

        shahidView.setOnTouchListener { _, event ->
            val now = SystemClock.uptimeMillis()

            // Feed velocity tracker with raw coordinates
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain()
            }
            val copyEvent = MotionEvent.obtain(event).apply {
                setLocation(event.rawX, event.rawY)
            }
            velocityTracker?.addMovement(copyEvent)
            copyEvent.recycle()

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopAllAnimations()

                    downRawX = event.rawX
                    downRawY = event.rawY
                    initialWindowX = yamiParams.x
                    initialWindowY = yamiParams.y
                    downTime = now
                    isDragging = false

                    val px = getSpritePx()
                    homeY = yamiParams.y + 8 * px

                    shaken = false
                    shakeFlipTimes.clear()
                    lastShakeSign = 0
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    if (!isDragging && hypot(dx, dy) > slopPx) {
                        isDragging = true
                        hideBubbleNow()
                        shahidView.setDragged(true, 0f)
                    }

                    if (isDragging) {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val currentVx = velocityTracker?.xVelocity ?: 0f
                        shahidView.setDragged(true, currentVx)

                        yamiParams.x = (initialWindowX + dx).roundToInt()
                        yamiParams.y = (initialWindowY + dy).roundToInt()

                        if (isYamiAttached) {
                            try {
                                windowManager.updateViewLayout(shahidView, yamiParams)
                            } catch (e: Exception) {
                                Log.e("YamiDetect", "Error updating drag layout", e)
                            }
                        }

                        // Shake detection
                        val threshold900 = 900f * density
                        if (abs(currentVx) > threshold900) {
                            val sign = if (currentVx > 0f) 1 else -1
                            if (lastShakeSign != 0 && sign != lastShakeSign) {
                                shakeFlipTimes.add(now)
                                while (shakeFlipTimes.isNotEmpty() && now - shakeFlipTimes.first() > 1200L) {
                                    shakeFlipTimes.removeAt(0)
                                }
                                if (shakeFlipTimes.size >= 4) {
                                    shaken = true
                                }
                            }
                            lastShakeSign = sign
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val pressDuration = now - downTime
                    if (!isDragging && pressDuration < 300L) {
                        // Tap without drag
                        velocityTracker?.recycle()
                        velocityTracker = null

                        shahidView.playWave()
                        val total = prefs.todayTotal()
                        val limits = prefs.limits()
                        val text = service.getString(R.string.msg_tap_stats, total, limits.dailyLimit)
                        showBubble(text, angry = false)
                        checkAndSchedulePatrol()
                    } else if (isDragging) {
                        isDragging = false
                        velocityTracker?.computeCurrentVelocity(1000)
                        val vx = velocityTracker?.xVelocity ?: 0f
                        val vy = velocityTracker?.yVelocity ?: 0f
                        velocityTracker?.recycle()
                        velocityTracker = null

                        val speed = hypot(vx, vy)
                        val threshold1100 = 1100f * density

                        if (speed < threshold1100) {
                            // Slow drop
                            onSlowDrop()
                        } else {
                            // THROW
                            startFlight(vx, vy)
                        }
                    } else {
                        velocityTracker?.recycle()
                        velocityTracker = null
                        checkAndSchedulePatrol()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onSlowDrop() {
        shahidView.setDragged(false, 0f)

        val metrics = getScreenMetrics()
        val density = service.resources.displayMetrics.density
        val px = getSpritePx()
        val dp4 = (4f * density).roundToInt()
        val dp8 = (8f * density).roundToInt()

        val bodyMidX = yamiParams.x + 22 * px
        val snapLeft = bodyMidX < metrics.width / 2
        val targetSide = if (snapLeft) "left" else "right"
        val targetX = if (snapLeft) dp4 - 10 * px else metrics.width - dp4 - 34 * px

        val bodyH = 32 * px
        val minY = metrics.statusBarHeight + dp8
        val maxY = metrics.height - metrics.navBarHeight - dp8 - bodyH
        val currentBodyTop = yamiParams.y + 8 * px
        val clampedBodyTop = currentBodyTop.coerceIn(minY, maxY.coerceAtLeast(minY))
        yamiParams.y = clampedBodyTop - 8 * px

        shahidView.faceRight = snapLeft
        prefs.overlaySide = targetSide
        prefs.overlayYFrac = (clampedBodyTop.toFloat() / metrics.height.toFloat()).coerceIn(0f, 1f)

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(yamiParams.x, targetX).apply {
            duration = 260L
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                yamiParams.x = anim.animatedValue as Int
                if (isYamiAttached) {
                    try {
                        windowManager.updateViewLayout(shahidView, yamiParams)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error updating snap layout", e)
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shahidView.land()
                    if (shaken) {
                        shahidView.playDizzy(1400L) {}
                        showBubbleFromResource(R.array.msg_shaken, angry = false)
                    }
                    checkAndSchedulePatrol()
                }
            })
            start()
        }
    }

    private fun startFlight(initialVx: Float, initialVy: Float) {
        physicsState = PhysicsState.FLYING
        shahidView.setDragged(false, 0f)
        val spinDir = if (initialVx >= 0f) 1 else -1
        shahidView.setAirborne(true, spinDir)

        val density = service.resources.displayMetrics.density
        val metrics = getScreenMetrics()
        val px = getSpritePx()
        val dp8 = 8f * density
        val gravity = 2600f * density // 2600dp/s²
        val floorY = metrics.height - metrics.navBarHeight - dp8
        val ceilingY = metrics.statusBarHeight.toFloat()
        val screenW = metrics.width.toFloat()

        var floatX = yamiParams.x.toFloat()
        var floatY = yamiParams.y.toFloat()
        var vx = initialVx
        var vy = initialVy
        val flightStartTime = SystemClock.uptimeMillis()

        flightAnimator?.cancel()
        flightAnimator = TimeAnimator().apply {
            setTimeListener { _, _, deltaTime ->
                val dt = minOf(deltaTime, 32L) / 1000f

                vy += gravity * dt
                floatX += vx * dt
                floatY += vy * dt

                var bodyLeft = floatX + 10 * px
                var bodyRight = floatX + 34 * px
                var bodyTop = floatY + 8 * px
                var bodyBottom = floatY + 40 * px

                // Collisions on body box
                // 1. Left wall: bodyLeft < 0
                if (bodyLeft < 0f) {
                    bodyLeft = 0f
                    floatX = -10f * px
                    val impact = abs(vx)
                    vx = -vx * 0.55f
                    if (impact > 500f * density) {
                        shakeOnce()
                    }
                } else if (bodyRight > screenW) {
                    // 2. Right wall: bodyRight > screenW
                    bodyRight = screenW
                    floatX = screenW - 34f * px
                    val impact = abs(vx)
                    vx = -vx * 0.55f
                    if (impact > 500f * density) {
                        shakeOnce()
                    }
                }

                // 3. Ceiling: bodyTop < statusBar
                if (bodyTop < ceilingY) {
                    bodyTop = ceilingY
                    floatY = ceilingY - 8f * px
                    val impact = abs(vy)
                    vy = -vy * 0.55f
                    if (impact > 500f * density) {
                        shakeOnce()
                    }
                }

                // 4. Floor: bodyBottom > screenH - navBar - 8dp
                var onFloor = false
                if (bodyBottom >= floorY) {
                    bodyBottom = floorY
                    floatY = floorY - 40f * px
                    val impact = abs(vy)
                    vy = -vy * 0.45f
                    vx *= 0.8f
                    if (impact > 500f * density) {
                        shakeOnce()
                    }
                    onFloor = true
                }

                yamiParams.x = floatX.roundToInt()
                yamiParams.y = floatY.roundToInt()
                if (isYamiAttached) {
                    try {
                        windowManager.updateViewLayout(shahidView, yamiParams)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error updating flight layout", e)
                    }
                }

                val now = SystemClock.uptimeMillis()
                val stopThreshold = 250f * density
                val stoppedOnFloor = onFloor && abs(vy) < stopThreshold
                val timedOut = (now - flightStartTime) >= 4000L

                if (stoppedOnFloor || timedOut) {
                    flightAnimator?.cancel()
                    flightAnimator = null
                    startDizzyRecovery()
                }
            }
            start()
        }
    }

    private fun shakeOnce() {
        Log.d("YamiPhysics", "Collision impact shake")
    }

    private fun startDizzyRecovery() {
        shahidView.setAirborne(false, 0)
        physicsState = PhysicsState.DIZZY
        shahidView.playDizzy(1600L) {}
        showBubbleFromResource(R.array.msg_thrown, angry = true)

        recoveryRunnable = Runnable {
            startDustingRecovery()
        }
        handler.postDelayed(recoveryRunnable!!, 1600L)
    }

    private fun startDustingRecovery() {
        physicsState = PhysicsState.DUSTING
        hideBubbleNow()
        shahidView.playDustOff {}

        recoveryRunnable = Runnable {
            startWalkingRecovery()
        }
        handler.postDelayed(recoveryRunnable!!, 900L)
    }

    private fun startWalkingRecovery() {
        physicsState = PhysicsState.WALKING

        val metrics = getScreenMetrics()
        val density = service.resources.displayMetrics.density
        val px = getSpritePx()
        val dp4 = (4f * density).roundToInt()

        val bodyMidX = yamiParams.x + 22 * px
        val targetLeft = bodyMidX < metrics.width / 2
        val targetSide = if (targetLeft) "left" else "right"
        val restingX = if (targetLeft) dp4 - 10 * px else metrics.width - dp4 - 34 * px

        shahidView.faceRight = targetLeft

        val currentX = yamiParams.x
        val dist = restingX - currentX
        if (abs(dist) <= 2) {
            yamiParams.x = restingX
            startClimbingRecovery(targetSide)
            return
        }

        val walkSpeed = 110f * density // 110 dp/s
        val duration = (abs(dist) / walkSpeed * 1000f).roundToLong().coerceAtLeast(100L)
        val dir = if (dist > 0) 1 else -1
        shahidView.setWalking(dir)

        walkAnimator?.cancel()
        walkAnimator = ValueAnimator.ofInt(currentX, restingX).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                yamiParams.x = anim.animatedValue as Int
                if (isYamiAttached) {
                    try {
                        windowManager.updateViewLayout(shahidView, yamiParams)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error updating walking layout", e)
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shahidView.setWalking(0)
                    startClimbingRecovery(targetSide)
                }
            })
            start()
        }
    }

    private fun startClimbingRecovery(targetSide: String) {
        physicsState = PhysicsState.CLIMBING

        val density = service.resources.displayMetrics.density
        val px = getSpritePx()
        val targetY = homeY - 8 * px
        val currentY = yamiParams.y
        val distY = targetY - currentY
        val dp4 = 4f * density

        if (abs(distY) <= dp4) {
            yamiParams.y = targetY
            finishRecovery(targetSide)
            return
        }

        val climbSpeed = 160f * density // 160 dp/s
        val duration = (abs(distY) / climbSpeed * 1000f).roundToLong().coerceAtLeast(100L)
        shahidView.setClimbing(true)

        climbAnimator?.cancel()
        climbAnimator = ValueAnimator.ofInt(currentY, targetY).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                yamiParams.y = anim.animatedValue as Int
                if (isYamiAttached) {
                    try {
                        windowManager.updateViewLayout(shahidView, yamiParams)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error updating climbing layout", e)
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shahidView.setClimbing(false)
                    finishRecovery(targetSide)
                }
            })
            start()
        }
    }

    private fun finishRecovery(targetSide: String) {
        shahidView.setClimbing(false)
        physicsState = PhysicsState.IDLE

        val metrics = getScreenMetrics()
        prefs.overlaySide = targetSide
        prefs.overlayYFrac = (homeY.toFloat() / metrics.height.toFloat()).coerceIn(0f, 1f)

        checkAndSchedulePatrol()
    }

    private fun checkAndSchedulePatrol() {
        cancelPatrolTimer()
        if (!isYamiAttached || physicsState != PhysicsState.IDLE || currentMood != Mood.CALM || isBubbleAttached) {
            return
        }
        val delay = random.nextLong(18_000L, 35_001L)
        patrolRunnable = Runnable {
            tryStartPatrol()
        }
        handler.postDelayed(patrolRunnable!!, delay)
    }

    private fun cancelPatrolTimer() {
        patrolRunnable?.let { handler.removeCallbacks(it) }
        patrolRunnable = null
    }

    private fun cancelPatrol() {
        cancelPatrolTimer()
        patrolAnimator?.cancel()
        patrolAnimator = null
        if (isPatrolling) {
            isPatrolling = false
            shahidView.setWalking(0)
            val metrics = getScreenMetrics()
            val density = service.resources.displayMetrics.density
            val px = getSpritePx()
            val dp4 = (4f * density).roundToInt()
            val isLeft = prefs.overlaySide == "left"
            yamiParams.x = if (isLeft) dp4 - 10 * px else metrics.width - dp4 - 34 * px
            if (isYamiAttached) {
                try {
                    windowManager.updateViewLayout(shahidView, yamiParams)
                } catch (e: Exception) {
                    Log.e("YamiDetect", "Error resetting patrol position", e)
                }
            }
        }
    }

    private fun tryStartPatrol() {
        if (!isYamiAttached || physicsState != PhysicsState.IDLE || currentMood != Mood.CALM ||
            isBubbleAttached || shahidView.isBusy || shahidView.isSleeping
        ) {
            checkAndSchedulePatrol()
            return
        }

        isPatrolling = true
        val density = service.resources.displayMetrics.density
        val metrics = getScreenMetrics()
        val px = getSpritePx()
        val dp4 = (4f * density).roundToInt()
        val dp56 = (56f * density).roundToInt()
        val isLeft = prefs.overlaySide == "left"
        val restingX = if (isLeft) dp4 - 10 * px else metrics.width - dp4 - 34 * px

        val targetX = if (isLeft) restingX + dp56 else restingX - dp56
        val dirOut = if (isLeft) 1 else -1
        val speed = 45f * density // 45 dp/s
        val duration = (dp56 / speed * 1000f).roundToLong()

        shahidView.setWalking(dirOut)
        patrolAnimator = ValueAnimator.ofInt(restingX, targetX).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                yamiParams.x = anim.animatedValue as Int
                if (isYamiAttached) {
                    try {
                        windowManager.updateViewLayout(shahidView, yamiParams)
                    } catch (e: Exception) {
                        Log.e("YamiDetect", "Error updating patrol step out", e)
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    shahidView.setWalking(0)
                    handler.postDelayed({
                        if (!isPatrolling) return@postDelayed
                        val dirBack = -dirOut
                        shahidView.setWalking(dirBack)
                        patrolAnimator = ValueAnimator.ofInt(targetX, restingX).apply {
                            this.duration = duration
                            interpolator = LinearInterpolator()
                            addUpdateListener { animBack ->
                                yamiParams.x = animBack.animatedValue as Int
                                if (isYamiAttached) {
                                    try {
                                        windowManager.updateViewLayout(shahidView, yamiParams)
                                    } catch (e: Exception) {
                                        Log.e("YamiDetect", "Error updating patrol step back", e)
                                    }
                                }
                            }
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animBackEnd: Animator) {
                                    shahidView.setWalking(0)
                                    isPatrolling = false
                                    checkAndSchedulePatrol()
                                }
                            })
                            start()
                        }
                    }, 700L)
                }
            })
            start()
        }
    }

    private fun stopAllAnimations() {
        snapAnimator?.cancel()
        snapAnimator = null

        flightAnimator?.cancel()
        flightAnimator = null

        walkAnimator?.cancel()
        walkAnimator = null

        climbAnimator?.cancel()
        climbAnimator = null

        recoveryRunnable?.let { handler.removeCallbacks(it) }
        recoveryRunnable = null

        cancelPatrol()

        if (physicsState != PhysicsState.IDLE) {
            shahidView.setAirborne(false, 0)
            shahidView.setWalking(0)
            shahidView.setClimbing(false)
            physicsState = PhysicsState.IDLE
        }
    }

    @SuppressLint("InflateParams")
    fun showTouchGrass(total: Int, overage: Int, countdownSeconds: Int) {
        if (isTouchGrassAttached) return

        val themedContext = ContextThemeWrapper(service, R.style.Theme_Yami)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.overlay_touch_grass, null)
        touchGrassRoot = root

        val tgParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
        }

        val card = TouchGrassCard(
            root = root,
            onTakeMeOut = {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                removeTouchGrass()
            },
            onOneMore = {
                removeTouchGrass()
            }
        )
        touchGrassCard = card

        try {
            windowManager.addView(root, tgParams)
            isTouchGrassAttached = true
            card.start(prefs.pickBreakMessage(), total, overage, countdownSeconds)
        } catch (e: Exception) {
            Log.e("YamiDetect", "Error adding touch grass view", e)
        }
    }

    private fun removeTouchGrass() {
        if (isTouchGrassAttached && touchGrassRoot != null) {
            touchGrassCard?.cancel()
            touchGrassCard = null
            try {
                windowManager.removeView(touchGrassRoot)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error removing touch grass view", e)
            }
            touchGrassRoot = null
            isTouchGrassAttached = false
        }
    }

    fun onPrefsChanged() {
        if (!prefs.overlayEnabled) {
            hide()
            return
        }

        if (isYamiAttached) {
            val metrics = getScreenMetrics()
            calculateDimensions(metrics)
            try {
                windowManager.updateViewLayout(shahidView, yamiParams)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error updating ShahidView layout on prefs change", e)
            }
            checkAndSchedulePatrol()
        }
    }

    fun destroy() {
        stopAllAnimations()
        removeTouchGrass()
        hideBubbleNow()

        if (isYamiAttached) {
            try {
                windowManager.removeView(shahidView)
            } catch (e: Exception) {
                Log.e("YamiDetect", "Error removing ShahidView on destroy", e)
            }
            isYamiAttached = false
        }
    }
}
