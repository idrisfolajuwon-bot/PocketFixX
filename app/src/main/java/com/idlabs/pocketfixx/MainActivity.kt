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
import android.view.Gravity
import android.widget.*
import java.text.DecimalFormat

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private val fmt = DecimalFormat("#,##0.0")
    private val bg = Color.rgb(8, 16, 29)
    private val mint = Color.rgb(83, 224, 177)
    private val muted = Color.rgb(158, 173, 194)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 30)
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        setContentView(scroll)

        val brand = TextView(this).apply {
            text = "IDRIS"
            textSize = 13f
            letterSpacing = 0.28f
            setTextColor(mint)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
        }
        content.addView(brand)
        val title = TextView(this).apply {
            text = "PocketFix X"
            textSize = 31f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 2)
        }
        content.addView(title)
        val subtitle = TextView(this).apply {
            text = "YOUR PHONE. UNDERSTOOD."
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        content.addView(subtitle)
        text("DEVICE INTELLIGENCE  /  BY I D LABS", 12, mint, true)
        text("Native Android diagnostics", 15, Color.WHITE)
        text("Checks run only when you tap. No constant background scanning.", 13, muted)
        button("RUN FULL DIAGNOSTIC") { runDiagnostics() }
        button("CHECK HEAT & BATTERY") { showBattery() }
        button("CHECK MEMORY & STORAGE") { showSystem() }
        button("NETWORK STATUS") { showNetwork() }
        button("PHONE FINDER — SAFE MODE") {
            show("Phone Finder",
                "This version does not track another person. Any future sharing feature must be enabled by the device owner with clear consent. For this phone, use Android Find Hub / Find My Device with the owner's Google account.")
        }
        text("DIAGNOSTIC RESULTS", 13, mint, true)
        text("Choose a check above to see measured readings and their limits.", 14, Color.WHITE)
        text("IDRIS • POCKETFIX X • VERSION 1.1", 11, muted, true)
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
            celsius >= 42.0 -> "Elevated battery reading. Pause demanding use and charging, remove the case if safe, and let the phone cool naturally in a ventilated place. This reading alone does not identify the cause."
            celsius >= 38.0 -> "Warm battery reading. Compare with how the phone feels; charging, gaming, video and weak signal can raise temperature."
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
