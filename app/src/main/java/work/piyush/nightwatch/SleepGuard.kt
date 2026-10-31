package work.piyush.nightwatch

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/** Record of every auto-lock, so the timeline can show it. Kept 30 days. */
object LockLog {
    data class Ev(val t: Long, val pkg: String, val ms: Long)

    private const val KEY = "locks"
    private const val KEEP_MS = 30L * 24 * 60 * 60 * 1000

    fun all(ctx: Context): List<Ev> {
        val arr = try {
            JSONArray(Store.prefs(ctx).getString(KEY, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Ev(o.getLong("t"), o.getString("p"), o.getLong("m"))
        }
    }

    fun add(ctx: Context, e: Ev) {
        val cutoff = System.currentTimeMillis() - KEEP_MS
        val list = all(ctx).filter { it.t >= cutoff } + e
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.t).put("p", it.pkg).put("m", it.ms)) }
        Store.prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun clear(ctx: Context) = Store.prefs(ctx).edit().remove(KEY).apply()
}

/**
 * Runs inside WatchService only while the screen is on (checks every 20s, so battery cost is tiny).
 *
 * If a social/video app has been open for "ask after" minutes with no app switch, it posts a
 * "Still watching?" banner. No tap within "lock after" minutes = nobody's awake, so lock the screen.
 */
class SleepGuard(private val ctx: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val apps by lazy { Apps(ctx) }
    private var running = false

    private var promptKey: String? = null
    private var promptPkg = ""
    private var promptOpenFor = 0L
    private var deadline = 0L
    private var snoozeKey: String? = null
    private var snoozeUntil = 0L
    private var testDeadline = 0L

    private val tickRunnable = Runnable { tick() }

    fun start() {
        if (running) return
        running = true
        handler.post(tickRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
        testDeadline = 0L
        clearPrompt()
    }

    fun snooze() {
        snoozeKey = promptKey
        snoozeUntil = System.currentTimeMillis() + Prefs.guardAfterMin(ctx) * 60_000L
        testDeadline = 0L
        clearPrompt()
        Prefs.log(ctx, "Sleep guard: \"I'm awake\" tapped, asking again in ${Prefs.guardAfterMin(ctx)} min")
    }

    fun test() {
        testDeadline = System.currentTimeMillis() + TEST_GRACE
        Notifs.guardPrompt(ctx, "Test", 0L, TEST_GRACE)
        Prefs.log(ctx, "Sleep guard test: locking in 30s unless \"I'm awake\" is tapped")
        start()
    }

    private fun clearPrompt() {
        if (promptKey != null || deadline != 0L) Notifs.cancelGuard(ctx)
        promptKey = null
        deadline = 0L
    }

    private fun tick() {
        if (!running) return
        try {
            check()
        } catch (e: Exception) {
            Prefs.log(ctx, "Sleep guard error: ${e.message}")
        }
        handler.postDelayed(tickRunnable, 20_000)
    }

    private fun check() {
        val now = System.currentTimeMillis()

        if (testDeadline > 0L) {
            if (now >= testDeadline) {
                testDeadline = 0L
                lock(null, 0L)
            }
            return
        }

        if (!Prefs.guard(ctx) || (Prefs.guardQuietOnly(ctx) && !Prefs.inQuiet(ctx, now))) {
            clearPrompt(); return
        }

        val fg = Tracker.foreground(ctx, now)
        if (fg == null || apps.cat(fg.first) !in WATCHED) {
            clearPrompt(); return
        }
        val (pkg, since) = fg
        val key = "$pkg@$since"
        val openFor = now - since
        val askAfter = Prefs.guardAfterMin(ctx) * 60_000L
        val grace = Prefs.guardGraceMin(ctx) * 60_000L

        if (snoozeKey == key && now < snoozeUntil) return
        if (openFor < askAfter) {
            clearPrompt(); return
        }

        if (promptKey != key) {
            promptKey = key
            promptPkg = pkg
            promptOpenFor = openFor
            deadline = now + grace
            Notifs.guardPrompt(ctx, apps.label(pkg), openFor, grace)
            Prefs.log(ctx, "Sleep guard: ${apps.label(pkg)} open ${Data.dur(openFor)}, asked \"Still watching?\"")
        } else if (now >= deadline) {
            lock(pkg, openFor)
        }
    }

    private fun lock(pkg: String?, openFor: Long) {
        clearPrompt()
        snoozeKey = null
        val label = if (pkg == null) "Test" else apps.label(pkg)
        if (!AdminReceiver.active(ctx)) {
            Prefs.log(ctx, "Sleep guard wanted to lock, but auto-lock isn't allowed (Setup → step 5)")
            return
        }
        try {
            ctx.getSystemService(DevicePolicyManager::class.java).lockNow()
        } catch (e: Exception) {
            Prefs.log(ctx, "Lock failed: ${e.message}")
            return
        }
        if (pkg != null) LockLog.add(ctx, LockLog.Ev(System.currentTimeMillis(), pkg, openFor))
        Prefs.log(ctx, "Auto-locked: $label was open ${Data.dur(openFor)}, nobody answered")
        Notifs.guardLocked(ctx, label, openFor)
    }

    companion object {
        val WATCHED = setOf(Cat.SOCIAL, Cat.VIDEO)
        private const val TEST_GRACE = 30_000L
    }
}
