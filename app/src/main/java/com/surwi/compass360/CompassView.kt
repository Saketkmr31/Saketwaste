package com.surwi.compass360

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CompassView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var heading = 0f
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF526A83.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9FB1C5.toInt(); strokeWidth = 2f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F7FB.toInt(); textAlign = Paint.Align.CENTER; textSize = 22f; isFakeBoldText = true }
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE65353.toInt() }
    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8FA8C2.toInt() }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }

    fun setHeading(value: Float) { heading = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.43f
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawCircle(cx, cy, radius * 0.72f, ringPaint)
        canvas.save()
        canvas.rotate(-heading, cx, cy)
        for (i in 0 until 72) {
            val angle = Math.toRadians(i * 5.0)
            val r1 = radius - if (i % 9 == 0) 19f else 9f
            val r2 = radius - 2f
            canvas.drawLine(cx + (cos(angle) * r1).toFloat(), cy + (sin(angle) * r1).toFloat(),
                cx + (cos(angle) * r2).toFloat(), cy + (sin(angle) * r2).toFloat(), tickPaint)
        }
        val labels = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        for (i in labels.indices) {
            val angle = Math.toRadians(i * 45.0 - 90.0)
            val r = radius * 0.79f
            canvas.drawText(labels[i], cx + (cos(angle) * r).toFloat(), cy + (sin(angle) * r).toFloat() + 8f, textPaint)
        }
        canvas.restore()
        val north = Path().apply { moveTo(cx, cy - radius * 0.69f); lineTo(cx - 13f, cy + 8f); lineTo(cx + 13f, cy + 8f); close() }
        val south = Path().apply { moveTo(cx, cy + radius * 0.69f); lineTo(cx - 13f, cy - 8f); lineTo(cx + 13f, cy - 8f); close() }
        canvas.drawPath(north, redPaint)
        canvas.drawPath(south, bluePaint)
        canvas.drawCircle(cx, cy, 7f, centerPaint)
    }
}
