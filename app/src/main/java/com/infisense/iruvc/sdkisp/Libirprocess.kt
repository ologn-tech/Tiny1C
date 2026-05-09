package com.infisense.iruvc.sdkisp

/**
 * Thin JNI binding around the Infisense `libirprocess.so` thermography library.
 *
 * Only the YUYV → ARGB pseudocolor entry point is exposed because that's the only one the app
 * uses; native symbols are resolved lazily on first call, so the unused vendor entry points are
 * safe to omit. If you add another native call, mirror its C symbol name exactly so JNI lookup
 * (`Java_com_infisense_iruvc_sdkisp_Libirprocess_<name>`) succeeds.
 */
object Libirprocess {
    init {
        System.loadLibrary("irprocess")
    }

    @JvmStatic
    external fun yuyv_map_to_argb_pseudocolor(
        input: ByteArray,
        pixelCount: Long,
        mode: Int,
        output: ByteArray
    ): Int
}
