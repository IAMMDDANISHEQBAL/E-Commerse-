package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private final Long id;
    private final List<CartResponseItem> items;
    private final BigDecimal subtotal;

    public CartResponse(Cart cart) {
        this.id = cart.getId();
        this.items = cart.getItems().stream()
                .map(CartResponseItem::new)
                .toList();
        this.subtotal = items.stream()
                .map(CartResponseItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public List<CartResponseItem> getItems() {
        return items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
