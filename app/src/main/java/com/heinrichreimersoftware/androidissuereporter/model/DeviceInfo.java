package com.heinrichreimersoftware.androidissuereporter.model;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Arrays;
import java.util.Locale;

public class DeviceInfo {

    private final Context context;

    public DeviceInfo(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String toString() {
        String versionName = "unknown";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        String supportedAbis = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? Arrays.toString(Build.SUPPORTED_ABIS)
                : "[]";
        return String.format(
                Locale.US,
                "App: %s (%s)\nPackage: %s\nManufacturer: %s\nBrand: %s\nModel: %s\nDevice: %s\nProduct: %s\nAndroid: %s (SDK %d)\nABIs: %s",
                safe(BuildConfigValue.appName(context)),
                safe(versionName),
                safe(context.getPackageName()),
                safe(Build.MANUFACTURER),
                safe(Build.BRAND),
                safe(Build.MODEL),
                safe(Build.DEVICE),
                safe(Build.PRODUCT),
                safe(Build.VERSION.RELEASE),
                Build.VERSION.SDK_INT,
                supportedAbis
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class BuildConfigValue {
        private BuildConfigValue() {
        }

        private static String appName(Context context) {
            CharSequence label = context.getApplicationInfo().loadLabel(context.getPackageManager());
            return label == null ? "" : label.toString();
        }
    }
}
