package work.piyush.nightwatch

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dayFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val labelCache = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(40))
        }
        val scroll = ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ---------- settings ----------
    private val prefs get() = Store.prefs(this)
    private var quietStart: Int
        get() = prefs.getInt("qs", 23)
        set(v) = prefs.edit().putInt("qs", v).apply()
    private var quietEnd: Int
        get() = prefs.getInt("qe", 7)
        set(v) = prefs.edit().putInt("qe", v).apply()
    private var onlyQuiet: Boolean
        get() = prefs.getBoolean("oq", true)
        set(v) = prefs.edit().putBoolean("oq", v).apply()

    private fun inQuiet(ts: Long): Boolean {
        val h = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
        val s = quietStart
        val e = quietEnd
        return when {
            s == e -> true
            s < e -> h in s until e
            else -> h >= s || h < e
        }
    }

    // ---------- UI ----------
    private fun render() {
        root.removeAllViews()
        root.addView(text("Night Watch", 28f, bold = true))
        root.addView(text("Shows every time this phone was unlocked and which apps were opened.", 14f, alpha = 0.7f).apply {
            setPadding(0, dp(4), 0, dp(16))
        })

        if (!hasUsageAccess()) {
            root.addView(card(
                "Permission needed\n\nTap below, find \"Night Watch\" in the list and turn on usage access. Then come back here.",
                Color.argb(60, 255, 170, 0)
            ))
            root.addView(button("Grant usage access") {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            })
            return
        }

        // Quiet-hours controls
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(button("From ${hourLabel(quietStart)}") {
            quietStart = (quietStart + 1) % 24; render()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("Until ${hourLabel(quietEnd)}") {
            quietEnd = (quietEnd + 1) % 24; render()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(text("Quiet hours (tap to change)", 12f, alpha = 0.6f))
        root.addView(row)
        root.addView(button(if (onlyQuiet) "Showing: quiet hours only" else "Showing: all unlocks") {
            onlyQuiet = !onlyQuiet; render()
        })

        // Data
        val now = System.currentTimeMillis()
        val fresh = Tracker.read(this, now - 7L * 24 * 60 * 60 * 1000, now)
        val all = Store.merge(this, fresh).sortedByDescending { it.start }
        val shown = if (onlyQuiet) all.filter { inQuiet(it.start) } else all

        val quietCount = all.count { inQuiet(it.start) && !it.ongoing }
        root.addView(text(
            "$quietCount unlock${if (quietCount == 1) "" else "s"} during quiet hours (saved history)",
            16f, bold = true
        ).apply { setPadding(0, dp(16), 0, dp(8)) })

        if (shown.isEmpty()) {
            root.addView(text(
                "Nothing yet. Quick test: lock the phone, wait a few seconds, unlock, open a couple of apps, then come back here.",
                14f, alpha = 0.7f
            ))
            return
        }

        var lastDay = ""
        for (s in shown) {
            val day = dayFmt.format(Date(s.start))
            if (day != lastDay) {
                root.addView(text(day, 13f, bold = true, alpha = 0.6f).apply {
                    setPadding(0, dp(14), 0, dp(6))
                })
                lastDay = day
            }
            root.addView(sessionCard(s))
        }
    }

    private fun sessionCard(s: Session): View {
        val endLabel = if (s.ongoing) "now (this is you)" else timeFmt.format(Date(s.end))
        val header = "${timeFmt.format(Date(s.start))}  →  $endLabel   ·   ${dur(s.end - s.start)}"
        val appsLine = if (s.apps.isEmpty()) {
            "Unlocked, no app opened"
        } else {
            s.apps.joinToString("\n") { "• ${label(it.pkg)}  ${dur(it.ms)}" }
        }
        val color = if (inQuiet(s.start)) Color.argb(70, 255, 82, 82) else Color.argb(40, 140, 140, 140)
        return card("$header\n$appsLine", color)
    }

    // ---------- helpers ----------
    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun label(pkg: String): String = labelCache.getOrPut(pkg) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun dur(ms: Long): String {
        val sec = ms / 1000
        return when {
            sec < 60 -> "${sec}s"
            sec < 3600 -> "${sec / 60}m ${sec % 60}s"
            else -> "${sec / 3600}h ${(sec % 3600) / 60}m"
        }
    }

    private fun hourLabel(h: Int): String {
        val am = h < 12
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12 ${if (am) "AM" else "PM"}"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float, bold: Boolean = false, alpha: Float = 1f) =
        TextView(this).apply {
            text = s
            textSize = size
            this.alpha = alpha
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(s: String, onClick: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun card(s: String, bg: Int) = TextView(this).apply {
        text = s
        textSize = 14f
        setLineSpacing(0f, 1.2f)
        gravity = Gravity.START
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(bg)
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    }
}
