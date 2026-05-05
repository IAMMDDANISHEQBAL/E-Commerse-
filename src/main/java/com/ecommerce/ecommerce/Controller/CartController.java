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
    public CartResponse getCart(@RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId) {
        return cartService.getCart(guestCartId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest request,
                                @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId) {
        return cartService.addItem(request, guestCartId);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(@PathVariable Long productId,
                                   @RequestParam @Min(1) int quantity,
                                   @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId) {
        return cartService.updateItem(productId, quantity, guestCartId);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long productId,
                                   @RequestHeader(value = "X-Guest-Cart-Id", required = false) String guestCartId) {
        return cartService.removeItem(productId, guestCartId);
    }
}
