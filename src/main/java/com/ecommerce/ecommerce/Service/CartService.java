package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.CartRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.dto.CartItemRequest;
import com.ecommerce.ecommerce.dto.CartResponse;
import com.ecommerce.ecommerce.entity.Cart;
import com.ecommerce.ecommerce.entity.CartItem;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       CurrentUserService currentUserService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public CartResponse getCart() {
        return new CartResponse(getOrCreateCart(currentUserService.getCurrentUser()));
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User user = currentUserService.getCurrentUser();
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Requested quantity is not available");
        }

        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    cart.getItems().add(newItem);
                    return newItem;
                });

        int newQuantity = item.getQuantity() + request.getQuantity();
        if (product.getQuantity() < newQuantity) {
            throw new RuntimeException("Requested quantity is not available");
        }

        item.setQuantity(newQuantity);
        return new CartResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(Long productId, int quantity) {
        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        Cart cart = getOrCreateCart(currentUserService.getCurrentUser());
        CartItem item = findCartItem(cart, productId);

        if (item.getProduct().getQuantity() < quantity) {
            throw new RuntimeException("Requested quantity is not available");
        }

        item.setQuantity(quantity);
        return new CartResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = getOrCreateCart(currentUserService.getCurrentUser());
        CartItem item = findCartItem(cart, productId);
        cart.getItems().remove(item);
        return new CartResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    private CartItem findCartItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product is not in cart"));
    }
}
