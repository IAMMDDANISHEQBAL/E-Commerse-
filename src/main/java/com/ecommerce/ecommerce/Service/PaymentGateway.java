package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.entity.Payment;

public interface PaymentGateway {

    String charge(Payment payment, String providerToken);
}
