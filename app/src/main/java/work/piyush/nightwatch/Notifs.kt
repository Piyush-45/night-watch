package work.piyush.nightwatch

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifs {
    private const val CH_SERVICE = "watch"
    private const val CH_ALERT = "alerts"
    const val SERVICE_ID = 1
    private const val REARM_ID = 2
    private const val GUARD_ID = 3
    private const val LOCKED_ID = 4
    private const val CH_GUARD = "guard"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_GUARD, "Still watching? prompts", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Night Watch running", NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Night Watch alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun service(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Night Watch is on")
            .setContentText("Watching unlocks during quiet hours")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openApp(ctx))
            .build()

    @SuppressLint("MissingPermission")
    fun guardPrompt(ctx: Context, label: String, openFor: Long, graceMs: Long) {
        if (!Setup.notifications(ctx)) return
        channels(ctx)
        val awake = PendingIntent.getForegroundService(
            ctx, 1,
            Intent(ctx, WatchService::class.java).setAction(WatchService.ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val graceText = if (graceMs < 60_000) "${graceMs / 1000}s" else "${graceMs / 60_000} min"
        val body = if (openFor > 0) {
            "$label has been open ${Data.dur(openFor)}. The phone will lock in $graceText unless you tap I'm awake."
        } else {
            "Test: the phone will lock in $graceText unless you tap I'm awake."
        }
        val n = NotificationCompat.Builder(ctx, CH_GUARD)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Still watching?")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(awake)
            .addAction(0, "I'm awake", awake)
            .setAutoCancel(true)
            .setTimeoutAfter(graceMs + 30_000)
            .build()
        NotificationManagerCompat.from(ctx).notify(GUARD_ID, n)
    }

    fun cancelGuard(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(GUARD_ID)

    @SuppressLint("MissingPermission")
    fun guardLocked(ctx: Context, label: String, openFor: Long) {
        if (!Setup.notifications(ctx)) return
        val text = if (openFor > 0) "$label was open ${Data.dur(openFor)} and nobody answered." else "Test lock worked."
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Night Watch locked the phone")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(LOCKED_ID, n)
    }

    @SuppressLint("MissingPermission")
    fun rearm(ctx: Context) {
        if (!Setup.notifications(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Night Watch is paused")
            .setContentText("Phone restarted or app updated. Tap to turn it back on.")
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(REARM_ID, n)
    }
}
