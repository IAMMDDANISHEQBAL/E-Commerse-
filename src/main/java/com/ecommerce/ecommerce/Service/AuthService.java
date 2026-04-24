package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.dto.LoginRequest;
import com.ecommerce.ecommerce.dto.LoginResponse;
import com.ecommerce.ecommerce.dto.RegisterRequest;
import com.ecommerce.ecommerce.dto.VerifyOtpRequest;
import com.ecommerce.ecommerce.entity.Role;
import com.ecommerce.ecommerce.entity.User;
import com.ecommerce.ecommerce.Repository.UserRepository;
import com.ecommerce.ecommerce.Security.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       EmailService emailService,
                       RedisTemplate<String, String> redisTemplate) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    // =========================
    // REGISTER STEP 1 (SEND OTP)
    // =========================
    public void registerRequest(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String key = request.getEmail();

        // Generate OTP
        String otp = otpService.generateAndStoreOtp(key);

        // Store temp user data in Redis
        redisTemplate.opsForHash().put("register:" + key, "email", request.getEmail());
        redisTemplate.opsForHash().put("register:" + key, "phone", request.getPhone());
        redisTemplate.opsForHash().put("register:" + key, "username", request.getUsername());
        redisTemplate.opsForHash().put("register:" + key, "password",
                passwordEncoder.encode(request.getPassword()));

        redisTemplate.expire("register:" + key, Duration.ofMinutes(5));

        // Send OTP (mock email)
        emailService.sendOtp(request.getEmail(), otp);
    }

    // =========================
    // REGISTER STEP 2 (VERIFY OTP)
    // =========================
    public void verifyRegisterOtp(VerifyOtpRequest request) {

        String key = request.getEmail();

        boolean valid = otpService.verifyOtp(key, request.getOtp());
        if (!valid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        Map<Object, Object> data =
                redisTemplate.opsForHash().entries("register:" + key);

        if (data.isEmpty()) {
            throw new RuntimeException("No registration data found");
        }

        User user = new User();
        user.setEmail((String) data.get("email"));
        user.setPhone((String) data.get("phone"));
        user.setUsername((String) data.get("username"));
        user.setPassword((String) data.get("password"));
        user.setRole(Role.USER);

        userRepository.save(user);

        redisTemplate.delete("register:" + key);
    }

    // =========================
    // LOGIN WITH PASSWORD (OLD WAY)
    // =========================
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getRole().name()
        );

        return new LoginResponse(token);
    }
}
