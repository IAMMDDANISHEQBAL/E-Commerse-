package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.Service.CartService;
import com.ecommerce.ecommerce.dto.CartItemRequest;
import com.ecommerce.ecommerce.dto.CartResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(@PathVariable Long productId,
                                   @RequestParam @Min(1) int quantity) {
        return cartService.updateItem(productId, quantity);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long productId) {
        return cartService.removeItem(productId);
    }
}
