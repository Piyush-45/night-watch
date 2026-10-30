package work.piyush.nightwatch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val bg = Color(0xFF0D0E12)
    val card = Color(0xFF17181F)
    val cardHi = Color(0xFF1F2029)
    val line = Color(0xFF2A2B36)
    val text = Color(0xFFEDEDF2)
    val dim = Color(0xFF9597A6)
    val accent = Color(0xFF8B7CFF)
    val good = Color(0xFF3DDC97)
    val warn = Color(0xFFFFB547)
    val bad = Color(0xFFFF5C6C)
}

private val scheme = darkColorScheme(
    primary = C.accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2557),
    onPrimaryContainer = Color(0xFFDCD6FF),
    secondaryContainer = Color(0xFF2B2557),
    onSecondaryContainer = Color(0xFFDCD6FF),
    background = C.bg,
    onBackground = C.text,
    surface = C.bg,
    onSurface = C.text,
    surfaceVariant = C.card,
    onSurfaceVariant = C.dim,
    surfaceContainerLowest = C.bg,
    surfaceContainerLow = C.card,
    surfaceContainer = C.card,
    surfaceContainerHigh = C.cardHi,
    surfaceContainerHighest = C.card,
    outline = C.line,
    outlineVariant = C.line,
    error = C.bad,
)

@Composable
fun NightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
