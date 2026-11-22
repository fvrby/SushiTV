package tv.own.owntv.features.setup

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.math.sin
import tv.own.owntv.R
import tv.own.owntv.features.setup.SushiCatArt.Cat
import tv.own.owntv.features.setup.SushiCatArt.Face
import tv.own.owntv.features.setup.SushiCatArt.Pose
import tv.own.owntv.features.setup.SushiCatArt.bump
import tv.own.owntv.features.setup.SushiCatArt.cat
import tv.own.owntv.features.setup.SushiCatArt.deg
import tv.own.owntv.features.setup.SushiCatArt.eBack
import tv.own.owntv.features.setup.SushiCatArt.eIO
import tv.own.owntv.features.setup.SushiCatArt.eOut
import tv.own.owntv.features.setup.SushiCatArt.face
import tv.own.owntv.features.setup.SushiCatArt.food
import tv.own.owntv.features.setup.SushiCatArt.heart
import tv.own.owntv.features.setup.SushiCatArt.lerp
import tv.own.owntv.features.setup.SushiCatArt.nigiri
import tv.own.owntv.features.setup.SushiCatArt.oval
import tv.own.owntv.features.setup.SushiCatArt.paw
import tv.own.owntv.features.setup.SushiCatArt.rrect
import tv.own.owntv.features.setup.SushiCatArt.seg
import tv.own.owntv.features.setup.SushiCatArt.sparkle
import tv.own.owntv.features.setup.SushiCatArt.tail
import tv.own.owntv.features.setup.SushiCatArt.zz

/**
 * SushiTV's secret: holding OK on the Sushi logo at the top of the rail plays one of four little scenes,
 * picked at random (never the same twice in a row), floating over whatever is on screen:
 *  1. the box — she peeks out of a cardboard box, jumps out with paper flying and naps in it;
 *  2. the laser pointer — she follows a red dot, wiggles, pounces, misses, and licks her paw as if nothing;
 *  3. the nap — she sleeps curled up dreaming of flying sushi, wakes, stretches, yawns;
 *  4. sushi rain — she jumps to catch falling nigiri, maki and fish, counting them, and chews happily.
 *
 * Drawn in code like the welcome (SushiCatArt), with a short sound each (res/raw/sushitv_secreto_N.wav:
 * music made in code plus Sushi's own little sounds, from sushitv/tools/bienvenida/secretos.html, where
 * the same scenes run in a browser). Nothing is painted outside Sushi and her things; it never takes
 * focus or keys, so the app keeps working underneath. Stage: 1920×1080, scaled to the screen.
 */
@Composable
fun SushiSecrets() {
    val plays = SushiSecretPlayer.plays
    if (plays == 0) return
    key(plays) { SecretRun(SushiSecretPlayer.current) }
}

/** The rail logo's long press. */
object SushiSecretPlayer {
    internal var plays by mutableIntStateOf(0)
    internal var current = 0
    private var last = -1

    fun playRandom() {
        var n: Int
        do { n = Random.nextInt(SECRETS.size) } while (n == last)
        last = n
        current = n
        plays++
    }
}

private class Secret(val seconds: Float, val sound: Int)

private val SECRETS = listOf(
    Secret(7.2f, R.raw.sushitv_secreto_1), // the box
    Secret(7.2f, R.raw.sushitv_secreto_2), // the laser pointer
    Secret(8.0f, R.raw.sushitv_secreto_3), // the nap
    Secret(7.8f, R.raw.sushitv_secreto_4), // sushi rain
)

private class RainTexts(val plusOne: TextLayoutResult, val counts: List<TextLayoutResult>)

