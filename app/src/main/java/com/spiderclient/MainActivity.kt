package com.spiderclient

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val red = Color.rgb(255, 38, 56)
    private val backgroundColor = Color.rgb(13, 13, 15)
    private val cardColor = Color.rgb(26, 26, 31)
    private val white = Color.WHITE
    private val gray = Color.rgb(165, 165, 175)
    private val green = Color.rgb(53, 212, 154)

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var connectButton: TextView
    private lateinit var configSummary: TextView

    private var currentTab = "Home"
    private var connected = false
    private var activeConfig = "No configuration selected"

    private val configs = mutableListOf<String>()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.statusBarColor = Color.rgb(8, 8, 8)
    window.navigationBarColor = Color.rgb(8, 8, 8)

    try {
        createLayout()
        showHome()
    } catch (e: Exception) {
        AlertDialog.Builder(this)
            .setTitle("Spider Client Error")
            .setMessage(
                "${e.javaClass.simpleName}: ${e.message}\n\n" +
                e.stackTraceToString()
            )
            .setPositiveButton("OK", null)
            .show()
    }
}

    private fun createLayout() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }

        setContentView(root)

        createHeader()

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        createBottomNavigation()
    }

    private fun createHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(18))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_spider)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        val title = TextView(this).apply {
            text = "SPIDER CLIENT"
            textSize = 18f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "SECURE CONNECTION"
            textSize = 10f
            setTextColor(gray)
        }

        titleContainer.addView(title)
        titleContainer.addView(subtitle)

        header.addView(
            titleContainer,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val indicator = View(this).apply {
            background = roundedDrawable(
                if (connected) green else red,
                50
            )
        }

        header.addView(
            indicator,
            LinearLayout.LayoutParams(dp(8), dp(8))
        )

        statusText = TextView(this).apply {
            text = if (connected) "Connected" else "Disconnected"
            textSize = 11f
            setTextColor(if (connected) green else gray)
            setPadding(dp(7), 0, 0, 0)
        }

        header.addView(statusText)
        root.addView(header)
    }

    private fun createBottomNavigation() {
        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
        }

        addNavigationItem(navigation, "Home", R.drawable.ic_home)
        addNavigationItem(navigation, "Configs", R.drawable.ic_configs)
        addNavigationItem(navigation, "Settings", R.drawable.ic_settings)

        root.addView(
            navigation,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )
    }

    private fun addNavigationItem(
        parent: LinearLayout,
        label: String,
        iconRes: Int
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = roundedDrawable(
                if (currentTab == label) red else cardColor,
                14
            )
            setOnClickListener {
                currentTab = label
                refreshScreen()
            }
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
        }

        item.addView(
            icon,
            LinearLayout.LayoutParams(dp(24), dp(24))
        )

        val text = TextView(this).apply {
            this.text = label
            textSize = 11f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, 0)
        }

        item.addView(text)

        parent.addView(
            item,
            LinearLayout.LayoutParams(0, -1, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        )
    }

