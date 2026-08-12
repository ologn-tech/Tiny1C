package tech.ologn.tiny1c.thermal

import android.graphics.Matrix
import android.view.MotionEvent
import android.widget.ImageView
import com.infisense.iruvc.sdkisp.Libirtemp

/**
 * Infisense Tiny1C composite USB frame: one YUYV image stacked with a 16-bit temperature map
 * (USB_sample camera module: raw frame → image + temp; see README §3.1 / `raw_data_cut`).
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

    /** Thermal grid size after split (matches USB_sample `temp_info.width × temp_info.height`). */
    fun thermalSize(previewWidth: Int, previewHeight: Int): Pair<Int, Int> =
        previewWidth to (previewHeight / 2)

    /**
     * Point temperature via libirtemp [Libirtemp.get_point_temp] (USB_sample `point_temp_demo`).
     */
    fun celsiusAt(tempMap: ByteArray, width: Int, height: Int, x: Int, y: Int): Float =
        Libirtemp.celsiusAt(tempMap, width, height, x, y)

    /**
     * Full-frame max / min / average via [Libirtemp.get_rect_temp] over the whole map.
     */
    fun fullFrameTemp(
        tempMap: ByteArray,
        width: Int,
        height: Int
    ): Libirtemp.FrameTemp? = Libirtemp.fullFrameTemp(tempMap, width, height)

    /** Every pixel of the temperature frame in °C, row-major (`y * width + x`). */
    fun toCelsiusFrame(tempMap: ByteArray, width: Int, height: Int): FloatArray? =
        Libirtemp.toCelsiusFrame(tempMap, width, height)

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
