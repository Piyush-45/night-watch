package work.piyush.nightwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File

/**
 * Sits idle (no battery cost) until the phone is unlocked. On unlock during quiet hours it
 * opens the front camera, takes 2 photos (~1s and ~4s after unlock) and closes the camera.
 *
 * Android only lets a background app use the camera if this service was started while the
 * app was on screen, which is why it's started from the app and re-armed when the app opens.
 */
class WatchService : LifecycleService() {

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var registered = false
    private val guard by lazy { SleepGuard(this) }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_USER_PRESENT -> {
                    capture(force = false)
                    guard.start()
                }
                Intent.ACTION_SCREEN_OFF -> guard.stop()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
            Notifs.channels(this)
            ServiceCompat.startForeground(this, Notifs.SERVICE_ID, Notifs.service(this), type)
        } catch (e: Exception) {
            Prefs.log(this, "Couldn't start watcher: ${e.javaClass.simpleName}")
            Notifs.rearm(this)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!registered) {
            ContextCompat.registerReceiver(
                this, receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_OFF)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered = true
            Prefs.log(this, "Watcher on")
        }
        running = true

        if (getSystemService(PowerManager::class.java).isInteractive) guard.start()

        when (intent?.action) {
            ACTION_TEST -> capture(force = true)
            ACTION_SNOOZE -> guard.snooze()
            ACTION_GUARD_TEST -> guard.test()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) {
            try {
                unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
        registered = false
        guard.stop()
        running = false
        Prefs.log(this, "Watcher off")
        super.onDestroy()
    }

    private fun capture(force: Boolean) {
        if (!force) {
            if (!Prefs.selfie(this)) return
            if (Prefs.selfieQuietOnly(this) && !Prefs.inQuiet(this, System.currentTimeMillis())) return
        }
        if (busy) return
        if (!Setup.camera(this)) {
            Prefs.log(this, "Unlock seen, but camera permission is off")
            return
        }
        busy = true
        Prefs.log(this, if (force) "Test selfie started" else "Unlock seen, taking selfie")

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = try {
                future.get()
            } catch (e: Exception) {
                Prefs.log(this, "Camera unavailable: ${e.message}")
                busy = false
                return@addListener
            }
            val shot = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, shot)
            } catch (e: Exception) {
                Prefs.log(this, "Camera blocked: ${e.message}")
                busy = false
                return@addListener
            }

            fun finish() {
                if (!busy) return
                try {
                    provider.unbindAll()
                } catch (_: Exception) {
                }
                busy = false
            }

            // Safety net: never keep the camera open longer than 15s
            handler.postDelayed({ finish() }, 15_000)

            handler.postDelayed({
                takeOne(shot) {
                    handler.postDelayed({ takeOne(shot) { finish() } }, 2_800)
                }
            }, 1_200)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeOne(shot: ImageCapture, done: () -> Unit) {
        val f = File(Photos.dir(this), "${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(f).build()
        try {
            shot.takePicture(opts, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Prefs.log(this@WatchService, "Selfie saved")
                    done()
                }

                override fun onError(e: ImageCaptureException) {
                    f.delete()
                    Prefs.log(this@WatchService, "Selfie failed: ${e.message}")
                    done()
                }
            })
        } catch (e: Exception) {
            Prefs.log(this, "Selfie failed: ${e.message}")
            done()
        }
    }

    companion object {
        const val ACTION_TEST = "work.piyush.nightwatch.TEST"
        const val ACTION_SNOOZE = "work.piyush.nightwatch.SNOOZE"
        const val ACTION_GUARD_TEST = "work.piyush.nightwatch.GUARD_TEST"

        @Volatile
        var running = false

        fun start(ctx: Context, test: Boolean = false, action: String? = null) {
            val i = Intent(ctx, WatchService::class.java)
            if (test) i.action = ACTION_TEST
            if (action != null) i.action = action
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (e: Exception) {
                Prefs.log(ctx, "Couldn't start watcher: ${e.javaClass.simpleName}")
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WatchService::class.java))
        }
    }
}
