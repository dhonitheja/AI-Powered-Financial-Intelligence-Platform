package com.wealthix.dto;

public class UserInfoResponse {
    private String id;
    private String username;
    private String email;
    private boolean twoFactorEnabled;
    private String token;

    public UserInfoResponse(String id, String username, String email, boolean twoFactorEnabled, String token) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.twoFactorEnabled = twoFactorEnabled;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
