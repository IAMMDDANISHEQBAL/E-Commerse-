package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.CustomerOrder;
import com.ecommerce.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {

    private final Long id;
    private final OrderStatus status;
    private final AddressResponse shippingAddress;
    private final List<OrderResponseItem> items;
    private final BigDecimal subtotal;
    private final BigDecimal shippingFee;
    private final BigDecimal total;
    private final Instant createdAt;

    public OrderResponse(CustomerOrder order) {
        this.id = order.getId();
        this.status = order.getStatus();
        this.shippingAddress = new AddressResponse(order.getShippingAddress());
        this.items = order.getItems().stream()
                .map(OrderResponseItem::new)
                .toList();
        this.subtotal = order.getSubtotal();
        this.shippingFee = order.getShippingFee();
        this.total = order.getTotal();
        this.createdAt = order.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public AddressResponse getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderResponseItem> getItems() {
        return items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
