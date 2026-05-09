package com.infisense.iruvc.sdkisp;

/* JADX INFO: loaded from: classes.dex */
public class Libirprocess {
    public static final int IPROC_SRC_FMT_YUV422 = 1;
    public static final int IRPROC_COLOR_MODE_0 = 0;
    public static final int IRPROC_COLOR_MODE_1 = 1;
    public static final int IRPROC_COLOR_MODE_10 = 10;
    public static final int IRPROC_COLOR_MODE_11 = 11;
    public static final int IRPROC_COLOR_MODE_12 = 12;
    public static final int IRPROC_COLOR_MODE_13 = 13;
    public static final int IRPROC_COLOR_MODE_14 = 14;
    public static final int IRPROC_COLOR_MODE_15 = 15;
    public static final int IRPROC_COLOR_MODE_16 = 16;
    public static final int IRPROC_COLOR_MODE_17 = 17;
    public static final int IRPROC_COLOR_MODE_18 = 18;
    public static final int IRPROC_COLOR_MODE_19 = 19;
    public static final int IRPROC_COLOR_MODE_2 = 2;
    public static final int IRPROC_COLOR_MODE_20 = 20;
    public static final int IRPROC_COLOR_MODE_3 = 3;
    public static final int IRPROC_COLOR_MODE_4 = 4;
    public static final int IRPROC_COLOR_MODE_5 = 5;
    public static final int IRPROC_COLOR_MODE_6 = 6;
    public static final int IRPROC_COLOR_MODE_7 = 7;
    public static final int IRPROC_COLOR_MODE_8 = 8;
    public static final int IRPROC_COLOR_MODE_9 = 9;
    public static final int IRPROC_ERROR_PARAM = 1;
    public static final int IRPROC_FMT_NOT_SUPPORTED = 2;
    public static final int IRPROC_LOG_DEBUG = 0;
    public static final int IRPROC_LOG_ERROR = 1;
    public static final int IRPROC_LOG_NO_PRINT = 2;
    public static final int IRPROC_MALLOC_FAILED = 3;
    public static final int IRPROC_SRC_FMT_ARGB8888 = 5;
    public static final int IRPROC_SRC_FMT_BGR888 = 4;
    public static final int IRPROC_SRC_FMT_RGB888 = 3;
    public static final int IRPROC_SRC_FMT_Y14 = 0;
    public static final int IRPROC_SRC_FMT_YUV444 = 2;
    public static final int IRPROC_SUCCESS = 0;
    private static final String IRPROC_VERSION = "libirprocess 0.5.0";

    public static class ImageRes_t {
        public char height;
        public char width;
    }

    public static native void irproc_log_register(int i);

    public static String irproc_version() {
        return IRPROC_VERSION;
    }

    public static native int y14_image_enhance(byte[] bArr, ImageRes_t imageRes_t, ImgEnhanceParam_t imgEnhanceParam_t, byte[] bArr2);

    public static native int y14_map_to_yuyv_pseudocolor(byte[] bArr, long j, int i, byte[] bArr2);

    public static native int yuyv_map_to_argb_pseudocolor(byte[] bArr, long j, int i, byte[] bArr2);

    static {
        System.loadLibrary("irprocess");
    }

    public class AgcParam_t {
        float alphaRatio;
        float discardRatio;
        float offsetRatio;
        long rangeGain;
        float stepThdRatio;

        public AgcParam_t() {
        }
    }

    public class DdeParam_t {
        int endMaxThd;
        int endMinThd;
        float maxCoef;
        float minCoef;
        int startMaxThd;
        int startMinThd;

        public DdeParam_t() {
        }
    }

    public class ImgEnhanceParam_t {
        AgcParam_t agc_param;
        DdeParam_t dde_param;

        public ImgEnhanceParam_t() {
        }
    }

    public static int rotate_left_90(byte[] bArr, ImageRes_t imageRes_t, int i, byte[] bArr2) {
        if (bArr == null || bArr2 == null || imageRes_t.width < 1 || imageRes_t.height < 1) {
            return 1;
        }
        if (i == 0) {
            int i2 = 0;
            for (int i3 = imageRes_t.width - 1; i3 >= 0; i3--) {
                for (int i4 = 0; i4 < imageRes_t.height; i4++) {
                    int i5 = i2 * 2;
                    int i6 = ((imageRes_t.width * i4) + i3) * 2;
                    bArr2[i5] = bArr[i6];
                    bArr2[i5 + 1] = bArr[i6 + 1];
                    i2++;
                }
            }
        } else if (i == 3 || i == 4) {
            int i7 = 0;
            for (int i8 = imageRes_t.width - 1; i8 >= 0; i8--) {
                for (int i9 = 0; i9 < imageRes_t.height; i9++) {
                    int i10 = i7 * 3;
                    int i11 = ((imageRes_t.width * i9) + i8) * 3;
                    bArr2[i10 + 0] = bArr[i11 + 0];
                    bArr2[i10 + 1] = bArr[i11 + 1];
                    bArr2[i10 + 2] = bArr[i11 + 2];
                    i7++;
                }
            }
        } else {
            if (i != 5) {
                return 2;
            }
            int i12 = 0;
            for (int i13 = 0; i13 < imageRes_t.width; i13++) {
                for (int i14 = imageRes_t.height - 1; i14 >= 0; i14--) {
                    int i15 = i12 * 4;
                    int i16 = ((imageRes_t.width * i14) + i13) * 4;
                    bArr2[i15 + 0] = bArr[i16 + 0];
                    bArr2[i15 + 1] = bArr[i16 + 1];
                    bArr2[i15 + 2] = bArr[i16 + 2];
                    bArr2[i15 + 3] = bArr[i16 + 3];
                    i12++;
                }
            }
        }
        return 0;
    }

