package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.dto.PaymentRequest;
import com.ecommerce.ecommerce.entity.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String createProviderOrder(Payment payment) {
        return "mock_order_" + UUID.randomUUID();
    }

    @Override
    public String verifyAndCapture(Payment payment, PaymentRequest request) {
        if ((request.getProviderToken() == null || request.getProviderToken().isBlank())
                && (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank())) {
            throw new RuntimeException("Payment provider confirmation is required");
        }
        return "mock-" + UUID.randomUUID();
    }
}
