package com.heinrichreimersoftware.androidissuereporter.model.github;

import androidx.annotation.Nullable;

public class GithubLogin {

    @Nullable
    private final String username;
    @Nullable
    private final String password;
    @Nullable
    private final String apiToken;

    public GithubLogin(String username, String password) {
        this.username = username;
        this.password = password;
        this.apiToken = null;
    }

    public GithubLogin(String apiToken) {
        this.username = null;
        this.password = null;
        this.apiToken = apiToken;
    }

    public boolean shouldUseApiToken() {
        return apiToken != null && !apiToken.isEmpty();
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public String getApiToken() {
        return apiToken;
    }
}
