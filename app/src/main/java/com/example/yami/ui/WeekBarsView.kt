package com.example.yami.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.yami.R
import java.util.Calendar

class WeekBarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class BarItem(
        val dateKey: String,
        val value: Int,
        val valueText: String,
        val initial: String,
        val isToday: Boolean
    )

    private val density = context.resources.displayMetrics.density

    private val inkColor = ContextCompat.getColor(context, R.color.yami_ink)
    private val secondaryColor = ContextCompat.getColor(context, R.color.yami_text_secondary)
    private val barNormalColor = ContextCompat.getColor(context, R.color.yami_chart_normal)
    private val barOverLimitColor = ContextCompat.getColor(context, R.color.yami_chart_over)
    private val dashedColor = ContextCompat.getColor(context, R.color.yami_chart_dash)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = inkColor
    }

    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = dashedColor
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }

    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            11f,
            context.resources.displayMetrics
        )
        textAlign = Paint.Align.CENTER
        color = secondaryColor
    }

    private val weekdayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            12f,
            context.resources.displayMetrics
        )
        textAlign = Paint.Align.CENTER
        color = secondaryColor
    }

    private val barRadius = 6f * density
    private val barRect = RectF()
    private val outlineRect = RectF()
    private val dashedPath = Path()
    private val calendar = Calendar.getInstance()

    private var barItems: List<BarItem> = emptyList()
    private var dailyLimit: Int = 30

    fun setData(days: List<Pair<String, Int>>, limit: Int) {
        this.dailyLimit = limit
        this.barItems = days.mapIndexed { index, (dateKey, value) ->
            BarItem(
                dateKey = dateKey,
                value = value,
                valueText = value.toString(),
                initial = parseWeekdayInitial(dateKey),
                isToday = (index == days.lastIndex)
            )
        }
        invalidate()
    }

    private fun parseWeekdayInitial(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: 0
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: 1
            calendar.clear()
            calendar.set(y, m, d)
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "S"
                else -> ""
            }
        }
        return ""
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = paddingLeft.toFloat()
        val right = width - paddingRight.toFloat()
        val contentWidth = right - left
        if (contentWidth <= 0f || barItems.isEmpty()) return

        val weekdayDescent = weekdayTextPaint.fontMetrics.descent
        val weekdayBaseline = height - paddingBottom.toFloat() - weekdayDescent
        val weekdayTop = weekdayTextPaint.fontMetrics.top

        val chartBottom = weekdayBaseline + weekdayTop - (6f * density)
        val valueTextHeight = valueTextPaint.fontMetrics.bottom - valueTextPaint.fontMetrics.top
        val valueTextDescent = valueTextPaint.fontMetrics.descent
        val chartTop = paddingTop.toFloat() + valueTextHeight + (8f * density)
        val chartHeight = chartBottom - chartTop
        if (chartHeight <= 0f) return

        val maxVal = barItems.maxOfOrNull { it.value } ?: 0
        val scaleMax = maxOf(dailyLimit * 1.2f, maxVal.toFloat()).coerceAtLeast(1f)

        if (dailyLimit > 0) {
            val limitY = chartBottom - (dailyLimit.toFloat() / scaleMax) * chartHeight
            dashedPath.reset()
            dashedPath.moveTo(left, limitY)
            dashedPath.lineTo(right, limitY)
            canvas.drawPath(dashedPath, dashedLinePaint)
        }

        val count = barItems.size
        val slotWidth = contentWidth / count
        val barWidth = slotWidth * 0.55f

        for (i in 0 until count) {
            val item = barItems[i]
            val slotLeft = left + i * slotWidth
            val barLeft = slotLeft + (slotWidth - barWidth) / 2f
            val barRight = barLeft + barWidth
            val barCenterX = (barLeft + barRight) / 2f

            canvas.drawText(item.initial, barCenterX, weekdayBaseline, weekdayTextPaint)

            val barHeight = if (item.value > 0) {
                (item.value.toFloat() / scaleMax) * chartHeight
            } else {
                0f
            }
            val barTop = chartBottom - barHeight

            val valueY = barTop - (4f * density) - valueTextDescent
            canvas.drawText(item.valueText, barCenterX, valueY, valueTextPaint)

            if (barHeight > 0f) {
                barRect.set(barLeft, barTop, barRight, chartBottom)
                val r = minOf(barRadius, barHeight / 2f)
                barPaint.color = if (item.value > dailyLimit) barOverLimitColor else barNormalColor
                canvas.drawRoundRect(barRect, r, r, barPaint)

                if (item.isToday) {
                    val halfStroke = 1f * density
                    outlineRect.set(
                        barLeft + halfStroke,
                        barTop + halfStroke,
                        barRight - halfStroke,
                        chartBottom - halfStroke
                    )
                    val outlineR = maxOf(0f, r - halfStroke)
                    canvas.drawRoundRect(outlineRect, outlineR, outlineR, outlinePaint)
                }
            } else if (item.isToday) {
                val halfStroke = 1f * density
                outlineRect.set(
                    barLeft + halfStroke,
                    chartBottom - 2f * density,
                    barRight - halfStroke,
                    chartBottom
                )
                canvas.drawRoundRect(outlineRect, 1f * density, 1f * density, outlinePaint)
            }
        }
    }
}
