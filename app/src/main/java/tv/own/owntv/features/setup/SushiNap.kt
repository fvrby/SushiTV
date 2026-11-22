package tv.own.owntv.features.setup

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import tv.own.owntv.features.setup.SushiCatArt.Cat
import tv.own.owntv.features.setup.SushiCatArt.Face
import tv.own.owntv.features.setup.SushiCatArt.Pose
import tv.own.owntv.features.setup.SushiCatArt.bump
import tv.own.owntv.features.setup.SushiCatArt.cat
import tv.own.owntv.features.setup.SushiCatArt.seg
import tv.own.owntv.features.setup.SushiCatArt.zz

/**
 * SushiTV: Sushi asleep on the rail logo, the app's little seal. She breathes, her tail sways, now and
 * then an ear flicks and a "z" floats up. When the logo is focused ([awake]) she opens her eyes and looks
 * at you; while a secret animation plays (where she is the star) she is away from the logo.
 *
 * Drawn in code (SushiCatArt), 30 frames a second at most and only while the app is on screen (frames
 * stop with the app), on a 400×232 box with her ground at (210, 222). The lab page
 * sushitv/tools/bienvenida/siesta.html shows the same.
 */
@Composable
fun SushiNap(awake: Boolean, modifier: Modifier = Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    var eyes by remember { mutableFloatStateOf(0f) }
    var away by remember { mutableFloatStateOf(0f) }
    val wantAwake by rememberUpdatedState(awake)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            if (now - last >= FRAME_NS) {
                val dt = if (last == 0L) 0f else min(0.2f, (now - last) / 1e9f)
                last = now
                time = ((now % HOUR_NS) / 1_000_000L).toFloat() / 1000f
                eyes += ((if (wantAwake) 1f else 0f) - eyes) * min(1f, dt * 5f)
                away += ((if (SushiSecretPlayer.running) 1f else 0f) - away) * min(1f, dt * 6f)
            }
        }
    }
    Canvas(modifier.graphicsLayer { alpha = 1f - away }) {
        runCatching {
            val k = size.width / 400f
            withTransform({ scale(k, k, Offset.Zero) }) {
                val t = time
                val sw = sin(t * 1.1f) * (0.5f + 0.5f * sin(t * 0.37f))
                val ph = t % 7.3f
                val flick = -0.35f * bump(ph, 0f, 0.22f) + 0.12f * bump(ph, 0.22f, 0.4f)
                cat(210f, 222f, 1f, Cat(
                    Pose.LOAF,
                    Face(open = eyes, pd = 1.1f, lx = 10f * eyes, ly = -2f * eyes, smile = eyes < 0.5f, flick = flick * (1f - eyes)),
                    tilt = -0.12f, tailSw = sw, t = t,
                ))
                val p = seg(t % 5.5f, 0f, 2.2f)
                if (p > 0f && p < 1f && eyes < 0.5f) zz(95f + 30f * p, 75f - 60f * p, 0.5f + 0.4f * p, sin(p * PI.toFloat()) * (1f - eyes * 2f))
            }
        }
    }
}

private const val FRAME_NS = 33_000_000L // ~30 frames a second is plenty for a sleeping cat
private const val HOUR_NS = 3_600_000_000_000L // keeps the float clock precise