@Composable
private fun SecretRun(index: Int) {
    val secret = SECRETS[index]
    var visible by remember { mutableStateOf(true) }
    if (!visible) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val plusOne = stringResource(R.string.sushitv_secret_plus_one)
    val counts = (0..DROPS.size).map { stringResource(R.string.sushitv_secret_count, it) }
    val texts = remember(density, plusOne, counts) {
        fun style(px: Float) = TextStyle(fontSize = with(density) { px.toSp() }, fontWeight = FontWeight.ExtraBold)
        RainTexts(measurer.measure(plusOne, style(44f)), counts.map { measurer.measure(it, style(48f)) })
    }
    val sound = remember { SushiSound(context, secret.sound) }
    var time by remember { mutableFloatStateOf(0f) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { sound.release(); visible = false } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); sound.release() }
    }
    LaunchedEffect(Unit) {
        try {
            sound.start()
            var start = withFrameNanos { it }
            var synced = false
            while (time < secret.seconds) {
                val now = withFrameNanos { it }
                if (!synced) sound.positionMs()?.let { start = now - it * 1_000_000L; synced = true }
                time = (now - start) / 1e9f
            }
        } finally {
            sound.release()
            visible = false
        }
    }
    val scenes = remember { Scenes() }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = seg(time, 0f, 0.25f) * (1f - seg(time, secret.seconds - 0.5f, secret.seconds)) },
    ) {
        runCatching {
            val k = min(size.width / 1920f, size.height / 1080f)
            withTransform({ translate((size.width - 1920f * k) / 2f, (size.height - 1080f * k) / 2f); scale(k, k, Offset.Zero) }) {
                when (index) {
                    0 -> scenes.box(this, time)
                    1 -> scenes.laser(this, time)
                    2 -> scenes.nap(this, time)
                    else -> scenes.rain(this, time, texts)
                }
            }
        }.onFailure {
            android.util.Log.w("SushiSecrets", "secret skipped", it)
            visible = false
        }
    }
}

/** Falling food for the rain: start second, x, kind (0 nigiri, 1 maki, 2 fish). */
private val DROPS = listOf(Triple(0.3f, 700f, 0), Triple(1.25f, 1180f, 1), Triple(2.15f, 860f, 2), Triple(3.05f, 1300f, 0), Triple(3.95f, 620f, 1), Triple(4.9f, 1000f, 0))
private const val FALL = 0.95f
private const val G = 930f // the floor line

private class Shred(val vx: Float, val vy: Float, val r: Float, val len: Float)

private class Scenes {
    private val shreds = List(16) { i ->
        Shred((sin(i * 12.9) * 0.5 * 520).toFloat(), (-420 - abs(sin(i * 7.3)) * 380).toFloat(), (sin(i * 3.1) * 6).toFloat(), 26f + (i % 4) * 8f)
    }
    private val boxBack = Path().apply { moveTo(-180f, -230f); lineTo(180f, -230f); lineTo(210f, -250f); lineTo(-150f, -250f); close() }
    private val boxSide = Path().apply { moveTo(180f, -230f); lineTo(210f, -250f); lineTo(210f, -20f); lineTo(180f, 0f); close() }
    private val paper = Color(0xFFB38A55)

