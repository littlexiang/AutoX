package com.heinrichreimersoftware.androidissuereporter.model.github;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExtraInfo {

    private final Map<String, String> values = new LinkedHashMap<>();

    public void put(@NonNull String key, @Nullable String value) {
        values.put(key, value == null ? "" : value);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return builder.toString();
    }
}
