package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.CartRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.Repository.UserRepository;
import com.ecommerce.ecommerce.dto.CartItemRequest;
import com.ecommerce.ecommerce.dto.CartResponse;
import com.ecommerce.ecommerce.dto.CartResponseItem;
import com.ecommerce.ecommerce.entity.Cart;
import com.ecommerce.ecommerce.entity.CartItem;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.entity.Role;
import com.ecommerce.ecommerce.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String ACTIVE_USER_CARTS = "cart:active:users";
    private static final Duration GUEST_CART_TTL = Duration.ofDays(7);

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProductCacheService productCacheService;
    private final RedisTemplate<String, String> redisTemplate;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       CurrentUserService currentUserService,
                       ProductCacheService productCacheService,
                       RedisTemplate<String, String> redisTemplate) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.productCacheService = productCacheService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public CartResponse getCart(String guestCartId) {
        CartOwner owner = resolveOwner(guestCartId);
        ensureRedisCartLoaded(owner);
        return toResponse(owner);
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request, String guestCartId) {
        CartOwner owner = resolveOwner(guestCartId);
        ensureRedisCartLoaded(owner);
        Product product = productCacheService.getProduct(request.getProductId());
        int existingQuantity = getRedisQuantity(owner.redisKey(), request.getProductId());
        int newQuantity = existingQuantity + request.getQuantity();
        assertStock(product, newQuantity);
        redisTemplate.opsForHash().put(owner.redisKey(), request.getProductId().toString(), String.valueOf(newQuantity));
        touchOwner(owner);
        return toResponse(owner);
    }

    @Transactional
    public CartResponse updateItem(Long productId, int quantity, String guestCartId) {
        if (quantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        CartOwner owner = resolveOwner(guestCartId);
        ensureRedisCartLoaded(owner);
        Product product = productCacheService.getProduct(productId);
        assertStock(product, quantity);
        redisTemplate.opsForHash().put(owner.redisKey(), productId.toString(), String.valueOf(quantity));
        touchOwner(owner);
        return toResponse(owner);
    }

    @Transactional
    public CartResponse removeItem(Long productId, String guestCartId) {
        CartOwner owner = resolveOwner(guestCartId);
        ensureRedisCartLoaded(owner);
        redisTemplate.opsForHash().delete(owner.redisKey(), productId.toString());
        touchOwner(owner);
        return toResponse(owner);
    }

    @Transactional
    public void mergeGuestCartIntoUser(String guestCartId, User user) {
        if (guestCartId == null || guestCartId.isBlank() || user.getRole() == Role.ADMIN) {
            return;
        }

        CartOwner userOwner = CartOwner.user(user);
        ensureRedisCartLoaded(userOwner);

        String guestRedisKey = guestRedisKey(guestCartId);
        Map<Object, Object> guestItems = redisTemplate.opsForHash().entries(guestRedisKey);
        for (Map.Entry<Object, Object> entry : guestItems.entrySet()) {
            Long productId = Long.valueOf(entry.getKey().toString());
            int guestQuantity = Integer.parseInt(entry.getValue().toString());
            int mergedQuantity = getRedisQuantity(userOwner.redisKey(), productId) + guestQuantity;
            Product product = productCacheService.getProduct(productId);
            assertStock(product, mergedQuantity);
            redisTemplate.opsForHash().put(userOwner.redisKey(), productId.toString(), String.valueOf(mergedQuantity));
        }

        redisTemplate.delete(guestRedisKey);
        touchOwner(userOwner);
        syncUserCartToDatabase(user);
    }

    @Transactional
    public void forceSyncCurrentUserCart() {
        User user = currentUserService.getCurrentUser();
        rejectAdmin(user);
        ensureRedisCartLoaded(CartOwner.user(user));
        syncUserCartToDatabase(user);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
        redisTemplate.delete(CartOwner.user(user).redisKey());
        redisTemplate.opsForSet().remove(ACTIVE_USER_CARTS, user.getId().toString());
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        rejectAdmin(user);
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Scheduled(fixedDelayString = "${app.cart-sync-ms:180000}")
    @Transactional
    public void syncActiveUserCarts() {
        var activeUsers = redisTemplate.opsForSet().members(ACTIVE_USER_CARTS);
        if (activeUsers == null) {
            return;
        }

        for (String userId : activeUsers) {
            try {
                userRepository.findById(Long.valueOf(userId))
                        .ifPresent(this::syncUserCartToDatabase);
            } catch (Exception exception) {
                log.warn("Unable to sync Redis cart for user {}", userId, exception);
            }
        }
    }

    private void syncUserCartToDatabase(User user) {
        CartOwner owner = CartOwner.user(user);
        Map<Object, Object> redisItems = redisTemplate.opsForHash().entries(owner.redisKey());
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();

        for (Map.Entry<Object, Object> entry : redisItems.entrySet()) {
            Long productId = Long.valueOf(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());
            if (quantity < 1) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            assertStock(product, quantity);

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
    }

    private void ensureRedisCartLoaded(CartOwner owner) {
        if (redisTemplate.hasKey(owner.redisKey())) {
            touchOwner(owner);
            return;
        }

        if (owner.user().isEmpty()) {
            touchOwner(owner);
            return;
        }

        Cart cart = getOrCreateCart(owner.user().get());
        for (CartItem item : cart.getItems()) {
            redisTemplate.opsForHash().put(owner.redisKey(),
                    item.getProduct().getId().toString(),
                    String.valueOf(item.getQuantity()));
        }
        touchOwner(owner);
    }

    private CartResponse toResponse(CartOwner owner) {
        Map<Object, Object> items = redisTemplate.opsForHash().entries(owner.redisKey());
        List<CartResponseItem> responseItems = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : items.entrySet()) {
            Long productId = Long.valueOf(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());
            if (quantity > 0) {
                responseItems.add(new CartResponseItem(productCacheService.getProduct(productId), quantity));
            }
        }
        return new CartResponse(owner.publicKey(), responseItems);
    }

    private CartOwner resolveOwner(String guestCartId) {
        Optional<User> currentUser = currentUserService.getOptionalCurrentUser();
        if (currentUser.isPresent()) {
            rejectAdmin(currentUser.get());
            return CartOwner.user(currentUser.get());
        }

        String id = guestCartId == null || guestCartId.isBlank()
                ? UUID.randomUUID().toString()
                : guestCartId;
        return CartOwner.guest(id);
    }

    private void touchOwner(CartOwner owner) {
        if (owner.user().isPresent()) {
            redisTemplate.opsForSet().add(ACTIVE_USER_CARTS, owner.user().get().getId().toString());
        } else {
            redisTemplate.expire(owner.redisKey(), GUEST_CART_TTL);
        }
    }

    private int getRedisQuantity(String redisKey, Long productId) {
        Object value = redisTemplate.opsForHash().get(redisKey, productId.toString());
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    private void assertStock(Product product, int requestedQuantity) {
        int available = productCacheService.getAvailableStock(product.getId());
        if (available < requestedQuantity) {
            throw new RuntimeException("Requested quantity is not available");
        }
    }

    private void rejectAdmin(User user) {
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin accounts cannot use customer cart features");
        }
    }

    private String guestRedisKey(String guestCartId) {
        return "cart:guest:" + guestCartId;
    }

    private record CartOwner(Optional<User> user, String guestId) {
        static CartOwner user(User user) {
            return new CartOwner(Optional.of(user), null);
        }

        static CartOwner guest(String guestId) {
            return new CartOwner(Optional.empty(), guestId);
        }

        String redisKey() {
            return user.map(value -> "cart:user:" + value.getId())
                    .orElse("cart:guest:" + guestId);
        }

        String publicKey() {
            return user.map(value -> "user:" + value.getId())
                    .orElse(guestId);
        }
    }
}
