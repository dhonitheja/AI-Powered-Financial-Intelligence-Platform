package com.example.financial.dto;

public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUri;

    public TwoFactorSetupResponse(String secret, String qrCodeUri) {
        this.secret = secret;
        this.qrCodeUri = qrCodeUri;
    }

    public String getSecret() {
        return secret;
    }

    public String getQrCodeUri() {
        return qrCodeUri;
    }
}
