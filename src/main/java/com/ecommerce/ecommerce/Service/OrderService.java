package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.AddressRepository;
import com.ecommerce.ecommerce.Repository.OrderRepository;
import com.ecommerce.ecommerce.dto.CheckoutRequest;
import com.ecommerce.ecommerce.dto.OrderResponse;
import com.ecommerce.ecommerce.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(49.00);

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final AddressService addressService;
    private final ProductCacheService productCacheService;

    public OrderService(OrderRepository orderRepository,
                        AddressRepository addressRepository,
                        CurrentUserService currentUserService,
                        CartService cartService,
                        PaymentService paymentService,
                        AddressService addressService,
                        ProductCacheService productCacheService) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.currentUserService = currentUserService;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.addressService = addressService;
        this.productCacheService = productCacheService;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin accounts cannot place orders");
        }

        cartService.forceSyncCurrentUserCart();
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Address shippingAddress = resolveAddress(request, user);

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCreatedAt(Instant.now());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Product out of stock: " + product.getName());
            }

            BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);
            order.getItems().add(orderItem);
        }

        order.setSubtotal(subtotal);
        order.setShippingFee(SHIPPING_FEE);
        order.setTotal(subtotal.add(SHIPPING_FEE));

        CustomerOrder saved = orderRepository.save(order);
        paymentService.createPendingPayment(saved, request.getPaymentMethod());

        return new OrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders() {
        User user = currentUserService.getCurrentUser();
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long orderId) {
        User user = currentUserService.getCurrentUser();
        return new OrderResponse(orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found")));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        CustomerOrder order = getMyOrderEntity(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new OrderResponse(order);
        }
        if (order.getStatus() == OrderStatus.RETURNED || order.getStatus() == OrderStatus.RETURN_REQUESTED) {
            throw new RuntimeException("Returned orders cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CONFIRMED) {
            restoreInventory(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        return new OrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse requestReturn(Long orderId) {
        CustomerOrder order = getMyOrderEntity(orderId);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Only paid orders can be returned");
        }
        restoreInventory(order);
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        return new OrderResponse(orderRepository.save(order));
    }

    CustomerOrder getMyOrderEntity(Long orderId) {
        User user = currentUserService.getCurrentUser();
        return orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public void markPaid(CustomerOrder order) {
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    private void restoreInventory(CustomerOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productCacheService.cacheProduct(product);
        }
    }

    private Address resolveAddress(CheckoutRequest request, User user) {
        if (request.getAddressId() != null) {
            return addressRepository.findByIdAndUser(request.getAddressId(), user)
                    .orElseThrow(() -> new RuntimeException("Address not found"));
        }

        if (request.getAddress() == null) {
            throw new RuntimeException("Shipping address is required");
        }

        return addressRepository.save(addressService.toAddress(request.getAddress(), user));
    }
}
