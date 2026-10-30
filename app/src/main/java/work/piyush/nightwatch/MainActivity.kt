package work.piyush.nightwatch

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import work.piyush.nightwatch.ui.HomeScreen
import work.piyush.nightwatch.ui.NightTheme
import work.piyush.nightwatch.ui.SettingsScreen
import work.piyush.nightwatch.ui.SetupScreen

class MainActivity : ComponentActivity() {

    /** Bumped on every resume so screens re-check permissions and reload data. */
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        Notifs.channels(this)
        setContent { NightTheme { App(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
        // Re-arm the camera watcher whenever the app is opened (needed after restarts/updates)
        if (Prefs.armed(this) && Setup.ready(this) && !WatchService.running) {
            WatchService.start(this)
        }
    }
}

@Composable
private fun App(tick: Int) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(if (Setup.usage(ctx)) "home" else "setup") }
    BackHandler(enabled = screen != "home") { screen = "home" }

    when (screen) {
        "settings" -> SettingsScreen(tick, onBack = { screen = "home" }, onSetup = { screen = "setup" })
        "setup" -> SetupScreen(tick, onDone = { screen = "home" })
        else -> HomeScreen(tick, onSettings = { screen = "settings" }, onSetup = { screen = "setup" })
    }
}
