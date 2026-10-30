package work.piyush.nightwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * After a restart or app update Android won't let the camera watcher start in the
 * background, so we post a "tap to turn back on" notification instead.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!Prefs.armed(ctx)) return
        Prefs.log(ctx, "Phone restarted / app updated: watcher paused until app is opened")
        Notifs.rearm(ctx)
    }
}