    // ---- 1. the box ---------------------------------------------------------------------------------
    fun box(d: DrawScope, t: Float): Unit = with(d) {
        val x0 = 960f
        val bx = lerp(1500f, x0, eBack(seg(t, 0f, 0.7f)))
        val wob = bump(t, 2.7f, 3.6f) * sin(t * 30f) * 0.06f
        withTransform({ translate(bx, G); rotate(deg(wob), Offset.Zero) }) {
            oval(0f, 4f, 230f, 22f, Color.Black, 0.25f)
            drawPath(boxBack, Color(0xFF5E4126))
            val peek = bump(t, 0.9f, 2.6f)
            val eyesL = seg(t, 1.4f, 1.7f) - seg(t, 1.9f, 2.2f)
            val jump = seg(t, 3.6f, 4.7f)
            val landed = t > 4.7f
            when {
                t >= 3.6f && !landed -> {
                    val yo = -420f * 4f * jump * (1f - jump)
                    cat(0f, -40f + yo, 0.95f, Cat(Pose.SIT, Face(pd = 1.2f, mouth = 0.6f * bump(jump, 0f, 0.5f)), t = t))
                }
                landed -> {
                    val s = seg(t, 4.7f, 5.1f)
                    withTransform({ translate(0f, lerp(-200f, -268f, eBack(s))); scale(.62f, .62f, Offset.Zero) }) {
                        face(Face(open = max(0f, 1f - seg(t, 5.3f, 5.9f)), smile = t > 5.4f, ly = 3f))
                    }
                }
                t > 0.8f -> withTransform({ translate(0f, lerp(-110f, -262f, eOut(min(1f, peek * 1.6f)))); scale(.62f, .62f, Offset.Zero) }) {
                    face(Face(pd = 1.1f, lx = -10f * eyesL + 10f * (seg(t, 1.9f, 2.2f) - seg(t, 2.3f, 2.5f)), ly = -2f))
                }
            }
            drawRect(Color(0xFFC9A06A), Offset(-180f, -230f), Size(360f, 230f))
            drawPath(boxSide, Color(0xFFA97E4B))
            drawRect(Color(0xFFB88E58), Offset(-180f, -230f), Size(360f, 14f))
            drawLine(Color(0xFF8E6A3C), Offset(-60f, -150f), Offset(60f, -150f), strokeWidth = 3f)
            drawLine(Color(0xFF8E6A3C), Offset(-40f, -130f), Offset(40f, -130f), strokeWidth = 3f)
            val flaps = Color(0xFFD8B27A)
            drawPath(Path().apply { moveTo(-180f, -230f); lineTo(-250f, -300f + 8f * sin(t * 3f)); lineTo(-90f, -300f); lineTo(-40f, -230f); close() }, flaps)
            drawPath(Path().apply { moveTo(180f, -230f); lineTo(260f, -290f); lineTo(120f, -305f + 8f * sin(t * 3f + 1f)); lineTo(60f, -230f); close() }, flaps)
            if (landed) {
                paw(-48f, -228f); paw(48f, -228f)
                val sw = sin((t - 4.7f) * 2.4f)
                tail(170f, -215f, 250f, -210f, 235f + sw * 25f, -90f, 18f, 13f)
            }
            val strip = Stroke(4f, cap = StrokeCap.Round)
            for (i in 0 until 7) {
                val x = -150f + i * 48f
                drawPath(Path().apply { moveTo(x, -232f); quadraticTo(x + 12f, -246f - 6f * sin(t * 4f + i), x + 26f, -232f) }, paper, style = strip)
            }
            if (t > 3.6f && t < 5.4f) {
                val u = t - 3.6f
                for (s in shreds) withTransform({ translate(s.vx * u, -240f + s.vy * u + 900f * u * u); rotate(deg(s.r + u * 6f), Offset.Zero) }) {
                    drawPath(Path().apply { moveTo(-s.len / 2f, 0f); quadraticTo(0f, -10f, s.len / 2f, 0f) }, paper, style = strip)
                }
            }
        }
        if (t > 5.6f) for (i in 0 until 3) {
            val p = seg(t, 5.6f + i * 0.45f, 6.9f + i * 0.45f)
            if (p > 0f && p < 1f) zz(x0 + 90f + 40f * p + i * 18f, G - 330f - 120f * p - i * 20f, 0.6f + 0.5f * p, sin(p * PI.toFloat()))
        }
    }

    // ---- 2. the laser pointer -----------------------------------------------------------------------
    private fun dotAt(t: Float): Offset {
        fun path(t: Float) = Offset(960f + 650f * sin(1.3f * t + 0.5f), 540f + 250f * sin(2.1f * t))
        if (t < 3.5f) return path(t)
        if (t < 3.9f) { val u = eIO(seg(t, 3.5f, 3.9f)); val a = path(3.5f); return Offset(lerp(a.x, 1250f, u), lerp(a.y, 920f, u)) }
        if (t < 4.95f) return Offset(1250f, 920f)
        return Offset(lerp(1250f, 2050f, eIO(seg(t, 4.95f, 5.25f))), 920f)
    }

