package tv.own.owntv.features.setup

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.features.setup.SushiCatArt.Black
import tv.own.owntv.features.setup.SushiCatArt.Caramel
import tv.own.owntv.features.setup.SushiCatArt.Face
import tv.own.owntv.features.setup.SushiCatArt.Fur
import tv.own.owntv.features.setup.SushiCatArt.Orange
import tv.own.owntv.features.setup.SushiCatArt.White
import tv.own.owntv.features.setup.SushiCatArt.bump
import tv.own.owntv.features.setup.SushiCatArt.deg
import tv.own.owntv.features.setup.SushiCatArt.eBack
import tv.own.owntv.features.setup.SushiCatArt.eIO
import tv.own.owntv.features.setup.SushiCatArt.eOut
import tv.own.owntv.features.setup.SushiCatArt.face
import tv.own.owntv.features.setup.SushiCatArt.food
import tv.own.owntv.features.setup.SushiCatArt.heart
import tv.own.owntv.features.setup.SushiCatArt.oval
import tv.own.owntv.features.setup.SushiCatArt.rrect
import tv.own.owntv.features.setup.SushiCatArt.seg
import tv.own.owntv.features.setup.SushiCatArt.sparkle

/**
 * SushiTV: the welcome shown over the app on a cold start (and from More › About) — ten seconds, all
 * drawn in code (no images): an old TV switches on, Sushi (SushiCatArt) climbs into it and holds the edge
 * with her paws, opens her eyes, flicks an ear, tilts her head, a heart floats up, her tail peeks out,
 * "SushiTV" drops in letter by letter, and she gives the slow blink cats use to say "I love you".
 *
 * A cold start keeps a TV busy for a moment, so first the TV waits switched off with its red standby
 * light blinking until frames come smoothly (at most [MAX_STANDBY] s); only then the show starts.
 *
 * The sound is one 10 s track (res/raw/sushitv_bienvenida.wav, made in sushitv/tools/bienvenida/): music
 * generated in code plus Sushi's own little sounds. The picture follows the sound's clock.
 *
 * It never takes focus: the first key press (other than volume) only skips it, see [SushiWelcomeSkip].
 * Scene coordinates are a 1920×1080 stage, scaled to the screen.
 */
@Composable
fun SushiWelcome() {
    // Each request is one showing: the cold start is request 0, More › About can ask for more.
    val request = SushiWelcomeReplay.requests
    key(request) { WelcomeRun(request) }
}

/** More › About › "Ver bienvenida de Sushi": play the welcome again. */
object SushiWelcomeReplay {
    internal var requests by mutableIntStateOf(0)
    fun play() { requests++ }
}

