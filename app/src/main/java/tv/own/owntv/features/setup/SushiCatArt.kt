package tv.own.owntv.features.setup

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * SushiTV: Sushi drawn in code, shared by the welcome (SushiWelcome) and the secret animations
 * (SushiSecrets). Her real markings, from the user's photos: a black band over the head running down
 * her left side, caramel round her right eye, a white stripe from a little white spot on top down to a
 * pink nose with a black tip, a caramel dot on the chin, light green eyes, a black-and-caramel tail.
 *
 * Head coordinates: head centre at 0,0, about 300 wide. Body coordinates: 0,0 on the ground under her.
 * The browser lab (the .js files in sushitv/tools/bienvenida/) draws the same shapes: keep both in step.
 */
internal object SushiCatArt {
    val White = Color(0xFFFFFDF8)
    val Fur = Color(0xFFE6D8C8)
    val Black = Color(0xFF1A1614)
    val Orange = Color(0xFFD9955E)
    val Caramel = Color(0xFFC98B5B)
    val Pink = Color(0xFFF2B3AE)
    val Salmon = Color(0xFFFF7A59)
    private val EarLight = Color(0xFFEEE6DD)
    private val Nose = Color(0xFFEBA9A6)
    private val EyeLine = Color(0xFF3F5C34)
    private val Tongue = Color(0xFFF29AA0)

