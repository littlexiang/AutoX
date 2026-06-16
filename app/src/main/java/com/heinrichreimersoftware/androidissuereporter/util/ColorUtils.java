package com.heinrichreimersoftware.androidissuereporter.util;

import androidx.annotation.ColorInt;

public final class ColorUtils {

    private ColorUtils() {
    }

    public static boolean isDark(@ColorInt int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color)
                + 0.587 * android.graphics.Color.green(color)
                + 0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
}
