package figueroa.enrique.reproducers.ui.player

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class VuMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var leftTarget = 0f
    private var rightTarget = 0f

    private var leftNeedleAngle = -36f
    private var rightNeedleAngle = -36f

    private var leftVelocity = 0f
    private var rightVelocity = 0f

    private val bgPaint = Paint().apply {
        color = "#121212".toColorInt()
        style = Paint.Style.FILL
    }

    private val meterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FAF8F5".toColorInt()
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#222222".toColorInt()
        style = Paint.Style.STROKE
    }

    private val arcBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A1A1A".toColorInt()
        style = Paint.Style.STROKE
    }

    private val arcRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E53935".toColorInt()
        style = Paint.Style.STROKE
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A1A1A".toColorInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A1A1A".toColorInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A1A1A".toColorInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#444444".toColorInt()
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    private val channelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#D32F2F".toColorInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.LEFT
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#111111".toColorInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1A1A1A".toColorInt()
        style = Paint.Style.FILL
    }

    private val animRunnable = object : Runnable {
        override fun run() {
            updatePhysics()
            invalidate()
            postDelayed(this, 16)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(animRunnable)
    }

    fun updateWaveform(waveform: ByteArray) {
        if (waveform.isEmpty()) return

        var sumL = 0.0
        var sumR = 0.0
        val half = waveform.size / 2

        for (i in 0 until half) {
            val v = (waveform[i].toInt() and 0xFF) - 128
            sumL += (v * v).toDouble()
        }
        for (i in half until waveform.size) {
            val v = (waveform[i].toInt() and 0xFF) - 128
            sumR += (v * v).toDouble()
        }

        val rmsL = sqrt(sumL / half) / 128.0
        val rmsR = sqrt(sumR / (waveform.size - half)) / 128.0

        leftTarget = min(1.0, rmsL * 3.2).toFloat()
        rightTarget = min(1.0, rmsR * 3.2).toFloat()
    }

    private fun updatePhysics() {
        // Balística amortiguada de aguja VU
        val spring = 0.18f
        val damping = 0.72f

        val targetAngleL = -36f + leftTarget * 54f
        val forceL = (targetAngleL - leftNeedleAngle) * spring
        leftVelocity = (leftVelocity + forceL) * damping
        leftNeedleAngle += leftVelocity

        val targetAngleR = -36f + rightTarget * 54f
        val forceR = (targetAngleR - rightNeedleAngle) * spring
        rightVelocity = (rightVelocity + forceR) * damping
        rightNeedleAngle += rightVelocity
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Fondo oscuro del contenedor
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val padding = min(w, h) * 0.04f

        val maxMeterWidth = (w - padding * 3) / 2f
        val maxMeterHeight = h - padding * 2

        val side = min(maxMeterWidth, maxMeterHeight)

        if (side <= 0f) return

        val totalMetersWidth = side * 2 + padding
        val startX = (w - totalMetersWidth) / 2f
        val startY = (h - side) / 2f

        // Dibujar VU meter Izquierdo (L)
        drawSingleMeter(canvas, startX, startY, side, leftNeedleAngle, "L")

        // Dibujar VU meter Derecho (R)
        drawSingleMeter(canvas, startX + side + padding, startY, side, rightNeedleAngle, "R")
    }

    private fun drawSingleMeter(
        canvas: Canvas,
        x: Float,
        y: Float,
        side: Float,
        angleDeg: Float,
        channelName: String
    ) {
        val cornerRadius = side * 0.06f
        val rect = RectF(x, y, x + side, y + side)

        // Fondo blanco vintage del vúmetro y borde redondeado
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, meterBgPaint)

        borderPaint.strokeWidth = side * 0.018f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val centerX = x + side / 2f
        val pivotY = y + side * 0.80f
        val arcRadius = side * 0.48f

        // Indicador de canal (L / R)
        channelPaint.textSize = side * 0.08f
        canvas.drawText(channelName, x + side * 0.08f, y + side * 0.16f, channelPaint)

        // Textos del usuario
        labelPaint.textSize = side * 0.08f
        canvas.drawText("dB", centerX, y + side * 0.16f, labelPaint)

        labelPaint.textSize = side * 0.11f
        canvas.drawText("VU", centerX, y + side * 0.54f, labelPaint)

        subTextPaint.textSize = side * 0.048f
        canvas.drawText("MR Reproducer.", centerX, y + side * 0.65f, subTextPaint)

        // Arco de escala centrado en el pivote de la aguja
        val arcOval = RectF(
            centerX - arcRadius,
            pivotY - arcRadius,
            centerX + arcRadius,
            pivotY + arcRadius
        )

        arcBlackPaint.strokeWidth = side * 0.022f
        arcRedPaint.strokeWidth = side * 0.026f

        // Arco negro (-20 a 0 dB)
        canvas.drawArc(arcOval, 234f, 36f, false, arcBlackPaint)
        // Arco rojo (0 a +3 dB)
        canvas.drawArc(arcOval, 270f, 18f, false, arcRedPaint)

        // Marcas y números de la escala
        val ticks = listOf(
            Pair(-36f, "20"),
            Pair(-26f, "7"),
            Pair(-18f, "5"),
            Pair(-11f, "3"),
            Pair(-5f, "1"),
            Pair(0f, "0"),
            Pair(6f, "1"),
            Pair(12f, "2"),
            Pair(18f, "3")
        )

        val innerR = arcRadius - side * 0.02f
        val outerR = arcRadius + side * 0.025f
        val textR = arcRadius + side * 0.07f

        textPaint.textSize = side * 0.052f

        for ((tickAngle, label) in ticks) {
            val rad = Math.toRadians((tickAngle - 90).toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()

            val x1 = centerX + innerR * cosA
            val y1 = pivotY + innerR * sinA
            val x2 = centerX + outerR * cosA
            val y2 = pivotY + outerR * sinA

            val isRed = tickAngle > 0f
            tickPaint.color = if (isRed) "#E53935".toColorInt() else "#1A1A1A".toColorInt()
            tickPaint.strokeWidth = if (tickAngle == 0f || tickAngle == -36f || tickAngle == 18f) side * 0.014f else side * 0.008f
            canvas.drawLine(x1, y1, x2, y2, tickPaint)

            val tx = centerX + textR * cosA
            val ty = pivotY + textR * sinA
            textPaint.color = if (isRed) "#E53935".toColorInt() else "#1A1A1A".toColorInt()
            canvas.drawText(label, tx, ty + side * 0.018f, textPaint)
        }

        // Signos - y +
        val radMinus = Math.toRadians((-42 - 90).toDouble())
        val xMinus = centerX + textR * cos(radMinus).toFloat()
        val yMinus = pivotY + textR * sin(radMinus).toFloat()
        textPaint.color = "#1A1A1A".toColorInt()
        canvas.drawText("-", xMinus, yMinus + side * 0.018f, textPaint)

        val radPlus = Math.toRadians((24 - 90).toDouble())
        val xPlus = centerX + textR * cos(radPlus).toFloat()
        val yPlus = pivotY + textR * sin(radPlus).toFloat()
        textPaint.color = "#E53935".toColorInt()
        canvas.drawText("+", xPlus, yPlus + side * 0.018f, textPaint)

        // Aguja
        val needleLen = arcRadius + side * 0.01f
        val needleRad = Math.toRadians((angleDeg - 90).toDouble())
        val needleX = centerX + needleLen * cos(needleRad).toFloat()
        val needleY = pivotY + needleLen * sin(needleRad).toFloat()

        needlePaint.strokeWidth = side * 0.012f
        canvas.drawLine(centerX, pivotY, needleX, needleY, needlePaint)

        // Pivote semicircular en la base
        val pivotRadius = side * 0.09f
        val pivotRect = RectF(
            centerX - pivotRadius,
            pivotY - pivotRadius * 0.5f,
            centerX + pivotRadius,
            pivotY + pivotRadius * 0.7f
        )
        canvas.drawArc(pivotRect, 180f, 180f, true, pivotPaint)
    }
}
