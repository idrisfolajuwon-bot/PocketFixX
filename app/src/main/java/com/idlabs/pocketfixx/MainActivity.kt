package com.idlabs.pocketfixx

import android.app.Activity
import android.os.BatteryManager
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.content.IntentFilter
import android.app.ActivityManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.DecimalFormat

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var batteryValue: TextView
    private lateinit var tempValue: TextView
    private lateinit var ramValue: TextView
    private lateinit var storageValue: TextView
    private lateinit var networkValue: TextView
    private lateinit var healthValue: TextView
    private lateinit var scoreValue: TextView
    private lateinit var scoreNote: TextView
    private val fmt = DecimalFormat("#,##0.0")
    private val bg = Color.rgb(7, 13, 25)
    private val panel = Color.rgb(17, 28, 46)
    private val panel2 = Color.rgb(22, 36, 57)
    private val mint = Color.rgb(83, 224, 177)
    private val blue = Color.rgb(104, 174, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(151, 169, 193)
    private val amber = Color.rgb(255, 190, 92)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        render()
        refreshDashboard()
    }

    private fun render() {
        val scroll = ScrollView(this).apply { fillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 28)
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        setContentView(scroll)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brandCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandCol.addView(label("IDRIS", 12, mint, true))
        brandCol.addView(label("POCKETFIX X", 23, white, true))
        top.addView(brandCol, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(pill("DEVICE COMMAND CENTRE", mint))
        content.addView(top)
        content.addView(label("YOUR PHONE. UNDERSTOOD.", 10, muted, true).apply {
            letterSpacing = 0.16f
            setPadding(0, 5, 0, 18)
        })

        val hero = card(panel2)
        hero.addView(label("SYSTEM OVERVIEW", 11, mint, true))
        val scoreRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scoreValue = label("--", 42, white, true)
        scoreRow.addView(scoreValue)
        scoreRow.addView(label("  /  100", 14, muted, true).apply { setPadding(5, 12, 0, 0) })
        hero.addView(scoreRow)
        scoreNote = label("Tap refresh to read your device status.", 12, muted)
        hero.addView(scoreNote)
        val refresh = actionButton("↻   REFRESH DEVICE STATUS", mint)
        refresh.setOnClickListener { refreshDashboard() }
        hero.addView(refresh)
        content.addView(hero)

        section("LIVE DEVICE READINGS")
        val grid1 = horizontalRow()
        val batteryCard = metricCard("BATTERY", "--", "charge level", mint)
        batteryValue = batteryCard.second
        val heatCard = metricCard("BATTERY TEMP", "--", "sensor reading", amber)
        tempValue = heatCard.second
        grid1.addView(batteryCard.first, weighted())
        grid1.addView(space(10), LinearLayout.LayoutParams(10, 1))
        grid1.addView(heatCard.first, weighted())
        content.addView(grid1)

        val grid2 = horizontalRow()
        val ramCard = metricCard("AVAILABLE RAM", "--", "system estimate", blue)
        ramValue = ramCard.second
        val storageCard = metricCard("FREE STORAGE", "--", "internal storage", mint)
        storageValue = storageCard.second
        grid2.addView(ramCard.first, weighted())
        grid2.addView(space(10), LinearLayout.LayoutParams(10, 1))
        grid2.addView(storageCard.first, weighted())
        content.addView(grid2)

        val net = card(panel)
        net.addView(label("CONNECTIVITY", 11, muted, true))
        networkValue = label("Checking…", 18, white, true)
        net.addView(networkValue)
        healthValue = label("Battery health: reading not loaded", 12, muted)
        net.addView(healthValue)
        content.addView(net)

        section("QUICK TOOLS")
        val tools = card(panel)
        toolRow(tools, "◉", "Heat & battery", "Temperature, charge and battery flags") { showBattery() }
        separator(tools)
        toolRow(tools, "▦", "Memory & storage", "RAM, free space and Android version") { showSystem() }
        separator(tools)
        toolRow(tools, "⌁", "Network status", "Connection type and Android internet check") { showNetwork() }
        separator(tools)
        toolRow(tools, "✦", "Full diagnostic report", "Combine current readings into one report") { runDiagnostics() }
        separator(tools)
        toolRow(tools, "⌖", "Phone finder — safe mode", "No tracking without clear owner consent") {
            show("Phone Finder", "This version does not track another person. Any future sharing feature must be enabled by the device owner with clear consent. For this phone, use Android Find Hub / Find My Device with the owner's Google account.")
        }
        content.addView(tools)

        section("SMART NOTES")
        val notes = card(panel)
        notes.addView(label("What this dashboard can measure", 14, white, true))
        notes.addView(label("Battery level and temperature sensor, Android battery-health flag, available RAM, internal storage, Android version and connection type.", 12, muted))
        notes.addView(label("What it cannot see", 14, white, true).apply { setPadding(0, 12, 0, 3) })
        notes.addView(label("It cannot read every CPU/thermal sensor, inspect private activity inside other apps, guarantee internet speed, or identify the exact cause of heating.", 12, muted))
        content.addView(notes)

        content.addView(label("IDRIS  •  I D LABS  •  POCKETFIX X", 10, muted, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 4)
        })
        content.addView(label("Checks run only when you ask. No constant background scanning.", 10, muted).apply {
            gravity = Gravity.CENTER
        })
    }

    private fun refreshDashboard() {
        if (!::batteryValue.isInitialized) return
        val intent = registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val stat = StatFs(Environment.getDataDirectory().path)
        batteryValue.text = if (pct >= 0) "$pct%" else "N/A"
        tempValue.text = if (temp != Int.MIN_VALUE) "${fmt.format(temp / 10.0)}°C" else "N/A"
        ramValue.text = "${fmt.format(mi.availMem / 1e9)} GB"
        storageValue.text = "${fmt.format(stat.availableBytes / 1e9)} GB"
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> "No active network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other / VPN"
        }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        networkValue.text = "$type  •  ${if (validated) "Internet verified" else "Internet not verified"}"
        val healthName = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good (Android flag)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat warning"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead flag"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-voltage flag"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold flag"
            else -> "Not reported"
        }
        healthValue.text = "Battery health: $healthName"
        val c = if (temp != Int.MIN_VALUE) temp / 10.0 else Double.NaN
        val score = when {
            c.isNaN() -> if (mi.lowMemory) 68 else 82
            c >= 42 -> 58
            c >= 38 -> 76
            mi.lowMemory -> 72
            else -> 94
        }
        scoreValue.text = score.toString()
        scoreNote.text = when {
            c >= 42 -> "Warmth needs attention. Let the phone cool naturally."
            c >= 38 -> "A little warm. Keep an eye on temperature during heavy use."
            mi.lowMemory -> "Android reports low available memory. Close unused apps if needed."
            else -> "No major warning in the readings available right now."
        }
    }

    private fun section(value: String) {
        content.addView(label(value, 11, mint, true).apply { setPadding(2, 19, 0, 9); letterSpacing = 0.1f })
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 3, 0, 3)
        }

    private fun card(color: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 14, 16, 14)
        background = rounded(color, 18)
        val p = LinearLayout.LayoutParams(-1, -2)
        p.bottomMargin = 10
        layoutParams = p
    }

    private fun metricCard(title: String, initial: String, caption: String, accent: Int): Pair<LinearLayout, TextView> {
        val c = card(panel)
        c.addView(label(title, 10, muted, true))
        val value = label(initial, 25, accent, true).apply { setPadding(0, 6, 0, 1) }
        c.addView(value)
        c.addView(label(caption, 10, muted))
        return Pair(c, value)
    }

    private fun horizontalRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun weighted() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun space(w: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(w, 1) }

    private fun pill(value: String, color: Int): TextView = label(value, 8, color, true).apply {
        setPadding(10, 8, 10, 8)
        background = rounded(panel2, 30)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

    private fun actionButton(value: String, color: Int): TextView = label(value, 12, bg, true).apply {
        gravity = Gravity.CENTER
        setPadding(14, 13, 14, 13)
        background = rounded(color, 12)
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = 12
        layoutParams = p
    }

    private fun toolRow(parent: LinearLayout, symbol: String, title: String, desc: String, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        row.addView(label(symbol, 22, mint, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(42, -2))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label(title, 14, white, true))
        col.addView(label(desc, 10, muted))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label("›", 24, muted, true))
        parent.addView(row)
    }

    private fun separator(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(Color.rgb(37, 52, 72))
            layoutParams = LinearLayout.LayoutParams(-1, 1)
        })
    }

    private fun show(title: String, body: String) {
        android.app.AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton("OK", null).show()
    }

    private fun batteryText(): String {
        val intent = registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            ?: return "Battery broadcast unavailable on this device."
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val current = if (Build.VERSION.SDK_INT >= 21) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) else Int.MIN_VALUE
        val statusName = when(status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        val healthName = when(health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good (reported by Android)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat warning reported"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Not reported"
        }
        return buildString {
            appendLine("Charge: ${if(pct >= 0) "$pct%" else "Unavailable"}")
            appendLine("State: $statusName")
            appendLine("Battery temperature: ${if(temp != Int.MIN_VALUE) "${fmt.format(temp / 10.0)} °C" else "Unavailable"}")
            appendLine("Android battery health flag: $healthName")
            appendLine("Plugged in: ${if(plugged == 0) "No" else "Yes"}")
            appendLine("Current reading: ${if(current != Int.MIN_VALUE) "$current µA (may be unsupported/approximate)" else "Unavailable"}")
            appendLine()
            append("Battery temperature is not CPU temperature. Ordinary apps cannot read every internal thermal sensor.")
        }
    }

    private fun showBattery() = show("Heat & Battery", batteryText())

    private fun showSystem() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val stat = StatFs(Environment.getDataDirectory().path)
        val body = "RAM available: ${fmt.format(mi.availMem / 1e9)} GB\n" +
            "Low-memory condition: ${mi.lowMemory}\n\n" +
            "Internal storage used: ${fmt.format((stat.totalBytes - stat.availableBytes) / 1e9)} GB\n" +
            "Internal storage available: ${fmt.format(stat.availableBytes / 1e9)} GB\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n\n" +
            "These are system-level estimates; private data from other apps is not inspected."
        show("System Status", body)
    }

    private fun showNetwork() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> "No active network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other / VPN"
        }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        show("Network Status", "Connection: $type\nInternet validated by Android: ${if(validated) "Yes" else "No / unknown"}\n\nThis is not an internet speed test.")
    }

    private fun runDiagnostics() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val stat = StatFs(Environment.getDataDirectory().path)
        val temp = registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val celsius = if (temp != Int.MIN_VALUE) temp / 10.0 else Double.NaN
        val tempLine = if (!celsius.isNaN()) "${fmt.format(celsius)} °C" else "not exposed"
        val heatNote = when {
            celsius.isNaN() -> "Battery temperature is unavailable. Check how the phone feels and watch for Android warnings."
            celsius >= 42.0 -> "Elevated battery reading. Pause demanding use and charging, and let the phone cool naturally in a ventilated place. This reading alone does not identify the cause."
            celsius >= 38.0 -> "Warm battery reading. Charging, gaming, video and weak signal can raise temperature."
            else -> "No elevated battery-temperature reading at this check. Other components may still become warm."
        }
        val report = "IDRIS • POCKETFIX X — QUICK REPORT\n\n" +
            "Battery: ${batteryText()}\n\n" +
            "Available RAM: ${fmt.format(mi.availMem / 1e9)} GB\n" +
            "Storage free: ${fmt.format(stat.availableBytes / 1e9)} GB\n" +
            "Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n\n" +
            "HEAT INTERPRETATION\nBattery sensor: $tempLine\n$heatNote\n\n" +
            "This is a screening report, not an exact diagnosis. Battery temperature is not CPU temperature."
        show("Full Diagnostic Report", report)
    }
}
