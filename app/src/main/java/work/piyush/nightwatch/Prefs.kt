package work.piyush.nightwatch

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** All user settings + a tiny activity log for debugging the camera service. */
object Prefs {
    private fun p(ctx: Context) = Store.prefs(ctx)

    // Quiet hours stored as minutes from midnight (default 12:00 AM – 6:00 AM)
    fun quietStart(ctx: Context) = p(ctx).getInt("qs_min", 0)
    fun quietEnd(ctx: Context) = p(ctx).getInt("qe_min", 360)
    fun setQuietStart(ctx: Context, v: Int) = p(ctx).edit().putInt("qs_min", v).apply()
    fun setQuietEnd(ctx: Context, v: Int) = p(ctx).edit().putInt("qe_min", v).apply()

    fun armed(ctx: Context) = p(ctx).getBoolean("armed", false)
    fun setArmed(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("armed", v).apply()

    fun selfie(ctx: Context) = p(ctx).getBoolean("selfie", true)
    fun setSelfie(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("selfie", v).apply()

    fun selfieQuietOnly(ctx: Context) = p(ctx).getBoolean("selfie_quiet", true)
    fun setSelfieQuietOnly(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("selfie_quiet", v).apply()

    fun inQuiet(ctx: Context, ts: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val s = quietStart(ctx)
        val e = quietEnd(ctx)
        return when {
            s == e -> true
            s < e -> m in s until e
            else -> m >= s || m < e
        }
    }

    fun fmtMin(min: Int): String {
        val h = min / 60
        val m = min % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h12, m, if (h < 12) "AM" else "PM")
    }

    fun log(ctx: Context, msg: String) {
        val time = SimpleDateFormat("d MMM, hh:mm:ss a", Locale.getDefault()).format(Date())
        val lines = (listOf("$time · $msg") + diag(ctx)).take(12)
        p(ctx).edit().putString("diag", lines.joinToString("\n")).apply()
    }

    fun diag(ctx: Context): List<String> =
        p(ctx).getString("diag", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
}
