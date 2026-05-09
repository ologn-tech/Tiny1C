package tech.ologn.tiny1c.thermal

import android.graphics.Matrix
import android.view.MotionEvent
import android.widget.ImageView

/**
 * Infisense Tiny1C composite USB frame: one YUYV image stacked with a 16-bit temperature map
 * (same layout as the original Thermography app — see `TemperatureMap` / `PhotoFragment` raw decode).
 *
 * For 256×384 YUYV, the buffer is [top: 256×192 YUYV][bottom: 256×192 temperature].
 */
object InfisenseThermal {

    /**
     * @return pair of (yuyvOnly, temperatureRaw) or null if [fullFrame] is not a composite layout.
     */
    fun splitCompositeIfPresent(
        previewWidth: Int,
        previewHeight: Int,
        fullFrame: ByteArray
    ): Pair<ByteArray, ByteArray>? {
        if (previewWidth <= 0 || previewHeight <= 1 || previewHeight % 2 != 0) return null
        val expected = previewWidth * previewHeight * 2
        if (fullFrame.size != expected) return null
        val halfPixels = previewWidth * (previewHeight / 2)
        val halfBytes = halfPixels * 2
        if (halfBytes * 2 != fullFrame.size) return null
        val yuyv = fullFrame.copyOfRange(0, halfBytes)
        val temp = fullFrame.copyOfRange(halfBytes, fullFrame.size)
        return yuyv to temp
    }

    /** Thermal grid size after split (matches Thermography `cameraWidth × (cameraHeight/2)`). */
    fun thermalSize(previewWidth: Int, previewHeight: Int): Pair<Int, Int> =
        previewWidth to (previewHeight / 2)

    /**
     * Decode one pixel from the raw temperature map (Kelvin × 64 → °C), same as stock
     * `((map[i+1]<<8)|map[i]) / 64.0 - 273.15`.
     */
    fun celsiusAt(tempMap: ByteArray, width: Int, x: Int, y: Int): Float {
        if (x !in 0 until width) return Float.NaN
        val height = tempMap.size / (width * 2)
        if (y !in 0 until height) return Float.NaN
        val i = (y * width + x) * 2
        val raw = (tempMap[i].toInt() and 0xff) or ((tempMap[i + 1].toInt() and 0xff) shl 8)
        return raw / 64.0f - 273.15f
    }

    /**
     * Maps a touch on an [ImageView] (any scale type) to bitmap pixel coordinates using
     * [ImageView.getImageMatrix] — [Drawable.copyBounds] does not match screen placement for
     * `fitCenter`, so touch tests against bounds often fail silently.
     */
    fun touchToBitmapCoords(
        view: ImageView,
        bitmapWidth: Int,
        bitmapHeight: Int,
        event: MotionEvent
    ): Pair<Int, Int>? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
        if (view.drawable == null) return null
        val inverse = Matrix()
        if (!view.imageMatrix.invert(inverse)) return null
        val pts = floatArrayOf(event.x, event.y)
        inverse.mapPoints(pts)
        val bx = pts[0].toInt().coerceIn(0, bitmapWidth - 1)
        val by = pts[1].toInt().coerceIn(0, bitmapHeight - 1)
        return bx to by
    }

    /** Bitmap pixel center → view coords via [ImageView.getImageMatrix] (inverse of [touchToBitmapCoords]). */
    fun bitmapPixelCenterToView(
        view: ImageView,
        bitmapWidth: Int,
        bitmapHeight: Int,
        bx: Int,
        by: Int
    ): Pair<Float, Float>? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return null
        if (bx !in 0 until bitmapWidth || by !in 0 until bitmapHeight) return null
        if (view.drawable == null) return null
        val pts = floatArrayOf(bx + 0.5f, by + 0.5f)
        view.imageMatrix.mapPoints(pts)
        return pts[0] to pts[1]
    }
}
