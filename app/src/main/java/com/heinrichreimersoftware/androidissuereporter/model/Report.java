package com.heinrichreimersoftware.androidissuereporter.model;

import androidx.annotation.Nullable;

import com.heinrichreimersoftware.androidissuereporter.model.github.ExtraInfo;

public class Report {

    private final String title;
    private final String description;
    private final DeviceInfo deviceInfo;
    private final ExtraInfo extraInfo;
    @Nullable
    private final String email;

    public Report(String title, String description, DeviceInfo deviceInfo, ExtraInfo extraInfo, @Nullable String email) {
        this.title = title;
        this.description = description;
        this.deviceInfo = deviceInfo;
        this.extraInfo = extraInfo;
        this.email = email;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        StringBuilder builder = new StringBuilder(description);
        String deviceInfoText = deviceInfo.toString();
        if (!deviceInfoText.isEmpty()) {
            builder.append("\n\n---\n").append(deviceInfoText);
        }
        if (!extraInfo.isEmpty()) {
            builder.append("\n\nExtra info:\n").append(extraInfo);
        }
        if (email != null && !email.isEmpty()) {
            builder.append("\n\nReporter email: ").append(email);
        }
        return builder.toString();
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public ExtraInfo getExtraInfo() {
        return extraInfo;
    }

    @Nullable
    public String getEmail() {
        return email;
    }
}
