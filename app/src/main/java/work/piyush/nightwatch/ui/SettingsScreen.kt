package work.piyush.nightwatch.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import work.piyush.nightwatch.Photos
import work.piyush.nightwatch.Prefs
import work.piyush.nightwatch.Setup
import work.piyush.nightwatch.Store
import work.piyush.nightwatch.WatchService

@Composable
fun SettingsScreen(tick: Int, onBack: () -> Unit, onSetup: () -> Unit) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var qs by remember { mutableIntStateOf(Prefs.quietStart(ctx)) }
    var qe by remember { mutableIntStateOf(Prefs.quietEnd(ctx)) }
    var selfie by remember { mutableStateOf(Prefs.selfie(ctx)) }
    var selfieQuiet by remember { mutableStateOf(Prefs.selfieQuietOnly(ctx)) }
    var confirmDelete by remember { mutableStateOf(false) }

    val photoCount = remember(tick, refresh) { Photos.all(ctx).size }
    val log = remember(tick, refresh) { Prefs.diag(ctx) }
    val setupDone = remember(tick) { Setup.doneCount(ctx) }

    fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, false).show()
    }

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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.text) }
            Title("Settings")
        }

        SectionTitle("Quiet hours")
        Panel {
            Column {
                SettingRow("Starts", Prefs.fmtMin(qs), "Tap to change") {
                    pickTime(qs) { qs = it; Prefs.setQuietStart(ctx, it) }
                }
                HorizontalDivider(color = C.line)
                SettingRow("Ends", Prefs.fmtMin(qe), "Tap to change") {
                    pickTime(qe) { qe = it; Prefs.setQuietEnd(ctx, it) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Hint("Unlocks inside this window are flagged red and shown in the \"Last night\" summary.")

        SectionTitle("Selfie proof")
        Panel {
            Column {
                SwitchRow("Take a selfie on unlock", "Front camera, 2 photos: about 1s and 4s after unlocking", selfie) {
                    selfie = it; Prefs.setSelfie(ctx, it)
                }
                HorizontalDivider(color = C.line)
                SwitchRow(
                    "Only during quiet hours",
                    if (selfieQuiet) "No photos during the day" else "Photo on every unlock, day and night",
                    selfieQuiet, enabled = selfie
                ) {
                    selfieQuiet = it; Prefs.setSelfieQuietOnly(ctx, it)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        WatchService.start(ctx, test = true)
                        refresh++
                    },
                    enabled = Setup.camera(ctx),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Test selfie now") }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (Setup.camera(ctx)) "Takes 2 photos in ~5s. Go back to Home and refresh to see them under \"Today\" → \"All unlocks\"."
                    else "Allow camera in setup first.",
                    color = C.dim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Hint("Photos stay private inside the app, not in your gallery. Android briefly shows a green camera dot while a photo is taken.")

        SectionTitle("Setup")
        Panel {
            SettingRow("Permissions & battery", "$setupDone of 4", "Check what's missing", onClick = onSetup)
        }

        SectionTitle("Data")
        Panel {
            Column {
                Text("$photoCount photos saved · history kept 30 days", color = C.text, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = C.bad.copy(alpha = 0.18f), contentColor = C.bad),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete all history & photos") }
            }
        }

        SectionTitle("Activity log")
        Panel {
            Column {
                if (log.isEmpty()) {
                    Text("Nothing yet.", color = C.dim, fontSize = 13.sp)
                } else {
                    log.forEach { Text(it, color = C.dim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                }
                TextButton(onClick = { refresh++ }) { Text("Refresh log") }
            }
        }
        Spacer(Modifier.height(6.dp))
        Hint("If selfies don't appear, this log shows why (e.g. camera blocked, watcher stopped).")

        SectionTitle("Good to know")
        Panel {
            Column {
                listOf(
                    "Shows that the phone was unlocked and what was opened. The photo shows who.",
                    "After a phone restart, open Night Watch once to turn the camera watcher back on. You'll get a reminder notification.",
                    "Open the app at least every few days. Android only keeps raw unlock data for a short time.",
                    "Smart Lock (trusted places/devices) unlocks count as unlocks too.",
                ).forEach {
                    Text("• $it", color = C.dim, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete everything?") },
            text = { Text("All saved unlocks and photos will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    Store.clear(ctx)
                    Photos.deleteAll(ctx)
                    confirmDelete = false
                    refresh++
                }) { Text("Delete", color = C.bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
