package com.ecommerce.ecommerce.Service;



import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendOtp(String email, String otp) {
        // For now, just print OTP to console
        System.out.println("Sending OTP to " + email + ": " + otp);
    }
}
