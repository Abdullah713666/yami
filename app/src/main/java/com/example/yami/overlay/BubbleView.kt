package com.example.yami.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.ceil

class BubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val COLOR_OUTLINE = 0xFF1A1511.toInt()
        private const val COLOR_FILL_NORMAL = 0xFFF6F1E7.toInt()
        private const val COLOR_TEXT_NORMAL = 0xFF1A1511.toInt()
        private const val COLOR_FILL_ANGRY = 0xFFF9D9D2.toInt()
        private const val COLOR_TEXT_ANGRY = 0xFF7A1E16.toInt()
    }

    enum class Tail { START, END, TOP }

    var pixelPx: Int = (3f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        set(value) {
            val v = value.coerceAtLeast(1)
            if (field != v) {
                field = v
                requestLayout()
                invalidate()
            }
        }

    var tail: Tail = Tail.START
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                invalidate()
            }
        }

    var maxBubbleWidth: Int = (220f * resources.displayMetrics.density).toInt()
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                invalidate()
            }
        }

    var onHidden: (() -> Unit)? = null

    private var isAngry: Boolean = false
    private var currentText: String = ""

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = COLOR_FILL_NORMAL
    }

    private val outlinePaint = Paint().apply {
        style = Paint.Style.FILL
        color = COLOR_OUTLINE
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = COLOR_TEXT_NORMAL
    }

    private val outerPath = Path()
    private val innerPath = Path()
    private var staticLayout: StaticLayout? = null

    private val handler = Handler(Looper.getMainLooper())
    private val popHandler = Handler(Looper.getMainLooper())
    private var fadeAnimator: Animator? = null

    private val autoHideRunnable = Runnable {
        fadeOut()
    }

    init {
        visibility = GONE
    }

    fun setAngry(angry: Boolean) {
        isAngry = angry
        updateColors()
        invalidate()
    }

    private fun updateColors() {
        if (isAngry) {
            fillPaint.color = COLOR_FILL_ANGRY
            textPaint.color = COLOR_TEXT_ANGRY
        } else {
            fillPaint.color = COLOR_FILL_NORMAL
            textPaint.color = COLOR_TEXT_NORMAL
        }
        outlinePaint.color = COLOR_OUTLINE
    }

    fun show(text: String, angry: Boolean) {
        currentText = text
        isAngry = angry
        updateColors()

        handler.removeCallbacks(autoHideRunnable)
        popHandler.removeCallbacksAndMessages(null)
        fadeAnimator?.cancel()

        visibility = VISIBLE
        alpha = 1.0f
        scaleX = 0.85f
        scaleY = 0.85f

        rebuildLayout()
        requestLayout()

        post {
            setupPivot()
            popHandler.postDelayed({
                scaleX = 1.0f
                scaleY = 1.0f
            }, 60L)
        }

        handler.postDelayed(autoHideRunnable, 3200L)
    }

    private fun setupPivot() {
        when (tail) {
            Tail.START -> {
                pivotX = 0f
                pivotY = height / 2f
            }
            Tail.END -> {
                pivotX = width.toFloat()
                pivotY = height / 2f
            }
            Tail.TOP -> {
                pivotX = width / 2f
                pivotY = 0f
            }
        }
    }

    private fun fadeOut() {
        fadeAnimator?.cancel()
        fadeAnimator = ObjectAnimator.ofFloat(this, ALPHA, 1.0f, 0.0f).apply {
            duration = 150L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = GONE
                    onHidden?.invoke()
                }
            })
            start()
        }
    }

    fun hideNow() {
        handler.removeCallbacks(autoHideRunnable)
        popHandler.removeCallbacksAndMessages(null)
        fadeAnimator?.cancel()
        visibility = GONE
        alpha = 0f
        onHidden?.invoke()
    }

    private fun rebuildLayout() {
        val u = pixelPx
        val hPadding = 3 * u
        val tailReserve = if (tail != Tail.TOP) 3 * u else 0
        val maxTextW = (maxBubbleWidth - hPadding * 2 - tailReserve).coerceAtLeast(1)

        staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(currentText, 0, currentText.length, textPaint, maxTextW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(currentText, textPaint, maxTextW, Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (currentText.isEmpty()) {
            setMeasuredDimension(0, 0)
            return
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val allowed = if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) {
            minOf(maxBubbleWidth, widthSize)
        } else {
            maxBubbleWidth
        }

        val u = pixelPx
        val hPadding = 3 * u
        val vPadding = 2 * u
        val tailReserve = if (tail != Tail.TOP) 3 * u else 0
        val maxTextW = (allowed - hPadding * 2 - tailReserve).coerceAtLeast(1)

        val sl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(currentText, 0, currentText.length, textPaint, maxTextW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(currentText, textPaint, maxTextW, Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false)
        }
        staticLayout = sl

        var maxLineWidth = 0f
        for (i in 0 until sl.lineCount) {
            val lw = sl.getLineWidth(i)
            if (lw > maxLineWidth) {
                maxLineWidth = lw
            }
        }

        val textW = ceil(maxLineWidth).toInt()
        val textH = sl.height

        val bodyW = (textW + 2 * hPadding).coerceAtLeast(10 * u)
        val bodyH = (textH + 2 * vPadding).coerceAtLeast(8 * u)

        val contentW = bodyW + (if (tail != Tail.TOP) tailReserve else 0)
        val contentH = bodyH + (if (tail == Tail.TOP) 3 * u else 0)

        val finalW = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            contentW.coerceAtMost(allowed)
        }
        val finalH = contentH

        setMeasuredDimension(finalW, finalH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupPivot()
        buildPixelBubblePaths(w.toFloat(), h.toFloat())
    }

    private fun buildPixelBubblePaths(w: Float, h: Float) {
        outerPath.reset()
        innerPath.reset()
        if (w <= 0f || h <= 0f) return

        val u = pixelPx.toFloat()
        when (tail) {
            Tail.START -> {
                val bx0 = 3f * u
                val bx1 = w
                val by0 = 0f
                val by1 = h
                val cy = ((h / 2f) / u).toInt() * u

                // Outer silhouette (outline color)
                outerPath.moveTo(bx0 + u, by0)
                outerPath.lineTo(bx1 - u, by0)
                outerPath.lineTo(bx1 - u, by0 + u)
                outerPath.lineTo(bx1, by0 + u)
                outerPath.lineTo(bx1, by1 - u)
                outerPath.lineTo(bx1 - u, by1 - u)
                outerPath.lineTo(bx1 - u, by1)
                outerPath.lineTo(bx0 + u, by1)
                outerPath.lineTo(bx0 + u, by1 - u)
                outerPath.lineTo(bx0, by1 - u)

                // Tail staircase down to tip
                outerPath.lineTo(bx0, cy + 3f * u)
                outerPath.lineTo(bx0 - u, cy + 3f * u)
                outerPath.lineTo(bx0 - u, cy + 2f * u)
                outerPath.lineTo(bx0 - 2f * u, cy + 2f * u)
                outerPath.lineTo(bx0 - 2f * u, cy + 1f * u)
                outerPath.lineTo(0f, cy + 1f * u)
                outerPath.lineTo(0f, cy)
                outerPath.lineTo(bx0 - 2f * u, cy)
                outerPath.lineTo(bx0 - 2f * u, cy - 1f * u)
                outerPath.lineTo(bx0 - u, cy - 1f * u)
                outerPath.lineTo(bx0 - u, cy - 2f * u)
                outerPath.lineTo(bx0, cy - 2f * u)

                outerPath.lineTo(bx0, by0 + u)
                outerPath.lineTo(bx0 + u, by0 + u)
                outerPath.close()

                // Inner fill
                innerPath.moveTo(bx0 + u, by0 + u)
                innerPath.lineTo(bx1 - u, by0 + u)
                innerPath.lineTo(bx1 - u, by1 - u)
                innerPath.lineTo(bx0 + u, by1 - u)

                innerPath.lineTo(bx0 + u, cy + 2f * u)
                innerPath.lineTo(bx0 - u, cy + 2f * u)
                innerPath.lineTo(bx0 - u, cy + 1f * u)
                innerPath.lineTo(bx0 - 2f * u, cy + 1f * u)
                innerPath.lineTo(bx0 - 2f * u, cy)
                innerPath.lineTo(bx0 - u, cy)
                innerPath.lineTo(bx0 - u, cy - 1f * u)
                innerPath.lineTo(bx0 + u, cy - 1f * u)
                innerPath.close()
            }
            Tail.END -> {
                val bx0 = 0f
                val bx1 = w - 3f * u
                val by0 = 0f
                val by1 = h
                val cy = ((h / 2f) / u).toInt() * u

                // Outer silhouette (outline color)
                outerPath.moveTo(bx0 + u, by0)
                outerPath.lineTo(bx1 - u, by0)
                outerPath.lineTo(bx1 - u, by0 + u)
                outerPath.lineTo(bx1, by0 + u)

                // Tail staircase down to tip
                outerPath.lineTo(bx1, cy - 2f * u)
                outerPath.lineTo(bx1 + u, cy - 2f * u)
                outerPath.lineTo(bx1 + u, cy - 1f * u)
                outerPath.lineTo(bx1 + 2f * u, cy - 1f * u)
                outerPath.lineTo(bx1 + 2f * u, cy)
                outerPath.lineTo(w, cy)
                outerPath.lineTo(w, cy + 1f * u)
                outerPath.lineTo(bx1 + 2f * u, cy + 1f * u)
                outerPath.lineTo(bx1 + 2f * u, cy + 2f * u)
                outerPath.lineTo(bx1 + u, cy + 2f * u)
                outerPath.lineTo(bx1 + u, cy + 3f * u)
                outerPath.lineTo(bx1, cy + 3f * u)

                outerPath.lineTo(bx1, by1 - u)
                outerPath.lineTo(bx1 - u, by1 - u)
                outerPath.lineTo(bx1 - u, by1)
                outerPath.lineTo(bx0 + u, by1)
                outerPath.lineTo(bx0 + u, by1 - u)
                outerPath.lineTo(bx0, by1 - u)
                outerPath.lineTo(bx0, by0 + u)
                outerPath.lineTo(bx0 + u, by0 + u)
                outerPath.close()

                // Inner fill
                innerPath.moveTo(bx0 + u, by0 + u)
                innerPath.lineTo(bx1 - u, by0 + u)

                innerPath.lineTo(bx1 - u, cy - 1f * u)
                innerPath.lineTo(bx1 + u, cy - 1f * u)
                innerPath.lineTo(bx1 + u, cy)
                innerPath.lineTo(bx1 + 2f * u, cy)
                innerPath.lineTo(bx1 + 2f * u, cy + 1f * u)
                innerPath.lineTo(bx1 + u, cy + 1f * u)
                innerPath.lineTo(bx1 + u, cy + 2f * u)
                innerPath.lineTo(bx1 - u, cy + 2f * u)

                innerPath.lineTo(bx1 - u, by1 - u)
                innerPath.lineTo(bx0 + u, by1 - u)
                innerPath.close()
            }
            Tail.TOP -> {
                val bx0 = 0f
                val bx1 = w
                val by0 = 3f * u
                val by1 = h
                val cx = ((w / 2f) / u).toInt() * u

                // Outer silhouette (outline color)
                outerPath.moveTo(bx0 + u, by0)
                outerPath.lineTo(cx - 2f * u, by0)
                outerPath.lineTo(cx - 2f * u, by0 - u)
                outerPath.lineTo(cx - 1f * u, by0 - u)
                outerPath.lineTo(cx - 1f * u, by0 - 2f * u)
                outerPath.lineTo(cx, by0 - 2f * u)
                outerPath.lineTo(cx, 0f)
                outerPath.lineTo(cx + u, 0f)
                outerPath.lineTo(cx + u, by0 - 2f * u)
                outerPath.lineTo(cx + 2f * u, by0 - 2f * u)
                outerPath.lineTo(cx + 2f * u, by0 - u)
                outerPath.lineTo(cx + 3f * u, by0 - u)
                outerPath.lineTo(cx + 3f * u, by0)
                outerPath.lineTo(bx1 - u, by0)
                outerPath.lineTo(bx1 - u, by0 + u)
                outerPath.lineTo(bx1, by0 + u)
                outerPath.lineTo(bx1, by1 - u)
                outerPath.lineTo(bx1 - u, by1 - u)
                outerPath.lineTo(bx1 - u, by1)
                outerPath.lineTo(bx0 + u, by1)
                outerPath.lineTo(bx0 + u, by1 - u)
                outerPath.lineTo(bx0, by1 - u)
                outerPath.lineTo(bx0, by0 + u)
                outerPath.lineTo(bx0 + u, by0 + u)
                outerPath.close()

                // Inner fill
                innerPath.moveTo(bx0 + u, by0 + u)
                innerPath.lineTo(cx - 1f * u, by0 + u)
                innerPath.lineTo(cx - 1f * u, by0 - u)
                innerPath.lineTo(cx, by0 - u)
                innerPath.lineTo(cx, by0 - 2f * u)
                innerPath.lineTo(cx + u, by0 - 2f * u)
                innerPath.lineTo(cx + u, by0 - u)
                innerPath.lineTo(cx + 2f * u, by0 - u)
                innerPath.lineTo(cx + 2f * u, by0 + u)
                innerPath.lineTo(bx1 - u, by0 + u)
                innerPath.lineTo(bx1 - u, by1 - u)
                innerPath.lineTo(bx0 + u, by1 - u)
                innerPath.close()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentText.isEmpty()) return

        buildPixelBubblePaths(width.toFloat(), height.toFloat())
        canvas.drawPath(outerPath, outlinePaint)
        canvas.drawPath(innerPath, fillPaint)

        val sl = staticLayout ?: return
        val u = pixelPx.toFloat()
        canvas.save()
        val textLeft = when (tail) {
            Tail.START -> 3f * u + 3f * u
            else -> 3f * u
        }
        val textTop = when (tail) {
            Tail.TOP -> 3f * u + 2f * u
            else -> 2f * u
        }
        canvas.translate(textLeft, textTop)
        sl.draw(canvas)
        canvas.restore()
    }
}
