package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {

    private Long addressId;

    @Valid
    private AddressRequest address;

    @NotNull
    private PaymentMethod paymentMethod;

    public Long getAddressId() {
        return addressId;
    }

    public AddressRequest getAddress() {
        return address;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public void setAddress(AddressRequest address) {
        this.address = address;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
