package com.example.yami.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yami.R
import com.example.yami.character.ShahidView
import com.example.yami.core.BreakMessage
import com.example.yami.core.CustomTexts
import com.example.yami.core.Mood
import com.example.yami.data.YamiPrefs
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class MessagesActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SECTION = "extra_section"
        const val SECTION_BREAK = 0
        const val SECTION_LINES = 1
    }

    private lateinit var prefs: YamiPrefs

    private lateinit var tabGroup: MaterialButtonToggleGroup
    private lateinit var breakSection: View
    private lateinit var linesSection: View

    // Break messages UI
    private lateinit var previewShahid: ShahidView
    private lateinit var previewTitle: TextView
    private lateinit var addBreakInput: TextInputEditText
    private lateinit var addBreakButton: MaterialButton
    private lateinit var recyclerBreak: RecyclerView
    private lateinit var breakAdapter: BreakMessageAdapter
    private var breakList = mutableListOf<BreakMessage>()

    // Custom lines UI
    private lateinit var addLineInput: TextInputEditText
    private lateinit var addLineButton: MaterialButton
    private lateinit var recyclerLines: RecyclerView
    private lateinit var linesAdapter: CustomLineAdapter
    private var linesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)

        prefs = YamiPrefs(this)

        val root = findViewById<View>(R.id.messages_root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            root.setPadding(0, statusBars.top, 0, 0)
            recyclerBreak.setPadding(0, 0, 0, navBars.bottom)
            recyclerLines.setPadding(0, 0, 0, navBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        bindViews()
        setupBreakSection()
        setupLinesSection()

        val initialSection = intent.getIntExtra(EXTRA_SECTION, SECTION_BREAK)
        if (initialSection == SECTION_LINES) {
            tabGroup.check(R.id.tab_lines)
            showLinesSection()
        } else {
            tabGroup.check(R.id.tab_break)
            showBreakSection()
        }

        tabGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.tab_lines) {
                    showLinesSection()
                } else {
                    showBreakSection()
                }
            }
        }
    }

    private fun bindViews() {
        tabGroup = findViewById(R.id.messages_tab_group)
        breakSection = findViewById(R.id.break_section_container)
        linesSection = findViewById(R.id.lines_section_container)

        // Break messages
        previewShahid = findViewById(R.id.preview_shahid)
        previewShahid.showBadge = false
        previewShahid.setMood(Mood.BREAK, animate = false)
        previewTitle = findViewById(R.id.preview_title)

        addBreakInput = findViewById(R.id.add_break_input)
        addBreakButton = findViewById(R.id.add_break_button)
        recyclerBreak = findViewById(R.id.recycler_break_messages)

        // Custom lines
        addLineInput = findViewById(R.id.add_line_input)
        addLineButton = findViewById(R.id.add_line_button)
        recyclerLines = findViewById(R.id.recycler_custom_lines)
    }

    private fun showBreakSection() {
        breakSection.visibility = View.VISIBLE
        linesSection.visibility = View.GONE
    }

    private fun showLinesSection() {
        breakSection.visibility = View.GONE
        linesSection.visibility = View.VISIBLE
    }

    private fun setupBreakSection() {
        breakList = prefs.breakMessages().toMutableList()
        val initialPreview = breakList.firstOrNull { it.enabled }?.text
            ?: breakList.firstOrNull()?.text
            ?: getString(R.string.break_title)
        previewTitle.text = initialPreview

        breakAdapter = BreakMessageAdapter(
            items = breakList,
            onSelect = { msg ->
                previewTitle.text = msg.text
            },
            onToggle = { msg, isEnabled ->
                handleBreakToggle(msg, isEnabled)
            },
            onEdit = { msg ->
                showEditBreakDialog(msg)
            },
            onDelete = { msg ->
                showDeleteBreakDialog(msg)
            }
        )

        recyclerBreak.layoutManager = LinearLayoutManager(this)
        recyclerBreak.adapter = breakAdapter

        addBreakButton.setOnClickListener {
            val text = addBreakInput.text?.toString() ?: ""
            val clean = CustomTexts.clean(text)
            if (clean != null) {
                val newMsg = BreakMessage(
                    id = UUID.randomUUID().toString(),
                    text = clean,
                    enabled = true,
                    builtIn = false
                )
                breakList.add(newMsg)
                prefs.saveBreakMessages(breakList)
                breakAdapter.notifyItemInserted(breakList.size - 1)
                recyclerBreak.scrollToPosition(breakList.size - 1)
                addBreakInput.text = null
                previewTitle.text = clean
            }
        }
    }

    private fun handleBreakToggle(msg: BreakMessage, enabled: Boolean) {
        val index = breakList.indexOfFirst { it.id == msg.id }
        if (index == -1) return

        if (!enabled) {
            val enabledCount = breakList.count { it.enabled }
            if (enabledCount <= 1 && breakList[index].enabled) {
                // Cannot disable the last remaining enabled message
                Toast.makeText(this, R.string.err_min_one_break, Toast.LENGTH_SHORT).show()
                breakAdapter.notifyItemChanged(index)
                return
            }
        }

        breakList[index] = breakList[index].copy(enabled = enabled)
        prefs.saveBreakMessages(breakList)
        breakAdapter.notifyItemChanged(index)
    }

    private fun showEditBreakDialog(msg: BreakMessage) {
        val input = EditText(this).apply {
            setText(msg.text)
            setSelection(msg.text.length)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_dialog_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val cleaned = CustomTexts.clean(input.text.toString())
                if (cleaned != null) {
                    val index = breakList.indexOfFirst { it.id == msg.id }
                    if (index != -1) {
                        breakList[index] = breakList[index].copy(text = cleaned)
                        prefs.saveBreakMessages(breakList)
                        breakAdapter.notifyItemChanged(index)
                        previewTitle.text = cleaned
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteBreakDialog(msg: BreakMessage) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                val index = breakList.indexOfFirst { it.id == msg.id }
                if (index != -1) {
                    val wasEnabled = breakList[index].enabled
                    val enabledCount = breakList.count { it.enabled }
                    if (wasEnabled && enabledCount <= 1) {
                        Toast.makeText(this, R.string.err_min_one_break, Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    breakList.removeAt(index)
                    prefs.saveBreakMessages(breakList)
                    breakAdapter.notifyItemRemoved(index)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupLinesSection() {
        linesList = prefs.customBubbles().toMutableList()

        linesAdapter = CustomLineAdapter(
            items = linesList,
            onEdit = { line, pos ->
                showEditLineDialog(line, pos)
            },
            onDelete = { pos ->
                showDeleteLineDialog(pos)
            }
        )

        recyclerLines.layoutManager = LinearLayoutManager(this)
        recyclerLines.adapter = linesAdapter

        // Preset Chips
        findViewById<Chip>(R.id.chip_preset_1).setOnClickListener {
            addCustomLine(getString(R.string.chip_preset_1))
        }
        findViewById<Chip>(R.id.chip_preset_2).setOnClickListener {
            addCustomLine(getString(R.string.chip_preset_2))
        }
        findViewById<Chip>(R.id.chip_preset_3).setOnClickListener {
            addCustomLine(getString(R.string.chip_preset_3))
        }

        addLineButton.setOnClickListener {
            val text = addLineInput.text?.toString() ?: ""
            addCustomLine(text)
            addLineInput.text = null
        }
    }

    private fun addCustomLine(text: String) {
        val clean = CustomTexts.clean(text) ?: return
        if (clean in linesList) {
            Toast.makeText(this, "Line already exists", Toast.LENGTH_SHORT).show()
            return
        }
        linesList.add(clean)
        prefs.saveCustomBubbles(linesList)
        linesAdapter.notifyItemInserted(linesList.size - 1)
        recyclerLines.scrollToPosition(linesList.size - 1)
    }

    private fun showEditLineDialog(line: String, position: Int) {
        val input = EditText(this).apply {
            setText(line)
            setSelection(line.length)
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_dialog_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val cleaned = CustomTexts.clean(input.text.toString())
                if (cleaned != null && position in linesList.indices) {
                    linesList[position] = cleaned
                    prefs.saveCustomBubbles(linesList)
                    linesAdapter.notifyItemChanged(position)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteLineDialog(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (position in linesList.indices) {
                    linesList.removeAt(position)
                    prefs.saveCustomBubbles(linesList)
                    linesAdapter.notifyItemRemoved(position)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // --- Adapters ---

    private class BreakMessageAdapter(
        private val items: List<BreakMessage>,
        private val onSelect: (BreakMessage) -> Unit,
        private val onToggle: (BreakMessage, Boolean) -> Unit,
        private val onEdit: (BreakMessage) -> Unit,
        private val onDelete: (BreakMessage) -> Unit
    ) : RecyclerView.Adapter<BreakMessageAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.item_break_text)
            val badge: TextView = view.findViewById(R.id.item_break_badge)
            val switch: MaterialSwitch = view.findViewById(R.id.item_break_switch)
            val editBtn: ImageButton = view.findViewById(R.id.item_break_edit)
            val deleteBtn: ImageButton = view.findViewById(R.id.item_break_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_break_message, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.text
            holder.badge.text = if (item.builtIn) {
                holder.itemView.context.getString(R.string.official_badge)
            } else {
                holder.itemView.context.getString(R.string.custom_badge)
            }

            holder.switch.setOnCheckedChangeListener(null)
            holder.switch.isChecked = item.enabled
            holder.switch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }

            if (item.builtIn) {
                holder.editBtn.visibility = View.GONE
                holder.deleteBtn.visibility = View.GONE
            } else {
                holder.editBtn.visibility = View.VISIBLE
                holder.deleteBtn.visibility = View.VISIBLE
                holder.editBtn.setOnClickListener { onEdit(item) }
                holder.deleteBtn.setOnClickListener { onDelete(item) }
            }

            holder.itemView.setOnClickListener {
                onSelect(item)
                if (!item.builtIn) {
                    onEdit(item)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class CustomLineAdapter(
        private val items: List<String>,
        private val onEdit: (String, Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<CustomLineAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.item_line_text)
            val editBtn: ImageButton = view.findViewById(R.id.item_line_edit)
            val deleteBtn: ImageButton = view.findViewById(R.id.item_line_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_line, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item
            holder.editBtn.setOnClickListener { onEdit(item, holder.bindingAdapterPosition) }
            holder.deleteBtn.setOnClickListener { onDelete(holder.bindingAdapterPosition) }
            holder.itemView.setOnClickListener { onEdit(item, holder.bindingAdapterPosition) }
        }

        override fun getItemCount(): Int = items.size
    }
}
