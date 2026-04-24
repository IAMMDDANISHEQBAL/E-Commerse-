package com.ecommerce.ecommerce.config;

import com.ecommerce.ecommerce.Repository.UserRepository;
import com.ecommerce.ecommerce.entity.Role;
import com.ecommerce.ecommerce.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminPhone;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:admin@example.com}") String adminEmail,
                          @Value("${app.admin.password:admin12345}") String adminPassword,
                          @Value("${app.admin.phone:7000000000}") String adminPhone) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminPhone = adminPhone;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPhone(adminPhone);
        admin.setUsername("Admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
    }
}
