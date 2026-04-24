package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String charge(Payment payment, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new RuntimeException("Payment provider token is required");
        }
        return "mock-" + UUID.randomUUID();
    }
}
