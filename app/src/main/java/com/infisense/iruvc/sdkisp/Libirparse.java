package com.infisense.iruvc.sdkisp;

/* JADX INFO: loaded from: classes.dex */
public class Libirparse {
    public static final int IRPARSE_ERROR_PARAM = -1;
    public static final int IRPARSE_SUCCESS = 0;
    public static final String IRPARSE_VERSION = "libirparse 0.3.0";

    public static String irparse_version() {
        return IRPARSE_VERSION;
    }

    public static byte num_y14_to_y8(char c) {
        if (c >= 16383) {
            c = 16383;
        }
        return (byte) ((c * 255) / 16383);
    }

    public static int y16_to_y14(char[] cArr, int i, char[] cArr2) {
        int i2 = -1;
        if (cArr != null && cArr2 != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                cArr2[i3] = (char) (cArr[i3] >> 2);
            }
        }
        return i2;
    }

    public static int y14_to_y8(char[] cArr, int i, byte[] bArr) {
        int i2 = -1;
        if (cArr != null && bArr != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                bArr[i3] = num_y14_to_y8(cArr[i3]);
            }
        }
        return i2;
    }

    public static int y14_to_rgb(char[] cArr, int i, byte[] bArr) {
        int i2 = -1;
        if (bArr != null && cArr != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                byte bNum_y14_to_y8 = num_y14_to_y8(cArr[i3]);
                int i4 = i3 * 3;
                bArr[i4 + 0] = bNum_y14_to_y8;
                bArr[i4 + 1] = bNum_y14_to_y8;
                bArr[i4 + 2] = bNum_y14_to_y8;
            }
        }
        return i2;
    }

    public static int y14_to_argb(byte[] bArr, int i, byte[] bArr2) {
        if (bArr2 == null || bArr == null || i <= 0) {
            return -1;
        }
        int i2 = 0;
        while (i2 < i) {
            int i3 = i2 + 1;
            byte bNum_y14_to_y8 = num_y14_to_y8((char) (bArr[i2] + (bArr[i3] * 256)));
            int i4 = i2 * 4;
            bArr2[i4 + 0] = bNum_y14_to_y8;
            bArr2[i4 + 1] = bNum_y14_to_y8;
            bArr2[i4 + 2] = bNum_y14_to_y8;
            bArr2[i4 + 3] = -1;
            i2 = i3;
        }
        return 0;
    }

    public static int y14_to_yuv444(char[] cArr, int i, byte[] bArr) {
        int i2 = -1;
        if (cArr != null && bArr != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i; i3++) {
                int i4 = i3 * 3;
                bArr[i4 + 0] = num_y14_to_y8(cArr[i3]);
                bArr[i4 + 1] = -128;
                bArr[i4 + 2] = -128;
            }
        }
        return i2;
    }

    public static int yuv444_to_yuv422(byte[] bArr, int i, byte[] bArr2) {
        int i2 = -1;
        if (bArr != null && bArr2 != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i / 2; i3++) {
                int i4 = i3 * 4;
                int i5 = i3 * 6;
                bArr2[i4 + 0] = bArr[i5];
                bArr2[i4 + 1] = (byte) (((char) (bArr[i5 + 1] + ((char) bArr[i5 + 4]))) / 2);
                bArr2[i4 + 2] = bArr[i5 + 3];
                bArr2[i4 + 3] = (byte) ((((char) bArr[i5 + 2]) + ((char) bArr[i5 + 5])) / 2);
            }
        }
        return i2;
    }

    public static int yuv422_to_rgb(byte[] bArr, int i, byte[] bArr2) {
        if (bArr2 == null || bArr == null || i <= 0) {
            return -1;
        }
        int i2 = 0;
        int i3 = 0;
        while (i2 < i * 2) {
            short s = 255;
            short s2 = (short) (bArr[i2] & 255);
            short s3 = (short) (bArr[i2 + 2] & 255);
            double d = s2;
            double d2 = ((short) (bArr[i2 + 3] & 255)) - 128;
            double d3 = 1.14d * d2;
            short s4 = (short) (d + d3);
            double d4 = ((short) (bArr[i2 + 1] & 255)) - 128;
            double d5 = 0.394d * d4;
            double d6 = d2 * 0.581d;
            int i4 = i2;
            short s5 = (short) ((d - d5) - d6);
            double d7 = d4 * 2.032d;
            short s6 = (short) (d + d7);
            double d8 = s3;
            short s7 = (short) (d3 + d8);
            short s8 = (short) ((d8 - d5) - d6);
            short s9 = (short) (d8 + d7);
            if (s4 > 255) {
                s4 = 255;
            } else if (s4 <= 0) {
                s4 = 0;
            }
            if (s5 > 255) {
                s5 = 255;
            } else if (s5 <= 0) {
                s5 = 0;
            }
            if (s6 > 255) {
                s6 = 255;
            } else if (s6 <= 0) {
                s6 = 0;
            }
            if (s7 > 255) {
                s7 = 255;
            } else if (s7 <= 0) {
                s7 = 0;
            }
            if (s8 > 255) {
                s8 = 255;
            } else if (s8 <= 0) {
                s8 = 0;
            }
            if (s9 <= 255) {
                s = s9 <= 0 ? (short) 0 : s9;
            }
            bArr2[i3] = (byte) s4;
            bArr2[i3 + 1] = (byte) s5;
            bArr2[i3 + 2] = (byte) s6;
            bArr2[i3 + 3] = (byte) s7;
            bArr2[i3 + 4] = (byte) s8;
            bArr2[i3 + 5] = (byte) s;
            i2 = i4 + 4;
            i3 += 6;
        }
        return 0;
    }

    public static int yuv422_to_argb(byte[] bArr, int i, byte[] bArr2) {
        if (bArr2 == null || bArr == null || i <= 0) {
            return -1;
        }
        int i2 = 0;
        int i3 = 0;
        while (i2 < i * 2) {
            short s = 255;
            short s2 = (short) (bArr[i2] & 255);
            short s3 = (short) (bArr[i2 + 2] & 255);
            double d = s2;
            double d2 = ((short) (bArr[i2 + 3] & 255)) - 128;
            double d3 = 1.14d * d2;
            int i4 = i2;
            short s4 = (short) (d + d3);
            double d4 = ((short) (bArr[i2 + 1] & 255)) - 128;
            double d5 = 0.394d * d4;
            double d6 = d2 * 0.581d;
            int i5 = i3;
            short s5 = (short) ((d - d5) - d6);
            double d7 = d4 * 2.032d;
            short s6 = (short) (d + d7);
            double d8 = s3;
            short s7 = (short) (d3 + d8);
            short s8 = (short) ((d8 - d5) - d6);
            short s9 = (short) (d8 + d7);
            short s10 = s4;
            if (s10 > 255) {
                s10 = 255;
            } else if (s10 <= 0) {
                s10 = 0;
            }
            if (s5 > 255) {
                s5 = 255;
            } else if (s5 <= 0) {
                s5 = 0;
            }
            if (s6 > 255) {
                s6 = 255;
            } else if (s6 <= 0) {
                s6 = 0;
            }
            if (s7 > 255) {
                s7 = 255;
            } else if (s7 <= 0) {
                s7 = 0;
            }
            if (s8 > 255) {
                s8 = 255;
            } else if (s8 <= 0) {
                s8 = 0;
            }
            if (s9 <= 255) {
                s = s9 <= 0 ? (short) 0 : s9;
            }
            bArr2[i5] = (byte) s10;
            bArr2[i5 + 1] = (byte) s5;
            bArr2[i5 + 2] = (byte) s6;
            bArr2[i5 + 3] = -1;
            bArr2[i5 + 4] = (byte) s7;
            bArr2[i5 + 5] = (byte) s8;
            bArr2[i5 + 6] = (byte) s;
            bArr2[i5 + 7] = -1;
            i2 = i4 + 4;
            i3 = i5 + 8;
        }
        return 0;
    }

    public static int yuv444_to_rgb(byte[] bArr, int i, byte[] bArr2) {
        int i2 = -1;
        if (bArr2 != null && bArr != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i * 3; i3 += 3) {
                short s = 255;
                char c = (char) (bArr[i3] & 255);
                int i4 = i3 + 1;
                int i5 = i3 + 2;
                double d = c;
                double d2 = ((char) (bArr[i5] & 255)) - 128;
                short s2 = (short) ((1.14d * d2) + d);
                double d3 = ((char) (bArr[i4] & 255)) - 128;
                short s3 = (short) ((d - (0.394d * d3)) - (d2 * 0.581d));
                short s4 = (short) (d + (d3 * 2.032d));
                if (s2 > 255) {
                    s2 = 255;
                } else if (s2 <= 0) {
                    s2 = 0;
                }
                if (s3 > 255) {
                    s3 = 255;
                } else if (s3 <= 0) {
                    s3 = 0;
                }
                if (s4 <= 255) {
                    s = s4 <= 0 ? (short) 0 : s4;
                }
                bArr2[i3 + 0] = (byte) s2;
                bArr2[i4] = (byte) s3;
                bArr2[i5] = (byte) s;
            }
        }
        return i2;
    }

    public static int rgb_to_bgr(byte[] bArr, int i, byte[] bArr2) {
        int i2 = -1;
        if (bArr != null && bArr2 != null) {
            if (i <= 0) {
                return -1;
            }
            i2 = 0;
            for (int i3 = 0; i3 < i * 3; i3 += 3) {
                byte b = bArr[i3];
                int i4 = i3 + 1;
                byte b2 = bArr[i4];
                int i5 = i3 + 2;
                bArr2[i3 + 0] = bArr[i5];
                bArr2[i4] = b2;
                bArr2[i5] = b;
            }
        }
        return i2;
    }

    public static int yuv422_add_y(byte[] bArr, int i, int i2, byte[] bArr2) {
        int i3 = -1;
        if (bArr != null && bArr2 != null) {
            if (i <= 0) {
                return -1;
            }
            i3 = 0;
            for (int i4 = 0; i4 < i * 2; i4 += 4) {
                short s = 255;
                int i5 = i2 + 255;
                short s2 = (short) (bArr[i4] & i5);
                int i6 = i4 + 2;
                short s3 = (short) (i5 & bArr[i6]);
                if (s2 > 255) {
                    s2 = 255;
                } else if (s2 <= 0) {
                    s2 = 0;
                }
                if (s3 <= 255) {
                    s = s2;
                    if (s3 <= 0) {
                        s3 = 0;
                    }
                }
                bArr2[i4] = (byte) s;
                bArr2[i6] = (byte) s3;
            }
        }
        return i3;
    }

    public static int raw_data_cut(byte[] bArr, int i, int i2, byte[] bArr2, byte[] bArr3) {
        if (bArr == null || bArr2 == null || bArr3 == null) {
            return -1;
        }
        System.arraycopy(bArr, 0, bArr2, 0, i);
        System.arraycopy(bArr, i, bArr3, 0, i2);
        return 0;
    }
}
