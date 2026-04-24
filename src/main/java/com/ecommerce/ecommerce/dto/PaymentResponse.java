package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.Payment;
import com.ecommerce.ecommerce.entity.PaymentMethod;
import com.ecommerce.ecommerce.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {

    private final Long id;
    private final Long orderId;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final BigDecimal amount;
    private final String providerReference;
    private final Instant paidAt;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrder().getId();
        this.method = payment.getMethod();
        this.status = payment.getStatus();
        this.amount = payment.getAmount();
        this.providerReference = payment.getProviderReference();
        this.paidAt = payment.getPaidAt();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
