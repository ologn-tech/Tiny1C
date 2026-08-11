package com.infisense.iruvc.sdkisp

/**
 * JNI bindings for Infisense `libirtemp.so` (USB_sample temperature module).
 *
 * Native `get_point_temp` uses `GetByteArrayElements` for the frame and
 * `SetCharArrayRegion` for the output (uint16 Kelvin×64).
 * Then [tempValueToCelsius] (`temp_val / 64 - 273.15`).
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

    /**
     * Temperature of one pixel from a temperature frame.
     *
     * @param src little-endian temperature map bytes (same layout as USB_sample `temp_frame`)
     * @param tempRes frame width/height
     * @param point pixel to sample
     * @param dst length-1 output: raw value (Kelvin × 64) as unsigned 16-bit in a `char`
     * @return [IRTEMP_SUCCESS] or [IRTEMP_ERROR_PARAM]
     */
    @JvmStatic
    external fun get_point_temp(
        src: ByteArray,
        tempRes: TempDataRes_t,
        point: Dot_t,
        dst: CharArray
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
}
