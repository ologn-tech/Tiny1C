package tech.ologn.tiny1c.thermal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Dot at the tapped pixel plus semi-transparent temperature text above it.
 */
class ThermalSpotOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    /** Space between dot top edge and text (dp above the dot). */
    private val textGapAboveDot = 4f * density
    private val dotRadius = 5f * density

    private val paintDotFill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(220, 255, 255, 255)
        }
    private val paintDotStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            color = Color.argb(200, 0, 0, 0)
        }

    private val paintText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 18f * density
            isFakeBoldText = true
            setShadowLayer(3f * density, 0f, 1f * density, Color.argb(160, 0, 0, 0))
        }

    private var anchorX = 0f
    private var anchorY = 0f
    private var label: String? = null

    fun setSpotAtViewCoords(viewX: Float, viewY: Float, text: String) {
        anchorX = viewX
        anchorY = viewY
        label = text
        invalidate()
    }

    fun clearSpot() {
        label = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val text = label ?: return

        val tw = paintText.measureText(text)
        val fm = paintText.fontMetrics
        val textHeight = fm.descent - fm.ascent

        var left = anchorX - tw / 2f
        // Place text just above the dot (gap measured from dot top to text box bottom).
        var top = anchorY - dotRadius - textGapAboveDot - textHeight

        left = left.coerceIn(0f, (width - tw).coerceAtLeast(0f))
        top = top.coerceIn(0f, (height - textHeight).coerceAtLeast(0f))

        canvas.drawCircle(anchorX, anchorY, dotRadius, paintDotFill)
        canvas.drawCircle(anchorX, anchorY, dotRadius, paintDotStroke)

        val baseline = top - fm.ascent
        canvas.drawText(text, left, baseline, paintText)
    }
}