    fun laser(d: DrawScope, t: Float): Unit = with(d) {
        val dot = dotAt(t)
        val da = seg(t, 0.2f, 0.5f) * (1f - seg(t, 5.1f, 5.25f))
        var x = 420f; var pose = Pose.SIT; var sq = 1f; var lx = 0f; var ly = 0f; var tilt = 0f
        var blush = 0f; var lift = 0f; var tongue = 0f; var wig = 0f; var pd = 1f; var flip = false; var yOff = 0f
        val dx = dot.x - x; val dy = dot.y - (G - 230f); val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        if (t < 4.95f) { lx = dx / len * 10f; ly = dy / len * 10f; tilt = (dx / 2500f).coerceIn(-.25f, .25f); pd = 1f + 0.45f * seg(t, 0.6f, 1.5f) }
        if (t >= 3.9f && t < 4.4f) { pose = Pose.CROUCH; wig = sin(t * 45f) * 5f; pd = 1.5f; flip = true; lx = -10f; ly = 4f; tilt = 0f }
        if (t >= 4.4f && t < 4.95f) {
            val u = seg(t, 4.4f, 4.95f)
            x = lerp(420f, 1250f, u); pose = Pose.JUMP; flip = true; lx = -10f; tilt = 0f
            yOff = -300f * 4f * u * (1f - u)
        }
        if (t >= 4.95f) {
            x = 1250f; sq = lerp(0.8f, 1f, eBack(seg(t, 4.95f, 5.3f))); lx = 0f; ly = 0f
            blush = seg(t, 5.3f, 5.6f); lift = bump(t, 5.8f, 6.7f); tongue = if (lift > 0.5f) 1f else 0f
        }
        if (t >= 4.95f && t < 5.6f) { // dust where she lands
            val p = seg(t, 4.95f, 5.6f)
            for (i in 0 until 5) drawCircle(Color(230, 220, 210), 10f + 20f * p, Offset(1250f + (i - 2) * 45f * (0.4f + p), G - 10f - 15f * p), alpha = 0.45f * (1f - p))
        }
        cat(x, G + yOff, 1f, Cat(
            pose, Face(open = if (t > 6.9f) 0.6f else 1f, pd = pd, lx = lx, ly = ly, blush = blush, tongue = tongue),
            flip = flip, squash = sq, tilt = tilt, tailSw = sin(t * (if (pose == Pose.CROUCH) 18f else 3f)), wig = wig, lift = lift, t = t,
        ))
        if (t > 5.35f && t < 6.1f) { // a little drop of embarrassment
            val p = seg(t, 5.35f, 6.1f); val sx = x + 72f; val sy = G - 290f + 40f * p
            drawPath(Path().apply { moveTo(sx, sy - 16f); quadraticTo(sx + 12f, sy, sx, sy + 8f); quadraticTo(sx - 12f, sy, sx, sy - 16f); close() }, Color(0xFF8FC8F2), alpha = 1f - p)
        }
        if (da > 0f) {
            val red = Color(255, 40, 40)
            drawCircle(Brush.radialGradient(0f to red.copy(alpha = da), 0.25f to red.copy(alpha = 0.8f * da), 1f to red.copy(alpha = 0f), center = dot, radius = 34f), 34f, dot)
            drawCircle(Color(255, 230, 230), 6f, dot, alpha = da)
        }
    }

    // ---- 3. the nap ---------------------------------------------------------------------------------
    private fun DrawScope.cloud(x: Float, y: Float, w: Float, h: Float, a: Float) {
        for ((ox, oy, r) in listOf(Triple(-.32f, .05f, .3f), Triple(-.1f, -.18f, .34f), Triple(.18f, -.15f, .32f), Triple(.36f, .06f, .28f), Triple(.05f, .16f, .36f))) {
            drawCircle(Color.White, r * h, Offset(x + ox * w, y + oy * h * 1.4f), alpha = 0.95f * a)
        }
    }

    fun nap(d: DrawScope, t: Float): Unit = with(d) {
        val x0 = 960f
        when {
            t <= 5.3f -> cat(x0, G, 1.05f, Cat(Pose.LOAF, Face(open = 0f, smile = t > 1.8f && t < 5.2f, twitch = sin(t * 9f) * 3f * bump(t, 2f, 5f)), tilt = -0.12f, t = t))
            t < 5.8f -> cat(x0, G, 1.05f, Cat(Pose.LOAF, Face(open = eOut(seg(t, 5.35f, 5.7f)), pd = 0.8f), tilt = -0.12f, t = t))
            t < 6.7f -> cat(x0, G, 1.05f, Cat(Pose.STRETCH, Face(open = 0.35f, mouth = bump(t, 5.9f, 6.6f)), tailSw = sin(t * 3f), t = t))
            else -> {
                val p = seg(t, 6.7f, 7.0f)
                cat(x0, G, 1.05f, Cat(Pose.SIT, Face(pd = 1.1f, blush = 0.4f), squash = lerp(0.9f, 1f, eBack(p)), tailSw = sin(t * 2.5f), t = t))
                heart(x0 + 70f, G - 420f - 80f * seg(t, 7.0f, 7.8f), 0.9f + 0.4f * seg(t, 7.0f, 7.3f), 1f - seg(t, 7.5f, 7.9f))
            }
        }
        if (t < 5.2f) for (i in 0 until 4) {
            val p = (t * 0.55f + i * 0.25f) % 1f
            zz(x0 - 150f + 45f * p + i * 6f, G - 180f - 150f * p, 0.45f + 0.5f * p, sin(p * PI.toFloat()) * seg(t, 0.3f, 0.8f))
        }
        val ba = seg(t, 1.2f, 1.7f) * (1f - seg(t, 5.0f, 5.25f))
        if (ba > 0f) { // the dream: chasing flying sushi
            val pop = 1f + 0.2f * seg(t, 5.0f, 5.25f)
            drawCircle(Color.White, 10f * ba, Offset(x0 - 20f, G - 260f), alpha = 0.9f)
            drawCircle(Color.White, 16f * ba, Offset(x0 + 20f, G - 320f), alpha = 0.9f)
            val cx = x0 + 200f; val cy = G - 500f; val w = 460f * pop; val h = 260f * pop
            cloud(cx, cy, w, h, ba)
            clipPath(Path().apply { addOval(Rect(cx - w * 0.42f, cy - h * 0.42f, cx + w * 0.42f, cy + h * 0.42f)) }) {
                val u = t * 0.45f
                for (i in 0 until 3) food(i, cx - 260f + ((u * 400f + i * 170f) % 520f), cy - 20f + sin(t * 3f + i) * 25f, 26f, sin(t * 2f + i) * .3f, ba)
                val hx = cx - 260f + ((u * 400f - 110f + 520f) % 520f)
                withTransform({ translate(hx, cy + 10f + sin(t * 6f) * 8f); scale(.26f, .26f, Offset.Zero) }) {
                    face(Face(pd = 1.3f, lx = 10f, mouth = 0.5f + 0.3f * sin(t * 8f)))
                }
            }
        }
    }

