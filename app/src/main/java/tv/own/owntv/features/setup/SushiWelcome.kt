package tv.own.owntv.features.setup

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tv.own.owntv.R

/**
 * SushiTV: the pixel-art welcome shown over the app on a cold start. Sushi's face
 * (res/drawable-nodpi/sushitv_pixel.png, 48×48, from sushitv/tools/make-brand.ps1) assembles pixel by
 * pixel in a diagonal wave, then "SushiTV" and the greeting appear, and it fades into the app.
 *
 * About four seconds, drawn on one Canvas (no assets beyond a 48 px PNG, no WebView). It never takes
 * focus, so the screen underneath keeps the remote exactly as if the welcome were not there.
 */
@Composable
fun SushiWelcome() {
    var visible by remember { mutableStateOf(!WelcomeOnce.shown) }
    if (!visible) return
    val context = LocalContext.current
    val pixels = remember { loadPixels(context) }
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        WelcomeOnce.shown = true
        progress.animateTo(1f, tween(durationMillis = 2600, easing = LinearEasing))
        delay(1000)
        fade.animateTo(0f, tween(durationMillis = 500))
        visible = false
    }
    val p = progress.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Canvas(Modifier.size(240.dp)) {
                val cell = size.minDimension / GRID
                for (px in pixels) {
                    // Each pixel pops in on a diagonal wave: its start follows x + y.
                    val start = (px.x + px.y) / (2f * GRID) * WAVE
                    val t = ((p - start) / POP).coerceIn(0f, 1f)
                    if (t <= 0f) continue
                    val s = cell * easeOutBack(t)
                    val cx = (px.x + 0.5f) * cell
                    val cy = (px.y + 0.5f) * cell
                    drawRect(px.color.copy(alpha = t), topLeft = Offset(cx - s / 2f, cy - s / 2f), size = Size(s, s))
                }
            }
            val title = ((p - 0.62f) / 0.25f).coerceIn(0f, 1f)
            Row(Modifier.graphicsLayer { alpha = title; translationY = (1f - title) * 24f }) {
                BasicText(stringResource(R.string.sushitv_word_sushi), style = Retro.copy(color = Cream))
                BasicText(stringResource(R.string.sushitv_word_tv), style = Retro.copy(color = Salmon))
            }
            val hello = ((p - 0.8f) / 0.2f).coerceIn(0f, 1f)
            BasicText(
                stringResource(R.string.sushitv_welcome),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, color = Cream.copy(alpha = 0.75f)),
                modifier = Modifier.graphicsLayer { alpha = hello },
            )
        }
    }
}

/** Once per app process: a cold start shows it, a configuration change or returning to the app does not. */
private object WelcomeOnce { var shown = false }

private class Pixel(val x: Int, val y: Int, val color: Color)

private fun loadPixels(context: android.content.Context): List<Pixel> {
    val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.sushitv_pixel, BitmapFactory.Options().apply { inScaled = false })
        ?: return emptyList()
    val out = ArrayList<Pixel>(bmp.width * bmp.height)
    for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
        val argb = bmp.getPixel(x, y)
        if ((argb ushr 24) > 128) out += Pixel(x, y, Color(argb))
    }
    bmp.recycle()
    return out
}

/** A little overshoot, so each block "lands". */
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

private const val GRID = 48f
private const val WAVE = 0.55f // share of the animation over which the wave crosses the picture
private const val POP = 0.18f  // share each pixel takes to land
private val Ink = Color(0xFF0E1113)
private val Cream = Color(0xFFF4EFE6)
private val Salmon = Color(0xFFFF7A59)
private val Retro = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 64.sp)
