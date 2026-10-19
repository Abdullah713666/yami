package com.example.yami.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.example.yami.core.Mood
import com.example.yami.core.Reaction
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class PropAt(
    val name: String,
    val x: Int,
    val y: Int,
    val anchor: Anchor = Anchor.BODY,
    val behind: Boolean = false,
    val flipX: Boolean = false
)

enum class Anchor { BODY, HAND_R }

data class Frame(
    val ms: Int,
    val face: String? = null,
    val armR: String? = null,
    val armL: String? = null,
    val legs: String? = null,
    val dy: Int? = null,
    val props: List<PropAt>? = null,
    val shakeX: Int = 0,
    val rotation: Int? = null
)

class Action(val frames: List<Frame>, val loop: Boolean) {
    val totalMs: Int = frames.sumOf { it.ms }
}

class ShahidView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val STAGE_SIZE = 44
        const val BODY_X = 10
        const val BODY_Y = 8

        private val DEFAULT_FRAME = Frame(ms = 1000)

        // Pre-allocated static props lists
        private val PROPS_SWEAT = listOf(PropAt("sweat", 18, 4, Anchor.BODY))
        private val PROPS_WHISTLE_1 = listOf(PropAt("whistle_notes", 5, -6, Anchor.HAND_R))
        private val PROPS_WHISTLE_2 = listOf(PropAt("whistle_notes", 5, -7, Anchor.HAND_R))

        private val PROPS_ANGRY_1 = listOf(
            PropAt("baton", 0, -9, Anchor.HAND_R, behind = true),
            PropAt("steam_a", -1, -2, Anchor.BODY),
            PropAt("steam_a", 21, -2, Anchor.BODY, flipX = true)
        )
        private val PROPS_ANGRY_2 = listOf(
            PropAt("steam_b", -1, -2, Anchor.BODY),
            PropAt("steam_b", 21, -2, Anchor.BODY, flipX = true)
        )

        private val PROPS_FURIOUS_1 = listOf(
            PropAt("bang", 11, -8, Anchor.BODY),
            PropAt("steam_a", -1, -2, Anchor.BODY),
            PropAt("steam_a", 21, -2, Anchor.BODY, flipX = true)
        )
        private val PROPS_FURIOUS_2 = listOf(
            PropAt("steam_b", -1, -2, Anchor.BODY),
            PropAt("steam_b", 21, -2, Anchor.BODY, flipX = true)
        )

        private val PROPS_CHALLAN = listOf(PropAt("challan", 1, -6, Anchor.HAND_R))
        private val PROPS_ZZZ_1 = listOf(PropAt("zzz", 19, -5, Anchor.BODY))
        private val PROPS_ZZZ_2 = listOf(PropAt("zzz", 19, -6, Anchor.BODY))

        private val PROPS_HEART_1 = listOf(PropAt("heart", 11, -6, Anchor.BODY))
        private val PROPS_HEART_2 = listOf(PropAt("heart", 11, -8, Anchor.BODY))
        private val PROPS_BATON_BEHIND = listOf(PropAt("baton", 0, -9, Anchor.HAND_R, behind = true))
        private val PROPS_LAND_DUST = listOf(
            PropAt("dust", 1, 28, Anchor.BODY),
            PropAt("dust", 17, 28, Anchor.BODY)
        )

        private val STARS_0 = listOf(PropAt("star", 10, -6), PropAt("star", 19, -3), PropAt("star", 10, -1))
        private val STARS_1 = listOf(PropAt("star", 1, -3), PropAt("star", 19, -3), PropAt("star", 10, -1))
        private val STARS_2 = listOf(PropAt("star", 1, -3), PropAt("star", 10, -6), PropAt("star", 10, -1))
        private val STARS_3 = listOf(PropAt("star", 1, -3), PropAt("star", 10, -6), PropAt("star", 19, -3))
        private val DIZZY_STARS = arrayOf(STARS_0, STARS_1, STARS_2, STARS_3)

        // R3: Mood base actions
        val ACTION_CALM = Action(
            listOf(
                Frame(ms = 700, dy = 0),
                Frame(ms = 700, dy = 1)
            ),
            loop = true
        )

        val ACTION_CONCERNED = Action(
            listOf(
                Frame(ms = 500, face = "worried", armR = "across", dy = 0, props = PROPS_SWEAT),
                Frame(ms = 300, face = "worried", armR = "across", dy = 0, props = emptyList()),
                Frame(ms = 700, face = "worried", armR = "across", dy = 1, props = PROPS_SWEAT)
            ),
            loop = true
        )

        val ACTION_LIMIT_HIT = Action(
            listOf(
                Frame(ms = 200, face = "whistle", armR = "mouth", armL = "out", props = PROPS_WHISTLE_1),
                Frame(ms = 200, face = "whistle", armR = "mouth", armL = "out", props = PROPS_WHISTLE_2)
            ),
            loop = true
        )

        val ACTION_ANNOYED = Action(
            listOf(
                Frame(ms = 300, face = "angry", armR = "hip", armL = "hip", legs = "stand"),
                Frame(ms = 300, face = "angry", armR = "hip", armL = "hip", legs = "tap")
            ),
            loop = true
        )

        val ACTION_ANGRY = Action(
            listOf(
                Frame(ms = 350, face = "angry", armR = "up", armL = "hip", props = PROPS_ANGRY_1),
                Frame(ms = 350, face = "angry", armR = "hip", armL = "hip", props = PROPS_ANGRY_2)
            ),
            loop = true
        )

        val ACTION_FURIOUS = Action(
            listOf(
                Frame(ms = 180, face = "furious", armR = "wave_a", armL = "wave_b", legs = "tuck", dy = -3, props = PROPS_FURIOUS_1),
                Frame(ms = 180, face = "furious", armR = "flail_b", armL = "flail_a", legs = "stand", dy = 0, props = PROPS_FURIOUS_2)
            ),
            loop = true
        )

        val ACTION_BREAK = Action(
            listOf(
                Frame(ms = 400, face = "shout", armR = "out", props = PROPS_CHALLAN),
                Frame(ms = 400, face = "normal", armR = "out", props = PROPS_CHALLAN)
            ),
            loop = true
        )

        val ACTION_SLEEP = Action(
            listOf(
                Frame(ms = 600, face = "blink", legs = "sit", dy = 4, props = PROPS_ZZZ_1),
                Frame(ms = 600, face = "blink", legs = "sit", dy = 4, props = PROPS_ZZZ_2)
            ),
            loop = true
        )

        val ACTION_WAKE = Action(
            listOf(
                Frame(ms = 300, face = "surprised", armR = "up", armL = "up")
            ),
            loop = false
        )

        // R4: Reactions
        val ACTION_BOING_HAPPY = Action(
            listOf(
                Frame(ms = 140, face = "happy", legs = "tuck", dy = -3),
                Frame(ms = 140, legs = "stand", dy = 0)
            ),
            loop = false
        )

        val ACTION_BOING_NORMAL = Action(
            listOf(
                Frame(ms = 140, legs = "tuck", dy = -3),
                Frame(ms = 140, legs = "stand", dy = 0)
            ),
            loop = false
        )

        val ACTION_MILESTONE_HAPPY = Action(
            listOf(
                Frame(ms = 900, face = "happy", armR = "salute", props = PROPS_HEART_1),
                Frame(ms = 300, face = "happy", armR = "salute", props = PROPS_HEART_2),
                Frame(ms = 140, face = "happy", legs = "tuck", dy = -3),
                Frame(ms = 140, legs = "stand", dy = 0)
            ),
            loop = false
        )

        val ACTION_MILESTONE_NORMAL = Action(
            listOf(
                Frame(ms = 900, face = "happy", armR = "salute", props = PROPS_HEART_1),
                Frame(ms = 300, face = "happy", armR = "salute", props = PROPS_HEART_2),
                Frame(ms = 140, legs = "tuck", dy = -3),
                Frame(ms = 140, legs = "stand", dy = 0)
            ),
            loop = false
        )

        val ACTION_LIMIT_REACHED = Action(
            buildList {
                add(Frame(ms = 300, face = "surprised", armR = "up", armL = "up"))
                repeat(6) {
                    add(Frame(ms = 200, face = "whistle", armR = "mouth", armL = "out", props = PROPS_WHISTLE_1))
                    add(Frame(ms = 200, face = "whistle", armR = "mouth", armL = "out", props = PROPS_WHISTLE_2))
                }
            },
            loop = false
        )

        val ACTION_HEAD_SHAKE = Action(
            buildList {
                repeat(4) {
                    add(Frame(ms = 110, face = "look_l", armR = "out"))
                    add(Frame(ms = 110, face = "look_r", armR = "wave_b"))
                }
            },
            loop = false
        )

        val ACTION_ANGRY_SHAKE = Action(
            buildList {
                repeat(6) {
                    add(Frame(ms = 100, armR = "up", props = PROPS_BATON_BEHIND, shakeX = 1))
                    add(Frame(ms = 100, armR = "hip", shakeX = -1))
                }
            },
            loop = false
        )

        val ACTION_FURIOUS_SHAKE = Action(
            buildList {
                var remaining = 1000
                var shake = 1
                while (remaining > 0) {
                    val frameMs = minOf(80, remaining)
                    add(Frame(ms = frameMs, face = "shout", armR = "out", props = PROPS_CHALLAN, shakeX = shake))
                    shake = -shake
                    remaining -= frameMs
                }
            },
            loop = false
        )

        val ACTION_WAVE = Action(
            listOf(Frame(ms = 800, armR = "salute")),
            loop = false
        )

        val ACTION_DISAPPEAR_SALUTE = Action(
            listOf(Frame(ms = 300, armR = "salute")),
            loop = false
        )

        val ACTION_LAND = Action(
            listOf(
                Frame(ms = 120, legs = "tuck", dy = -1, props = PROPS_LAND_DUST),
                Frame(ms = 200, legs = "stand", props = PROPS_LAND_DUST)
            ),
            loop = false
        )

        val ACTION_DUST_OFF = Action(
            buildList {
                repeat(3) {
                    add(Frame(ms = 150, face = "angry", armR = "across", armL = "down"))
                    add(Frame(ms = 150, face = "angry", armR = "down", armL = "across"))
                }
            },
            loop = false
        )

        val ACTION_DRAGGED_SLOW = Action(
            listOf(
                Frame(ms = 160, face = "surprised", armR = "flail_a", armL = "flail_b", legs = "dangle_a"),
                Frame(ms = 160, face = "surprised", armR = "flail_b", armL = "flail_a", legs = "dangle_b")
            ),
            loop = true
        )

        val ACTION_DRAGGED_FAST = Action(
            listOf(
                Frame(ms = 90, face = "surprised", armR = "flail_a", armL = "flail_b", legs = "dangle_a"),
                Frame(ms = 90, face = "surprised", armR = "flail_b", armL = "flail_a", legs = "dangle_b")
            ),
            loop = true
        )

        val ACTION_WALK_LEFT = Action(
            listOf(
                Frame(ms = 180, face = "look_l", armR = "hip", legs = "step_a"),
                Frame(ms = 180, face = "look_l", armR = "down", legs = "step_b")
            ),
            loop = true
        )

        val ACTION_WALK_RIGHT = Action(
            listOf(
                Frame(ms = 180, face = "look_r", armR = "hip", legs = "step_a"),
                Frame(ms = 180, face = "look_r", armR = "down", legs = "step_b")
            ),
            loop = true
        )

        val ACTION_CLIMB = Action(
            listOf(
                Frame(ms = 180, armR = "up", armL = "down", legs = "step_a"),
                Frame(ms = 180, armR = "down", armL = "up", legs = "step_b")
            ),
            loop = true
        )
    }

    // --- Pre-allocated memory for Rendering (NO allocations in onDraw) ---
    private val framebuffer = IntArray(STAGE_SIZE * STAGE_SIZE)
    private val paletteNormal = IntArray(128)
    private val paletteAngry = IntArray(128)
    private val paletteFurious = IntArray(128)
    private val bitmap = Bitmap.createBitmap(STAGE_SIZE, STAGE_SIZE, Bitmap.Config.ARGB_8888)

    private val bitmapPaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
    }

    private val badgeFillPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val badgeOutlinePaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val destRect = Rect()
    private val badgePath = android.graphics.Path()
    private val badgeInnerPath = android.graphics.Path()

    // --- Public API Properties ---
    var pixelDp: Float = 3f
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    var faceRight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var showBadge: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // --- State & Timers ---
    private var currentPx: Int = 1
    private var currentMood: Mood = Mood.CALM
    private var baseStartTime: Long = 0L

    private var badgeCount: Int = 0
    private var badgeLimit: Int? = null
    private var isOverLimit: Boolean = false
    private var cachedBadgeText: String = "0"
    private var cachedBadgeTextWidth: Float = 0f

    private var lastActivityTime: Long = 0L
    var isSleeping: Boolean = false
        private set

    val isBusy: Boolean
        get() = currentReaction != null || isDizzy || isDustOff || isWalking || isClimbing || isDragged || isAirborne

    private var sleepStartTime: Long = 0L

    // Reactions & Overrides
    private var currentReaction: Action? = null
    private var reactionStartTime: Long = 0L
    private var currentReactionTotalMs: Int = 0
    private var reactionCallback: (() -> Unit)? = null

    // Continuous states
    private var isDragged: Boolean = false
    private var dragVx: Float = 0f
    private var dragStartTime: Long = 0L

    private var isAirborne: Boolean = false
    private var airborneSpinDir: Int = 1
    private var airborneStartTime: Long = 0L
    private var airborneRotation: Int = 0

    private var isWalking: Boolean = false
    private var walkDir: Int = 0
    private var walkStartTime: Long = 0L

    private var isClimbing: Boolean = false
    private var climbStartTime: Long = 0L

    private var isDizzy: Boolean = false
    private var dizzyStartTime: Long = 0L
    private var dizzyDuration: Long = 0L
    private var dizzyCallback: (() -> Unit)? = null

    private var isDustOff: Boolean = false
    private var dustOffStartTime: Long = 0L
    private var dustOffCallback: (() -> Unit)? = null

    // Overlays: Blink & Look
    private val random = Random(42)
    private var nextBlinkTime: Long = 0L
    private var nextLookTime: Long = 0L
    private var currentLookFace: String = "look_l"

    // Caching of resolved frame for Bitmap recomposition
    private var cachedFace: String? = null
    private var cachedArmR: String? = null
    private var cachedArmL: String? = null
    private var cachedLegs: String? = null
    private var cachedDy: Int? = null
    private var cachedProps: List<PropAt>? = null
    private var cachedMoodSkin: Int? = null
    private var cachedFaceRight: Boolean? = null

    init {
        // Build palettes
        for ((c, color) in ShahidSprites.PALETTE) {
            val code = c.code
            if (code in 0..127) {
                paletteNormal[code] = color
                paletteAngry[code] = color
                paletteFurious[code] = color
            }
        }
        val fCode = 'F'.code
        paletteAngry[fCode] = 0xFFC9775A.toInt()
        paletteFurious[fCode] = 0xFFCF6A52.toInt()

        val now = SystemClock.uptimeMillis()
        baseStartTime = now
        lastActivityTime = now
        nextBlinkTime = now + random.nextLong(2500, 5001)
        nextLookTime = now + random.nextLong(4000, 8001)
        currentLookFace = if (random.nextBoolean()) "look_l" else "look_r"
        updateBadgeTextMetrics()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val px = if (widthMode == MeasureSpec.EXACTLY) {
            max(1, widthSize / STAGE_SIZE)
        } else {
            val calculatedPx = max(1, (pixelDp * resources.displayMetrics.density).roundToInt())
            if (widthMode == MeasureSpec.AT_MOST) {
                val maxPx = widthSize / STAGE_SIZE
                if (maxPx > 0) min(calculatedPx, maxPx) else calculatedPx
            } else {
                calculatedPx
            }
        }
        currentPx = px
        val size = STAGE_SIZE * px
        destRect.set(0, 0, size, size)
        updateBadgeTextMetrics()
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val px = max(1, w / STAGE_SIZE)
        currentPx = px
        val size = STAGE_SIZE * px
        destRect.set(0, 0, size, size)
        updateBadgeTextMetrics()
    }

    private fun updateBadgeTextMetrics() {
        badgeTextPaint.textSize = 6f * currentPx
        cachedBadgeTextWidth = badgeTextPaint.measureText(cachedBadgeText)
    }

    fun setMood(mood: Mood, animate: Boolean = true) {
        if (currentMood != mood) {
            currentMood = mood
            baseStartTime = SystemClock.uptimeMillis()
        }
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        invalidate()
    }

    fun setCount(count: Int, limit: Int?) {
        if (badgeCount != count || badgeLimit != limit) {
            wakeUpIfNeeded()
            lastActivityTime = SystemClock.uptimeMillis()
        }
        badgeCount = count
        badgeLimit = limit
        isOverLimit = limit != null && count > limit
        cachedBadgeText = if (limit != null) "$count/$limit" else count.toString()
        updateBadgeTextMetrics()
        invalidate()
    }

    fun playReaction(r: Reaction) {
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        val isCalmOrConcerned = currentMood == Mood.CALM || currentMood == Mood.CONCERNED
        val action = when (r) {
            Reaction.BOING -> if (isCalmOrConcerned) ACTION_BOING_HAPPY else ACTION_BOING_NORMAL
            Reaction.MILESTONE -> if (isCalmOrConcerned) ACTION_MILESTONE_HAPPY else ACTION_MILESTONE_NORMAL
            Reaction.LIMIT_REACHED -> ACTION_LIMIT_REACHED
            Reaction.HEAD_SHAKE -> ACTION_HEAD_SHAKE
            Reaction.ANGRY_SHAKE -> ACTION_ANGRY_SHAKE
            Reaction.FURIOUS_SHAKE -> ACTION_FURIOUS_SHAKE
        }
        playCustomReaction(action, null)
    }

    fun playWave() {
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        playCustomReaction(ACTION_WAVE, null)
    }

    fun land() {
        isDragged = false
        isAirborne = false
        airborneRotation = 0
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        playCustomReaction(ACTION_LAND, null)
    }

    fun playDizzy(ms: Long, onEnd: () -> Unit = {}) {
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        isDizzy = true
        dizzyStartTime = SystemClock.uptimeMillis()
        dizzyDuration = ms
        dizzyCallback = onEnd
        invalidate()
    }

    fun playDustOff(onEnd: () -> Unit = {}) {
        wakeUpIfNeeded()
        lastActivityTime = SystemClock.uptimeMillis()
        isDustOff = true
        dustOffStartTime = SystemClock.uptimeMillis()
        dustOffCallback = onEnd
        invalidate()
    }

    fun setDragged(dragging: Boolean, vx: Float) {
        if (dragging) {
            wakeUpIfNeeded()
            lastActivityTime = SystemClock.uptimeMillis()
            if (!isDragged) {
                dragStartTime = SystemClock.uptimeMillis()
            }
        }
        isDragged = dragging
        dragVx = vx
        invalidate()
    }

    fun setAirborne(airborne: Boolean, spinDir: Int) {
        if (airborne) {
            wakeUpIfNeeded()
            lastActivityTime = SystemClock.uptimeMillis()
            if (!isAirborne) {
                airborneStartTime = SystemClock.uptimeMillis()
                dragStartTime = airborneStartTime
            }
            airborneSpinDir = if (spinDir >= 0) 1 else -1
        } else {
            airborneRotation = 0
        }
        isAirborne = airborne
        invalidate()
    }

    fun setWalking(dir: Int) {
        if (dir != 0) {
            wakeUpIfNeeded()
            lastActivityTime = SystemClock.uptimeMillis()
            isWalking = true
            walkDir = dir
            walkStartTime = SystemClock.uptimeMillis()
        } else {
            isWalking = false
        }
        invalidate()
    }

    fun setClimbing(climbing: Boolean) {
        if (climbing) {
            wakeUpIfNeeded()
            lastActivityTime = SystemClock.uptimeMillis()
            climbStartTime = SystemClock.uptimeMillis()
        }
        isClimbing = climbing
        invalidate()
    }

    fun appear() {
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(160L)
            .withEndAction {
                playReaction(Reaction.BOING)
                postDelayed({
                    playWave()
                }, 280L)
            }
            .start()
    }

    fun disappear(onEnd: () -> Unit) {
        playCustomReaction(ACTION_DISAPPEAR_SALUTE) {
            animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction {
                    onEnd()
                }
                .start()
        }
    }

    fun playAction(name: String) {
        when (name) {
            "milestone" -> playReaction(Reaction.MILESTONE)
            "limit" -> playReaction(Reaction.LIMIT_REACHED)
            "annoyed" -> playReaction(Reaction.HEAD_SHAKE)
            "angry" -> playReaction(Reaction.ANGRY_SHAKE)
            "furious" -> playReaction(Reaction.FURIOUS_SHAKE)
            "dizzy" -> playDizzy(2000L) {}
            "dustoff" -> playDustOff {}
            "walk" -> {
                setWalking(1)
                postDelayed({ setWalking(0) }, 2000L)
            }
            "climb" -> {
                setClimbing(true)
                postDelayed({ setClimbing(false) }, 2000L)
            }
            "sleep" -> {
                isSleeping = true
                sleepStartTime = SystemClock.uptimeMillis()
                currentReaction = null
                invalidate()
            }
            "dragged" -> {
                setDragged(true, 0f)
                postDelayed({
                    setDragged(false, 0f)
                    land()
                }, 2000L)
            }
        }
    }

    fun bodyRectPx(out: Rect) {
        out.set(
            10 * currentPx,
            8 * currentPx,
            34 * currentPx,
            40 * currentPx
        )
    }

    private fun playCustomReaction(action: Action, onEnd: (() -> Unit)?) {
        currentReaction = action
        reactionStartTime = SystemClock.uptimeMillis()
        currentReactionTotalMs = action.totalMs
        reactionCallback = onEnd
        invalidate()
    }

    private fun wakeUpIfNeeded() {
        if (isSleeping) {
            isSleeping = false
            currentReaction = ACTION_WAKE
            reactionStartTime = SystemClock.uptimeMillis()
            currentReactionTotalMs = ACTION_WAKE.totalMs
            reactionCallback = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postInvalidateOnAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE && isShown) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()

        // 1. Check sleep transition (CALM only, 25 s inactivity)
        if (currentMood == Mood.CALM && !isSleeping && currentReaction == null &&
            !isDragged && !isAirborne && !isDizzy && !isDustOff && !isWalking && !isClimbing
        ) {
            if (now - lastActivityTime >= 25_000L) {
                isSleeping = true
                sleepStartTime = now
            }
        }

        // 2. Resolve base frame
        val baseAction = if (isSleeping) {
            ACTION_SLEEP
        } else {
            when (currentMood) {
                Mood.CALM -> ACTION_CALM
                Mood.CONCERNED -> ACTION_CONCERNED
                Mood.LIMIT_HIT -> ACTION_LIMIT_HIT
                Mood.ANNOYED -> ACTION_ANNOYED
                Mood.ANGRY -> ACTION_ANGRY
                Mood.FURIOUS -> ACTION_FURIOUS
                Mood.BREAK -> ACTION_BREAK
            }
        }
        val baseTime = if (isSleeping) now - sleepStartTime else now - baseStartTime
        val baseFrame = getFrame(baseAction, baseTime)

        // 3. Resolve active higher-priority override frame
        // Priority: dragged/airborne > dizzy/dust-off/walk/climb > reactions > sleep > base
        var overrideFrame: Frame? = null

        if (isDragged || isAirborne) {
            val action = if (kotlin.math.abs(dragVx) > 1500f) ACTION_DRAGGED_FAST else ACTION_DRAGGED_SLOW
            overrideFrame = getFrame(action, now - dragStartTime)
            if (isAirborne) {
                val steps = (((now - airborneStartTime) / 70) * airborneSpinDir).toInt()
                airborneRotation = ((steps % 4) + 4) % 4
            }
        } else if (isDizzy) {
            val elapsed = now - dizzyStartTime
            if (elapsed >= dizzyDuration) {
                isDizzy = false
                val cb = dizzyCallback
                dizzyCallback = null
                cb?.invoke()
            } else {
                val idx = ((elapsed / 150) % 4).toInt()
                overrideFrame = Frame(ms = 150, face = "dizzy", legs = "sit", dy = 4, props = DIZZY_STARS[idx])
            }
        } else if (isDustOff) {
            val elapsed = now - dustOffStartTime
            if (elapsed >= ACTION_DUST_OFF.totalMs) {
                isDustOff = false
                val cb = dustOffCallback
                dustOffCallback = null
                cb?.invoke()
            } else {
                overrideFrame = getFrame(ACTION_DUST_OFF, elapsed)
            }
        } else if (isWalking) {
            val action = if (walkDir < 0) ACTION_WALK_LEFT else ACTION_WALK_RIGHT
            overrideFrame = getFrame(action, now - walkStartTime)
        } else if (isClimbing) {
            overrideFrame = getFrame(ACTION_CLIMB, now - climbStartTime)
        } else if (currentReaction != null) {
            val elapsed = now - reactionStartTime
            if (elapsed >= currentReactionTotalMs) {
                currentReaction = null
                val cb = reactionCallback
                reactionCallback = null
                cb?.invoke()
            } else {
                overrideFrame = getFrame(currentReaction!!, elapsed)
            }
        }

        // 4. Resolve effective fields
        var resFace = overrideFrame?.face ?: baseFrame.face ?: "normal"
        val resArmR = overrideFrame?.armR ?: baseFrame.armR ?: "down"
        val resArmL = overrideFrame?.armL ?: baseFrame.armL ?: "down"
        val resLegs = overrideFrame?.legs ?: baseFrame.legs ?: "stand"
        val resDy = overrideFrame?.dy ?: baseFrame.dy ?: 0
        val resProps = overrideFrame?.props ?: baseFrame.props
        val resShakeX = overrideFrame?.shakeX ?: baseFrame.shakeX
        val resRotation = overrideFrame?.rotation ?: baseFrame.rotation ?: if (isAirborne) airborneRotation else 0

        // 5. Overlays (Blink & Look) on top of everything
        if (now >= nextBlinkTime && now < nextBlinkTime + 130L) {
            if (resFace == "normal" || resFace == "worried" || resFace == "angry") {
                resFace = "blink"
            }
        } else if (now >= nextBlinkTime + 130L) {
            nextBlinkTime = now + random.nextLong(2500, 5001)
        }

        if (now >= nextLookTime && now < nextLookTime + 900L) {
            if (resFace == "normal") {
                resFace = currentLookFace
            }
        } else if (now >= nextLookTime + 900L) {
            nextLookTime = now + random.nextLong(4000, 8001)
            currentLookFace = if (random.nextBoolean()) "look_l" else "look_r"
        }

        // Mirroring of look faces
        val effectiveFace = if (!faceRight) {
            when (resFace) {
                "look_l" -> "look_r"
                "look_r" -> "look_l"
                else -> resFace
            }
        } else {
            resFace
        }

        // 6. Framebuffer cache check & recomposition
        val moodSkin = when (currentMood) {
            Mood.ANGRY -> 1
            Mood.FURIOUS -> 2
            else -> 0
        }

        val needsRecompose = cachedFace != effectiveFace ||
                cachedArmR != resArmR ||
                cachedArmL != resArmL ||
                cachedLegs != resLegs ||
                cachedDy != resDy ||
                cachedProps != resProps ||
                cachedMoodSkin != moodSkin ||
                cachedFaceRight != faceRight

        if (needsRecompose) {
            composeFrame(effectiveFace, resArmR, resArmL, resLegs, resDy, resProps, moodSkin, faceRight)
            cachedFace = effectiveFace
            cachedArmR = resArmR
            cachedArmL = resArmL
            cachedLegs = resLegs
            cachedDy = resDy
            cachedProps = resProps
            cachedMoodSkin = moodSkin
            cachedFaceRight = faceRight
        }

        // 7. Draw crisp bitmap to canvas with shake & rotation
        canvas.save()
        if (resShakeX != 0) {
            canvas.translate(resShakeX * currentPx.toFloat(), 0f)
        }
        if (resRotation != 0) {
            canvas.rotate(90f * ((resRotation % 4 + 4) % 4), width / 2f, height / 2f)
        }
        canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
        canvas.restore()

        // 8. Badge rendering
        if (showBadge) {
            drawBadge(canvas)
        }

        // 9. Keep animation loop running while attached and isShown
        if (isShown) {
            postInvalidateOnAnimation()
        }
    }

    private fun getFrame(action: Action, elapsedMs: Long): Frame {
        val frames = action.frames
        if (frames.isEmpty()) return DEFAULT_FRAME
        val total = action.totalMs
        val modMs = if (action.loop) {
            if (total > 0) (elapsedMs % total).toInt() else 0
        } else {
            minOf(elapsedMs, (total - 1).coerceAtLeast(0).toLong()).toInt()
        }
        var acc = 0
        for (i in 0 until frames.size) {
            val f = frames[i]
            acc += f.ms
            if (modMs < acc) {
                return f
            }
        }
        return frames.last()
    }

    private fun composeFrame(
        face: String,
        armR: String,
        armL: String,
        legs: String,
        dy: Int,
        props: List<PropAt>?,
        moodSkin: Int,
        faceRight: Boolean
    ) {
        framebuffer.fill(0)
        val palette = when (moodSkin) {
            1 -> paletteAngry
            2 -> paletteFurious
            else -> paletteNormal
        }

        // 1. Legs
        val legRows = ShahidSprites.LEGS[legs] ?: ShahidSprites.LEGS["stand"]!!
        val legsDy = if (legs == "tuck" || legs == "sit") dy else 0
        val legStageY = BODY_Y + ShahidSprites.LEGS_TOP + legsDy
        val legStageX = BODY_X
        for (r in 0 until legRows.size) {
            val sy = legStageY + r
            if (sy in 0 until STAGE_SIZE) {
                val rowStr = legRows[r]
                val maxC = minOf(24, rowStr.length)
                val rowOffset = sy * STAGE_SIZE
                for (c in 0 until maxC) {
                    val sx = legStageX + c
                    if (sx in 0 until STAGE_SIZE) {
                        val ch = rowStr[c]
                        if (ch != '.') {
                            framebuffer[rowOffset + sx] = palette[ch.code]
                        }
                    }
                }
            }
        }

        // 2. Torso
        val torsoStageX = BODY_X
        val torsoStageY = BODY_Y + ShahidSprites.TORSO_TOP + dy
        for (r in 0 until ShahidSprites.TORSO.size) {
            val sy = torsoStageY + r
            if (sy in 0 until STAGE_SIZE) {
                val rowStr = ShahidSprites.TORSO[r]
                val maxC = minOf(24, rowStr.length)
                val rowOffset = sy * STAGE_SIZE
                for (c in 0 until maxC) {
                    val sx = torsoStageX + c
                    if (sx in 0 until STAGE_SIZE) {
                        val ch = rowStr[c]
                        if (ch != '.') {
                            framebuffer[rowOffset + sx] = palette[ch.code]
                        }
                    }
                }
            }
        }

        // 3. Head (HEAD with face patch applied)
        val headStageX = BODY_X
        val headStageY = BODY_Y + dy
        for (r in 0 until ShahidSprites.HEAD.size) {
            val sy = headStageY + r
            if (sy in 0 until STAGE_SIZE) {
                val rowStr = ShahidSprites.HEAD[r]
                val maxC = minOf(24, rowStr.length)
                val rowOffset = sy * STAGE_SIZE
                for (c in 0 until maxC) {
                    val sx = headStageX + c
                    if (sx in 0 until STAGE_SIZE) {
                        val ch = rowStr[c]
                        if (ch != '.') {
                            framebuffer[rowOffset + sx] = palette[ch.code]
                        }
                    }
                }
            }
        }
        val facePatch = ShahidSprites.FACES[face]
        if (facePatch != null) {
            for (i in 0 until facePatch.size) {
                val px = facePatch[i]
                val sx = headStageX + px.x
                val sy = headStageY + px.y
                if (sx in 0 until STAGE_SIZE && sy in 0 until STAGE_SIZE) {
                    framebuffer[sy * STAGE_SIZE + sx] = palette[px.c.code]
                }
            }
        }

        // Arm poses
        val armRObj = ShahidSprites.ARMS_RIGHT[armR] ?: ShahidSprites.ARMS_RIGHT["down"]!!
        val armLObj = ShahidSprites.ARMS_LEFT[armL] ?: ShahidSprites.ARMS_LEFT["down"]!!

        // 4. Props flagged behind
        if (props != null) {
            for (i in 0 until props.size) {
                val prop = props[i]
                if (prop.behind) {
                    drawProp(prop, dy, armRObj, palette)
                }
            }
        }

        // 5. Left arm
        drawArm(armLObj, dy, palette)

        // 6. Right arm
        drawArm(armRObj, dy, palette)

        // 7. Other props (behind == false)
        if (props != null) {
            for (i in 0 until props.size) {
                val prop = props[i]
                if (!prop.behind) {
                    drawProp(prop, dy, armRObj, palette)
                }
            }
        }

        // Mirroring: if faceRight == false, flip framebuffer horizontally
        if (!faceRight) {
            for (y in 0 until STAGE_SIZE) {
                val rowOffset = y * STAGE_SIZE
                for (x in 0 until STAGE_SIZE / 2) {
                    val leftIdx = rowOffset + x
                    val rightIdx = rowOffset + (STAGE_SIZE - 1 - x)
                    val tmp = framebuffer[leftIdx]
                    framebuffer[leftIdx] = framebuffer[rightIdx]
                    framebuffer[rightIdx] = tmp
                }
            }
        }

        bitmap.setPixels(framebuffer, 0, STAGE_SIZE, 0, 0, STAGE_SIZE, STAGE_SIZE)
    }

    private fun drawArm(arm: ShahidSprites.Arm, dy: Int, palette: IntArray) {
        val sprite = arm.sprite
        val stageX = BODY_X + sprite.dx
        val stageY = BODY_Y + sprite.dy + dy
        for (r in 0 until sprite.h) {
            val sy = stageY + r
            if (sy in 0 until STAGE_SIZE) {
                val rowStr = sprite.rows[r]
                val maxC = minOf(sprite.w, rowStr.length)
                val rowOffset = sy * STAGE_SIZE
                for (c in 0 until maxC) {
                    val sx = stageX + c
                    if (sx in 0 until STAGE_SIZE) {
                        val ch = rowStr[c]
                        if (ch != '.') {
                            framebuffer[rowOffset + sx] = palette[ch.code]
                        }
                    }
                }
            }
        }
    }

    private fun drawProp(
        prop: PropAt,
        dy: Int,
        armR: ShahidSprites.Arm,
        palette: IntArray
    ) {
        val sprite = ShahidSprites.PROPS[prop.name] ?: return
        val stageX: Int
        val stageY: Int
        when (prop.anchor) {
            Anchor.BODY -> {
                stageX = BODY_X + prop.x
                stageY = BODY_Y + prop.y + dy
            }
            Anchor.HAND_R -> {
                stageX = BODY_X + armR.handX + prop.x
                stageY = BODY_Y + armR.handY + dy + prop.y
            }
        }
        for (r in 0 until sprite.h) {
            val sy = stageY + r
            if (sy in 0 until STAGE_SIZE) {
                val rowStr = sprite.rows[r]
                val maxC = minOf(sprite.w, rowStr.length)
                val rowOffset = sy * STAGE_SIZE
                for (c in 0 until maxC) {
                    val sx = stageX + c
                    if (sx in 0 until STAGE_SIZE) {
                        val charCol = if (prop.flipX) sprite.w - 1 - c else c
                        val ch = rowStr[charCol]
                        if (ch != '.') {
                            framebuffer[rowOffset + sx] = palette[ch.code]
                        }
                    }
                }
            }
        }
    }

    private fun drawBadge(canvas: Canvas) {
        val notch = currentPx.toFloat()
        val badgeW = maxOf(8 * currentPx, (cachedBadgeTextWidth + 4 * currentPx).roundToInt()).toFloat()
        val badgeLeft = if (faceRight) max(0, STAGE_SIZE * currentPx - badgeW.toInt()).toFloat() else 0f
        val badgeRight = badgeLeft + badgeW
        val badgeTop = 0f
        val badgeBottom = (8 * currentPx).toFloat()

        // Notched outer path (1*px outline)
        badgePath.rewind()
        badgePath.moveTo(badgeLeft + notch, badgeTop)
        badgePath.lineTo(badgeRight - notch, badgeTop)
        badgePath.lineTo(badgeRight - notch, badgeTop + notch)
        badgePath.lineTo(badgeRight, badgeTop + notch)
        badgePath.lineTo(badgeRight, badgeBottom - notch)
        badgePath.lineTo(badgeRight - notch, badgeBottom - notch)
        badgePath.lineTo(badgeRight - notch, badgeBottom)
        badgePath.lineTo(badgeLeft + notch, badgeBottom)
        badgePath.lineTo(badgeLeft + notch, badgeBottom - notch)
        badgePath.lineTo(badgeLeft, badgeBottom - notch)
        badgePath.lineTo(badgeLeft, badgeTop + notch)
        badgePath.lineTo(badgeLeft + notch, badgeTop + notch)
        badgePath.close()

        badgeOutlinePaint.color = 0xFF1A1511.toInt()
        canvas.drawPath(badgePath, badgeOutlinePaint)

        // Notched inner path (fill)
        val innerL = badgeLeft + notch
        val innerT = badgeTop + notch
        val innerR = badgeRight - notch
        val innerB = badgeBottom - notch
        badgeInnerPath.rewind()
        badgeInnerPath.moveTo(innerL, innerT)
        badgeInnerPath.lineTo(innerR, innerT)
        badgeInnerPath.lineTo(innerR, innerB)
        badgeInnerPath.lineTo(innerL, innerB)
        badgeInnerPath.close()

        badgeFillPaint.color = if (isOverLimit) 0xFFC7372B.toInt() else 0xFFF6F1E7.toInt()
        canvas.drawPath(badgeInnerPath, badgeFillPaint)

        // Text
        badgeTextPaint.color = if (isOverLimit) 0xFFF6F1E7.toInt() else 0xFF1A1511.toInt()
        val textX = (badgeLeft + badgeRight) / 2f
        val textY = badgeTop + (8f * currentPx) / 2f - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
        canvas.drawText(cachedBadgeText, textX, textY, badgeTextPaint)
    }
}
