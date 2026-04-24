package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.OrderItem;

import java.math.BigDecimal;

public class OrderResponseItem {

    private final Long productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public OrderResponseItem(OrderItem item) {
        this.productId = item.getProduct().getId();
        this.productName = item.getProductName();
        this.quantity = item.getQuantity();
        this.unitPrice = item.getUnitPrice();
        this.lineTotal = item.getLineTotal();
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
