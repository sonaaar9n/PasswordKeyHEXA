package com.mykey.passwords

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import java.util.Random

/**
 * Nothing OS jaisa dot-matrix launch animation. Puri screen ek dot grid hai.
 *
 *  0.0s - 1.1s  WAVE    : center se ek roshan ring bahar failti hai, peeche halke dots bachte hain
 *  0.6s - 1.7s  FORM    : kuch dots flicker karke jalte hain aur "PASS / KEY." likh dete hain
 *  1.7s - 2.2s  BLINK   : laal (red) dot teen baar blink karta hai (Nothing ka LED feel)
 *  2.2s - 3.0s  DISSOLVE: dots center se bahar ki taraf random bujhte hain, neeche app dikhti hai
 *
 * Koi image / video nahi: sab Canvas par circles hain, isliye halka aur sharp rehta hai.
 */
class DotSplashView(
    ctx: Context,
    private val onReveal: () -> Unit,
    private val onDone: () -> Unit
) : View(ctx) {

    companion object {
        const val TOTAL = 3000f
        const val REVEAL_AT = 2200f

        // 5 x 7 dot-matrix letters. '#' = dot jalega
        private val GLYPHS = mapOf(
            'P' to arrayOf("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
            'A' to arrayOf(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
            'S' to arrayOf(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
            'K' to arrayOf("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
            'E' to arrayOf("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
            'Y' to arrayOf("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..")
        )
        private val LINES = arrayOf("PASS", "KEY")
        private const val BLOCK_COLS = 23 // "PASS" = 4*5 + 3 gap
        private const val BLOCK_ROWS = 16 // 7 + 2 gap + 7
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val red = Theme.ACCENT

    private var cols = 0
    private var rows = 0
    private var cell = 0f
    private var ox = 0f
    private var oy = 0f

    private lateinit var kind: IntArray      // 0 = ambient, 1 = text dot, 2 = red dot
    private lateinit var rnd: FloatArray     // har dot ka random number (0..1)
    private lateinit var dist: FloatArray    // center se doori (0..1)
    private lateinit var appearAt: FloatArray // text dots kab jalenge (ms)

    private var t = 0f
    private var revealed = false
    private var animator: ValueAnimator? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true // touch neeche na jaye
        setOnClickListener { skip() }
    }

    /** Tap karne par seedha dissolve par jao. */
    private fun skip() {
        val a = animator ?: return
        if (a.currentPlayTime < REVEAL_AT - 60f) a.currentPlayTime = (REVEAL_AT - 60f).toLong()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        cell = (w * 0.84f) / BLOCK_COLS
        cols = (w / cell).toInt()
        rows = (h / cell).toInt()
        ox = (w - cols * cell) / 2f
        oy = (h - rows * cell) / 2f

        val n = cols * rows
        kind = IntArray(n)
        rnd = FloatArray(n)
        dist = FloatArray(n)
        appearAt = FloatArray(n)

        val r = Random(7)
        val cx = (cols - 1) / 2f
        val cy = (rows - 1) / 2f
        val maxD = sqrt(cx * cx + cy * cy)
        for (y in 0 until rows) for (x in 0 until cols) {
            val i = y * cols + x
            rnd[i] = r.nextFloat()
            dist[i] = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxD
        }

        // Text mask: "PASS" upar, "KEY" neeche + laal full-stop
        val col0 = (cols - BLOCK_COLS) / 2
        val row0 = (rows - BLOCK_ROWS) / 2 - 2
        for ((li, line) in LINES.withIndex()) {
            val baseRow = row0 + li * 9
            for ((ci, ch) in line.withIndex()) {
                val g = GLYPHS[ch] ?: continue
                for (gy in 0 until 7) for (gx in 0 until 5) {
                    if (g[gy][gx] == '#') {
                        val x = col0 + ci * 6 + gx
                        val y = baseRow + gy
                        if (x in 0 until cols && y in 0 until rows) kind[y * cols + x] = 1
                    }
                }
            }
        }
        val dotX = col0 + 18
        val dotY = row0 + 9 + 6
        if (dotX in 0 until cols && dotY in 0 until rows) kind[dotY * cols + dotX] = 2

        // Text dots left -> right sweep + thoda random, taaki "likhte hue" lage
        for (i in 0 until n) {
            if (kind[i] != 0) {
                val x = (i % cols - col0).coerceIn(0, BLOCK_COLS) / BLOCK_COLS.toFloat()
                appearAt[i] = 600f + x * 500f + rnd[i] * 350f
            }
        }

        if (animator != null) return // size badle (jaise keyboard) to animation dobara shuru na ho
        animator = ValueAnimator.ofFloat(0f, TOTAL).apply {
            duration = TOTAL.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                t = it.animatedValue as Float
                if (!revealed && t >= REVEAL_AT) {
                    revealed = true
                    onReveal()
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!revealed) { revealed = true; onReveal() }
                    onDone()
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.removeAllListeners()
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun clamp01(v: Float) = max(0f, min(1f, v))

    override fun onDraw(canvas: Canvas) {
        if (cols == 0) return

        // Dissolve progress (0..1)
        val dt = clamp01((t - REVEAL_AT) / (TOTAL - REVEAL_AT))

        // Background: dissolve ke dauran black fade hota hai, neeche ki app dikhti hai
        val bgA = 1f - clamp01((dt - 0.05f) / 0.75f)
        canvas.drawColor(Color.argb((bgA * 255).toInt(), 0, 0, 0))

        // Wave ring ki position
        val ring = t / 1000f * 1.5f

        // Ambient dots ki halki roshni: wave ke baad 0.16 -> form ke baad 0.06
        val ambient = 0.16f - 0.10f * clamp01((t - 900f) / 700f)

        // Red dot blink (1.7s - 2.2s), uske baad solid
        val blinkOn = if (t < 1700f) 1f
        else if (t < REVEAL_AT) (if (((t - 1700f) / 125f).toInt() % 2 == 0) 1f else 0.1f)
        else 1f

        val maxR = cell * 0.5f
        for (y in 0 until rows) {
            val cy = oy + (y + 0.5f) * cell
            for (x in 0 until cols) {
                val i = y * cols + x
                val d = dist[i]
                var a = 0f
                var radius = cell * 0.16f
                var color = Color.WHITE

                // 1) wave: ring ke andar ambient, ring par chamak
                if (d <= ring) a = ambient
                val glow = clamp01(1f - abs(d - ring) / 0.10f)
                if (glow > 0f && t < 1500f) {
                    a = max(a, glow)
                    radius += cell * 0.16f * glow
                }

                // 2) text / red dots
                if (kind[i] != 0) {
                    val local = t - appearAt[i]
                    if (local >= 0f) {
                        val flick = if (local < 180f) (if ((local / 45f).toInt() % 2 == 0) 1f else 0.25f) else 1f
                        a = max(a, flick)
                        radius = cell * 0.36f + (if (local < 180f) cell * 0.08f else 0f)
                    }
                    if (kind[i] == 2 && local >= 0f) {
                        color = red
                        a *= blinkOn
                        radius = cell * 0.40f
                    }
                }

                // 3) dissolve: center se bahar, random order me bujhte hain
                if (dt > 0f) {
                    val thr = 0.5f * rnd[i] + 0.5f * d
                    val k = clamp01((dt * 1.4f - thr) / 0.4f)
                    a *= (1f - k)
                    radius *= (1f - 0.5f * k)
                }

                if (a <= 0.01f) continue
                paint.color = color
                paint.alpha = (clamp01(a) * 255).toInt()
                canvas.drawCircle(ox + (x + 0.5f) * cell, cy, min(radius, maxR), paint)
            }
        }
    }
}
