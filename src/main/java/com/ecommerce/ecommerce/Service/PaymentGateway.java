package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.entity.Payment;
import com.ecommerce.ecommerce.dto.PaymentRequest;

public interface PaymentGateway {

    String createProviderOrder(Payment payment);

    String verifyAndCapture(Payment payment, PaymentRequest request);
}
