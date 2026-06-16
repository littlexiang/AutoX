package com.heinrichreimersoftware.androidissuereporter.util;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.appcompat.R;

public final class ThemeUtils {

    private ThemeUtils() {
    }

    @ColorInt
    public static int getColorAccent(Context context) {
        Integer appCompatAccent = resolveColor(context, R.attr.colorAccent);
        if (appCompatAccent != null) {
            return appCompatAccent;
        }
        Integer materialSecondary = resolveColor(context, com.google.android.material.R.attr.colorSecondary);
        return materialSecondary != null ? materialSecondary : 0xff000000;
    }

    private static Integer resolveColor(Context context, @AttrRes int attr) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true)) {
            return null;
        }
        return value.data;
    }
}
