package figueroa.enrique.reproducers.ui.player

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SpectrumVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val numBars = 22
    private val numSegments = 32

    private val barHeights = FloatArray(numBars)
    private val barTargets = FloatArray(numBars)
    private val peakHeights = FloatArray(numBars)
    private val peakVelocities = FloatArray(numBars)

    private val backgroundPaint = Paint().apply {
        color = "#0E0E10".toColorInt()
        style = Paint.Style.FILL
    }

    private val unlitSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1C1C20".toColorInt()
        style = Paint.Style.FILL
    }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFFFFF".toColorInt()
        style = Paint.Style.FILL
    }

    private var barWidth = 0f
    private var barGap = 0f
    private var segmentHeight = 0f
    private var segmentGap = 0f

    private var animRunnable: Runnable? = null

    init {
        animRunnable = object : Runnable {
            override fun run() {
                updateBarsAnimation()
                invalidate()
                postDelayed(this, 16)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animRunnable?.let { post(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animRunnable?.let { removeCallbacks(it) }
    }

    fun reset() {
        barHeights.fill(0f)
        barTargets.fill(0f)
        peakHeights.fill(0f)
        peakVelocities.fill(0f)
        invalidate()
    }

    fun updateFft(fft: ByteArray) {
        if (fft.size < 2) return
        val n = fft.size / 2
        val magnitudes = FloatArray(n)
        magnitudes[0] = hypot(fft[0].toFloat(), fft[1].toFloat())
        for (i in 1 until n) {
            val r = fft[2 * i].toFloat()
            val im = fft[2 * i + 1].toFloat()
            magnitudes[i] = hypot(r, im)
        }

        // Mapeo logarítmico de bandas de frecuencia
        for (b in 0 until numBars) {
            val startIdx = (2.0.pow((b.toDouble() / numBars) * 6.0) - 1).toInt()
            val endIdx = (2.0.pow(((b + 1).toDouble() / numBars) * 6.0) - 1).toInt()
            val s = min(max(startIdx, 0), n - 1)
            val e = min(max(endIdx, s + 1), n)

            var sum = 0f
            var count = 0
            for (i in s until e) {
                sum += magnitudes[i]
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            // Normalizar a rango 0..1
            var norm = (avg / 90f)
            if (norm > 1f) norm = 1f
            if (norm < 0.02f) norm = 0f

            barTargets[b] = norm
        }
    }

    private fun updateBarsAnimation() {
        for (i in 0 until numBars) {
            // Suavizado de altura de barra
            if (barTargets[i] > barHeights[i]) {
                barHeights[i] = barHeights[i] * 0.35f + barTargets[i] * 0.65f
            } else {
                barHeights[i] = barHeights[i] * 0.82f + barTargets[i] * 0.18f
            }

            // Animación de picos flotantes con gravedad
            if (barHeights[i] >= peakHeights[i]) {
                peakHeights[i] = barHeights[i]
                peakVelocities[i] = 0.003f
            } else {
                peakHeights[i] -= peakVelocities[i]
                peakVelocities[i] += 0.005f
                if (peakHeights[i] < barHeights[i]) {
                    peakHeights[i] = barHeights[i]
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        barGap = w * 0.012f
        barWidth = (w - (numBars + 1) * barGap) / numBars
        segmentGap = h * 0.005f
        segmentHeight = (h - (numSegments + 1) * segmentGap) / numSegments
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Fondo oscuro
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        // Reservar los últimos segmentos para margen superior y picos flotantes
        val maxActiveSegments = numSegments - 3

        for (b in 0 until numBars) {
            val left = barGap + b * (barWidth + barGap)
            val right = left + barWidth

            val fillRatio = barHeights[b]
            val activeSegments = (fillRatio * maxActiveSegments).toInt().coerceIn(0, maxActiveSegments)

            val peakRatio = peakHeights[b]
            val peakSeg = (peakRatio * (numSegments - 1)).toInt().coerceIn(0, numSegments - 1)
            val effectivePeakSeg = max(peakSeg, activeSegments)

            // Dibujar la columna completa de bloques de matriz LED
            for (s in 0 until numSegments) {
                val bottom = h - (s * (segmentHeight + segmentGap) + segmentGap)
                val top = bottom - segmentHeight

                if (s < activeSegments) {
                    // Segmento encendido (degradado rojo / naranja)
                    val ratio = s.toFloat() / maxActiveSegments
                    segmentPaint.color = when {
                        ratio > 0.8f -> "#FF7043".toColorInt()
                        ratio > 0.5f -> "#FF5252".toColorInt()
                        else -> "#E53935".toColorInt()
                    }
                    canvas.drawRect(left, top, right, bottom, segmentPaint)
                } else if (s == effectivePeakSeg && peakRatio > 0.02f) {
                    // Pico flotante (segmento blanco)
                    canvas.drawRect(left, top, right, bottom, peakPaint)
                } else {
                    // Segmento apagado (fondo de cuadrícula LED)
                    canvas.drawRect(left, top, right, bottom, unlitSegmentPaint)
                }
            }
        }
    }
}
