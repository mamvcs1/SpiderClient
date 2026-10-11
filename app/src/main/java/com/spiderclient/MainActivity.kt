package com.spiderclient

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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
            background = panelDrawable(Color.argb(150, 28, 20, 25), 20, red, 1)
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
            setOnClickListener { currentTab = key; render() }
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
        card.addView(text(t("DISCONNECTED", "قطع اتصال"), 19f, white, true))
        space(card, 4)
        val active = configs.firstOrNull { it.id == selectedId }
        card.addView(text(active?.name ?: t("No configuration selected", "پیکربندی انتخاب نشده"), 12f, gray))
        space(card, 12)
        card.addView(button(t("CONNECT", "اتصال")) {
            toast(t("Real VPN is not enabled yet. The Xray/VpnService connection must be completed first.", "اتصال واقعی VPN هنوز فعال نیست؛ ابتدا باید Xray و VpnService تکمیل شوند."))
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
        content.addView(text(t("Connection is currently a UI prototype; no active tunnel is claimed.", "اتصال در حال حاضر نمونه رابط کاربری است؛ تونل فعال ادعا نمی‌شود."), 11f, gray))
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
            actions.addView(smallButton(t("Ping", "پینگ")) { pingOne(cfg) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(5), 0, 0, 0) })
            actions.addView(smallButton("⋮") { showConfigActions(cfg) }, LinearLayout.LayoutParams(dp(42), dp(38)).apply { setMargins(dp(5), 0, 0, 0) })
            card.addView(actions)
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
        if (requestCode == 41 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try { contentResolver.openInputStream(uri)?.bufferedReader()?.use { addConfigText(it.readText(), true) } }
            catch (e: Exception) { toast(t("Could not read that file", "خواندن فایل ممکن نشد")) }
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
        val name = EditText(this).apply { setText(cfg.name); hint = t("Name", "نام"); setTextColor(white) }
        val raw = EditText(this).apply { setText(cfg.raw); hint = t("Configuration URI / JSON", "پیوند یا JSON پیکربندی"); minLines = 3; setTextColor(white) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(name); addView(raw) }
        AlertDialog.Builder(this).setTitle(t("Edit configuration", "ویرایش پیکربندی")).setView(box)
            .setNegativeButton(t("Cancel", "لغو"), null).setPositiveButton(t("Save", "ذخیره")) { _, _ ->
                val newRaw = raw.text.toString().trim()
                if (newRaw.isNotBlank()) { cfg.name = name.text.toString().trim().ifBlank { protocolOf(newRaw) }; cfg.raw = newRaw; saveConfigs(); render() }
            }.show()
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

    private fun pingOne(cfg: Config) {
        cfg.ping = t("Checking…", "در حال بررسی…"); render()
        io.execute {
            val result = tcpPing(cfg.raw)
            runOnUiThread { configs.firstOrNull { it.id == cfg.id }?.ping = result; saveConfigs(); render() }
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

    private fun showSettings() {
        content.addView(text(t("Settings", "تنظیمات"), 23f, white, true))
        content.addView(text(t("Personalize Spider Client", "شخصی‌سازی اسپایدر کلاینت"), 12f, gray))
        space(content, 12)
        setting(t("Language", "زبان"), if (fa) "فارسی" else "English") {
            AlertDialog.Builder(this).setTitle(t("Language", "زبان")).setItems(arrayOf("English", "فارسی")) { _, which ->
                fa = which == 1; prefs.edit().putBoolean("fa", fa).apply(); render()
            }.show()
        }
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
            background = panelDrawable(Color.argb(215, 150, 20, 38), 14, red, 1)
            setPadding(dp(10), dp(12), dp(10), dp(12)); isClickable = true; isFocusable = true
            setOnClickListener { animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(90).start(); action() }.start() }
        }

    private fun smallButton(label: String, action: () -> Unit): TextView =
        TextView(this).apply { text = label; textSize = 11f; setTextColor(white); gravity = Gravity.CENTER; background = panelDrawable(Color.argb(150, 45, 31, 37), 10, Color.argb(120, 255, 38, 56), 1); isClickable = true; setOnClickListener { action() } }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = panelDrawable(Color.argb(178, 31, 24, 29), 18, Color.argb(110, 255, 55, 75), 1)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(6)) }
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
