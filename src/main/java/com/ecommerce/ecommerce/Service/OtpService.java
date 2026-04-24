package com.ecommerce.ecommerce.Service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;

    public OtpService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndStoreOtp(String key) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        redisTemplate.opsForValue()
                .set("otp:" + key, otp, Duration.ofMinutes(5));

        return otp;
    }

    public boolean verifyOtp(String key, String inputOtp) {
        String savedOtp = redisTemplate.opsForValue().get("otp:" + key);

        if (savedOtp == null) return false;
        if (!savedOtp.equals(inputOtp)) return false;

        redisTemplate.delete("otp:" + key);
        return true;
    }
}
