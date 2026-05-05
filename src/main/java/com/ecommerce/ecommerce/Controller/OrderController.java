package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.Service.OrderService;
import com.ecommerce.ecommerce.dto.CheckoutRequest;
import com.ecommerce.ecommerce.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping
    public List<OrderResponse> listMyOrders() {
        return orderService.listMyOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponse getMyOrder(@PathVariable Long orderId) {
        return orderService.getMyOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @PostMapping("/{orderId}/return")
    public OrderResponse requestReturn(@PathVariable Long orderId) {
        return orderService.requestReturn(orderId);
    }
}
