package work.piyush.nightwatch

import android.content.Context
import java.io.File
import java.util.Calendar

enum class Range(val label: String) {
    NIGHT("Last night"),
    TODAY("Today"),
    WEEK("7 days"),
    MONTH("30 days"),
}

data class AppRow(val pkg: String, val label: String, val cat: Cat, val ms: Long)

data class Unlock(
    val s: Session,
    val apps: List<AppRow>,
    val photos: List<File>,
    val quiet: Boolean,
    val sensitive: Boolean,
    val peek: Boolean,
    val autoLocked: Boolean,
)

object Data {
    const val DAY = 24L * 60 * 60 * 1000

    fun load(ctx: Context, apps: Apps): List<Unlock> {
        if (!Setup.usage(ctx)) return emptyList()
        val now = System.currentTimeMillis()
        val all = Store.merge(ctx, Tracker.read(ctx, now - 7 * DAY, now))
        val photos = Photos.all(ctx)
        val locks = LockLog.all(ctx)
        return all.sortedByDescending { it.start }.map { s ->
            val ar = s.apps
                .filter { !apps.hidden(it.pkg) }
                .map { AppRow(it.pkg, apps.label(it.pkg), apps.cat(it.pkg), it.ms) }
                .sortedByDescending { it.ms }
            val real = ar.filter { it.cat != Cat.HOME }
            Unlock(
                s = s,
                apps = ar,
                photos = Photos.forSession(photos, s),
                quiet = Prefs.inQuiet(ctx, s.start),
                sensitive = real.any { it.cat.sensitive },
                peek = real.isEmpty() && s.end - s.start < 30_000,
                autoLocked = !s.ongoing && locks.any { it.t in (s.end - 20_000)..(s.end + 20_000) },
            )
        }
    }

    /** The most recent quiet-hours window that has already started. */
    fun nightWindow(ctx: Context, now: Long): Pair<Long, Long> {
        val qs = Prefs.quietStart(ctx)
        val qe = Prefs.quietEnd(ctx)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, qs / 60)
            set(Calendar.MINUTE, qs % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var start = cal.timeInMillis
        if (start > now) start -= DAY
        var len = ((qe - qs + 1440) % 1440) * 60_000L
        if (len == 0L) len = DAY
        return start to start + len
    }

    fun filter(ctx: Context, rows: List<Unlock>, range: Range, quietOnly: Boolean, now: Long): List<Unlock> {
        val base = when (range) {
            Range.NIGHT -> {
                val (a, b) = nightWindow(ctx, now)
                rows.filter { it.s.start in a..b }
            }
            Range.TODAY -> {
                val sod = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                rows.filter { it.s.start >= sod }
            }
            Range.WEEK -> rows.filter { it.s.start >= now - 7 * DAY }
            Range.MONTH -> rows.filter { it.s.start >= now - 30 * DAY }
        }
        return if (range != Range.NIGHT && quietOnly) base.filter { it.quiet } else base
    }

    fun dur(ms: Long): String {
        val sec = ms / 1000
        return when {
            sec < 60 -> "${sec}s"
            sec < 3600 -> "${sec / 60}m ${sec % 60}s"
            else -> "${sec / 3600}h ${(sec % 3600) / 60}m"
        }
    }
}
