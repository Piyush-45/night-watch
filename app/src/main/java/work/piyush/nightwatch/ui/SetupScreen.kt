package work.piyush.nightwatch.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import work.piyush.nightwatch.AdminReceiver
import work.piyush.nightwatch.Prefs
import work.piyush.nightwatch.Setup
import work.piyush.nightwatch.WatchService

@Composable
fun SetupScreen(tick: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var local by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { local++ }

    val usage = remember(tick, local) { Setup.usage(ctx) }
    val camera = remember(tick, local) { Setup.camera(ctx) }
    val notifs = remember(tick, local) { Setup.notifications(ctx) }
    val battery = remember(tick, local) { Setup.battery(ctx) }
    val admin = remember(tick, local) { Setup.admin(ctx) }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.text) }
            Title("Set up Night Watch")
        }
        Hint("5 quick steps. Each one opens a system screen. Turn it on, then come back here.")
        Spacer(Modifier.height(16.dp))

        Step(
            n = 1, done = usage, required = true,
            title = "Usage access",
            desc = "Lets the app see when the phone was unlocked and which apps were used. Find \"Night Watch\" in the list and turn it on.",
            action = "Open settings"
        ) {
            ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        Step(
            n = 2, done = camera, required = true,
            title = "Camera",
            desc = "Takes a selfie when someone unlocks the phone during quiet hours.",
            action = "Allow camera"
        ) {
            permLauncher.launch(Manifest.permission.CAMERA)
        }

        Step(
            n = 3, done = notifs, required = false,
            title = "Notifications",
            desc = "Shows a small \"Night Watch is on\" notice (required by Android) and reminds you if it gets paused.",
            action = "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        Step(
            n = 4, done = battery, required = false,
            title = "Unrestricted battery",
            desc = "Stops the phone from killing the watcher overnight. It only wakes up on unlock, so battery use stays tiny.",
            action = "Allow"
        ) {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                )
            } catch (_: Exception) {
                ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        Step(
            n = 5, done = admin, required = false,
            title = "Auto-lock (sleep guard)",
            desc = "Lets Night Watch lock the screen if someone falls asleep with reels playing. It only asks for \"lock screen\", nothing else. Tap Activate on the next screen.",
            action = "Allow auto-lock"
        ) {
            ctx.startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(ctx))
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Night Watch uses this only to turn the screen off when nobody answers \"Still watching?\" at night."
                    )
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                Prefs.setArmed(ctx, true)
                WatchService.start(ctx)
                onDone()
            },
            enabled = usage && camera,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (usage && camera) "Turn on Night Watch" else "Finish steps 1 and 2 first", fontSize = 16.sp) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Step(n: Int, done: Boolean, required: Boolean, title: String, desc: String, action: String, onAction: () -> Unit) {
    Panel(Modifier.padding(bottom = 10.dp), color = if (done) C.good.copy(alpha = 0.08f) else C.card) {
        Row {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (done) C.good else C.line),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Filled.Check, null, tint = C.bg, modifier = Modifier.size(18.dp))
                } else {
                    Text("$n", color = C.text, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Tag(if (required) "Required" else "Recommended", if (required) C.accent else C.dim)
                }
                Spacer(Modifier.height(4.dp))
                Text(desc, color = C.dim, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                if (done) {
                    Text("Done ✓", color = C.good, fontWeight = FontWeight.SemiBold)
                } else {
                    FilledTonalButton(onClick = onAction) { Text(action) }
                }
            }
        }
    }
}
