package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.OrderRepository;
import com.ecommerce.ecommerce.Repository.PaymentRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.dto.PaymentRequest;
import com.ecommerce.ecommerce.dto.PaymentResponse;
import com.ecommerce.ecommerce.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final PaymentGateway paymentGateway;
    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;
    private final CartService cartService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          CurrentUserService currentUserService,
                          PaymentGateway paymentGateway,
                          ProductRepository productRepository,
                          ProductCacheService productCacheService,
                          CartService cartService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
        this.paymentGateway = paymentGateway;
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
        this.cartService = cartService;
    }

    @Transactional
    public Payment createPendingPayment(CustomerOrder order, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(order.getTotal());
        payment.setStatus(method == PaymentMethod.CASH_ON_DELIVERY
                ? PaymentStatus.PAID
                : PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());

        if (method == PaymentMethod.CASH_ON_DELIVERY) {
            payment.setProviderReference("cod-" + UUID.randomUUID());
            payment.setPaidAt(Instant.now());
            completePaidOrder(order);
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponse payOrder(Long orderId, PaymentRequest request) {
        CustomerOrder order = getCurrentUserOrder(orderId);
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return new PaymentResponse(payment);
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderReference(paymentGateway.charge(payment, request.getProviderToken()));
        payment.setPaidAt(Instant.now());
        completePaidOrder(order);

        return new PaymentResponse(paymentRepository.save(payment));
    }

    public PaymentResponse getPayment(Long orderId) {
        CustomerOrder order = getCurrentUserOrder(orderId);
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return new PaymentResponse(payment);
    }

    private CustomerOrder getCurrentUserOrder(Long orderId) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin accounts cannot use customer payment features");
        }
        return orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    private void completePaidOrder(CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            int updatedQuantity = product.getQuantity() - item.getQuantity();
            if (updatedQuantity < 0) {
                throw new RuntimeException("Product out of stock: " + product.getName());
            }
            product.setQuantity(updatedQuantity);
            productCacheService.cacheProduct(productRepository.save(product));
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        cartService.clearCart(order.getUser());
    }
}