@Composable
private fun WelcomeRun(request: Int) {
    var visible by remember { mutableStateOf(WelcomeOnce.shown < request) }
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
    val sound = remember { SushiSound(context, R.raw.sushitv_bienvenida) }
    var standby by remember { mutableFloatStateOf(0f) }
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
        WelcomeOnce.shown = request
        SushiWelcomeSkip.skip = false
        SushiWelcomeSkip.active = true
        try {
            // Standby: wait until the TV draws smoothly, so nobody misses the TV switching on.
            val waitFrom = withFrameNanos { it }
            var prev = waitFrom
            var smooth = 0
            while (!SushiWelcomeSkip.skip) {
                val now = withFrameNanos { it }
                smooth = if (now - prev < SMOOTH_FRAME_NS) smooth + 1 else 0
                prev = now
                standby = (now - waitFrom) / 1e9f
                if ((smooth >= SMOOTH_FRAMES && standby >= MIN_STANDBY) || standby >= MAX_STANDBY) break
            }
            if (!SushiWelcomeSkip.skip) sound.start()
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
        runCatching { scene.draw(this, time, standby, texts) }.onFailure {
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

/**
 * The last request already shown (-1: none yet). So a cold start shows it once, and a configuration
 * change or returning to the app does not show it again; only a new request does.
 */
private object WelcomeOnce { var shown = -1 }

private const val DURATION = 10f
private const val MIN_STANDBY = 0.8f
private const val MAX_STANDBY = 3f
private const val SMOOTH_FRAMES = 20
private const val SMOOTH_FRAME_NS = 34_000_000L // a frame within ~30 fps counts as smooth
private const val MAX_HOLD_MS = 15_000L
private const val SKIP_STEPS = 12
private const val SKIP_MS = 300L

private class Texts(val letters: List<TextLayoutResult>, val tvFrom: Int, val hello: TextLayoutResult, val hint: TextLayoutResult)

private val Ink = Color(0xFF0E0B09)
private val TvBody = Color(0xFF1B1512)
private val Cafe = Color(0xFF8B5A3C)
private val Green = Color(0xFFA8DB8E)
private val Cream = Color(0xFFF3E6D6)
private val Screen = Color(0xFFFFF6EA)

/** One floating nigiri / maki / paw / fish / sparkle in the background. */
private class Floater(val x: Float, val y: Float, val v: Float, val sz: Float, val kind: Int, val ph: Float, val rot: Float)

private class Scene {
    private fun hash(i: Int, n: Int): Float { val x = sin(i * 127.1 + n * 311.7) * 43758.5453; return (x - floor(x)).toFloat() }
    private val floaters = List(16) { i ->
        Floater(hash(i, 1) * 1920f, hash(i, 2) * 1280f, 22f + hash(i, 3) * 34f, 20f + hash(i, 4) * 24f, i % 5, hash(i, 5) * 6.28f, hash(i, 6) * 6.28f)
    }
    private val tufts = Path().apply {
        for ((x, y) in listOf(-40f to 140f, 0f to 150f, 40f to 140f, -75f to 128f, 75f to 128f)) { moveTo(x - 14, y); lineTo(x, y + 18); lineTo(x + 14, y); close() }
    }
    private val screenBrush = Brush.radialGradient(0f to Color(0xFFE9CBA7), 0.1f to Color(0xFFE9CBA7), 1f to Color(0xFFB98558), center = Offset(960f, 415f), radius = 420f)
    private val backBrush = Brush.radialGradient(listOf(Color(0xFF3A271B), Ink), center = Offset(960f, 480f), radius = 1150f)

    fun draw(d: DrawScope, t: Float, standby: Float, texts: Texts): Unit = with(d) {
        val k = min(size.width / 1920f, size.height / 1080f)
        drawRect(Ink)
        withTransform({ translate((size.width - 1920f * k) / 2f, (size.height - 1080f * k) / 2f); scale(k, k, Offset.Zero) }) {
            drawRect(backBrush, Offset.Zero, Size(1920f, 1080f))
            val z = 1f + 0.04f * eIO(seg(t, 9.3f, 10f))
            withTransform({ scale(z, z, Offset(960f, 540f)) }) {
                val fa = seg(t, 1.3f, 2.4f)
                if (fa > 0f) floaters.forEach { floater(it, t, 0.3f * fa) }
                tv(t, standby)
                title(t, texts)
            }
            val h = seg(t, 1.5f, 2.2f)
            if (h > 0f) drawText(texts.hint, Cream, Offset(1880f - texts.hint.size.width, 1040f - texts.hint.firstBaseline), alpha = 0.4f * h)
        }
        Unit
    }

    private fun DrawScope.floater(f: Floater, t: Float, a: Float) {
        val y = ((f.y - t * f.v) % 1280f + 1280f) % 1280f - 100f
        val x = f.x + 26f * sin(t * 0.8f + f.ph)
        val s = f.sz
        withTransform({ translate(x, y); rotate(deg(f.rot + 0.25f * sin(t * 0.6f + f.ph)), Offset.Zero) }) {
            when (f.kind) {
                0 -> food(0, 0f, 0f, s, 0f, a)
                1 -> food(1, 0f, 0f, s, 0f, a)
                2 -> { // paw print
                    oval(0f, s * .15f, s * .38f, s * .3f, Caramel, a)
                    for (ang in listOf(-.9f, -.3f, .3f, .9f)) drawCircle(Caramel, s * .14f, Offset(sin(ang) * s * .5f, -kotlin.math.cos(ang) * s * .42f), alpha = a)
                }
                3 -> food(2, 0f, 0f, s, 0f, a)
                else -> sparkle(0f, 0f, s * .45f, Color(0xFFFFE7C7), a)
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
            var open = if (t < 3.2f) 0f else eOut(seg(t, 3.2f, 3.5f))
            open *= blinkAt(t, 6.3f, 0.09f, 0.04f, 0.1f)
            open *= blinkAt(t, 7.9f, 0.32f, 0.22f, 0.36f) // the slow blink
            val look = seg(t, 5.0f, 5.3f) * (1f - seg(t, 6.6f, 7.0f)) // she follows the letters
            face(Face(
                open = open,
                pd = 0.78f + 0.32f * eOut(seg(t, 3.35f, 3.9f)),
                lx = look * (-8f + 16f * seg(t, 5.2f, 6.3f)),
                ly = look * 7f,
                flick = -0.28f * bump(t, 3.9f, 4.12f) + 0.1f * bump(t, 4.12f, 4.32f),
            ))
            for ((x, y, dl) in listOf(Triple(-118f, -40f, 0f), Triple(122f, -30f, 0.15f))) {
                val s = bump(t, 3.45f + dl, 4.25f + dl)
                if (s > 0f) sparkle(x, y, 20f * s, Color.White)
            }
            val hz = seg(t, 4.7f, 6.2f)
            if (hz > 0f && hz < 1f) heart(70f + 18f * sin(hz * 6f), -205f - 110f * eOut(hz), 0.6f + 0.5f * eBack(min(1f, hz * 3f)), 1f - seg(hz, 0.6f, 1f))
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

    private fun DrawScope.tv(t: Float, standby: Float) {
        val dot = seg(t, 0.2f, 0.5f); val line = seg(t, 0.5f, 0.9f); val on = seg(t, 0.9f, 1.4f)
        val ba = max(seg(standby, 0f, 0.5f), seg(t, 0f, 0.6f))
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
        // The standby light: blinking red while the TV waits, green once it is on.
        val led = if (t > 0.2f) Color(0xFF8BE38B) else Color(0xFFFF4D4D)
        val ledA = if (t > 0.2f) 1f else 0.35f + 0.65f * (0.5f + 0.5f * sin(standby * 5f))
        drawCircle(led.copy(alpha = 0.25f), 16f, Offset(1262f, 655f), alpha = ba * ledA)
        drawCircle(led, 6f, Offset(1262f, 655f), alpha = ba * ledA)
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

    private fun blinkAt(t: Float, a: Float, c: Float, h: Float, o: Float): Float = when {
        t < a || t > a + c + h + o -> 1f
        t < a + c -> 1f - eIO(seg(t, a, a + c))
        t < a + c + h -> 0f
        else -> eIO(seg(t, a + c + h, a + c + h + o))
    }
}
