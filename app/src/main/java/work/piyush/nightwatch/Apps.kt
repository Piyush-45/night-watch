package work.piyush.nightwatch

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

enum class Cat(val label: String, val sensitive: Boolean) {
    CHATS("Chats", true),
    SOCIAL("Social", true),
    PHOTOS("Photos", true),
    CALLS("Calls", true),
    SETTINGS("Settings", true),
    BROWSER("Browser", false),
    VIDEO("Video", false),
    GAMES("Games", false),
    HOME("Home", false),
    OTHER("App", false),
}

/** App names, icons and a rough "what kind of app is this" classification. */
class Apps(ctx: Context) {
    private val pm = ctx.packageManager
    private val own = ctx.packageName
    private val home: Set<String> = try {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet()
    } catch (_: Exception) {
        emptySet()
    }
    private val labels = ConcurrentHashMap<String, String>()
    private val cats = ConcurrentHashMap<String, Cat>()
    private val icons = HashMap<String, ImageBitmap?>()

    fun hidden(pkg: String) =
        pkg.isBlank() || pkg == own || pkg == "com.android.systemui" || pkg == "android"

    fun label(pkg: String): String = labels.getOrPut(pkg) {
        if (pkg in home) {
            "Home screen"
        } else {
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
        }
    }

    fun cat(pkg: String): Cat = cats.getOrPut(pkg) { classify(pkg) }

    /** Main thread only. */
    fun icon(pkg: String): ImageBitmap? {
        if (icons.containsKey(pkg)) return icons[pkg]
        val b = try {
            pm.getApplicationIcon(pkg).toBitmap(72, 72).asImageBitmap()
        } catch (_: Exception) {
            null
        }
        icons[pkg] = b
        return b
    }

    private fun classify(pkg: String): Cat {
        val p = pkg.lowercase()
        fun has(vararg k: String) = k.any { p.contains(it) }
        return when {
            pkg in home -> Cat.HOME
            has("whatsapp", "telegram", "thoughtcrime", "signal", "messaging", "mms", "messenger", "discord") -> Cat.CHATS
            has("instagram", "facebook.katana", "snapchat", "twitter", "musically", "threads", "linkedin", "reddit", "sharechat", "moj") -> Cat.SOCIAL
            has("photos", "gallery", "camera") -> Cat.PHOTOS
            has("dialer", "contacts", "incallui", ".phone") -> Cat.CALLS
            has("settings") -> Cat.SETTINGS
            has("chrome", "browser", "firefox", "opera", "brave") -> Cat.BROWSER
            has("youtube", "netflix", "hotstar", "primevideo", "jiocinema", "spotify") -> Cat.VIDEO
            else -> try {
                when (pm.getApplicationInfo(pkg, 0).category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> Cat.SOCIAL
                    ApplicationInfo.CATEGORY_IMAGE -> Cat.PHOTOS
                    ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> Cat.VIDEO
                    ApplicationInfo.CATEGORY_GAME -> Cat.GAMES
                    else -> Cat.OTHER
                }
            } catch (_: Exception) {
                Cat.OTHER
            }
        }
    }
}