private fun refreshScreen() {
    root.removeAllViews()

    createHeader()

    content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    val scroll = ScrollView(this).apply {
        isFillViewport = true
        clipToPadding = false

        addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    root.addView(
        scroll,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    )

    createBottomNavigation()

    when (currentTab) {
        "Home" -> showHome()
        "Configs" -> showConfigs()
        "Settings" -> showSettings()
    }
}
        private fun showHome() {
        content.removeAllViews()

        addText(
            content,
            "Your connection",
            23f,
            white,
            true
        )

        addText(
            content,
            "Manage your private network connection.",
            13f,
            gray
        )

        addSpace(content, 22)

        val connectionCard = createCard()
        content.addView(connectionCard)

        val statusIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_shield)
        }

        connectionCard.addView(
            statusIcon,
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        addSpace(connectionCard, 12)

        statusText = addText(
            connectionCard,
            if (connected) "CONNECTED" else "NOT CONNECTED",
            19f,
            if (connected) green else white,
            true
        )

        addSpace(connectionCard, 6)

        configSummary = addText(
            connectionCard,
            activeConfig,
            13f,
            gray
        )

        addSpace(connectionCard, 22)

        connectButton = addText(
            connectionCard,
            if (connected) "DISCONNECT" else "CONNECT",
            15f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
            background = roundedDrawable(red, 18)
            setPadding(dp(20), dp(15), dp(20), dp(15))
            isClickable = true
            isFocusable = true

            setOnClickListener {
                toggleConnection()
            }
        }

        connectionCard.addView(
            connectButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            )
        )

        addSpace(content, 20)

        addText(
            content,
            "CONNECTION DETAILS",
            12f,
            gray,
            true
        )

        addSpace(content, 10)

        val detailsCard = createCard()
        content.addView(detailsCard)

        addDetailRow(
            detailsCard,
            "Selected configuration",
            activeConfig
        )

        addDivider(detailsCard)

        addDetailRow(
            detailsCard,
            "Connection duration",
            if (connected) "Session active" else "--"
        )

        addDivider(detailsCard)

        addDetailRow(
            detailsCard,
            "Download speed",
            "--"
        )

        addDivider(detailsCard)

        addDetailRow(
            detailsCard,
            "Upload speed",
            "--"
        )

        addDivider(detailsCard)

        addDetailRow(
            detailsCard,
            "Latency",
            "--"
        )

        addSpace(content, 16)

        val notice = createCard()
        content.addView(notice)

        addText(
            notice,
            "Prototype mode",
            15f,
            red,
            true
        )

        addSpace(notice, 6)

        addText(
            notice,
            "This version contains the user interface only. " +
                "A real VPN connection requires an Xray core " +
                "and Android VPN service integration.",
            12f,
            gray
        )
    }

    private fun toggleConnection() {
        if (connected) {
            connected = false
            Toast.makeText(
                this,
                "Disconnected",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (activeConfig == "No configuration selected") {
                Toast.makeText(
                    this,
                    "Please select a configuration first",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            Toast.makeText(
                this,
                "VPN core is not integrated yet",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        refreshScreen()
    }

    private fun addDetailRow(
        parent: LinearLayout,
        label: String,
        value: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(gray)
        }

        row.addView(
            labelView,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val valueView = TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(white)
            gravity = Gravity.END
        }

        row.addView(valueView)

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            )
        )
    }
        private fun showConfigs() {
        content.removeAllViews()

        addText(content, "Configurations", 24f, white, true)
        addText(
            content,
            "Manage your server configurations.",
            13f,
            gray
        )

        addSpace(content, 20)

        val addButton = makeButton("+  Add Configuration")
        content.addView(addButton)
        addButton.setOnClickListener {
            showAddConfigDialog()
        }

        addSpace(content, 12)

        val clipboardButton = makeButton("Import from Clipboard")
        content.addView(clipboardButton)
        clipboardButton.setOnClickListener {
            importFromClipboard()
        }

        addSpace(content, 20)

        addText(content, "SAVED CONFIGURATIONS", 12f, gray, true)
        addSpace(content, 10)

        if (configs.isEmpty()) {
            val emptyCard = createCard()
            content.addView(emptyCard)

            addText(
                emptyCard,
                "No configurations yet",
                16f,
                white,
                true
            )

            addSpace(emptyCard, 6)

            addText(
                emptyCard,
                "Add a configuration using its share link or text.",
                12f,
                gray
            )
        } else {
            configs.forEach { config ->
                val card = createCard()
                content.addView(card)

                val titleRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                card.addView(titleRow)

                val name = TextView(this).apply {
                    text = config
                    textSize = 14f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                }

                titleRow.addView(
                    name,
                    LinearLayout.LayoutParams(0, -2, 1f)
                )

                if (config == activeConfig) {
                    val selected = TextView(this).apply {
                        text = "ACTIVE"
                        textSize = 10f
                        setTextColor(green)
                    }
                    titleRow.addView(selected)
                }

                addSpace(card, 12)

                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                card.addView(actions)

                val selectButton = smallButton(
                    if (config == activeConfig) "Selected" else "Select"
                )

                actions.addView(
                    selectButton,
                    LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        setMargins(0, 0, dp(5), 0)
                    }
                )

                selectButton.setOnClickListener {
                    activeConfig = config
                    refreshScreen()
                }

                val editButton = smallButton("Edit")

                actions.addView(
                    editButton,
                    LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        setMargins(dp(5), 0, dp(5), 0)
                    }
                )

                editButton.setOnClickListener {
                    showEditConfigDialog(config)
                }

                val deleteButton = smallButton("Delete")

                actions.addView(
                    deleteButton,
                    LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        setMargins(dp(5), 0, 0, 0)
                    }
                )

                deleteButton.setOnClickListener {
                    confirmDeleteConfig(config)
                }
            }
        }

        addSpace(content, 16)

        val note = createCard()
        content.addView(note)

        addText(note, "Supported input", 14f, red, true)
        addSpace(note, 6)

        addText(
            note,
            "You can save configuration links or text here. " +
                "Connecting to a server is not available until " +
                "the VPN core is integrated.",
            12f,
            gray
        )
    }

    private fun showAddConfigDialog() {
        val input = EditText(this).apply {
            hint = "Paste configuration link or text"
            setHintTextColor(gray)
            setTextColor(white)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedDrawable(cardColor, 12)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(120)
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Add Configuration")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()

                if (value.isNotEmpty()) {
                    val name = "Config ${configs.size + 1}"
                    configs.add("$name|$value")
                    activeConfig = "$name|$value"
                    refreshScreen()
                } else {
                    Toast.makeText(
                        this,
                        "Enter configuration text first",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun importFromClipboard() {
        val clipboard = getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        val clip = clipboard.primaryClip

        val value = if (
            clip != null && clip.itemCount > 0
        ) {
            clip.getItemAt(0).coerceToText(this).toString().trim()
        } else {
            ""
        }

        if (value.isBlank()) {
            Toast.makeText(
                this,
                "Clipboard is empty",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val name = "Config ${configs.size + 1}"
        configs.add("$name|$value")
        activeConfig = "$name|$value"

        Toast.makeText(
            this,
            "Configuration imported",
            Toast.LENGTH_SHORT
        ).show()

        refreshScreen()
    }

    private fun showEditConfigDialog(config: String) {
        val parts = config.split("|", limit = 2)
        val oldName = parts.getOrElse(0) { "Config" }
        val oldValue = parts.getOrElse(1) { "" }

        val input = EditText(this).apply {
            setText(oldValue)
            setTextColor(white)
            setHintTextColor(gray)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedDrawable(cardColor, 12)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Configuration")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newValue = input.text.toString().trim()

                if (newValue.isNotEmpty()) {
                    val updated = "$oldName|$newValue"
                    val index = configs.indexOf(config)

                    if (index >= 0) {
                        configs[index] = updated
                    }

                    if (activeConfig == config) {
                        activeConfig = updated
                    }

                    refreshScreen()
                }
            }
            .show()
    }

    private fun confirmDeleteConfig(config: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Configuration")
            .setMessage("Are you sure you want to delete this configuration?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                configs.remove(config)

                if (activeConfig == config) {
                    activeConfig = "No configuration selected"
                }

                refreshScreen()
            }
            .show()
    }
        private fun showSettings() {
        content.removeAllViews()

        addText(content, "Settings", 24f, white, true)
        addText(content, "Customize Spider Client.", 13f, gray)

        addSpace(content, 20)

        addSettingsSection("GENERAL")

        addSettingsItem(
            "Language",
            "English",
            R.drawable.ic_language
        ) {
            AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(arrayOf("English", "فارسی")) { _, which ->
                    Toast.makeText(
                        this,
                        if (which == 0) "English selected" else "فارسی انتخاب شد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }

        addSettingsItem(
            "Appearance",
            "Dark theme",
            R.drawable.ic_theme
        ) {
            Toast.makeText(
                this,
                "Dark theme is currently enabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        addSettingsItem(
            "Device ID",
            "Copy device identifier",
            R.drawable.ic_info
        ) {
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "Unavailable"

            val clipboard = getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText("Device ID", deviceId)
            )

            Toast.makeText(
                this,
                "Device ID copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        addSpace(content, 16)
        addSettingsSection("NETWORK")

        addSettingsItem(
            "DNS",
            "Coming soon",
            R.drawable.ic_settings
        ) {
            showComingSoon()
        }

        addSettingsItem(
            "Routing",
            "Coming soon",
            R.drawable.ic_configs
        ) {
            showComingSoon()
        }

        addSettingsItem(
            "IPv6",
            "Coming soon",
            R.drawable.ic_settings
        ) {
            showComingSoon()
        }

        addSettingsItem(
            "VPN mode",
            "Not integrated",
            R.drawable.ic_shield
        ) {
            showComingSoon()
        }

        addSettingsItem(
            "Connection logs",
            "Coming soon",
            R.drawable.ic_info
        ) {
            showComingSoon()
        }

        addSpace(content, 16)
        addSettingsSection("ABOUT")

        addSettingsItem(
            "App version",
            "1.0.0",
            R.drawable.ic_info
        ) {
            AlertDialog.Builder(this)
                .setTitle("Spider Client")
                .setMessage(
                    "Version 1.0.0\n\n" +
                    "A VPN client interface prototype."
                )
                .setPositiveButton("OK", null)
                .show()
        }

        addSettingsItem(
            "Backup configurations",
            "Export saved configurations",
            R.drawable.ic_backup
        ) {
            exportConfigs()
        }

        addSettingsItem(
            "Privacy policy",
            "Privacy information",
            R.drawable.ic_lock
        ) {
            AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage(
                    "This prototype stores configuration data " +
                    "in memory during the current app session. " +
                    "Persistent storage and VPN functionality " +
                    "have not yet been implemented."
                )
                .setPositiveButton("OK", null)
                .show()
        }

        addSettingsItem(
            "Subscribe Info",
            "Coming soon",
            R.drawable.ic_refresh
        ) {
            showComingSoon()
        }
    }

    private fun addSettingsSection(title: String) {
        addText(content, title, 12f, red, true)
        addSpace(content, 8)
    }

    private fun addSettingsItem(
        title: String,
        subtitle: String,
        iconRes: Int,
        action: () -> Unit
    ) {
        val card = createCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            setOnClickListener { action() }
        }

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
        )

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
        }

        card.addView(
            icon,
            LinearLayout.LayoutParams(dp(28), dp(28))
        )

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }

        labels.addView(
            TextView(this).apply {
                text = title
                textSize = 14f
                setTextColor(white)
            }
        )

        labels.addView(
            TextView(this).apply {
                text = subtitle
                textSize = 11f
                setTextColor(gray)
            }
        )

        card.addView(
            labels,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        card.addView(
            TextView(this).apply {
                text = "›"
                textSize = 25f
                setTextColor(gray)
            }
        )
    }

    private fun exportConfigs() {
        if (configs.isEmpty()) {
            Toast.makeText(
                this,
                "There are no configurations to export",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val text = configs.joinToString("\n\n") {
            it.substringAfter("|", it)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Spider Client Configurations")
            putExtra(Intent.EXTRA_TEXT, text)
        }

        startActivity(
            Intent.createChooser(intent, "Export configurations")
        )
    }

    private fun showComingSoon() {
        Toast.makeText(
            this,
            "This feature is not implemented yet",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedDrawable(cardColor, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            ).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        val textView = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        parent.addView(
            textView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            )
        )

        return textView
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(
            View(this).apply {
                setBackgroundColor(Color.rgb(55, 55, 60))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
    }

    private fun makeButton(label: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = roundedDrawable(red, 14)
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
    }

    private fun smallButton(label: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = roundedDrawable(
                Color.rgb(48, 35, 39),
                10
            )
        }
    }

    private fun roundedDrawable(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
