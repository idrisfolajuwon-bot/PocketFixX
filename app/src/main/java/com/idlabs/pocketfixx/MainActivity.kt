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
import android.widget.*
import java.text.DecimalFormat

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private val fmt = DecimalFormat("#,##0.0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 16, 29)
        window.navigationBarColor = android.graphics.Color.rgb(8, 16, 29)
        window.decorView.systemUiVisibility = 0
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 30)
            setBackgroundColor(android.graphics.Color.rgb(8, 16, 29))
        }
        scroll.addView(content)
        setContentView(scroll)

        text("I D LABS  /  DEVICE INTELLIGENCE", 12, 0xFF53E0B1.toInt(), true)
        text("PocketFix X", 32, 0xFFFFFFFF.toInt(), true)
        text("Native Android diagnostics", 15, 0xFF9EADC2.toInt())
        text("Checks run only when you tap. No constant background scanning.", 13, 0xFF9EADC2.toInt())
        button("RUN FULL DIAGNOSTIC") { runDiagnostics() }
        button("CHECK HEAT & BATTERY") { showBattery() }
        button("CHECK MEMORY & STORAGE") { showSystem() }
        button("NETWORK STATUS") { showNetwork() }
        button("PHONE FINDER — SAFE MODE") {
            show("Phone Finder",
                "This starter build does not track another person. A future sharing feature must be enabled by the device owner with clear consent. " +
                "For this phone, use Android Find Hub / Find My Device with the owner's Google account.")
        }
        text("DIAGNOSTIC RESULTS", 13, 0xFF53E0B1.toInt(), true)
        text("Choose a check above to see measured readings and their limits.", 14, 0xFFFFFFFF.toInt())
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) {
        content.addView(TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, 7, 0, 7)
        })
    }

    private fun button(label: String, action: () -> Unit) {
        content.addView(Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        })
    }

    private fun show(title: String, body: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(title).setMessage(body).setPositiveButton("OK", null).create()
        dialog.show()
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
        val current = if (Build.VERSION.SDK_INT >= 21)
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) else Int.MIN_VALUE
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
            append("Note: battery temperature is not the same as CPU temperature. Android does not expose every internal sensor to ordinary apps.")
        }
    }

    private fun showBattery() = show("Heat & Battery", batteryText())

    private fun showSystem() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free
        val body = "RAM available: ${fmt.format(mi.availMem / 1024.0 / 1024 / 1024)} GB\n" +
            "Low-memory condition: ${mi.lowMemory}\n\n" +
            "Internal storage used: ${fmt.format(used / 1e9)} GB\n" +
            "Internal storage available: ${fmt.format(free / 1e9)} GB\n" +
            "Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n\n" +
            "These are system-level estimates; this app does not inspect private data from other apps."
        show("System Status", body)
    }

    private fun showNetwork() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = if (network != null) cm.getNetworkCapabilities(network) else null
        val type = when {
            caps == null -> "No active network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other / VPN"
        }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        show("Network Status", "Connection: $type\nInternet validated by Android: ${if(validated) "Yes" else "No / unknown"}\n\nThis is a connection-status check, not an internet speed test.")
    }

    private fun runDiagnostics() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val stat = StatFs(Environment.getDataDirectory().path)
        val temp = registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempLine = if (temp != Int.MIN_VALUE) "${fmt.format(temp / 10.0)} °C" else "not exposed"
        val heatNote = if (temp != Int.MIN_VALUE && temp / 10.0 >= 42.0)
            "Battery temperature is elevated. Stop intensive use and charging, move the phone to a cool, ventilated place, and let it cool naturally."
        else "No high battery-temperature reading was reported. The phone may still feel warm because other sensors are not exposed."
        val report = "POCKETFIX X — QUICK REPORT\n\n" +
            "Battery: ${batteryText()}\n\n" +
            "Available RAM: ${fmt.format(mi.availMem / 1e9)} GB\n" +
            "Storage free: ${fmt.format(stat.availableBytes / 1e9)} GB\n" +
            "Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}\n\n" +
            "HEAT INTERPRETATION\nBattery sensor: $tempLine\n$heatNote\n\n" +
            "This first version reports readings, not an exact cause. Gaming, video, weak signal, charging and background activity can contribute to heat."
        show("Full Diagnostic Report", report)
    }
}
