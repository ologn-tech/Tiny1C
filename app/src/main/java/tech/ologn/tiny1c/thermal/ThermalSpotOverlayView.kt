package tech.ologn.tiny1c.thermal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Temperature markers on the preview: spot, plus full-frame max / min.
 */
class ThermalSpotOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Marker(
        val viewX: Float,
        val viewY: Float,
        val text: String,
        val fillColor: Int,
    )

    private val density = resources.displayMetrics.density
    private val textGapAboveDot = 4f * density
    private val dotRadius = 5f * density

    private val paintDotFill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val paintDotStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            color = Color.argb(200, 0, 0, 0)
        }
    private val paintText =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 16f * density
            isFakeBoldText = true
            setShadowLayer(3f * density, 0f, 1f * density, Color.argb(160, 0, 0, 0))
        }

    private var markers: List<Marker> = emptyList()

    fun setMarkers(newMarkers: List<Marker>) {
        markers = newMarkers
        invalidate()
    }

    fun setSpotAtViewCoords(viewX: Float, viewY: Float, text: String) {
        setMarkers(
            listOf(
                Marker(viewX, viewY, text, Color.argb(220, 255, 255, 255))
            )
        )
    }

    fun clearSpot() {
        markers = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (marker in markers) {
            drawMarker(canvas, marker)
        }
    }

    private fun drawMarker(canvas: Canvas, marker: Marker) {
        paintDotFill.color = marker.fillColor
        canvas.drawCircle(marker.viewX, marker.viewY, dotRadius, paintDotFill)
        canvas.drawCircle(marker.viewX, marker.viewY, dotRadius, paintDotStroke)

        val tw = paintText.measureText(marker.text)
        val fm = paintText.fontMetrics
        val textHeight = fm.descent - fm.ascent
        var left = marker.viewX - tw / 2f
        var top = marker.viewY - dotRadius - textGapAboveDot - textHeight
        left = left.coerceIn(0f, (width - tw).coerceAtLeast(0f))
        top = top.coerceIn(0f, (height - textHeight).coerceAtLeast(0f))
        canvas.drawText(marker.text, left, top - fm.ascent, paintText)
    }
}
