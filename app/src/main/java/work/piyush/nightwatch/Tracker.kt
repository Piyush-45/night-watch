package work.piyush.nightwatch

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

data class AppUse(val pkg: String, val ms: Long)

/** One unlock → lock window. */
data class Session(
    val start: Long,
    val end: Long,
    val apps: List<AppUse>,
    val ongoing: Boolean = false,
)

/**
 * Reads Android's own usage-event log. The OS records these whether or not
 * this app is running, so nothing has to stay alive in the background.
 */
object Tracker {
    // Raw event-type values (stable across Android versions)
    private const val ACTIVITY_RESUMED = 1
    private const val ACTIVITY_PAUSED = 2
    private const val SCREEN_NON_INTERACTIVE = 16
    private const val KEYGUARD_SHOWN = 17
    private const val KEYGUARD_HIDDEN = 18 // = phone actually unlocked

    fun read(ctx: Context, from: Long, to: Long): List<Session> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from, to) ?: return emptyList()
        val e = UsageEvents.Event()
        val out = mutableListOf<Session>()

        var sessionStart = -1L
        var apps = LinkedHashMap<String, Long>()
        var fgPkg: String? = null
        var fgSince = 0L

        fun addFgTime(ts: Long) {
            val p = fgPkg ?: return
            if (sessionStart < 0) return
            val since = maxOf(fgSince, sessionStart)
            apps[p] = (apps[p] ?: 0L) + maxOf(0L, ts - since)
        }

        fun endSession(ts: Long, ongoing: Boolean) {
            if (sessionStart < 0) return
            addFgTime(ts)
            val list = apps
                .filterKeys { it != ctx.packageName }
                .map { AppUse(it.key, it.value) }
            out.add(Session(sessionStart, ts, list, ongoing))
            sessionStart = -1L
            apps = LinkedHashMap()
            fgSince = ts
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val ts = e.timeStamp
            val pkg = e.packageName ?: ""
            when (e.eventType) {
                KEYGUARD_HIDDEN -> if (sessionStart < 0) {
                    sessionStart = ts
                    fgPkg?.let { apps.getOrPut(it) { 0L } }
                }

                ACTIVITY_RESUMED -> {
                    addFgTime(ts)
                    if (sessionStart >= 0) apps.getOrPut(pkg) { 0L }
                    fgPkg = pkg
                    fgSince = ts
                }

                ACTIVITY_PAUSED -> if (pkg == fgPkg) {
                    addFgTime(ts)
                    fgPkg = null
                }

                KEYGUARD_SHOWN, SCREEN_NON_INTERACTIVE -> endSession(ts, false)
            }
        }
        if (sessionStart >= 0) endSession(to, true)
        return out
    }

    /**
     * App currently in front + since when it has been in front continuously.
     * Moving between screens inside the same app (pause → resume within 3s) doesn't reset the timer.
     */
    fun foreground(ctx: Context, now: Long): Pair<String, Long>? {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(now - 6L * 60 * 60 * 1000, now) ?: return null
        val e = UsageEvents.Event()
        var fg: String? = null
        var since = 0L
        var lastPkg: String? = null
        var lastPause = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                ACTIVITY_RESUMED -> {
                    val continued = (pkg == fg) || (pkg == lastPkg && e.timeStamp - lastPause < 3_000)
                    if (!continued) since = e.timeStamp
                    fg = pkg
                }
                ACTIVITY_PAUSED -> if (pkg == fg) {
                    lastPkg = pkg
                    lastPause = e.timeStamp
                    fg = null
                }
            }
        }
        val f = fg ?: return null
        return f to since
    }
}
