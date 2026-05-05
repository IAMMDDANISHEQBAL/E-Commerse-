package com.ecommerce.ecommerce.dto;

public class FrontendConfigResponse {

    private final String paymentProvider;
    private final String razorpayKeyId;
    private final String googleClientId;
    private final boolean googleVerificationEnabled;

    public FrontendConfigResponse(String paymentProvider,
                                  String razorpayKeyId,
                                  String googleClientId,
                                  boolean googleVerificationEnabled) {
        this.paymentProvider = paymentProvider;
        this.razorpayKeyId = razorpayKeyId;
        this.googleClientId = googleClientId;
        this.googleVerificationEnabled = googleVerificationEnabled;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public String getGoogleClientId() {
        return googleClientId;
    }

    public boolean isGoogleVerificationEnabled() {
        return googleVerificationEnabled;
    }
}
