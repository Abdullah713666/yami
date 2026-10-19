package com.example.yami.ui

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.yami.R
import com.example.yami.character.ShahidView
import com.example.yami.core.Bubble
import com.example.yami.core.MessageKind
import com.example.yami.core.Messages
import com.example.yami.core.Mood
import com.example.yami.core.MoodEvaluator
import com.example.yami.core.Reaction
import com.example.yami.core.SupportedApp
import com.example.yami.data.YamiPrefs
import com.example.yami.overlay.BubbleTexts
import com.example.yami.overlay.BubbleView
import com.example.yami.overlay.TouchGrassCard
import com.example.yami.sound.SoundPlayer
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: YamiPrefs
    private lateinit var soundPlayer: SoundPlayer

    // 1. Header
    private lateinit var heroYami: ShahidView
    private lateinit var heroBubble: BubbleView
    private lateinit var todayCountText: TextView
    private lateinit var todayLimitTargetText: TextView
    private lateinit var todayStatusText: TextView
    private lateinit var todayProgress: LinearProgressIndicator

    // 2. Setup Card
    private lateinit var setupCard: MaterialCardView
    private lateinit var enableButton: MaterialButton

    // 3. Limits
    private lateinit var dailyLimitMinus: MaterialButton
    private lateinit var dailyLimitValue: TextView
    private lateinit var dailyLimitPlus: MaterialButton

    private lateinit var igLimitSwitch: MaterialSwitch
    private lateinit var igLimitMinus: MaterialButton
    private lateinit var igLimitValue: TextView
    private lateinit var igLimitPlus: MaterialButton

    private lateinit var breakAfterMinus: MaterialButton
    private lateinit var breakAfterValue: TextView
    private lateinit var breakAfterPlus: MaterialButton

    // 4. Sound
    private lateinit var soundChoiceGroup: MaterialButtonToggleGroup
    private lateinit var soundCustomContainer: View
    private lateinit var soundCustomName: TextView
    private lateinit var soundCustomBtn: MaterialButton
    private lateinit var soundTestBtn: MaterialButton
    private lateinit var soundLimitSwitch: MaterialSwitch
    private lateinit var soundBreakSwitch: MaterialSwitch
    private lateinit var soundEverySwitch: MaterialSwitch

    // 5. Messages
    private lateinit var messagesBreakRow: View
    private lateinit var messagesBreakCount: TextView
    private lateinit var messagesLinesRow: View
    private lateinit var messagesLinesCount: TextView

    // 6. Shahid Overlay
    private lateinit var overlaySwitch: MaterialSwitch
    private lateinit var sizeGroup: MaterialButtonToggleGroup

    // 7. Last 7 Days
    private lateinit var weekBarsView: WeekBarsView

    // Playground
    private var previewTotal: Int? = null
    private var previewMood: Mood? = null
    private var lastTotal: Int? = null

    private var isUpdatingUi = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (!isUpdatingUi) {
            updateUi()
        }
    }

    private val pickSoundLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to take persistable URI permission", e)
                }
                prefs.soundCustomUri = uri.toString()
                val displayName = queryDisplayName(uri) ?: "custom_sound"
                prefs.soundCustomName = displayName
                prefs.soundChoice = "custom"
                updateSoundUi()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        prefs = YamiPrefs(this)
        soundPlayer = SoundPlayer(this, prefs)

        val mainRoot = findViewById<View>(R.id.main_root)
        val scrollView = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            mainRoot.setPadding(0, statusBars.top, 0, 0)
            scrollView.setPadding(0, 0, 0, navBars.bottom)
            insets
        }

        bindViews()
        setupListeners()
        setupPlayground()
    }

    private fun bindViews() {
        // Hero setup
        val heroContainer = findViewById<FrameLayout>(R.id.hero_yami_container)
        heroYami = ShahidView(this).apply {
            showBadge = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        heroContainer.addView(heroYami)

        heroBubble = findViewById(R.id.hero_bubble)
        heroBubble.tail = BubbleView.Tail.START
        heroBubble.pixelPx = (3f * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        heroBubble.maxBubbleWidth = (130f * resources.displayMetrics.density).toInt()

        todayCountText = findViewById(R.id.today_count)
        todayLimitTargetText = findViewById(R.id.today_limit_target)
        todayStatusText = findViewById(R.id.today_status)
        todayProgress = findViewById(R.id.today_progress)

        setupCard = findViewById(R.id.setup_card)
        enableButton = findViewById(R.id.enable_button)

        dailyLimitMinus = findViewById(R.id.daily_limit_minus)
        dailyLimitValue = findViewById(R.id.daily_limit_value)
        dailyLimitPlus = findViewById(R.id.daily_limit_plus)

        igLimitSwitch = findViewById(R.id.ig_limit_switch)
        igLimitMinus = findViewById(R.id.ig_limit_minus)
        igLimitValue = findViewById(R.id.ig_limit_value)
        igLimitPlus = findViewById(R.id.ig_limit_plus)

        breakAfterMinus = findViewById(R.id.break_after_minus)
        breakAfterValue = findViewById(R.id.break_after_value)
        breakAfterPlus = findViewById(R.id.break_after_plus)

        // Sound
        soundChoiceGroup = findViewById(R.id.sound_choice_group)
        soundCustomContainer = findViewById(R.id.sound_custom_container)
        soundCustomName = findViewById(R.id.sound_custom_name)
        soundCustomBtn = findViewById(R.id.sound_custom_btn)
        soundTestBtn = findViewById(R.id.sound_test_button)
        soundLimitSwitch = findViewById(R.id.sound_limit_switch)
        soundBreakSwitch = findViewById(R.id.sound_break_switch)
        soundEverySwitch = findViewById(R.id.sound_every_switch)

        // Messages
        messagesBreakRow = findViewById(R.id.messages_break_row)
        messagesBreakCount = findViewById(R.id.messages_break_count)
        messagesLinesRow = findViewById(R.id.messages_lines_row)
        messagesLinesCount = findViewById(R.id.messages_lines_count)

        // Overlay
        overlaySwitch = findViewById(R.id.overlay_switch)
        sizeGroup = findViewById(R.id.size_group)
        weekBarsView = findViewById(R.id.week_bars)
    }

    private fun setupListeners() {
        // Tapping ShahidView waves
        findViewById<View>(R.id.hero_yami_container).setOnClickListener {
            heroYami.playWave()
        }

        enableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        dailyLimitMinus.setOnClickListener {
            prefs.dailyLimit = prefs.dailyLimit - 5
        }
        dailyLimitPlus.setOnClickListener {
            prefs.dailyLimit = prefs.dailyLimit + 5
        }

        igLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                prefs.igLimitEnabled = isChecked
            }
        }
        igLimitMinus.setOnClickListener {
            prefs.igLimit = prefs.igLimit - 5
        }
        igLimitPlus.setOnClickListener {
            prefs.igLimit = prefs.igLimit + 5
        }

        breakAfterMinus.setOnClickListener {
            prefs.breakAfter = prefs.breakAfter - 5
        }
        breakAfterPlus.setOnClickListener {
            prefs.breakAfter = prefs.breakAfter + 5
        }

        // Sound listeners
        soundChoiceGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isUpdatingUi) {
                when (checkedId) {
                    R.id.sound_choice_off -> prefs.soundChoice = "off"
                    R.id.sound_choice_whistle -> prefs.soundChoice = "whistle"
                    R.id.sound_choice_siren -> prefs.soundChoice = "siren"
                    R.id.sound_choice_custom -> {
                        prefs.soundChoice = "custom"
                        if (prefs.soundCustomUri.isNullOrBlank()) {
                            launchSoundPicker()
                        }
                    }
                }
                updateSoundUi()
            }
        }

        soundCustomBtn.setOnClickListener {
            launchSoundPicker()
        }

        soundTestBtn.setOnClickListener {
            soundPlayer.play()
        }

        soundLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                prefs.soundOnLimit = isChecked
            }
        }

        soundBreakSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                prefs.soundOnBreak = isChecked
            }
        }

        soundEverySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                prefs.soundEveryOver = isChecked
            }
        }

        // Messages rows
        messagesBreakRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java).apply {
                putExtra(MessagesActivity.EXTRA_SECTION, MessagesActivity.SECTION_BREAK)
            }
            startActivity(intent)
        }

        messagesLinesRow.setOnClickListener {
            val intent = Intent(this, MessagesActivity::class.java).apply {
                putExtra(MessagesActivity.EXTRA_SECTION, MessagesActivity.SECTION_LINES)
            }
            startActivity(intent)
        }

        // Overlay settings
        overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                prefs.overlayEnabled = isChecked
            }
        }

        sizeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isUpdatingUi) {
                when (checkedId) {
                    R.id.size_s -> prefs.overlaySize = "S"
                    R.id.size_m -> prefs.overlaySize = "M"
                    R.id.size_l -> prefs.overlaySize = "L"
                }
            }
        }
    }

    private fun launchSoundPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/mp4"))
        }
        try {
            pickSoundLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error launching sound document picker", e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun setupPlayground() {
        findViewById<Chip>(R.id.chip_plus_one).setOnClickListener {
            val limits = prefs.limits()
            val nextTotal = (previewTotal ?: prefs.todayTotal()) + 1
            val currentTotal = previewTotal ?: prefs.todayTotal()
            previewTotal = nextTotal

            val mood = MoodEvaluator.mood(nextTotal, nextTotal, limits)
            val previousMood = previewMood ?: MoodEvaluator.mood(currentTotal, currentTotal, limits)
            previewMood = mood

            heroYami.setMood(mood)
            heroYami.setCount(nextTotal, limits.dailyLimit)
            val reaction = MoodEvaluator.reactionFor(nextTotal, mood)
            heroYami.playReaction(reaction)

            todayCountText.text = nextTotal.toString()
            todayProgress.max = limits.dailyLimit
            todayProgress.progress = minOf(nextTotal, limits.dailyLimit)
            updateProgressColor(mood)

            val overage = MoodEvaluator.overage(nextTotal, nextTotal, limits)
            val remaining = MoodEvaluator.remaining(nextTotal, nextTotal, limits)
            todayStatusText.text = when {
                overage > 0 -> getString(R.string.status_over_limit, overage)
                remaining == 0 -> getString(R.string.status_at_limit)
                else -> getString(R.string.status_under_limit, remaining)
            }

            val bubble = Messages.bubbleFor(
                mood = mood,
                previousMood = previousMood,
                total = nextTotal,
                remaining = remaining,
                overage = overage,
                listSize = { kind -> BubbleTexts.listSize(this, kind) }
            )
            if (bubble != null) {
                val text = BubbleTexts.bubbleText(this, bubble)
                val isAngry = mood == Mood.ANGRY || mood == Mood.FURIOUS
                heroBubble.show(text, isAngry)
            }
        }

        findViewById<Chip>(R.id.chip_milestone).setOnClickListener {
            heroYami.playReaction(Reaction.MILESTONE)
        }

        findViewById<Chip>(R.id.chip_hit_limit).setOnClickListener {
            previewMood = Mood.LIMIT_HIT
            heroYami.setMood(Mood.LIMIT_HIT)
            heroYami.playReaction(Reaction.LIMIT_REACHED)

            val total = previewTotal ?: prefs.todayTotal()
            val text = BubbleTexts.bubbleText(this, Bubble(MessageKind.LIMIT_HIT, total, 0))
            heroBubble.show(text, angry = false)
        }

        findViewById<Chip>(R.id.chip_annoyed).setOnClickListener {
            previewMood = Mood.ANNOYED
            heroYami.setMood(Mood.ANNOYED)
            heroYami.playReaction(Reaction.HEAD_SHAKE)

            val total = previewTotal ?: prefs.todayTotal()
            val text = BubbleTexts.bubbleText(this, Bubble(MessageKind.ANNOYED, total, 3))
            heroBubble.show(text, angry = false)
        }

        findViewById<Chip>(R.id.chip_angry).setOnClickListener {
            previewMood = Mood.ANGRY
            heroYami.setMood(Mood.ANGRY)
            heroYami.playReaction(Reaction.ANGRY_SHAKE)

            val total = previewTotal ?: prefs.todayTotal()
            val text = BubbleTexts.bubbleText(this, Bubble(MessageKind.ANGRY, total, 7))
            heroBubble.show(text, angry = true)
        }

        findViewById<Chip>(R.id.chip_furious).setOnClickListener {
            previewMood = Mood.FURIOUS
            heroYami.setMood(Mood.FURIOUS)
            heroYami.playReaction(Reaction.FURIOUS_SHAKE)

            val total = previewTotal ?: prefs.todayTotal()
            val text = BubbleTexts.bubbleText(this, Bubble(MessageKind.FURIOUS, total, 12))
            heroBubble.show(text, angry = true)
        }

        findViewById<Chip>(R.id.chip_touch_grass).setOnClickListener {
            val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
            dialog.setContentView(R.layout.overlay_touch_grass)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val root = dialog.findViewById<View>(R.id.touch_grass_root)
            val card = TouchGrassCard(
                root = root,
                onTakeMeOut = { dialog.dismiss() },
                onOneMore = { dialog.dismiss() }
            )
            card.start(title = prefs.pickBreakMessage(), total = 42, overage = 12, countdownSeconds = 5)
            dialog.setOnDismissListener { card.cancel() }
            dialog.show()
        }

        findViewById<Chip>(R.id.chip_dizzy).setOnClickListener {
            heroYami.playAction("dizzy")
        }

        findViewById<Chip>(R.id.chip_walk).setOnClickListener {
            heroYami.playAction("walk")
        }

        findViewById<Chip>(R.id.chip_climb).setOnClickListener {
            heroYami.playAction("climb")
        }

        findViewById<Chip>(R.id.chip_sleep).setOnClickListener {
            heroYami.playAction("sleep")
        }

        findViewById<Chip>(R.id.chip_dragged).setOnClickListener {
            heroYami.playAction("dragged")
        }

        findViewById<Chip>(R.id.chip_test_sound).setOnClickListener {
            soundPlayer.play()
        }

        findViewById<Chip>(R.id.chip_reset_preview).setOnClickListener {
            previewTotal = null
            previewMood = null
            updateUi()
            heroBubble.hideNow()
        }

        findViewById<MaterialButton>(R.id.reset_today_button).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_dialog_title)
                .setMessage(R.string.reset_dialog_message)
                .setPositiveButton(R.string.reset) { _, _ ->
                    previewTotal = null
                    previewMood = null
                    prefs.resetToday()
                    heroBubble.hideNow()
                    updateUi()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        prefs.register(prefListener)
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateSetupCard()
        updateUi()
    }

    override fun onStop() {
        super.onStop()
        prefs.unregister(prefListener)
    }

    private fun isAccessibilityServiceOn(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val target = "$packageName/com.example.yami.service.ReelWatcherService"
        return enabledServices.contains(target, ignoreCase = true)
    }

    private fun updateSetupCard() {
        setupCard.visibility = if (isAccessibilityServiceOn()) View.GONE else View.VISIBLE
    }

    private fun updateProgressColor(mood: Mood) {
        val indicatorColor = when (mood) {
            Mood.CALM -> ContextCompat.getColor(this, R.color.yami_chart_normal)
            Mood.CONCERNED -> ContextCompat.getColor(this, R.color.yami_brass)
            Mood.LIMIT_HIT, Mood.ANNOYED -> ContextCompat.getColor(this, R.color.yami_challan_red)
            Mood.ANGRY, Mood.FURIOUS -> ContextCompat.getColor(this, R.color.yami_challan_red)
            Mood.BREAK -> ContextCompat.getColor(this, R.color.yami_chart_normal)
        }
        todayProgress.setIndicatorColor(indicatorColor)
    }

    private fun updateSoundUi() {
        val targetBtnId = when (prefs.soundChoice) {
            "off" -> R.id.sound_choice_off
            "siren" -> R.id.sound_choice_siren
            "custom" -> R.id.sound_choice_custom
            else -> R.id.sound_choice_whistle
        }
        if (soundChoiceGroup.checkedButtonId != targetBtnId) {
            soundChoiceGroup.check(targetBtnId)
        }

        if (prefs.soundChoice == "custom") {
            soundCustomContainer.visibility = View.VISIBLE
            val displayName = prefs.soundCustomName ?: getString(R.string.sound_custom_choose)
            soundCustomName.text = displayName
        } else {
            soundCustomContainer.visibility = View.GONE
        }

        if (soundLimitSwitch.isChecked != prefs.soundOnLimit) {
            soundLimitSwitch.isChecked = prefs.soundOnLimit
        }
        if (soundBreakSwitch.isChecked != prefs.soundOnBreak) {
            soundBreakSwitch.isChecked = prefs.soundOnBreak
        }
        if (soundEverySwitch.isChecked != prefs.soundEveryOver) {
            soundEverySwitch.isChecked = prefs.soundEveryOver
        }
    }

    private fun updateUi() {
        isUpdatingUi = true
        try {
            val total = prefs.todayTotal()
            val dailyLimit = prefs.dailyLimit
            val limits = prefs.limits()
            val igCount = prefs.todayCount(SupportedApp.INSTAGRAM)
            val mood = MoodEvaluator.mood(total, igCount, limits)

            if (previewTotal == null) {
                heroYami.setMood(mood)
                heroYami.setCount(total, dailyLimit)

                if (lastTotal != null && total > lastTotal!!) {
                    val reaction = MoodEvaluator.reactionFor(total, mood)
                    heroYami.playReaction(reaction)
                }
                lastTotal = total

                // 1. Hero
                todayCountText.text = total.toString()
                todayLimitTargetText.text = dailyLimit.toString()

                val isServiceOn = isAccessibilityServiceOn()
                todayStatusText.text = when {
                    !isServiceOn -> getString(R.string.status_service_off)
                    total < dailyLimit -> getString(R.string.status_under_limit, dailyLimit - total)
                    total == dailyLimit -> getString(R.string.status_at_limit)
                    else -> getString(R.string.status_over_limit, total - dailyLimit)
                }

                todayProgress.max = dailyLimit
                todayProgress.progress = minOf(total, dailyLimit)
                updateProgressColor(mood)
            }

            // 3. Limits card
            dailyLimitValue.text = dailyLimit.toString()

            val igEnabled = prefs.igLimitEnabled
            if (igLimitSwitch.isChecked != igEnabled) {
                igLimitSwitch.isChecked = igEnabled
            }
            igLimitValue.text = prefs.igLimit.toString()
            igLimitMinus.isEnabled = igEnabled
            igLimitPlus.isEnabled = igEnabled
            igLimitValue.alpha = if (igEnabled) 1.0f else 0.4f

            val breakAfter = prefs.breakAfter
            breakAfterValue.text = if (breakAfter == 0) getString(R.string.off) else breakAfter.toString()

            // 4. Sound card
            updateSoundUi()

            // 5. Messages card
            val activeBreakCount = prefs.breakMessages().count { it.enabled }
            messagesBreakCount.text = getString(R.string.messages_break_active, activeBreakCount)
            val customLinesCount = prefs.customBubbles().size
            messagesLinesCount.text = getString(R.string.messages_lines_count, customLinesCount)

            // 6. Shahid overlay card
            if (overlaySwitch.isChecked != prefs.overlayEnabled) {
                overlaySwitch.isChecked = prefs.overlayEnabled
            }
            val targetSizeButtonId = when (prefs.overlaySize) {
                "S" -> R.id.size_s
                "L" -> R.id.size_l
                else -> R.id.size_m
            }
            if (sizeGroup.checkedButtonId != targetSizeButtonId) {
                sizeGroup.check(targetSizeButtonId)
            }

            // 7. Week bars
            weekBarsView.setData(prefs.lastDays(7), dailyLimit)
        } finally {
            isUpdatingUi = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
    }
}
