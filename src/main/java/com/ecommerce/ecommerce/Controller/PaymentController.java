package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.Service.PaymentService;
import com.ecommerce.ecommerce.dto.PaymentRequest;
import com.ecommerce.ecommerce.dto.PaymentResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/pay")
    public PaymentResponse payOrder(@PathVariable Long orderId,
                                    @RequestBody PaymentRequest request) {
        return paymentService.payOrder(orderId, request);
    }

    @GetMapping("/orders/{orderId}")
    public PaymentResponse getPayment(@PathVariable Long orderId) {
        return paymentService.getPayment(orderId);
    }
}