    private val head = Path().apply {
        moveTo(0f, -110f)
        cubicTo(95f, -110f, 150f, -60f, 150f, 15f); cubicTo(150f, 85f, 85f, 122f, 0f, 122f)
        cubicTo(-85f, 122f, -150f, 85f, -150f, 15f); cubicTo(-150f, -60f, -95f, -110f, 0f, -110f); close()
    }
    private fun earOuter(s: Float) = Path().apply { moveTo(s * 150, -20f); quadraticTo(s * 156, -150f, s * 120, -182f); quadraticTo(s * 84, -150f, s * 36, -104f); close() }
    private fun earInner(s: Float) = Path().apply { moveTo(s * 128, -45f); quadraticTo(s * 132, -130f, s * 116, -155f); quadraticTo(s * 92, -128f, s * 62, -98f); close() }
    private val ears = listOf(-1f, 1f).associateWith { earOuter(it) to earInner(it) }
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
    private val stripe = Path().apply { moveTo(-15f, -96f); quadraticTo(0f, -104f, 15f, -96f); lineTo(18f, -30f); quadraticTo(0f, -20f, -18f, -30f); close() }
    private val nose = Path().apply { moveTo(-15f, 30f); quadraticTo(0f, 25f, 15f, 30f); quadraticTo(10f, 44f, 0f, 52f); quadraticTo(-10f, 44f, -15f, 30f); close() }
    private val noseTip = Path().apply { moveTo(-7f, 41f); quadraticTo(0f, 39f, 7f, 41f); lineTo(0f, 52f); close() }
    private val mouthW = Path().apply { moveTo(0f, 52f); lineTo(0f, 58f); moveTo(-19f, 61f); quadraticTo(-9f, 68f, 0f, 58f); quadraticTo(9f, 68f, 19f, 61f) }
    private val mouthSmile = Path().apply { moveTo(0f, 52f); lineTo(0f, 58f); moveTo(-24f, 58f); quadraticTo(0f, 76f, 24f, 58f) }
    private val closedEye = Path().apply { moveTo(-26f, -2f); quadraticTo(0f, 12f, 26f, -2f) }
    private val heartPath = Path().apply { moveTo(0f, 14f); cubicTo(-30f, -8f, -18f, -30f, 0f, -16f); cubicTo(18f, -30f, 30f, -8f, 0f, 14f); close() }
    private val starPath = Path().apply {
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f; val q = if (i % 2 == 1) 0.28f else 1f
            if (i == 0) moveTo(sin(a) * q, -cos(a) * q) else lineTo(sin(a) * q, -cos(a) * q)
        }
        close()
    }
    private val zPath = Path().apply { moveTo(-14f, -14f); lineTo(14f, -14f); lineTo(-14f, 14f); lineTo(14f, 14f) }
    private val fishTail = Path().apply { moveTo(0.45f, 0f); lineTo(0.85f, -0.3f); lineTo(0.85f, 0.3f); close() }
    private val eyeBrush = Brush.radialGradient(listOf(Color(0xFFDDF3C4), Color(0xFF93C47A)), center = Offset(-3f, -4f), radius = 34f)

    /** How her face looks right now. [open] 0 = eyes closed (a content little curve), 1 = open. */
    class Face(
        val open: Float = 1f, val pd: Float = 1f, val lx: Float = 0f, val ly: Float = 0f,
        val flick: Float = 0f, val blush: Float = 0f, val puff: Float = 0f, val mouth: Float = 0f,
        val smile: Boolean = false, val tongue: Float = 0f, val twitch: Float = 0f,
    )

    enum class Pose { SIT, CROUCH, JUMP, LOAF, STRETCH }

    /** A whole Sushi. [lift] raises her left front paw (0..1); [wig] wiggles her rear before a pounce. */
    class Cat(
        val pose: Pose, val face: Face, val flip: Boolean = false, val squash: Float = 1f, val tilt: Float = 0f,
        val tailSw: Float = 0f, val wig: Float = 0f, val lift: Float = 0f, val t: Float = 0f,
    )

    // ---- small helpers shared by both animations --------------------------------------------------

    fun cl(x: Float) = x.coerceIn(0f, 1f)
    fun seg(t: Float, a: Float, b: Float) = cl((t - a) / (b - a))
    fun lerp(a: Float, b: Float, u: Float) = a + (b - a) * u
    fun eOut(x: Float) = 1f - (1f - x).pow(3)
    fun eIO(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3) / 2f
    fun eBack(x: Float): Float { val c1 = 1.70158f; val c3 = c1 + 1f; val u = x - 1f; return 1f + c3 * u * u * u + c1 * u * u }
    fun bump(t: Float, a: Float, b: Float) = sin(seg(t, a, b) * PI.toFloat())
    fun deg(rad: Float) = rad * 180f / PI.toFloat()

    fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, c: Color, a: Float = 1f) =
        drawOval(c, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = a)

    fun DrawScope.rrect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, a: Float = 1f) =
        drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r), alpha = a)

    fun DrawScope.sparkle(x: Float, y: Float, r: Float, c: Color, a: Float = 1f) {
        if (r <= 0f) return
        withTransform({ translate(x, y); scale(r, r, Offset.Zero) }) { drawPath(starPath, c, alpha = a) }
    }

    fun DrawScope.heart(x: Float, y: Float, k: Float, a: Float) {
        if (a <= 0f || k <= 0f) return
        withTransform({ translate(x, y); scale(k, k, Offset.Zero) }) { drawPath(heartPath, Salmon, alpha = a) }
    }

    /** A sleepy "z", stroked. */
    fun DrawScope.zz(x: Float, y: Float, k: Float, a: Float) {
        if (a <= 0f || k <= 0f) return
        withTransform({ translate(x, y); scale(k, k, Offset.Zero) }) {
            drawPath(zPath, Color(0xFFE8EEF6), alpha = a, style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    fun DrawScope.nigiri(x: Float, y: Float, s: Float, rot: Float = 0f, a: Float = 1f) =
        withTransform({ translate(x, y); rotate(deg(rot), Offset.Zero) }) {
            rrect(-s * .8f, -s * .25f, s * 1.6f, s * .75f, s * .35f, White, a)
            rrect(-s * .9f, -s * .55f, s * 1.8f, s * .5f, s * .25f, Color(0xFFE8824B), a)
            drawRect(Color(0xFFF7B48A), Offset(-s * .4f, -s * .5f), Size(s * .12f, s * .4f), alpha = a)
            drawRect(Color(0xFFF7B48A), Offset(s * .15f, -s * .5f), Size(s * .12f, s * .4f), alpha = a)
        }

    fun DrawScope.maki(x: Float, y: Float, s: Float, a: Float = 1f) {
        drawCircle(Color(0xFF151515), s * .6f, Offset(x, y), alpha = a)
        drawCircle(White, s * .45f, Offset(x, y), alpha = a)
        drawCircle(Color(0xFF9BD27A), s * .2f, Offset(x, y), alpha = a)
    }

    fun DrawScope.fish(x: Float, y: Float, s: Float, a: Float = 1f) {
        oval(x, y, s * .55f, s * .3f, Color(0xFF9BD27A), a)
        withTransform({ translate(x, y); scale(s, s, Offset.Zero) }) { drawPath(fishTail, Color(0xFF9BD27A), alpha = a) }
        drawCircle(Black, s * .06f, Offset(x - s * .28f, y - s * .05f), alpha = a)
    }

    /** 0 nigiri, 1 maki, 2 fish. */
    fun DrawScope.food(kind: Int, x: Float, y: Float, s: Float, rot: Float = 0f, a: Float = 1f) = when (kind) {
        0 -> nigiri(x, y, s, rot, a)
        1 -> maki(x, y, s, a)
        else -> fish(x, y, s, a)
    }

    // ---- the face ---------------------------------------------------------------------------------

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
                drawPath(closedEye, Color(0xFF2B2320), style = Stroke(6f, cap = StrokeCap.Round))
            } else withTransform({ scale(1f, open, Offset.Zero) }) {
                drawOval(eyeBrush, Offset(-30f, -32f), Size(60f, 64f))
                drawOval(EyeLine, Offset(-30f, -32f), Size(60f, 64f), style = Stroke(3.5f))
                oval(lx, ly, 15f * pd, 18f * pd, Color(0xFF15110F))
                drawCircle(Color.White, 7f, Offset(lx - 9f, ly - 11f))
                drawCircle(Color.White, 3.5f, Offset(lx + 8f, ly + 9f))
            }
        }
    }

    /** Her face in head coordinates: ears, markings, eyes, cheeks, nose, mouth, whiskers. */
    fun DrawScope.face(f: Face) {
        ear(-1f, 0f, EarLight)
        ear(1f, f.flick, Black)
        drawPath(head, White)
        clipPath(head) {
            drawPath(blackPatch, Black)
            drawPath(orangePatch, Orange)
            drawPath(stripe, White)
            oval(0f, -104f, 24f, 11f, White)
            oval(6f, 101f, 10f, 7f, Orange)
        }
        drawPath(head, Fur, style = Stroke(3f))
        val lx = f.lx.coerceIn(-10f, 10f); val ly = f.ly.coerceIn(-10f, 10f)
        eye(-58f, f.open, f.pd, lx, ly)
        eye(58f, f.open, f.pd, lx, ly)
        for (s in listOf(-1f, 1f)) oval(s * 86f, 40f, 17f + 6f * f.puff, 11f + 4f * f.puff, Color(242, 150, 150), 0.25f + 0.45f * f.blush)
        drawPath(nose, Nose)
        drawPath(noseTip, Color(0xFF211816))
        if (f.mouth > 0.05f) {
            oval(0f, 72f, 17f * f.mouth + 3f, 22f * f.mouth + 3f, Color(0xFF6E2E2A))
            oval(0f, 78f + 6f * f.mouth, 11f * f.mouth + 2f, 8f * f.mouth + 2f, Tongue)
        }
        drawPath(if (f.smile) mouthSmile else mouthW, Color(0xFF6B4B3E), style = Stroke(3f, cap = StrokeCap.Round))
        if (f.tongue > 0f) oval(0f, 70f, 9f, 12f * f.tongue, Tongue)
        val whiskers = Path().apply {
            for (s in listOf(-1f, 1f)) for (d in listOf(-14f, 0f, 14f)) {
                moveTo(s * 42, 54 + d * 0.4f); quadraticTo(s * 100, 48 + d, s * 168, 42 + d * 1.6f + f.twitch * s)
            }
        }
        drawPath(whiskers, Color(150, 130, 120), alpha = 0.6f, style = Stroke(2f))
    }

    // ---- the body -----------------------------------------------------------------------------------

    /** Her fluffy tail along a curve: black, caramel, black. */
    fun DrawScope.tail(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, r0: Float, r1: Float) {
        for (i in 0..26) {
            val u = i / 26f; val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u
            val x = a * x0 + b * x1 + c * x2; val y = a * y0 + b * y1 + c * y2; val r = lerp(r0, r1, u)
            val dark = u < 0.38f || u >= 0.78f
            drawCircle(if (dark) Black else Orange, r, Offset(x, y))
            if (i % 3 == 0) drawCircle(if (dark) Color(0xFF2A2421) else Color(0xFFE8AD7A), r * 0.42f, Offset(x + r * 0.5f, y - r * 0.2f))
        }
    }

    /** A white body (an oval turned by [rot]) with a black patch on her back and a caramel spot. */
    private fun DrawScope.body(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float = 0f) {
        withTransform({ rotate(deg(rot), Offset(cx, cy)) }) {
            val shape = Path().apply { addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry)) }
            drawPath(shape, White)
            clipPath(shape) {
                oval(cx + rx * .5f, cy - ry * .55f, rx * .6f, ry * .45f, Black)
                oval(cx - rx * .45f, cy + ry * .2f, rx * .28f, ry * .22f, Orange)
            }
        }
    }

    fun DrawScope.paw(x: Float, y: Float) {
        oval(x, y, 26f, 15f, White)
        drawOval(Fur, Offset(x - 26f, y - 15f), Size(52f, 30f), style = Stroke(2.5f))
    }

    private fun DrawScope.shadow(x: Float, w: Float) = oval(x, 0f, w, 16f, Color.Black, 0.25f)

    /** A whole Sushi standing on (x, y), [s] times her usual size (about 330 tall sitting). */
    fun DrawScope.cat(x: Float, y: Float, s: Float, c: Cat) {
        withTransform({ translate(x, y); scale(s * (if (c.flip) -1f else 1f), s * c.squash, Offset.Zero) }) {
            var hx = 0f; var hy = -215f; var hs = .62f
            val sw = c.tailSw
            when (c.pose) {
                Pose.SIT -> {
                    shadow(0f, 105f)
                    tail(55f, -25f, 175f, -30f + sw * 10f, 150f + sw * 40f, -150f, 20f, 14f)
                    body(0f, -95f, 88f, 100f)
                    paw(-32f, -12f); paw(32f, -12f)
                    if (c.lift > 0f) paw(-30f + c.lift * 20f, -12f - c.lift * 170f)
                }
                Pose.CROUCH -> {
                    shadow(10f, 135f)
                    val w = c.wig
                    tail(120f, -80f + w, 190f, -120f, 205f + sw * 25f, -200f, 18f, 13f)
                    body(25f + w * .3f, -60f + w, 118f, 52f, -0.12f)
                    paw(-115f, -10f); paw(-75f, -8f)
                    hx = -105f; hy = -100f; hs = .58f
                }
                Pose.JUMP -> Unit // drawn below, turned as one piece
                Pose.LOAF -> {
                    shadow(0f, 175f)
                    val b = 1f + 0.03f * sin(c.t * 2.2f)
                    body(10f, -62f * b, 160f, 68f * b)
                    tail(155f, -25f, 130f, 22f, -50f, -4f, 17f, 13f)
                    hx = -112f; hy = -95f; hs = .58f
                }
                Pose.STRETCH -> {
                    shadow(0f, 160f)
                    tail(140f, -120f, 200f, -190f, 175f + sw * 30f, -260f, 17f, 12f)
                    body(30f, -85f, 145f, 46f, 0.32f)
                    paw(-170f, -10f); paw(-125f, -8f); paw(130f, -12f)
                    hx = -125f; hy = -62f; hs = .56f
                }
            }
            if (c.pose == Pose.JUMP) {
                withTransform({ rotate(deg(-0.22f), Offset.Zero) }) {
                    tail(125f, -70f, 220f, -95f, 290f, -80f, 18f, 12f)
                    body(0f, -70f, 135f, 46f)
                    paw(-175f, -55f); paw(-155f, -35f); paw(120f, -35f); paw(140f, -52f)
                    withTransform({ translate(-140f, -95f); rotate(deg(c.tilt), Offset.Zero); scale(.58f, .58f, Offset.Zero) }) { face(c.face) }
                }
            } else {
                withTransform({ translate(hx, hy); rotate(deg(c.tilt), Offset.Zero); scale(hs, hs, Offset.Zero) }) { face(c.face) }
            }
        }
    }
}

/** A short sound for SushiTV's animations. Without it (any error) the picture simply plays on its own. */
internal class SushiSound(context: Context, resId: Int) {
    private var player: MediaPlayer? = runCatching { MediaPlayer.create(context, resId) }.getOrNull()

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
