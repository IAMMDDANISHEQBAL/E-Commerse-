package com.ecommerce.ecommerce;

import com.ecommerce.ecommerce.Repository.*;
import com.ecommerce.ecommerce.Security.JwtUtil;
import com.ecommerce.ecommerce.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcommerceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.delete("otp:newcustomer@example.com");
        redisTemplate.delete("register:newcustomer@example.com");

        user = new User();
        user.setEmail("customer@example.com");
        user.setPhone("9999999999");
        user.setUsername("Customer");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        Brand brand = new Brand();
        brand.setName("Acme");
        brand.setLogoUrl("https://example.com/logo.png");
        brand = brandRepository.save(brand);

        product = new Product();
        product.setName("Running Shoes");
        product.setDescription("Daily running shoes");
        product.setPrice(2999.00);
        product.setQuantity(10);
        product.setImageUrl("https://example.com/shoes.png");
        product.setCategory(Category.FOOTWEAR);
        product.setBrand(brand);
        product = productRepository.save(product);
    }

    @Test
    void userCanRegisterWithRedisOtpAndThenLogin() throws Exception {
        mockMvc.perform(post("/auth/register-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newcustomer@example.com",
                                  "phone": "8888888888",
                                  "username": "New Customer",
                                  "password": "password123",
                                  "confirmPassword": "password123"
                                }
                                """))
                .andExpect(status().isOk());

        String otp = redisTemplate.opsForValue().get("otp:newcustomer@example.com");
        assertThat(otp).isNotBlank();

        mockMvc.perform(post("/auth/register-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newcustomer@example.com",
                                  "otp": "%s"
                                }
                                """.formatted(otp)))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey("otp:newcustomer@example.com")).isFalse();
        assertThat(redisTemplate.hasKey("register:newcustomer@example.com")).isFalse();
        assertThat(userRepository.findByEmail("newcustomer@example.com")).isPresent();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newcustomer@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void adminCanCreateBrandAndProductThenCustomerCanViewProduct() throws Exception {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPhone("7777777777");
        admin.setUsername("Admin");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        admin = userRepository.save(admin);

        String adminToken = jwtUtil.generateToken(admin.getId(), admin.getRole().name());

        String brandResponse = mockMvc.perform(post("/admin/brand")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nova",
                                  "logoUrl": "https://example.com/nova.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nova"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long brandId = objectMapper.readTree(brandResponse).get("id").asLong();

        mockMvc.perform(post("/admin/product")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nova Watch",
                                  "description": "Smart watch",
                                  "price": 4999.0,
                                  "quantity": 5,
                                  "imageUrl": "https://example.com/watch.png",
                                  "category": "ELECTRONICS",
                                  "brand": {
                                    "id": %d
                                  }
                                }
                                """.formatted(brandId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nova Watch"));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Nova Watch')]").exists());
    }

    @Test
    void customerCanCheckoutAndPayForAnOrder() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Running Shoes"));

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 2
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(5998.0));

        String checkoutResponse = mockMvc.perform(post("/orders/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "address": {
                                    "fullName": "Customer",
                                    "phone": "9999999999",
                                    "line1": "221B Baker Street",
                                    "line2": "Floor 2",
                                    "city": "Bengaluru",
                                    "state": "Karnataka",
                                    "postalCode": "560001",
                                    "country": "India"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode orderJson = objectMapper.readTree(checkoutResponse);
        long orderId = orderJson.get("id").asLong();

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(8);

        mockMvc.perform(get("/payments/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/payments/orders/{orderId}/pay", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerToken": "test-payment-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.providerReference").exists());

        mockMvc.perform(get("/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void protectedCartEndpointRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtTokenCanAccessProtectedCartEndpoint() throws Exception {
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
