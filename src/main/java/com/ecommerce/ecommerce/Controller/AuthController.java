package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.dto.LoginRequest;
import com.ecommerce.ecommerce.dto.LoginResponse;
import com.ecommerce.ecommerce.dto.GoogleLoginRequest;
import com.ecommerce.ecommerce.dto.RegisterRequest;
import com.ecommerce.ecommerce.dto.VerifyOtpRequest;
import com.ecommerce.ecommerce.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================
    // REGISTER STEP 1 (SEND OTP)
    // =========================
    @PostMapping("/register-request")
    public String registerRequest(@Valid @RequestBody RegisterRequest request) {
        authService.registerRequest(request);
        return "OTP sent to your email";
    }

    // =========================
    // REGISTER STEP 2 (VERIFY OTP)
    // =========================
    @PostMapping("/register-verify")
    public String verifyRegister(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyRegisterOtp(request);
        return "Registration successful";
    }

    // =========================
    // LOGIN (PASSWORD BASED - TEMP)
    // =========================
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public LoginResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.googleLogin(request);
    }
}
