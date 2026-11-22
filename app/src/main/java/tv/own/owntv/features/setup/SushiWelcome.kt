package tv.own.owntv.features.setup

import android.content.Context
import android.media.MediaPlayer
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import tv.own.owntv.R

/**
 * SushiTV: the welcome shown over the app on a cold start — ten seconds, all drawn in code (no images):
 * an old TV switches on, Sushi (white, black and caramel, green eyes) climbs into it and holds the edge
 * with her paws, opens her eyes, flicks an ear, tilts her head, a heart floats up, her tail peeks out,
 * "SushiTV" drops in letter by letter, and she gives the slow blink cats use to say "I love you".
 *
 * The sound is one 10 s track (res/raw/sushitv_bienvenida.wav, made in sushitv/tools/bienvenida/): music
 * generated in code plus Sushi's own little sounds. The picture follows the sound's clock.
 *
 * It never takes focus: the first key press (other than volume) only skips it, see [SushiWelcomeSkip].
 * Scene coordinates are a 1920×1080 stage, scaled to the screen.
 */
@Composable
fun SushiWelcome() {
    var visible by remember { mutableStateOf(!WelcomeOnce.shown) }
    if (!visible) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val sushi = stringResource(R.string.sushitv_word_sushi)
    val tv = stringResource(R.string.sushitv_word_tv)
    val hello = stringResource(R.string.sushitv_welcome)
    val skipHint = stringResource(R.string.sushitv_welcome_skip)
    val texts = remember(density, sushi, tv, hello, skipHint) {
        fun style(px: Float, weight: FontWeight) = TextStyle(fontSize = with(density) { px.toSp() }, fontWeight = weight)
        val big = style(128f, FontWeight.Black)
        Texts(
            letters = (sushi + tv).map { measurer.measure(it.toString(), big) },
            tvFrom = sushi.length,
            hello = measurer.measure(hello, style(44f, FontWeight.SemiBold)),
            hint = measurer.measure(skipHint, style(26f, FontWeight.Medium)),
        )
    }
    val sound = remember { WelcomeSound(context) }
    var time by remember { mutableFloatStateOf(0f) }
    var skipFade by remember { mutableFloatStateOf(1f) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        // Leaving the app mid-welcome (Home): stop the sound now; the picture ends when the app is back.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { sound.release(); SushiWelcomeSkip.skip = true }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); sound.release() }
    }
    LaunchedEffect(Unit) {
        WelcomeOnce.shown = true
        SushiWelcomeSkip.skip = false
        SushiWelcomeSkip.active = true
        try {
            sound.start()
            var start = withFrameNanos { it }
            var synced = false
            while (time < DURATION && !SushiWelcomeSkip.skip) {
                val now = withFrameNanos { it }
                // Follow the sound's own clock as soon as it is really playing.
                if (!synced) sound.positionMs()?.let { start = now - it * 1_000_000L; synced = true }
                time = (now - start) / 1e9f
            }
            if (time < DURATION) for (i in 1..SKIP_STEPS) { // skipped: a quick fade of picture and sound
                skipFade = 1f - i / SKIP_STEPS.toFloat()
                sound.volume(skipFade)
                delay(SKIP_MS / SKIP_STEPS)
            }
        } finally {
            sound.release()
            SushiWelcomeSkip.active = false
            visible = false
        }
    }
    val scene = remember { Scene() }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (1f - eIO(seg(time, 9.3f, 10f))) * skipFade },
    ) {
        // The welcome must never take the app down: if drawing fails, it simply ends.
        runCatching { scene.draw(this, time, texts) }.onFailure {
            android.util.Log.w("SushiWelcome", "welcome skipped", it)
            SushiWelcomeSkip.skip = true
        }
    }
}

/** The welcome's key handling, called first from MainActivity.dispatchKeyEvent. */
object SushiWelcomeSkip {
    @Volatile internal var skip = false
    @Volatile private var since = 0L
    @Volatile internal var active = false
        set(value) { field = value; since = android.os.SystemClock.elapsedRealtime() }

