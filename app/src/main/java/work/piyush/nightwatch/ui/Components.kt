package work.piyush.nightwatch.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import work.piyush.nightwatch.Apps
import work.piyush.nightwatch.Photos
import java.io.File

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = C.card, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = C.dim,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, color: Color = C.text) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(C.cardHi)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = C.dim, fontSize = 12.sp)
    }
}

@Composable
fun SettingRow(title: String, value: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = C.text)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.dim)
        }
        Text(value, color = C.accent, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = if (enabled) C.text else C.dim)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.dim)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun AppIcon(apps: Apps, pkg: String, size: Int = 22) {
    val icon: ImageBitmap? = remember(pkg) { apps.icon(pkg) }
    if (icon != null) {
        Image(icon, null, Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)))
    } else {
        Box(Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)).background(C.line))
    }
}

@Composable
fun PhotoThumb(file: File, modifier: Modifier, maxPx: Int = 300) {
    val bmp by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { Photos.load(file, maxPx)?.asImageBitmap() }
    }
    val b = bmp
    if (b != null) {
        Image(b, null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(C.line))
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
fun Hint(text: String) {
    Text(text, color = C.dim, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

val Gap = Arrangement.spacedBy(8.dp)

@Composable
fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = C.text)
}
