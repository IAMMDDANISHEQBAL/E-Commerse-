package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.dto.FrontendConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class AppConfigController {

    private final String paymentProvider;
    private final String razorpayKeyId;
    private final String googleClientId;
    private final boolean googleVerificationEnabled;

    public AppConfigController(@Value("${app.payment.provider:mock}") String paymentProvider,
                               @Value("${app.razorpay.key-id:}") String razorpayKeyId,
                               @Value("${app.google.client-id:}") String googleClientId,
                               @Value("${app.google.verify-token:false}") boolean googleVerificationEnabled) {
        this.paymentProvider = paymentProvider;
        this.razorpayKeyId = razorpayKeyId;
        this.googleClientId = googleClientId;
        this.googleVerificationEnabled = googleVerificationEnabled;
    }

    @GetMapping("/config")
    public FrontendConfigResponse config() {
        return new FrontendConfigResponse(paymentProvider, razorpayKeyId, googleClientId, googleVerificationEnabled);
    }
}
