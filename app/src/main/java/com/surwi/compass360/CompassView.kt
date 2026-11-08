package com.surwi.compass360

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CompassView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var heading = 0f
    private val gold = 0xFFFFC65C.toInt()
    private val ivory = 0xFFFFE8BD.toInt()
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFC65C.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD8A84E.toInt(); strokeWidth = 2f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ivory; textAlign = Paint.Align.CENTER; textSize = 21f; typeface = Typeface.create("serif", Typeface.BOLD) }
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE87535.toInt() }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ivory }

    fun setHeading(value: Float) { heading = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.43f

        // Outer engraved rings and soft inner halo.
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawCircle(cx, cy, radius * 0.72f, innerPaint)
        canvas.drawCircle(cx, cy, radius * 0.52f, innerPaint)

        // Large embossed Swastik watermark, kept inside the compass face.
        val symbolSize = radius * 1.42f
        val symbol = "卐"
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = symbolSize
            typeface = Typeface.create("serif", Typeface.BOLD)
            alpha = 105
            style = Paint.Style.FILL
            shader = LinearGradient(cx, cy - symbolSize * 0.48f, cx, cy + symbolSize * 0.30f,
                intArrayOf(0xFFFFE5A0.toInt(), 0xFFD99A35.toInt(), 0xFF8B4A16.toInt()), null, Shader.TileMode.CLAMP)
            setShadowLayer(8f, 3f, 5f, 0xCC000000.toInt())
        }
        canvas.drawText(symbol, cx, cy + symbolSize * 0.34f, symbolPaint)
        symbolPaint.clearShadowLayer()
        symbolPaint.shader = null
        symbolPaint.style = Paint.Style.STROKE
        symbolPaint.strokeWidth = 1.4f
        symbolPaint.color = 0x88FFE4A0.toInt()
        canvas.drawText(symbol, cx, cy + symbolSize * 0.34f, symbolPaint)

        // Rotating azimuth dial; fixed pointer remains at the top of the screen.
        canvas.save()
        canvas.rotate(-heading, cx, cy)
        for (i in 0 until 72) {
            val angle = Math.toRadians(i * 5.0 - 90.0)
            val r1 = radius - if (i % 9 == 0) 20f else if (i % 3 == 0) 12f else 7f
            val r2 = radius - 2f
            canvas.drawLine(cx + (cos(angle) * r1).toFloat(), cy + (sin(angle) * r1).toFloat(),
                cx + (cos(angle) * r2).toFloat(), cy + (sin(angle) * r2).toFloat(), tickPaint)
        }
        val labels = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        for (i in labels.indices) {
            val angle = Math.toRadians(i * 45.0 - 90.0)
            val r = radius * 0.79f
            canvas.drawText(labels[i], cx + (cos(angle) * r).toFloat(), cy + (sin(angle) * r).toFloat() + 7f, textPaint)
        }
        canvas.restore()

        // Gold and saffron needle with a small jewel at its pivot.
        val north = Path().apply { moveTo(cx, cy - radius * 0.69f); lineTo(cx - 11f, cy + 8f); lineTo(cx + 11f, cy + 8f); close() }
        val south = Path().apply { moveTo(cx, cy + radius * 0.69f); lineTo(cx - 11f, cy - 8f); lineTo(cx + 11f, cy - 8f); close() }
        canvas.drawPath(north, redPaint)
        canvas.drawPath(south, goldPaint)
        canvas.drawCircle(cx, cy, 7f, goldPaint)
        canvas.drawCircle(cx, cy, 3f, centerPaint)
    }
}