    // ---- 4. sushi rain ------------------------------------------------------------------------------
    fun rain(d: DrawScope, t: Float, texts: RainTexts): Unit = with(d) {
        val catchY = G - 330f
        var x = 960f; var yOff = 0f; var mouth = 0f; var caught = 0; var puff = 0f
        DROPS.forEachIndexed { i, (t0, dx, _) ->
            val prev = if (i > 0) DROPS[i - 1].second else 960f
            if (t >= t0 - 0.25f && t < t0 + FALL) x = lerp(prev, dx, eIO(seg(t, t0 - 0.25f, t0 + 0.45f))) else if (t >= t0 + FALL) x = dx
            if (t >= t0 + FALL) caught = i + 1
            val j = seg(t, t0 + FALL - 0.3f, t0 + FALL + 0.25f)
            if (j > 0f && j < 1f) { yOff = -170f * sin(j * PI.toFloat()); mouth = max(mouth, bump(j, 0f, 0.6f)) }
        }
        if (t > 5.9f && t < 6.9f) { puff = 0.5f + 0.5f * sin(t * 16f); mouth = 0f }
        cat(x, G + yOff, 0.95f, Cat(
            Pose.SIT, Face(pd = 1.2f, ly = if (t < 5.9f) -10f else 0f, mouth = mouth, puff = puff * 0.6f, blush = if (t > 5.9f) 0.7f else 0.1f),
            tailSw = sin(t * 3f), t = t,
        ))
        DROPS.forEachIndexed { i, (t0, dx, kind) ->
            val p = seg(t, t0, t0 + FALL)
            if (p > 0f && p < 1f) food(kind, dx, lerp(-60f, catchY, p * p), 38f, t * 2f + i)
            val q = seg(t, t0 + FALL, t0 + FALL + 0.8f)
            if (q > 0f && q < 1f) {
                val l = texts.plusOne
                drawText(l, Color(0xFFFFE08A), Offset(dx + 90f - l.size.width / 2f, catchY - 20f - 60f * q - l.firstBaseline), alpha = 1f - q)
                sparkle(dx - 80f, catchY + 10f - 30f * q, 18f * (1f - q), Color.White)
            }
        }
        val ca = seg(t, 0.2f, 0.6f) * (1f - seg(t, 7.2f, 7.8f))
        if (ca > 0f) { // the counter
            rrect(1430f, 150f, 250f, 92f, 46f, Color(20, 16, 14), 0.78f * ca)
            nigiri(1490f, 205f, 34f, 0f, ca)
            val pulse = if (caught > 0) { val c = DROPS[caught - 1].first + FALL; 1f + 0.25f * bump(t, c, c + 0.3f) } else 1f
            val l = texts.counts[caught.coerceIn(0, texts.counts.size - 1)]
            withTransform({ translate(1585f, 214f); scale(pulse, pulse, Offset.Zero) }) {
                drawText(l, Color(0xFFFFF6EA), Offset(-l.size.width / 2f, -l.firstBaseline), alpha = ca)
            }
        }
        if (t > 6.0f) heart(x + 80f, G - 430f - 90f * seg(t, 6.0f, 7.0f), 0.9f + 0.3f * seg(t, 6.0f, 6.3f), 1f - seg(t, 6.7f, 7.2f))
    }
}
