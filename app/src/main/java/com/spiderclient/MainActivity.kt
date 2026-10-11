package com.spiderclient

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val red = Color.rgb(255, 38, 56)
    private val white = Color.WHITE
    private val gray = Color.rgb(175, 175, 185)
    private val green = Color.rgb(53, 212, 154)
    private val prefs by lazy { getSharedPreferences("spider_client", MODE_PRIVATE) }
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var currentTab = "home"
    private var fa = false
    private var selectedId = ""
    private var pinging = false
    private var transitionBusy = false
    private var activePingId = ""
    private val vpnRequestCode = 42
    private data class Config(val id: String, var name: String, var raw: String, var ping: String = "—")
    private val configs = mutableListOf<Config>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fa = prefs.getBoolean("fa", false)
        selectedId = prefs.getString("selected_id", "") ?: ""
        loadConfigs()
        window.statusBarColor = Color.rgb(12, 8, 10)
        window.navigationBarColor = Color.rgb(12, 8, 10)
        render()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun t(en: String, faText: String): String = if (fa) faText else en
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun loadConfigs() {
        configs.clear()
        val raw = prefs.getString("configs_json", "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                configs.add(Config(o.optString("id"), o.optString("name"), o.optString("raw"), o.optString("ping", "—")))
            }
        } catch (_: Exception) { }
    }

    private fun saveConfigs() {
        val arr = org.json.JSONArray()
        configs.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("raw", it.raw).put("ping", it.ping))
        }
        prefs.edit().putString("configs_json", arr.toString()).putString("selected_id", selectedId).apply()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(5))
            setBackgroundColor(Color.rgb(12, 9, 12))
        }
        setContentView(root)
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(12))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.spider_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(42), dp(42)))
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        titleBox.addView(text("SPIDER CLIENT", 17f, white, true))
        titleBox.addView(text(t("PRIVATE CONNECTION", "اتصال خصوصی"), 10f, gray))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val status = text(t("NOT CONNECTED", "متصل نیست"), 10f, red, true)
        header.addView(status)
        root.addView(header)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (fa) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        when (currentTab) {
            "home" -> showHome()
            "configs" -> showConfigs()
            else -> showSettings()
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(7), dp(4), dp(2))
            background = glassDrawable(20)
        }
        navItem(nav, "home", t("Home", "خانه"), R.drawable.ic_home)
        navItem(nav, "configs", t("Configs", "پیکربندی‌ها"), R.drawable.ic_configs)
        navItem(nav, "settings", t("Settings", "تنظیمات"), R.drawable.ic_settings)
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(62)))
    }

    private fun navItem(parent: LinearLayout, key: String, label: String, iconRes: Int) {
        val selected = currentTab == key
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = panelDrawable(if (selected) Color.argb(30, 255, 38, 56) else Color.TRANSPARENT, 14, if (selected) red else Color.TRANSPARENT, if (selected) 1 else 0)
            setOnClickListener { switchTabAnimated(key) }
        }
        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(if (selected) red else gray)
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(23), dp(23)))
        item.addView(text(label, 10f, if (selected) red else gray, selected))
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun showHome() {
        content.addView(text(t("Your connection", "اتصال شما"), 23f, white, true))
        content.addView(text(t("Manage your server and connection status.", "سرور و وضعیت اتصال خود را مدیریت کنید."), 12f, gray))
        space(content, 12)
        val card = panel()
        content.addView(card)
        val shield = ImageView(this).apply { setImageResource(R.drawable.ic_shield); setColorFilter(red) }
        card.addView(shield, LinearLayout.LayoutParams(dp(38), dp(38)))
        space(card, 7)
        val isConnected = prefs.getBoolean("vpn_connected", false)
        card.addView(text(if (isConnected) t("CONNECTED", "متصل") else t("DISCONNECTED", "قطع اتصال"), 19f, if (isConnected) green else white, true))
        space(card, 4)
        val active = configs.firstOrNull { it.id == selectedId }
        card.addView(text(active?.name ?: t("No configuration selected", "پیکربندی انتخاب نشده"), 12f, gray))
        space(card, 12)
        card.addView(button(if (isConnected) t("DISCONNECT", "قطع اتصال") else t("CONNECT", "اتصال")) {
            if (prefs.getBoolean("vpn_connected", false)) {
                startService(Intent(this, SpiderVpnService::class.java).setAction(SpiderVpnService.ACTION_STOP))
                prefs.edit().putBoolean("vpn_connected", false).apply()
                render()
            } else {
                val selected = configs.firstOrNull { it.id == selectedId }
                if (selected == null) {
                    toast(t("Select or add a configuration first.", "ابتدا یک پیکربندی انتخاب یا اضافه کنید."))
                } else {
                    pendingVpnRaw = selected.raw
                    val permission = VpnService.prepare(this)
                    if (permission != null) startActivityForResult(permission, vpnRequestCode)
                    else startVpnService()
                }
            }
        })
        space(content, 12)
        val info = panel()
        content.addView(info)
        info.addView(text(t("QUICK STATUS", "وضعیت سریع"), 11f, red, true))
        space(info, 8)
        detail(info, t("Saved configurations", "پیکربندی‌های ذخیره‌شده"), configs.size.toString())
        detail(info, t("Selected server", "سرور انتخاب‌شده"), active?.let { maskedAddress(it.raw) } ?: "—")
        detail(info, t("Last ping", "آخرین پینگ"), active?.ping ?: "—")
        space(content, 8)
        content.addView(text(t("Powered by Xray-core. Connection is shown only when the core reports a running state.", "مبتنی بر Xray-core؛ اتصال فقط پس از اجرای واقعی هسته نمایش داده می‌شود."), 11f, gray))
    }

    private fun showConfigs() {
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        top.addView(text(t("Configurations", "پیکربندی‌ها"), 22f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button("+") { showImportMenu() }, LinearLayout.LayoutParams(dp(52), dp(46)))
        content.addView(top)
        content.addView(text(t("Server links are stored locally on this device.", "پیوندهای سرور به‌صورت محلی روی دستگاه ذخیره می‌شوند."), 12f, gray))
        space(content, 8)
        content.addView(button(t("↻  Ping all", "↻  پینگ همه")) { pingAll() })
        space(content, 10)
        if (configs.isEmpty()) {
            val empty = panel()
            content.addView(empty)
            empty.addView(text(t("No configurations yet", "هنوز پیکربندی‌ای ندارید"), 15f, white, true))
            space(empty, 5)
            empty.addView(text(t("Tap + to add a link, import text, or select a file.", "برای افزودن پیوند، واردکردن متن یا انتخاب فایل، + را بزنید."), 12f, gray))
        }
        configs.forEach { cfg ->
            val card = panel().apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
            content.addView(card)
            val title = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            titleBox.addView(text(cfg.name, 14f, white, true))
            titleBox.addView(text(maskedAddress(cfg.raw), 11f, gray))
            title.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
            title.addView(text(cfg.ping, 11f, pingColor(cfg.ping), true))
            card.addView(title)
            space(card, 9)
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(smallButton(if (cfg.id == selectedId) t("Selected", "انتخاب‌شده") else t("Select", "انتخاب")) {
                selectedId = cfg.id; saveConfigs(); render()
            }, LinearLayout.LayoutParams(0, dp(38), 1f))
            actions.addView(smallButton(t("Ping", "پینگ")) { pingOne(cfg, card) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(5), 0, 0, 0) })
            actions.addView(smallButton("⋮") { showConfigActions(cfg) }, LinearLayout.LayoutParams(dp(42), dp(38)).apply { setMargins(dp(5), 0, 0, 0) })
            card.addView(actions)
            if (activePingId == cfg.id) {
                val ripple = WaterRippleView(this, Color.YELLOW, true)
                card.addView(ripple, LinearLayout.LayoutParams(-1, dp(54)))
                ripple.start { }
            }
        }
    }

    private fun showImportMenu() {
        val items = arrayOf(
            t("Manual / paste configuration", "ورود دستی / چسباندن پیکربندی"),
            t("Import from clipboard", "واردکردن از کلیپ‌بورد"),
            t("Import from file", "واردکردن فایل"),
            t("Subscription URL", "نشانی اشتراک"),
            t("Scan QR code", "اسکن کد QR"),
            t("Bulk import text", "واردکردن گروهی متن")
        )
        AlertDialog.Builder(this).setTitle(t("Add configuration", "افزودن پیکربندی")).setItems(items) { _, which ->
            when (which) {
                0, 5 -> showManualDialog(which == 5)
                1 -> importClipboard()
                2 -> openConfigFile()
                3 -> showSubscriptionDialog()
                4 -> explain(t("A QR scanner library is not configured yet.", "کتابخانه اسکن QR هنوز به پروژه اضافه نشده است."))
            }
        }.show()
    }

    private fun showManualDialog(bulk: Boolean) {
        val input = EditText(this).apply {
            hint = if (bulk) t("Paste one configuration per line", "هر پیکربندی را در یک خط وارد کنید") else t("Paste a share link or JSON", "پیوند اشتراک یا JSON را وارد کنید")
            setTextColor(white); setHintTextColor(gray); minLines = if (bulk) 5 else 3
            background = panelDrawable(Color.rgb(38, 30, 34), 12, red, 1); setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val box = LinearLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(input, LinearLayout.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this).setTitle(t("Add configuration", "افزودن پیکربندی")).setView(box)
            .setNegativeButton(t("Cancel", "لغو"), null)
            .setPositiveButton(t("Import", "واردکردن")) { _, _ -> addConfigText(input.text.toString(), bulk) }.show()
    }

    private fun importClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val value = try { cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty() } catch (_: Exception) { "" }
        if (value.isBlank()) toast(t("Clipboard is empty", "کلیپ‌بورد خالی است")) else addConfigText(value, true)
    }

    private fun openConfigFile() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 41)
        } catch (_: Exception) { explain(t("No file picker is available.", "انتخاب‌گر فایل در دسترس نیست.")) }
    }

    @Deprecated("Deprecated by Android but works on supported versions")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequestCode) {
            if (resultCode == RESULT_OK) startVpnService()
            else toast(t("VPN permission was not granted.", "مجوز VPN داده نشد."))
            return
        }
        if (requestCode == 41 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try { contentResolver.openInputStream(uri)?.bufferedReader()?.use { addConfigText(it.readText(), true) } }
            catch (e: Exception) { toast(t("Could not read that file", "خواندن فایل ممکن نشد")) }
        }
    }

    private var pendingVpnRaw: String = ""

    private fun startVpnService() {
        val raw = pendingVpnRaw
        if (raw.isBlank()) return
        val intent = Intent(this, SpiderVpnService::class.java)
            .setAction(SpiderVpnService.ACTION_START)
            .putExtra(SpiderVpnService.EXTRA_RAW_CONFIG, raw)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        prefs.edit().putString("vpn_error", "").apply()
        toast(t("Xray core is not included in this test version.", "این نسخه آزمایشی فعلاً هسته Xray ندارد."))
        Handler(Looper.getMainLooper()).postDelayed({ refreshVpnStatus(0) }, 1000)
    }

    private fun refreshVpnStatus(attempt: Int) {
        if (isFinishing) return
        if (prefs.getBoolean("vpn_connected", false)) {
            render()
            toast(t("Xray VPN connected.", "VPN با هسته Xray متصل شد."))
            return
        }
        val error = prefs.getString("vpn_error", "").orEmpty()
        if (error.isNotBlank()) {
            render()
            toast(error)
            return
        }
        if (attempt < 20) {
            Handler(Looper.getMainLooper()).postDelayed({ refreshVpnStatus(attempt + 1) }, 1000)
        }
    }

    private fun showSubscriptionDialog() {
        val input = EditText(this).apply { hint = "https://"; setTextColor(white); setHintTextColor(gray) }
        AlertDialog.Builder(this).setTitle(t("Subscription URL", "نشانی اشتراک")).setView(input)
            .setNegativeButton(t("Cancel", "لغو"), null).setPositiveButton(t("Fetch", "دریافت")) { _, _ ->
                val url = input.text.toString().trim()
                if (!url.startsWith("https://")) { toast(t("Use a valid HTTPS URL", "نشانی HTTPS معتبر وارد کنید")); return@setPositiveButton }
                io.execute {
                    try {
                        val conn = java.net.URL(url).openConnection()
                        conn.connectTimeout = 8000; conn.readTimeout = 8000
                        val result = conn.getInputStream().bufferedReader().use { it.readText() }
                        runOnUiThread { addConfigText(result, true) }
                    } catch (_: Exception) { runOnUiThread { toast(t("Subscription could not be downloaded", "دریافت اشتراک ناموفق بود")) } }
                }
            }.show()
    }

    private fun addConfigText(value: String, multiple: Boolean) {
        val lines = if (multiple) value.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") } else listOf(value.trim()).filter { it.isNotBlank() }
        var added = 0
        lines.forEach { raw ->
            if (raw.length < 8) return@forEach
            val id = java.util.UUID.randomUUID().toString()
            val protocol = protocolOf(raw)
            val name = protocol + " " + (configs.size + 1)
            configs.add(Config(id, name, raw))
            if (selectedId.isBlank()) selectedId = id
            added++
        }
        saveConfigs()
        if (added == 0) toast(t("No usable configuration text found", "پیکربندی معتبری پیدا نشد")) else {
            toast(t("Imported ", "وارد شد: ") + added)
            currentTab = "configs"; render()
        }
    }

    private fun showConfigActions(cfg: Config) {
        val items = arrayOf(t("Edit name / link", "ویرایش نام / پیوند"), t("Copy link", "کپی پیوند"), t("Share", "اشتراک‌گذاری"), t("Refresh ping", "تازه‌سازی پینگ"), t("Delete", "حذف"))
        AlertDialog.Builder(this).setTitle(cfg.name).setItems(items) { _, which ->
            when (which) {
                0 -> editConfig(cfg)
                1 -> { val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText(cfg.name, cfg.raw)); toast(t("Copied", "کپی شد")) }
                2 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, cfg.raw), t("Share configuration", "اشتراک‌گذاری پیکربندی")))
                3 -> pingOne(cfg)
                4 -> AlertDialog.Builder(this).setMessage(t("Delete this configuration?", "این پیکربندی حذف شود؟")).setNegativeButton(t("Cancel", "لغو"), null).setPositiveButton(t("Delete", "حذف")) { _, _ ->
                    configs.remove(cfg); if (selectedId == cfg.id) selectedId = configs.firstOrNull()?.id ?: ""; saveConfigs(); render()
                }.show()
            }
        }.show()
    }

    private fun editConfig(cfg: Config) {
        val source = cfg.raw
        val initial = JSONObject()
        try {
            if (source.trim().startsWith("{")) {
                val parsed = JSONObject(source)
                if (parsed.has("spiderProfile")) {
                    val profile = parsed.optJSONObject("spiderProfile") ?: JSONObject()
                    val it = profile.keys()
                    while (it.hasNext()) { val key = it.next(); initial.put(key, profile.opt(key)) }
                } else {
                    val it = parsed.keys()
                    while (it.hasNext()) { val key = it.next(); initial.put(key, parsed.opt(key)) }
                }
            } else {
                val uri = URI(source)
                initial.put("Address", uri.host ?: "")
                initial.put("Port", if (uri.port > 0) uri.port.toString() else when (uri.scheme?.lowercase()) { "https", "trojan", "vless", "vmess" -> "443"; "http" -> "80"; else -> "" })
                val userInfo = uri.rawUserInfo.orEmpty()
                initial.put("Id", userInfo.substringBefore(':', userInfo))
                val query = uri.rawQuery.orEmpty().split("&").filter { it.isNotBlank() }
                query.forEach { pair ->
                    val k = java.net.URLDecoder.decode(pair.substringBefore("="), "UTF-8")
                    val v = java.net.URLDecoder.decode(pair.substringAfter("=", ""), "UTF-8")
                    val key = when (k.lowercase()) {
                        "type", "network" -> "Network"
                        "host" -> "Ws host"
                        "path" -> "Ws path"
                        "security" -> "Tls"
                        "sni" -> "Sni"
                        "fp" -> "Fingerprint"
                        "alpn" -> "Alpn"
                        "flow" -> "Flow"
                        "encryption" -> "Encryption"
                        "headerType" -> "Head type"
                        else -> k
                    }
                    initial.put(key, v)
                }
            }
        } catch (_: Exception) { }
        val name = EditText(this).apply { setText(cfg.name); hint = t("Name", "نام"); setTextColor(white); setSingleLine(true) }
        val fields = listOf(
            "Remake" to "Remake", "Address" to "Address / IP / domain", "Port" to "1–65535",
            "Id" to "UUID / user ID / password", "Flow" to "xtls-rprx-vision", "Encryption" to "none",
            "Transport" to "tcp / ws / grpc / httpupgrade / xhttp", "Network" to "tcp / ws / grpc / ...",
            "Head type" to "none / http", "Ws host" to "WebSocket Host header",
            "Ws path" to "/path", "Final mask raw json format (final mask object)" to "{ }",
            "Tls" to "none / tls / reality", "Sni" to "server name indication",
            "Fingerprint" to "chrome / firefox / safari / random", "Alpn" to "h2,http/1.1",
            "Ech config list" to "One ECH config per line or JSON list",
            "Certificate fingerprint (SHA-256)" to "SHA-256 certificate pin",
            "Fragment" to "Fragment options (core-dependent)", "Sni spoofing" to "SNI override (core-dependent)"
        )
        val edits = linkedMapOf<String, EditText>()
        val checks = linkedMapOf<String, android.widget.CheckBox>()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            layoutDirection = if (fa) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        box.addView(text(t("Profile name", "نام پروفایل"), 12f, gray, true))
        box.addView(name, LinearLayout.LayoutParams(-1, dp(44)))
        space(box, 8)
        box.addView(text(t("Protocol fields", "فیلدهای پروتکل"), 15f, red, true))
        box.addView(text(t("Values are saved to this profile. Support at connection time depends on the Xray core and protocol implementation.", "مقادیر در پروفایل ذخیره می‌شوند؛ پشتیبانی هنگام اتصال به هسته Xray و پیاده‌سازی پروتکل بستگی دارد."), 11f, gray))
        space(box, 6)
        fields.forEach { (label, hint) ->
            box.addView(text(label, 12f, white, true))
            val value = initial.optString(label, when (label) {
                "Remake" -> protocolOf(source)
                "Transport" -> initial.optString("Network", "")
                "Network" -> "tcp"
                "Tls" -> if (source.contains("security=tls")) "tls" else "none"
                "Encryption" -> if (protocolOf(source) == "VLESS") "none" else ""
                "Final mask raw json format (final mask object)" -> "{}"
                else -> ""
            })
            val optionValues = when (label) {
                "Fingerprint" -> arrayOf("chrome", "firefox", "safari", "ios", "android", "edge", "random", "randomized", "")
                "Network", "Transport" -> arrayOf("tcp", "ws", "grpc", "http", "httpupgrade", "xhttp", "kcp", "quic")
                "Head type" -> arrayOf("none", "http")
                "Tls" -> arrayOf("none", "tls", "reality")
                "Encryption" -> arrayOf("none", "auto", "aes-128-gcm", "chacha20-poly1305")
                "Flow" -> arrayOf("", "xtls-rprx-vision")
                "Alpn" -> arrayOf("h2,http/1.1", "h2", "http/1.1", "h3", "")
                else -> null
            }
            val edit = EditText(this).apply {
                setText(value); this.hint = hint; setTextColor(white); setHintTextColor(gray)
                textSize = 13f; setPadding(dp(10), dp(8), dp(10), dp(8))
                background = panelDrawable(Color.rgb(35, 27, 32), 10, Color.argb(150, 255, 38, 56), 1)
                if (label.contains("raw json", true) || label == "Ech config list") minLines = 2 else setSingleLine(true)
                inputType = if (label == "Port") android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                if (optionValues != null) {
                    isFocusable = false
                    isClickable = true
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0)
                    setOnClickListener {
                        val selected = optionValues.indexOf(text.toString()).let { if (it < 0) 0 else it }
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(label)
                            .setSingleChoiceItems(optionValues.map { if (it.isBlank()) t("(empty)", "(خالی)") else it }.toTypedArray(), selected) { dialog, which ->
                                setText(optionValues[which])
                                dialog.dismiss()
                            }
                            .setNegativeButton(t("Cancel", "لغو"), null)
                            .show()
                    }
                }
            }
            edits[label] = edit
            box.addView(edit, LinearLayout.LayoutParams(-1, if (label.contains("raw json", true) || label == "Ech config list") dp(78) else dp(44)))
            space(box, 5)
        }
        listOf("Allow insercure", "Verify peer cert by name").forEach { label ->
            val check = android.widget.CheckBox(this).apply {
                text = label; setTextColor(white); buttonTintList = android.content.res.ColorStateList.valueOf(red)
                isChecked = initial.optBoolean(label, initial.optBoolean(if (label == "Allow insercure") "allowInsecure" else "verifyPeerCertByName", false))
            }
            checks[label] = check
            box.addView(check)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(box) }
        val dialog = AlertDialog.Builder(this).setTitle(t("Edit protocol profile", "ویرایش پروفایل پروتکل")).setView(scroll)
            .setNegativeButton(t("Cancel", "لغو"), null)
            .setPositiveButton(t("Save", "ذخیره")) { _, _ ->
                val address = edits["Address"]?.text?.toString()?.trim().orEmpty()
                val portText = edits["Port"]?.text?.toString()?.trim().orEmpty()
                val port = portText.toIntOrNull()
                if (address.isBlank() || port == null || port !in 1..65535) {
                    toast(t("Enter a valid address and port (1–65535).", "نشانی معتبر و پورت ۱ تا ۶۵۵۳۵ وارد کنید."))
                    return@setPositiveButton
                }
                val profile = JSONObject()
                fields.forEach { (label, _) -> profile.put(label, edits[label]?.text?.toString()?.trim().orEmpty()) }
                checks.forEach { (label, check) -> profile.put(label, check.isChecked) }
                profile.put("protocol", edits["Remake"]?.text?.toString()?.trim().orEmpty().ifBlank { protocolOf(source) })
                profile.put("source", source)
                val stored = JSONObject().put("protocol", profile.optString("protocol"))
                    .put("address", address).put("port", port).put("spiderProfile", profile)
                cfg.name = name.text.toString().trim().ifBlank { profile.optString("protocol") }
                cfg.raw = stored.toString()
                saveConfigs()
                render()
                toast(t("Profile saved. Core-specific options are stored but not all are active until core integration.", "پروفایل ذخیره شد. گزینه‌های وابسته به هسته ذخیره می‌شوند، اما تا یکپارچه‌سازی هسته همگی فعال نیستند."))
            }.create()
        dialog.setOnShowListener {
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), (resources.displayMetrics.heightPixels * 0.82).toInt())
        }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), (resources.displayMetrics.heightPixels * 0.82).toInt())
    }

    private fun pingAll() {
        if (pinging) return
        if (configs.isEmpty()) { toast(t("Add a configuration first", "ابتدا پیکربندی اضافه کنید")); return }
        pinging = true
        toast(t("Checking server TCP reachability…", "در حال بررسی دسترسی TCP سرورها…"))
        val snapshot = configs.toList()
        io.execute {
            snapshot.forEach { cfg ->
                val result = tcpPing(cfg.raw)
                runOnUiThread { configs.firstOrNull { it.id == cfg.id }?.ping = result; saveConfigs(); if (currentTab == "configs") render() }
            }
            runOnUiThread { pinging = false; toast(t("Ping checks complete (TCP reachability, not VPN latency)", "بررسی پینگ تمام شد (دسترسی TCP، نه تأخیر تونل VPN)")) }
        }
    }

    private fun pingOne(cfg: Config, rippleTarget: LinearLayout? = null) {
        cfg.ping = t("Checking…", "در حال بررسی…")
        activePingId = cfg.id
        render()
        io.execute {
            val result = tcpPing(cfg.raw)
            runOnUiThread {
                configs.firstOrNull { it.id == cfg.id }?.ping = result
                activePingId = ""
                saveConfigs()
                render()
            }
        }
    }

    private fun switchTabAnimated(key: String) {
        if (transitionBusy || key == currentTab) return
        transitionBusy = true
        val old = content
        old.animate().alpha(0f).translationY(dp(12).toFloat()).setDuration(130).withEndAction {
            currentTab = key
            render()
            content.alpha = 0f
            content.translationY = dp(-8).toFloat()
            content.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction {
                transitionBusy = false
            }.start()
        }.start()
    }

    private fun addWaterRipple(target: LinearLayout, running: Boolean, success: Boolean = false) {
        val ripple = WaterRippleView(this, if (running) Color.YELLOW else if (success) red else Color.YELLOW, running)
        target.addView(ripple, LinearLayout.LayoutParams(-1, dp(54)))
        ripple.start {
            target.removeView(ripple)
        }
    }

    private inner class WaterRippleView(
        context: Context,
        private val rippleColor: Int,
        private val continuous: Boolean
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var progress = 0f
        private var animator: android.animation.ValueAnimator? = null

        init { setLayerType(View.LAYER_TYPE_HARDWARE, null) }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val maxRadius = minOf(width, height) * 0.42f
            for (ring in 0..2) {
                val phase = (progress + ring / 3f) % 1f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(1).toFloat()
                paint.color = rippleColor
                paint.alpha = ((1f - phase) * 150).toInt()
                canvas.drawCircle(cx, cy, maxRadius * phase, paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(22, 255, 38, 56)
            canvas.drawOval(cx - maxRadius * 1.5f, cy + maxRadius * 0.2f, cx + maxRadius * 1.5f, cy + maxRadius * 0.6f, paint)
        }

        fun start(onFinished: () -> Unit) {
            animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = if (continuous) 900L else 650L
                repeatCount = if (continuous) android.animation.ValueAnimator.INFINITE else 0
                addUpdateListener { progress = it.animatedValue as Float; invalidate() }
                if (!continuous) addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) { onFinished() }
                })
                start()
            }
        }

        override fun onDetachedFromWindow() {
            animator?.cancel()
            super.onDetachedFromWindow()
        }
    }

    private fun tcpPing(raw: String): String {
        val endpoint = endpoint(raw) ?: return t("N/A", "نامشخص")
        val start = System.currentTimeMillis()
        return try {
            Socket().use { it.connect(InetSocketAddress(endpoint.first, endpoint.second), 2500) }
            (System.currentTimeMillis() - start).toString() + " ms"
        } catch (_: Exception) { t("Timeout", "مهلت تمام شد") }
    }

    private fun endpoint(raw: String): Pair<String, Int>? {
        try {
            if (raw.startsWith("{")) {
                val j = JSONObject(raw)
                val host = j.optString("add", j.optString("address", ""))
                val port = j.optInt("port", 0)
                if (host.isNotBlank() && port in 1..65535) return host to port
            }
            val u = URI(raw)
            val host = u.host
            val port = if (u.port > 0) u.port else when (u.scheme?.lowercase()) { "https", "trojan", "vless", "vmess" -> 443; "http" -> 80; else -> -1 }
            if (!host.isNullOrBlank() && port in 1..65535) return host to port
        } catch (_: Exception) { }
        val m = Regex("@([^:/?#]+):(\\d+)").find(raw)
        if (m != null) return m.groupValues[1] to (m.groupValues[2].toIntOrNull() ?: return null)
        return null
    }

    private fun maskedAddress(raw: String): String {
        val e = endpoint(raw) ?: return t("Address hidden", "نشانی پنهان")
        val host = e.first
        val mask = if (host.length <= 5) host.take(1) + "***" else host.take(2) + "***" + host.takeLast(minOf(5, host.length - 2))
        return mask + ":" + e.second
    }

    private fun protocolOf(raw: String): String {
        val s = raw.trim().lowercase()
        return when {
            s.startsWith("vless://") -> "VLESS"
            s.startsWith("vmess://") -> "VMess"
            s.startsWith("trojan://") -> "Trojan"
            s.startsWith("ss://") || s.startsWith("shadowsocks://") -> "Shadowsocks"
            s.startsWith("socks://") -> "SOCKS"
            s.startsWith("http://") || s.startsWith("https://") -> "HTTP/HTTPS"
            s.startsWith("{") -> try { JSONObject(raw).optString("protocol", "JSON config").uppercase() } catch (_: Exception) { "JSON config" }
            else -> "Unknown"
        }
    }

    private fun languageSetting() {
        val card = panel().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        content.addView(card)
        card.addView(text(t("Language", "زبان"), 14f, white, true))
        card.addView(text(t("Choose the app language", "زبان برنامه را انتخاب کنید"), 11f, gray))
        space(card, 10)
        val choices = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        choices.addView(smallButton(if (!fa) "✓  English" else "English") {
            if (fa) {
                fa = false
                prefs.edit().putBoolean("fa", false).apply()
                render()
            }
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        choices.addView(smallButton(if (fa) "✓  فارسی" else "فارسی") {
            if (!fa) {
                fa = true
                prefs.edit().putBoolean("fa", true).apply()
                render()
            }
        }, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            setMargins(dp(8), 0, 0, 0)
        })
        card.addView(choices)
        space(content, 7)
    }

    private fun showSettings() {
        content.addView(text(t("Settings", "تنظیمات"), 23f, white, true))
        content.addView(text(t("Personalize Spider Client", "شخصی‌سازی اسپایدر کلاینت"), 12f, gray))
        space(content, 12)
        languageSetting()
        setting(t("Appearance", "ظاهر"), t("Black / red theme", "تم مشکی / قرمز")) {
            explain(t("The black and red theme is currently fixed. Glass blur effects depend on Android version and device support.", "تم مشکی و قرمز فعلاً ثابت است. افکت محوشدگی شیشه‌ای به نسخه اندروید و پشتیبانی دستگاه بستگی دارد."))
        }
        setting(t("Export configurations", "خروجی پیکربندی‌ها"), t("Share saved links", "اشتراک‌گذاری پیوندهای ذخیره‌شده")) {
            if (configs.isEmpty()) toast(t("Nothing to export", "چیزی برای خروجی وجود ندارد")) else {
                val all = configs.joinToString("\n") { it.raw }
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, all), t("Export configurations", "خروجی پیکربندی‌ها")))
            }
        }
        setting(t("Clear ping history", "پاک‌کردن سابقه پینگ"), t("Reset saved ping values", "بازنشانی مقادیر پینگ ذخیره‌شده")) {
            configs.forEach { it.ping = "—" }; saveConfigs(); render()
        }
        setting(t("Connection mode", "حالت اتصال"), t("Core not integrated", "هسته هنوز یکپارچه نشده")) {
            explain(t("The project contains the UI and configuration management, but a working Xray core + Android VpnService connection is not yet wired up. The Connect button will not fake a connected status.", "پروژه رابط کاربری و مدیریت پیکربندی دارد، اما اتصال واقعی Xray و Android VpnService هنوز متصل نشده است. دکمه اتصال وضعیت جعلی نمایش نمی‌دهد."))
        }
        setting(t("About Spider Client", "درباره اسپایدر کلاینت"), "1.0.0") {
            explain(t("Spider Client • version 1.0.0\nConfiguration links are stored locally. TCP ping tests server reachability only.", "اسپایدر کلاینت • نسخه ۱.۰.۰\nپیوندهای پیکربندی محلی ذخیره می‌شوند. پینگ TCP فقط دسترسی سرور را می‌سنجد."))
        }
    }

    private fun setting(title: String, subtitle: String, action: () -> Unit) {
        val c = panel().apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; isClickable = true; setOnClickListener { action() } }
        content.addView(c)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(text(title, 14f, white, true))
        box.addView(text(subtitle, 11f, gray))
        c.addView(box, LinearLayout.LayoutParams(0, -2, 1f))
        c.addView(text("›", 22f, red))
        space(content, 7)
    }

    private fun explain(message: String) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton(t("OK", "باشه"), null).show()
    }

    private fun detail(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        row.addView(text(label, 12f, gray), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(value, 12f, white, true))
        parent.addView(row)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; gravity = if (fa) Gravity.RIGHT else Gravity.LEFT }

    private fun button(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label; textSize = 13f; setTextColor(white); gravity = Gravity.CENTER
            background = glassDrawable(14, true)
            setPadding(dp(10), dp(12), dp(10), dp(12)); isClickable = true; isFocusable = true
            setOnClickListener { animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(90).start(); action() }.start() }
        }

    private fun smallButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply { text = label; textSize = 11f; setTextColor(white); gravity = Gravity.CENTER; background = glassDrawable(10); isClickable = true; setOnClickListener { action() } }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = glassDrawable(18)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(6)) }
    }

    private fun glassDrawable(radius: Int, emphasized: Boolean = false): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Color.argb(if (emphasized) 185 else 105, 255, 255, 255),
            Color.argb(if (emphasized) 170 else 72, 78, 38, 49),
            Color.argb(155, 18, 13, 19)
        )
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(if (emphasized) 210 else 100, 255, 65, 83))
    }

    private fun panelDrawable(color: Int, radius: Int, stroke: Int, width: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (width > 0) setStroke(dp(width), stroke)
    }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun pingColor(value: String): Int = when {
        value.contains("ms") -> red
        value.contains("Timeout", true) || value.contains("مهلت") -> Color.YELLOW
        else -> gray
    }
}