    /**
     * While the welcome plays, keys only skip it (they never reach the app); volume keys still work.
     * Never for more than [MAX_HOLD_MS], whatever happens to the animation.
     */
    fun consume(event: KeyEvent): Boolean {
        if (!active || android.os.SystemClock.elapsedRealtime() - since > MAX_HOLD_MS) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> return false
        }
        if (event.action == KeyEvent.ACTION_DOWN) skip = true
        return true
    }
}

/** Once per app process: a cold start shows it, a configuration change or returning to the app does not. */
private object WelcomeOnce { var shown = false }

private const val DURATION = 10f
private const val MAX_HOLD_MS = 12_000L
private const val SKIP_STEPS = 12
private const val SKIP_MS = 300L

/** The welcome's sound. Without it (any error) the picture simply plays on its own. */
private class WelcomeSound(context: Context) {
    private var player: MediaPlayer? = runCatching { MediaPlayer.create(context, R.raw.sushitv_bienvenida) }.getOrNull()

    fun start() { runCatching { player?.start() } }

    /** The playback position once the sound is really playing, else null. */
    fun positionMs(): Int? = runCatching { player?.takeIf { it.isPlaying }?.currentPosition }.getOrNull()?.takeIf { it > 0 }

    fun volume(v: Float) { runCatching { player?.setVolume(v, v) } }

    fun release() {
        val p = player ?: return
        player = null
        runCatching { p.release() }
    }
}

private class Texts(val letters: List<TextLayoutResult>, val tvFrom: Int, val hello: TextLayoutResult, val hint: TextLayoutResult)

// ---- timing helpers ------------------------------------------------------------------------------------

