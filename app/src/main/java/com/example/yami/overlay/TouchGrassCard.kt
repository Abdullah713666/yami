package com.example.yami.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.example.yami.R
import com.example.yami.character.ShahidView
import com.example.yami.core.Mood
import com.google.android.material.button.MaterialButton

class TouchGrassCard(
    private val root: View,
    private val onTakeMeOut: () -> Unit,
    private val onOneMore: () -> Unit
) {
    private val card: View = root.findViewById(R.id.touch_grass_card)
    private val yamiView: ShahidView = root.findViewById(R.id.touch_grass_yami)
    private val titleText: TextView = root.findViewById(R.id.touch_grass_title)
    private val bodyText: TextView = root.findViewById(R.id.touch_grass_body)
    private val waitButton: MaterialButton = root.findViewById(R.id.touch_grass_wait)
    private val actionsLayout: View = root.findViewById(R.id.touch_grass_actions)
    private val outButton: MaterialButton = root.findViewById(R.id.touch_grass_out)
    private val moreButton: MaterialButton = root.findViewById(R.id.touch_grass_more)

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 0
    private var countdownRunnable: Runnable? = null
    private var enterAnim: AnimatorSet? = null

    init {
        val bgCsl = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(root.context, R.color.yami_break_wait_bg))
        val textCsl = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(root.context, R.color.yami_break_wait_text))
        waitButton.backgroundTintList = bgCsl
        waitButton.setTextColor(textCsl)
        waitButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

        outButton.setOnClickListener {
            cancel()
            onTakeMeOut()
        }
        moreButton.setOnClickListener {
            cancel()
            onOneMore()
        }
    }

    fun start(title: String, total: Int, overage: Int, countdownSeconds: Int) {
        cancel()

        // Configure Yami
        yamiView.showBadge = false
        yamiView.setMood(Mood.BREAK, animate = false)

        // Title and body text
        titleText.text = title
        val context = root.context
        bodyText.text = context.getString(R.string.break_body, total, overage)

        // Reset buttons
        waitButton.visibility = View.VISIBLE
        waitButton.isEnabled = false
        val bgCsl = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(root.context, R.color.yami_break_wait_bg))
        val textCsl = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(root.context, R.color.yami_break_wait_text))
        waitButton.backgroundTintList = bgCsl
        waitButton.setTextColor(textCsl)
        waitButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        actionsLayout.visibility = View.GONE

        remainingSeconds = countdownSeconds
        updateCountdown()

        // Enter animation: root alpha 0 -> 1 in 200 ms, card scale 0.9 -> 1 in 200 ms
        root.alpha = 0f
        card.scaleX = 0.9f
        card.scaleY = 0.9f

        val rootAlphaAnim = ObjectAnimator.ofFloat(root, View.ALPHA, 0f, 1f)
        val cardScaleXAnim = ObjectAnimator.ofFloat(card, View.SCALE_X, 0.9f, 1f)
        val cardScaleYAnim = ObjectAnimator.ofFloat(card, View.SCALE_Y, 0.9f, 1f)

        enterAnim = AnimatorSet().apply {
            duration = 200L
            playTogether(rootAlphaAnim, cardScaleXAnim, cardScaleYAnim)
            start()
        }
    }

    private fun updateCountdown() {
        val context = root.context
        if (remainingSeconds > 0) {
            waitButton.text = context.getString(R.string.break_wait, remainingSeconds)
            countdownRunnable = Runnable {
                remainingSeconds--
                updateCountdown()
            }
            handler.postDelayed(countdownRunnable!!, 1000L)
        } else {
            waitButton.visibility = View.GONE
            actionsLayout.visibility = View.VISIBLE
        }
    }

    fun cancel() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        enterAnim?.cancel()
        enterAnim = null
    }
}
