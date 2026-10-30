package work.piyush.nightwatch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.TreeMap

/**
 * Android only keeps detailed usage events for a few days,
 * so every time the app opens we save finished sessions here (kept 30 days).
 */
object Store {
    private const val PREFS = "night_watch"
    private const val KEY = "sessions"
    private const val KEEP_MS = 30L * 24 * 60 * 60 * 1000

    fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun clear(ctx: Context) = prefs(ctx).edit().remove(KEY).apply()

    fun merge(ctx: Context, fresh: List<Session>): List<Session> {
        val map = TreeMap<Long, Session>()
        val arr = try {
            JSONArray(prefs(ctx).getString(KEY, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val a = o.getJSONArray("a")
            val apps = (0 until a.length()).map {
                val x = a.getJSONObject(it)
                AppUse(x.getString("p"), x.getLong("m"))
            }
            map[o.getLong("s")] = Session(o.getLong("s"), o.getLong("e"), apps)
        }

        fresh.filter { !it.ongoing }.forEach { map[it.start] = it }

        val cutoff = System.currentTimeMillis() - KEEP_MS
        map.keys.filter { it < cutoff }.forEach { map.remove(it) }

        val outArr = JSONArray()
        map.values.forEach { s ->
            val a = JSONArray()
            s.apps.forEach { a.put(JSONObject().put("p", it.pkg).put("m", it.ms)) }
            outArr.put(JSONObject().put("s", s.start).put("e", s.end).put("a", a))
        }
        prefs(ctx).edit().putString(KEY, outArr.toString()).apply()

        return map.values.toList() + fresh.filter { it.ongoing }
    }
}