private fun cl(x: Float) = x.coerceIn(0f, 1f)
private fun seg(t: Float, a: Float, b: Float) = cl((t - a) / (b - a))
private fun eOut(x: Float) = 1f - (1f - x).pow(3)
private fun eIO(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
private fun eBack(x: Float): Float { val c1 = 1.70158f; val c3 = c1 + 1f; val u = x - 1f; return 1f + c3 * u * u * u + c1 * u * u }
private fun bump(t: Float, a: Float, b: Float) = sin(seg(t, a, b) * PI.toFloat())
private fun deg(rad: Float) = rad * 180f / PI.toFloat()
private fun blinkAt(t: Float, a: Float, c: Float, h: Float, o: Float): Float = when {
    t < a || t > a + c + h + o -> 1f
    t < a + c -> 1f - eIO(seg(t, a, a + c))
    t < a + c + h -> 0f
    else -> eIO(seg(t, a + c + h, a + c + h + o))
}

// ---- colours -------------------------------------------------------------------------------------------

private val Ink = Color(0xFF0E0B09)
private val TvBody = Color(0xFF1B1512)
private val Cafe = Color(0xFF8B5A3C)
private val Caramel = Color(0xFFC98B5B)
private val Orange = Color(0xFFD9955E)
private val White = Color(0xFFFFFDF8)
private val Fur = Color(0xFFE6D8C8)
private val Black = Color(0xFF1A1614)
private val Pink = Color(0xFFF2B3AE)
private val Nose = Color(0xFFEBA9A6)
private val Eye1 = Color(0xFFDDF3C4)
private val Eye2 = Color(0xFF93C47A)
private val EyeLine = Color(0xFF3F5C34)
private val Green = Color(0xFFA8DB8E)
private val Salmon = Color(0xFFFF7A59)
private val Cream = Color(0xFFF3E6D6)
private val EarLight = Color(0xFFEEE6DD)
private val Screen = Color(0xFFFFF6EA)

// ---- the scene -----------------------------------------------------------------------------------------

/** One floating nigiri / maki / paw / fish / sparkle in the background. */
private class Floater(val x: Float, val y: Float, val v: Float, val sz: Float, val kind: Int, val ph: Float, val rot: Float)

private class Scene {
    private fun hash(i: Int, n: Int): Float { val x = sin(i * 127.1 + n * 311.7) * 43758.5453; return (x - floor(x)).toFloat() }
    private val floaters = List(16) { i ->
        Floater(hash(i, 1) * 1920f, hash(i, 2) * 1280f, 22f + hash(i, 3) * 34f, 20f + hash(i, 4) * 24f, i % 5, hash(i, 5) * 6.28f, hash(i, 6) * 6.28f)
    }

    // Sushi's face, in head coordinates (head centre at 0,0; about 300 wide).
    private val head = Path().apply {
        moveTo(0f, -110f)
        cubicTo(95f, -110f, 150f, -60f, 150f, 15f); cubicTo(150f, 85f, 85f, 122f, 0f, 122f)
        cubicTo(-85f, 122f, -150f, 85f, -150f, 15f); cubicTo(-150f, -60f, -95f, -110f, 0f, -110f); close()
    }
    private fun earOuter(s: Float) = Path().apply { moveTo(s * 150, -20f); quadraticTo(s * 156, -150f, s * 120, -182f); quadraticTo(s * 84, -150f, s * 36, -104f); close() }
    private fun earInner(s: Float) = Path().apply { moveTo(s * 128, -45f); quadraticTo(s * 132, -130f, s * 116, -155f); quadraticTo(s * 92, -128f, s * 62, -98f); close() }
    private val ears = listOf(-1f, 1f).associateWith { earOuter(it) to earInner(it) }
    // Her real markings: a black band over the head running down her left side, caramel round her
    // right eye, a white stripe from a little white spot on top down to the nose, a caramel dot on the chin.
    private val blackPatch = Path().apply {
        moveTo(-175f, -135f); lineTo(175f, -135f); lineTo(175f, -66f)
        quadraticTo(120f, -64f, 80f, -50f); quadraticTo(40f, -40f, 16f, -58f); lineTo(-16f, -58f)
        quadraticTo(-46f, -46f, -74f, -44f); quadraticTo(-100f, -38f, -104f, -8f); quadraticTo(-108f, 30f, -98f, 62f)
        quadraticTo(-135f, 74f, -175f, 66f); close()
    }
    private val orangePatch = Path().apply {
        moveTo(16f, -58f); quadraticTo(40f, -40f, 80f, -50f); quadraticTo(120f, -64f, 175f, -66f); lineTo(175f, 44f)
        quadraticTo(122f, 44f, 104f, 18f); quadraticTo(84f, -14f, 52f, -30f); quadraticTo(28f, -38f, 16f, -30f); close()
    }
    private val stripe = Path().apply {
        moveTo(-15f, -96f); quadraticTo(0f, -104f, 15f, -96f); lineTo(18f, -30f); quadraticTo(0f, -20f, -18f, -30f); close()
    }
    private val nose = Path().apply { moveTo(-15f, 30f); quadraticTo(0f, 25f, 15f, 30f); quadraticTo(10f, 44f, 0f, 52f); quadraticTo(-10f, 44f, -15f, 30f); close() }
    private val noseTip = Path().apply { moveTo(-7f, 41f); quadraticTo(0f, 39f, 7f, 41f); lineTo(0f, 52f); close() }
    private val mouth = Path().apply { moveTo(0f, 52f); lineTo(0f, 58f); moveTo(-19f, 61f); quadraticTo(-9f, 68f, 0f, 58f); quadraticTo(9f, 68f, 19f, 61f) }
    private val whiskers = Path().apply {
        for (s in listOf(-1f, 1f)) for (d in listOf(-14f, 0f, 14f)) { moveTo(s * 42, 54 + d * 0.4f); quadraticTo(s * 100, 48 + d, s * 168, 42 + d * 1.6f) }
    }
    private val closedEye = Path().apply { moveTo(-26f, -2f); quadraticTo(0f, 12f, 26f, -2f) }
    private val tufts = Path().apply {
        for ((x, y) in listOf(-40f to 140f, 0f to 150f, 40f to 140f, -75f to 128f, 75f to 128f)) { moveTo(x - 14, y); lineTo(x, y + 18); lineTo(x + 14, y); close() }
    }
    private val heart = Path().apply { moveTo(0f, 14f); cubicTo(-30f, -8f, -18f, -30f, 0f, -16f); cubicTo(18f, -30f, 30f, -8f, 0f, 14f); close() }
    /** A four-point sparkle of radius 1. */
    private val star = Path().apply {
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f; val q = if (i % 2 == 1) 0.28f else 1f
            if (i == 0) moveTo(sin(a) * q, -kotlin.math.cos(a) * q) else lineTo(sin(a) * q, -kotlin.math.cos(a) * q)
        }
        close()
    }
    private val fishTail = Path().apply { moveTo(0.45f, 0f); lineTo(0.85f, -0.3f); lineTo(0.85f, 0.3f); close() }
    private val eyeBrush = Brush.radialGradient(listOf(Eye1, Eye2), center = Offset(-3f, -4f), radius = 34f)
    private val screenBrush = Brush.radialGradient(0f to Color(0xFFE9CBA7), 0.1f to Color(0xFFE9CBA7), 1f to Color(0xFFB98558), center = Offset(960f, 415f), radius = 420f)
    private val backBrush = Brush.radialGradient(listOf(Color(0xFF3A271B), Ink), center = Offset(960f, 480f), radius = 1150f)

    fun draw(d: DrawScope, t: Float, texts: Texts): Unit = with(d) {
        val k = min(size.width / 1920f, size.height / 1080f)
        drawRect(Ink)
        withTransform({ translate((size.width - 1920f * k) / 2f, (size.height - 1080f * k) / 2f); scale(k, k, Offset.Zero) }) {
            drawRect(backBrush, Offset.Zero, Size(1920f, 1080f))
            val z = 1f + 0.04f * eIO(seg(t, 9.3f, 10f))
            withTransform({ scale(z, z, Offset(960f, 540f)) }) {
                val fa = seg(t, 1.3f, 2.4f)
                if (fa > 0f) floaters.forEach { floater(it, t, 0.3f * fa) }
                tv(t)
                title(t, texts)
            }
            val h = seg(t, 1.5f, 2.2f)
            if (h > 0f) drawText(texts.hint, Cream, Offset(1880f - texts.hint.size.width, 1040f - texts.hint.firstBaseline), alpha = 0.4f * h)
        }
        Unit
    }

    private fun DrawScope.rrect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, a: Float = 1f) =
        drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r), alpha = a)

    private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, c: Color, a: Float = 1f) =
        drawOval(c, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = a)

    private fun DrawScope.sparkle(x: Float, y: Float, r: Float, c: Color, a: Float = 1f) =
        withTransform({ translate(x, y); scale(r, r, Offset.Zero) }) { drawPath(star, c, alpha = a) }

    private fun DrawScope.floater(f: Floater, t: Float, a: Float) {
        val y = ((f.y - t * f.v) % 1280f + 1280f) % 1280f - 100f
        val x = f.x + 26f * sin(t * 0.8f + f.ph)
        val s = f.sz
        withTransform({ translate(x, y); rotate(deg(f.rot + 0.25f * sin(t * 0.6f + f.ph)), Offset.Zero) }) {
            when (f.kind) {
                0 -> { // nigiri
                    rrect(-s * .8f, -s * .25f, s * 1.6f, s * .75f, s * .35f, White, a)
                    rrect(-s * .9f, -s * .55f, s * 1.8f, s * .5f, s * .25f, Color(0xFFD9824B), a)
                    drawRect(Color(0xFFF2B48A), Offset(-s * .4f, -s * .5f), Size(s * .12f, s * .4f), alpha = a)
                    drawRect(Color(0xFFF2B48A), Offset(s * .15f, -s * .5f), Size(s * .12f, s * .4f), alpha = a)
                }
                1 -> { // maki
                    drawCircle(Color(0xFF151515), s * .6f, Offset.Zero, alpha = a)
                    drawCircle(White, s * .45f, Offset.Zero, alpha = a)
                    drawCircle(Color(0xFF9BD27A), s * .2f, Offset.Zero, alpha = a)
                }
                2 -> { // paw print
                    oval(0f, s * .15f, s * .38f, s * .3f, Caramel, a)
                    for (ang in listOf(-.9f, -.3f, .3f, .9f)) drawCircle(Caramel, s * .14f, Offset(sin(ang) * s * .5f, -kotlin.math.cos(ang) * s * .42f), alpha = a)
                }
                3 -> { // fish
                    oval(0f, 0f, s * .55f, s * .3f, Color(0xFF9BD27A), a)
                    withTransform({ scale(s, s, Offset.Zero) }) { drawPath(fishTail, Color(0xFF9BD27A), alpha = a) }
                    drawCircle(Black, s * .06f, Offset(-s * .28f, -s * .05f), alpha = a)
                }
                else -> sparkle(0f, 0f, s * .45f, Color(0xFFFFE7C7), a)
            }
        }
    }

    private fun DrawScope.ear(sign: Float, ang: Float, outer: Color) {
        val (o, i) = ears.getValue(sign)
        withTransform({ rotate(deg(sign * ang), Offset(sign * 92f, -72f)) }) {
            drawPath(o, outer)
            if (outer != Black) drawPath(o, Color(0xFFCFC4B8), style = Stroke(3f))
            drawPath(i, Pink)
        }
    }

    private fun DrawScope.eye(x: Float, open: Float, pd: Float, lx: Float, ly: Float) {
        withTransform({ translate(x, -5f) }) {
            if (open < 0.12f) {
                drawPath(closedEye, Color(0xFF2B2320), style = Stroke(5f, cap = StrokeCap.Round))
            } else withTransform({ scale(1f, open, Offset.Zero) }) {
                drawOval(eyeBrush, Offset(-30f, -32f), Size(60f, 64f))
                drawOval(EyeLine, Offset(-30f, -32f), Size(60f, 64f), style = Stroke(3.5f))
                oval(lx, ly, 15f * pd, 18f * pd, Color(0xFF15110F))
                drawCircle(Color.White, 7f, Offset(lx - 9f, ly - 11f))
                drawCircle(Color.White, 3.5f, Offset(lx + 8f, ly + 9f))
            }
        }
    }

    private fun DrawScope.sushi(t: Float) {
        val rise = eBack(seg(t, 1.6f, 2.6f))
        val hy = 455f + (1f - rise) * 380f
        val br = 1f + 0.012f * sin(t * 2.4f)
        val tilt = 0.12f * bump(t, 4.4f, 5.6f)
        withTransform({ translate(960f, hy); rotate(deg(tilt), Offset.Zero); scale(br, br, Offset.Zero) }) {
            oval(0f, 195f, 178f, 118f, White)
            drawPath(tufts, Fur)
            ear(-1f, 0f, EarLight)
            ear(1f, -0.28f * bump(t, 3.9f, 4.12f) + 0.1f * bump(t, 4.12f, 4.32f), Black)
            drawPath(head, White)
            clipPath(head) {
                drawPath(blackPatch, Black)
                drawPath(orangePatch, Orange)
                drawPath(stripe, White)
                oval(0f, -104f, 24f, 11f, White)
                oval(6f, 101f, 10f, 7f, Orange)
            }
            drawPath(head, Fur, style = Stroke(3f))
            var open = if (t < 3.2f) 0f else eOut(seg(t, 3.2f, 3.5f))
            open *= blinkAt(t, 6.3f, 0.09f, 0.04f, 0.1f)
            open *= blinkAt(t, 7.9f, 0.32f, 0.22f, 0.36f) // the slow blink
            val pd = 0.78f + 0.32f * eOut(seg(t, 3.35f, 3.9f))
            val look = seg(t, 5.0f, 5.3f) * (1f - seg(t, 6.6f, 7.0f)) // she follows the letters
            val lx = look * (-8f + 16f * seg(t, 5.2f, 6.3f))
            val ly = look * 7f
            eye(-58f, open, pd, lx, ly)
            eye(58f, open, pd, lx, ly)
            for (s in listOf(-1f, 1f)) oval(s * 86f, 40f, 17f, 11f, Color(242, 150, 150), 0.25f)
            drawPath(nose, Nose)
            drawPath(noseTip, Color(0xFF211816))
            drawPath(mouth, Color(0xFF6B4B3E), style = Stroke(3f, cap = StrokeCap.Round))
            drawPath(whiskers, Color(150, 130, 120), alpha = 0.6f, style = Stroke(2f))
            for ((x, y, dl) in listOf(Triple(-118f, -40f, 0f), Triple(122f, -30f, 0.15f))) {
                val s = bump(t, 3.45f + dl, 4.25f + dl)
                if (s > 0f) sparkle(x, y, 20f * s, Color.White)
            }
            val hz = seg(t, 4.7f, 6.2f)
            if (hz > 0f && hz < 1f) {
                val sc = 0.6f + 0.5f * eBack(min(1f, hz * 3f))
                withTransform({ translate(70f + 18f * sin(hz * 6f), -205f - 110f * eOut(hz)); scale(sc, sc, Offset.Zero) }) {
                    drawPath(heart, Salmon, alpha = 1f - seg(hz, 0.6f, 1f))
                }
            }
        }
    }

    private fun DrawScope.tail(t: Float) {
        val up = eOut(seg(t, 5.4f, 6.3f))
        if (up <= 0f) return
        val sw = sin((t - 5.4f) * 2.3f) * 48f
        val bx = 1262f; val by = 610f; val tx = 1400f + sw; val ty = 640f - 280f * up; val cx = 1440f + sw * 0.4f; val cy = 600f - 60f * up
        for (i in 0..34) {
            val u = i / 34f; val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u
            val x = a * bx + b * cx + c * tx; val y = a * by + b * cy + c * ty; val r = 27f - 6f * u
            val dark = u < 0.38f || u >= 0.78f
            drawCircle(if (dark) Black else Orange, r, Offset(x, y))
            if (i % 3 == 0) drawCircle(if (dark) Color(0xFF2A2421) else Color(0xFFE8AD7A), r * 0.42f, Offset(x + r * 0.55f, y - r * 0.2f))
        }
    }

    private fun DrawScope.tv(t: Float) {
        val dot = seg(t, 0.2f, 0.5f); val line = seg(t, 0.5f, 0.9f); val on = seg(t, 0.9f, 1.4f); val ba = seg(t, 0f, 0.6f)
        if (on > 0f) {
            val glow = Color(255, 196, 140)
            drawRect(Brush.radialGradient(0f to glow.copy(alpha = 0.22f * on), 0.1f to glow.copy(alpha = 0.22f * on), 1f to glow.copy(alpha = 0f), center = Offset(960f, 430f), radius = 620f), Offset.Zero, Size(1920f, 1080f))
        }
        tail(t)
        val wig = if (t > 2.5f) 0.2f * sin((t - 2.5f) * 14f) * exp(-(t - 2.5f) * 2.6f) else 0f
        for ((bx, ang, len) in listOf(Triple(900f, -0.55f + wig, 130f), Triple(1020f, 0.5f - wig, 142f))) {
            val end = Offset(bx + sin(ang) * len, 192f - kotlin.math.cos(ang) * len)
            drawLine(Cafe, Offset(bx, 192f), end, strokeWidth = 8f, cap = StrokeCap.Round, alpha = ba)
            drawCircle(Caramel, 12f, end, alpha = ba)
        }
        rrect(830f, 664f, 60f, 24f, 8f, Cafe, ba); rrect(1030f, 664f, 60f, 24f, 8f, Cafe, ba)
        rrect(620f, 190f, 680f, 480f, 72f, TvBody, ba)
        drawRoundRect(Cafe, Offset(620f, 190f), Size(680f, 480f), CornerRadius(72f, 72f), style = Stroke(6f), alpha = ba)
        val sx = 650f; val sy = 220f; val sw = 620f; val sh = 420f
        if (t < 0.9f) {
            rrect(sx, sy, sw, sh, 56f, Color(0xFF0A0807), ba)
            if (line > 0f) { val w = sw * eOut(line); drawRect(Screen, Offset(960f - w / 2f, 428f), Size(w, 4f)) }
            else if (dot > 0f) drawCircle(Screen, 7f * eOut(dot), Offset(960f, 430f))
        } else {
            val h = max(4f, sh * eOut(on))
            val r = min(56f, h / 2f)
            val opening = Path().apply { addRoundRect(RoundRect(sx, 430f - h / 2f, sx + sw, 430f + h / 2f, CornerRadius(r, r))) }
            clipPath(opening) {
                drawRect(screenBrush, Offset(sx, sy), Size(sw, sh))
                sushi(t)
                var y = sy
                while (y < sy + sh) { drawRect(Color.Black, Offset(sx, y), Size(sw, 2f), alpha = 0.04f); y += 6f }
                if (on < 1f) drawRect(Screen, Offset(sx, sy), Size(sw, sh), alpha = 0.85f * (1f - on))
            }
            drawRoundRect(Color.Black, Offset(sx, sy), Size(sw, sh), CornerRadius(56f, 56f), style = Stroke(5f), alpha = 0.45f)
        }
        val p = eBack(seg(t, 2.35f, 2.85f))
        if (p > 0f) for (dx in listOf(-82f, 82f)) { // her paws on the edge of the screen
            val y = 636f + (1f - p) * 30f
            oval(960f + dx, y, 40f, 25f, White)
            drawOval(Fur, Offset(960f + dx - 40f, y - 25f), Size(80f, 50f), style = Stroke(3f))
            for (kx in listOf(-13f, 0f, 13f)) drawLine(Color(0xFFCDBDAE), Offset(960f + dx + kx, y + 8f), Offset(960f + dx + kx, y + 22f), strokeWidth = 2.5f)
        }
    }

    private fun DrawScope.title(t: Float, texts: Texts) {
        val total = texts.letters.sumOf { it.size.width }.toFloat()
        var x = 960f - total / 2f
        texts.letters.forEachIndexed { i, layout ->
            val cw = layout.size.width.toFloat()
            val a = 5.2f + i * 0.09f
            val p = seg(t, a, a + 0.55f)
            val sc = eBack(p)
            if (p > 0f && sc > 0.001f) {
                val wave = sin((t - 6.6f) * 7f - i * 0.7f) * 10f * bump(t, 6.6f, 7.6f)
                val isTv = i >= texts.tvFrom
                val base = layout.firstBaseline
                withTransform({
                    translate(x + cw / 2f, 835f + (1f - eOut(p)) * 60f - wave)
                    rotate(deg((1f - eOut(p)) * -0.35f), Offset.Zero)
                    scale(sc, sc, Offset.Zero)
                }) {
                    drawText(layout, if (isTv) Color(0xFF2F5A24) else Color(0xFF5C3A21), Offset(-cw / 2f, 8f - base))
                    drawText(layout, if (isTv) Green else White, Offset(-cw / 2f, -base))
                }
            }
            x += cw
        }
        val s = seg(t, 7.0f, 7.8f)
        if (s > 0f) {
            val hl = texts.hello
            drawText(hl, Cream, Offset(960f - hl.size.width / 2f, 912f + (1f - eOut(s)) * 20f - hl.firstBaseline), alpha = eOut(s))
        }
    }
}