    public static int rotate_right_90(byte[] bArr, ImageRes_t imageRes_t, int i, byte[] bArr2) {
        if (bArr == null || bArr2 == null || imageRes_t.width < 1 || imageRes_t.height < 1) {
            return 1;
        }
        if (i == 0) {
            int i2 = 0;
            for (int i3 = 0; i3 < imageRes_t.width; i3++) {
                for (int i4 = imageRes_t.height - 1; i4 >= 0; i4--) {
                    int i5 = i2 * 2;
                    int i6 = ((imageRes_t.width * i4) + i3) * 2;
                    bArr2[i5] = bArr[i6];
                    bArr2[i5 + 1] = bArr[i6 + 1];
                    i2++;
                }
            }
        } else if (i == 3 || i == 4) {
            int i7 = 0;
            for (int i8 = 0; i8 < imageRes_t.width; i8++) {
                for (int i9 = imageRes_t.height - 1; i9 >= 0; i9--) {
                    int i10 = i7 * 3;
                    int i11 = ((imageRes_t.width * i9) + i8) * 3;
                    bArr2[i10 + 0] = bArr[i11 + 0];
                    bArr2[i10 + 1] = bArr[i11 + 1];
                    bArr2[i10 + 2] = bArr[i11 + 2];
                    i7++;
                }
            }
        } else {
            if (i != 5) {
                return 2;
            }
            int i12 = 0;
            for (int i13 = 0; i13 < imageRes_t.width; i13++) {
                for (int i14 = imageRes_t.height - 1; i14 >= 0; i14--) {
                    int i15 = i12 * 4;
                    int i16 = ((imageRes_t.width * i14) + i13) * 4;
                    bArr2[i15 + 0] = bArr[i16 + 0];
                    bArr2[i15 + 1] = bArr[i16 + 1];
                    bArr2[i15 + 2] = bArr[i16 + 2];
                    bArr2[i15 + 3] = bArr[i16 + 3];
                    i12++;
                }
            }
        }
        return 0;
    }

    public static int rotate_180(byte[] bArr, ImageRes_t imageRes_t, int i, byte[] bArr2) {
        if (bArr == null || bArr2 == null || imageRes_t.width < 1 || imageRes_t.height < 1) {
            return 1;
        }
        if (i == 0) {
            int i2 = 0;
            for (int i3 = (imageRes_t.width * imageRes_t.height) - 1; i3 >= 0; i3--) {
                int i4 = i2 * 2;
                int i5 = i3 * 2;
                bArr2[i4] = bArr[i5];
                bArr2[i4 + 1] = bArr[i5 + 1];
                i2++;
            }
        } else {
            if (i != 3 && i != 4) {
                return 2;
            }
            int i6 = 0;
            for (int i7 = (imageRes_t.width * imageRes_t.height) - 1; i7 >= 0; i7--) {
                int i8 = i6 * 3;
                int i9 = i7 * 3;
                bArr2[i8 + 0] = bArr[i9 + 0];
                bArr2[i8 + 1] = bArr[i9 + 1];
                bArr2[i8 + 2] = bArr[i9 + 2];
                i6++;
            }
        }
        return 0;
    }

    public static int mirror(byte[] bArr, ImageRes_t imageRes_t, int i, byte[] bArr2) {
        if (bArr == null || bArr2 == null || imageRes_t.width < 1 || imageRes_t.height < 1) {
            return 1;
        }
        if (i == 0) {
            int i2 = 0;
            for (int i3 = 0; i3 < imageRes_t.height; i3++) {
                for (int i4 = imageRes_t.width - 1; i4 >= 0; i4--) {
                    int i5 = i2 * 2;
                    int i6 = ((imageRes_t.width * i3) + i4) * 2;
                    bArr2[i5] = bArr[i6];
                    bArr2[i5 + 1] = bArr[i6 + 1];
                    i2++;
                }
            }
        } else {
            if (i != 3 && i != 4) {
                return 2;
            }
            int i7 = 0;
            for (int i8 = 0; i8 < imageRes_t.height; i8++) {
                for (int i9 = imageRes_t.width - 1; i9 >= 0; i9--) {
                    int i10 = i7 * 3;
                    int i11 = ((imageRes_t.width * i8) + i9) * 3;
                    bArr2[i10 + 0] = bArr[i11 + 0];
                    bArr2[i10 + 1] = bArr[i11 + 1];
                    bArr2[i10 + 2] = bArr[i11 + 2];
                    i7++;
                }
            }
        }
        return 0;
    }

    public static int flip(byte[] bArr, ImageRes_t imageRes_t, int i, byte[] bArr2) {
        if (bArr == null || bArr2 == null || imageRes_t.width < 1 || imageRes_t.height < 1) {
            return 1;
        }
        if (i == 0) {
            int i2 = imageRes_t.width * 2;
            for (int i3 = 0; i3 < imageRes_t.height; i3++) {
                System.arraycopy(bArr, ((imageRes_t.height - 1) - i3) * imageRes_t.width, bArr2, imageRes_t.width * i3, i2);
            }
        } else {
            if (i != 3 && i != 4) {
                return 2;
            }
            int i4 = imageRes_t.width * 3;
            for (int i5 = 0; i5 < imageRes_t.height; i5++) {
                System.arraycopy(bArr, ((imageRes_t.height - 1) - i5) * imageRes_t.width * 3, bArr2, imageRes_t.width * i5 * 3, i4);
            }
        }
        return 0;
    }
}
