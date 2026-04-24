package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.OrderRepository;
import com.ecommerce.ecommerce.Repository.PaymentRepository;
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

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          CurrentUserService currentUserService,
                          PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
        this.paymentGateway = paymentGateway;
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
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
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
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

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
        return orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
