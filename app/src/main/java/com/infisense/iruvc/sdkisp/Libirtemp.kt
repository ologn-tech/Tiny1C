package com.infisense.iruvc.sdkisp

/**
 * JNI bindings for Infisense `libirtemp.so` (USB_sample temperature module).
 *
 * Native arrays: frame is `byte[]` (`GetByteArrayElements`); point output is `char[]`
 * (`SetCharArrayRegion`, uint16 Kelvin×64). Rect results are written onto [TempInfo_t].
 */
object Libirtemp {
    const val IRTEMP_SUCCESS = 0
    const val IRTEMP_ERROR_PARAM = -1

    init {
        System.loadLibrary("irtemp")
    }

    /** Resolution of a temperature frame (`TempDataRes_t`). Fields are Java `char` (uint16). */
    class TempDataRes_t {
        @JvmField var width: Char = 0.toChar()
        @JvmField var height: Char = 0.toChar()
    }

    /** Pixel coordinate (`Dot_t`), origin top-left, axis from 0. */
    class Dot_t {
        @JvmField var x: Int = 0
        @JvmField var y: Int = 0
    }

    /** Rectangle (`Area_t`) for [get_rect_temp]. Fields are Java `int`. */
    class Area_t {
        @JvmField var start_x: Int = 0
        @JvmField var start_y: Int = 0
        @JvmField var width: Int = 0
        @JvmField var height: Int = 0
    }

    /**
     * Rectangle temperature (`TempInfo_t`). [max_cord] / [min_cord] must be non-null;
     * native `setTempInfo` writes into those objects.
     */
    class TempInfo_t {
        @JvmField var max_temp: Char = 0.toChar()
        @JvmField var min_temp: Char = 0.toChar()
        @JvmField var avr_temp: Char = 0.toChar()
        @JvmField var max_cord: Dot_t = Dot_t()
        @JvmField var min_cord: Dot_t = Dot_t()
    }

    data class FrameTemp(
        val maxCelsius: Float,
        val minCelsius: Float,
        val avgCelsius: Float,
        val maxX: Int,
        val maxY: Int,
        val minX: Int,
        val minY: Int,
    )

    /**
     * Temperature of one pixel from a temperature frame.
     *
     * @param src little-endian temperature map bytes (USB_sample `temp_frame`)
     * @param dst length-1 output: raw value (Kelvin × 64) as unsigned 16-bit in a `char`
     */
    @JvmStatic
    external fun get_point_temp(
        src: ByteArray,
        tempRes: TempDataRes_t,
        point: Dot_t,
        dst: CharArray
    ): Int

    /**
     * Max / min / average temperature of a rectangle (USB_sample `rect_temp_demo`).
     * Pass the full frame size in [rect] for whole-frame stats.
     */
    @JvmStatic
    external fun get_rect_temp(
        src: ByteArray,
        tempRes: TempDataRes_t,
        rect: Area_t,
        tempInfo: TempInfo_t
    ): Int

    @JvmStatic
    external fun irtemp_version(): String?

    /** Same as USB_sample `temp_value_converter`. */
    fun tempValueToCelsius(tempVal: Int): Float = (tempVal and 0xffff) / 64f - 273.15f

    /**
     * Point temperature in °C, or [Float.NaN] on failure.
     * Equivalent to USB_sample `point_temp_demo` + `temp_value_converter`.
     */
    fun celsiusAt(tempMap: ByteArray, width: Int, height: Int, x: Int, y: Int): Float {
        if (width <= 0 || height <= 0) return Float.NaN
        if (tempMap.size < width * height * 2) return Float.NaN
        if (x !in 0 until width || y !in 0 until height) return Float.NaN
        val dst = CharArray(1)
        val err =
            get_point_temp(
                tempMap,
                TempDataRes_t().apply {
                    this.width = width.toChar()
                    this.height = height.toChar()
                },
                Dot_t().apply {
                    this.x = x
                    this.y = y
                },
                dst
            )
        if (err != IRTEMP_SUCCESS) return Float.NaN
        return tempValueToCelsius(dst[0].code)
    }

    /**
     * Full-frame max / min / average via [get_rect_temp] over the entire map
     * (USB_sample rectangle covering `temp_info.width × temp_info.height`).
     */
    fun fullFrameTemp(tempMap: ByteArray, width: Int, height: Int): FrameTemp? {
        if (width <= 0 || height <= 0) return null
        if (tempMap.size < width * height * 2) return null
        val info =
            TempInfo_t().apply {
                max_cord = Dot_t()
                min_cord = Dot_t()
            }
        val err =
            get_rect_temp(
                tempMap,
                TempDataRes_t().apply {
                    this.width = width.toChar()
                    this.height = height.toChar()
                },
                Area_t().apply {
                    start_x = 0
                    start_y = 0
                    this.width = width
                    this.height = height
                },
                info
            )
        if (err != IRTEMP_SUCCESS) return null
        return FrameTemp(
            maxCelsius = tempValueToCelsius(info.max_temp.code),
            minCelsius = tempValueToCelsius(info.min_temp.code),
            avgCelsius = tempValueToCelsius(info.avr_temp.code),
            maxX = info.max_cord.x,
            maxY = info.max_cord.y,
            minX = info.min_cord.x,
            minY = info.min_cord.y,
        )
    }

    /** Decode every pixel of the temperature map to °C (row-major). */
    fun toCelsiusFrame(tempMap: ByteArray, width: Int, height: Int): FloatArray? {
        val pixels = width * height
        if (width <= 0 || height <= 0 || tempMap.size < pixels * 2) return null
        val out = FloatArray(pixels)
        var bi = 0
        for (i in 0 until pixels) {
            val raw =
                (tempMap[bi].toInt() and 0xff) or
                    ((tempMap[bi + 1].toInt() and 0xff) shl 8)
            out[i] = tempValueToCelsius(raw)
            bi += 2
        }
        return out
    }
}
