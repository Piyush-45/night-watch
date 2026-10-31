package work.piyush.nightwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import work.piyush.nightwatch.Apps
import work.piyush.nightwatch.Cat
import work.piyush.nightwatch.Data
import work.piyush.nightwatch.Photos
import work.piyush.nightwatch.Prefs
import work.piyush.nightwatch.Range
import work.piyush.nightwatch.Unlock
import work.piyush.nightwatch.Setup
import work.piyush.nightwatch.WatchService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dayFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
private val photoFmt = SimpleDateFormat("EEE d MMM, h:mm:ss a", Locale.getDefault())

@Composable
fun HomeScreen(tick: Int, onSettings: () -> Unit, onSetup: () -> Unit) {
    val ctx = LocalContext.current
    val apps = remember { Apps(ctx) }
    var reload by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<Unlock>?>(null) }
    var range by remember { mutableStateOf(Range.NIGHT) }
    var quietOnly by remember { mutableStateOf(true) }
    var viewer by remember { mutableStateOf<Unlock?>(null) }
    var armed by remember(tick) { mutableStateOf(Prefs.armed(ctx)) }
    val ready = remember(tick) { Setup.ready(ctx) }
    val setupDone = remember(tick) { Setup.doneCount(ctx) }

    LaunchedEffect(tick, reload) {
        rows = withContext(Dispatchers.IO) { Data.load(ctx, apps) }
    }

    val now = System.currentTimeMillis()
    val all = rows ?: emptyList()
    val (nightA, nightB) = Data.nightWindow(ctx, now)
    val night = all.filter { it.s.start in nightA..nightB }
    val shown = Data.filter(ctx, all, range, quietOnly, now)

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Title("Night Watch")
                    Text("Who used this phone, and when", color = C.dim, fontSize = 14.sp)
                }
                IconButton(onClick = { reload++ }) { Icon(Icons.Filled.Refresh, "Refresh", tint = C.dim) }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = C.dim) }
            }
        }

        // Setup banner
        if (setupDone < 5) {
            item {
                Panel(Modifier.clickable { onSetup() }, color = C.warn.copy(alpha = 0.12f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = C.warn)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Finish setup ($setupDone of 5 done)", color = C.text, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (ready) "Optional steps left. They make it more reliable." else "Some permissions are missing, so tracking can't run yet.",
                                color = C.dim, fontSize = 13.sp
                            )
                        }
                        Text("Open", color = C.warn, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Status
        item {
            val qs = Prefs.fmtMin(Prefs.quietStart(ctx))
            val qe = Prefs.fmtMin(Prefs.quietEnd(ctx))
            val selfieOn = Prefs.selfie(ctx)
            Panel(color = if (armed) C.good.copy(alpha = 0.10f) else C.card) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (armed) C.good.copy(alpha = 0.2f) else C.line),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = if (armed) C.good else C.dim)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (armed) "Watching" else "Paused",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = if (armed) C.good else C.text
                        )
                        Text("Quiet hours $qs – $qe", color = C.dim, fontSize = 13.sp)
                        Text(
                            if (selfieOn) "Selfie on unlock: on" else "Selfie on unlock: off",
                            color = C.dim, fontSize = 13.sp
                        )
                    }
                    Switch(checked = armed, onCheckedChange = { on ->
                        if (on && !Setup.ready(ctx)) {
                            onSetup()
                        } else {
                            Prefs.setArmed(ctx, on)
                            armed = on
                            if (on) WatchService.start(ctx) else WatchService.stop(ctx)
                        }
                    })
                }
            }
        }

        // Last night summary
        item {
            val title = if (now < nightB) "Tonight so far" else "Last night"
            val totalMs = night.sumOf { it.s.end - it.s.start }
            val photos = night.sumOf { it.photos.size }
            val top = night.flatMap { it.apps }
                .filter { it.cat != Cat.HOME }
                .groupBy { it.pkg }
                .map { (pkg, list) -> Triple(pkg, list.first().label, list.sumOf { it.ms }) }
                .sortedByDescending { it.third }
                .take(3)

            Panel {
                Column {
                    Text(title, color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "${timeFmt.format(Date(nightA))} – ${timeFmt.format(Date(nightB))}",
                        color = C.dim, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Gap) {
                        StatTile(
                            "${night.size}", if (night.size == 1) "unlock" else "unlocks",
                            Modifier.weight(1f), color = if (night.isEmpty()) C.good else C.bad
                        )
                        StatTile(Data.dur(totalMs), "screen time", Modifier.weight(1f))
                        StatTile("$photos", if (photos == 1) "photo" else "photos", Modifier.weight(1f))
                    }
                    val locked = night.filter { it.autoLocked }
                    if (locked.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Fell asleep on the phone? Auto-locked ${locked.size}× (last at ${timeFmt.format(Date(locked.first().s.end))})",
                            color = C.accent, fontSize = 13.sp
                        )
                    }
                    if (top.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Most used", color = C.dim, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Gap, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            top.forEach { (pkg, label, ms) ->
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(C.cardHi)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(apps, pkg, 18)
                                    Spacer(Modifier.width(6.dp))
                                    Text("$label · ${Data.dur(ms)}", fontSize = 13.sp, color = C.text)
                                }
                            }
                        }
                    } else if (night.isEmpty() && rows != null) {
                        Spacer(Modifier.height(10.dp))
                        Text("No unlocks. All quiet. ✓", color = C.good, fontSize = 14.sp)
                    }
                }
            }
        }

        // Filters
        item {
            Column {
                SectionTitle("Timeline")
                Row(horizontalArrangement = Gap, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Range.values().forEach { r ->
                        FilterChip(
                            selected = range == r,
                            onClick = { range = r },
                            label = { Text(r.label) },
                            leadingIcon = if (range == r) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                if (range != Range.NIGHT) {
                    Row(horizontalArrangement = Gap) {
                        FilterChip(
                            selected = quietOnly,
                            onClick = { quietOnly = true },
                            label = { Text("Quiet hours only") }
                        )
                        FilterChip(
                            selected = !quietOnly,
                            onClick = { quietOnly = false },
                            label = { Text("All unlocks") }
                        )
                    }
                }
            }
        }

        // Timeline
        when {
            rows == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !Setup.usage(ctx) -> item {
                Hint("Grant usage access in setup to see unlocks.")
            }

            shown.isEmpty() -> item {
                Panel {
                    Column {
                        Text("Nothing here", color = C.text, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No unlocks in this view. Quick test: lock the phone, wait 10 seconds, unlock, open a couple of apps, then come back and tap refresh.",
                            color = C.dim, fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            else -> {
                val groups = shown.groupBy { dayLabel(it.s.start, now) }
                groups.forEach { (day, list) ->
                    item(key = "h$day") {
                        Text(
                            "$day · ${list.size} unlock${if (list.size == 1) "" else "s"}",
                            color = C.dim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }
                    items(list, key = { it.s.start }) { row ->
                        SessionCard(row, apps) { viewer = row }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    viewer?.let { PhotoViewer(it) { viewer = null } }
}

private fun dayLabel(ts: Long, now: Long): String {
    val a = Calendar.getInstance().apply { timeInMillis = ts }
    val b = Calendar.getInstance().apply { timeInMillis = now }
    val sameYear = a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
    val diff = b.get(Calendar.DAY_OF_YEAR) - a.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && diff == 0 -> "Today"
        sameYear && diff == 1 -> "Yesterday"
        else -> dayFmt.format(Date(ts))
    }
}

@Composable
private fun SessionCard(row: Unlock, apps: Apps, onPhotos: () -> Unit) {
    val s = row.s
    val accent = if (row.quiet) C.bad else C.dim
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.card)
            .clickable(enabled = row.photos.isNotEmpty(), onClick = onPhotos)
    ) {
        Row(Modifier.padding(14.dp)) {
            // Accent bar
            Box(
                Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(timeFmt.format(Date(s.start)), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.text)
                    Text(
                        if (s.ongoing) "  → now" else "  → ${timeFmt.format(Date(s.end))}",
                        fontSize = 13.sp, color = C.dim
                    )
                    Spacer(Modifier.weight(1f))
                    Text(Data.dur(s.end - s.start), fontSize = 13.sp, color = C.dim)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.ongoing) Tag("This is you, now", C.accent)
                    if (row.quiet) Tag("Quiet hours", C.bad)
                    if (row.sensitive) Tag("Private apps", C.warn)
                    if (row.peek) Tag("Quick peek", C.dim)
                    if (row.autoLocked) Tag("Auto-locked", C.accent)
                }

                val list = row.apps
                if (list.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Unlocked, no apps opened", color = C.dim, fontSize = 13.sp)
                } else {
                    Spacer(Modifier.height(8.dp))
                    list.take(5).forEach { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            AppIcon(apps, a.pkg)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                a.label, color = C.text, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(6.dp))
                            if (a.cat != Cat.OTHER && a.cat != Cat.HOME) {
                                Tag(a.cat.label, if (a.cat.sensitive) C.warn else C.dim)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(Data.dur(a.ms), color = C.dim, fontSize = 13.sp)
                        }
                    }
                    if (list.size > 5) {
                        Text("+${list.size - 5} more", color = C.dim, fontSize = 12.sp)
                    }
                }
            }

            if (row.photos.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Box {
                    PhotoThumb(
                        row.photos.first(),
                        Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
                        maxPx = 200
                    )
                    if (row.photos.size > 1) {
                        Text(
                            "${row.photos.size}",
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoViewer(row: Unlock, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Unlock at ${timeFmt.format(Date(row.s.start))}",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                row.photos.forEach { f: File ->
                    Column {
                        PhotoThumb(
                            f,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(16.dp)),
                            maxPx = 1200
                        )
                        Text(
                            photoFmt.format(Date(Photos.ts(f))),
                            color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
        }
    }
}
