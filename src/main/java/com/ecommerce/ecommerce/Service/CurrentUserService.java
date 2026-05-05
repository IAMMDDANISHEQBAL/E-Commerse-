package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.UserRepository;
import com.ecommerce.ecommerce.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        return getOptionalCurrentUser()
                .orElseThrow(() -> new RuntimeException("Authentication required"));
    }

    public Optional<User> getOptionalCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        if ("anonymousUser".equals(authentication.getPrincipal().toString())) {
            return Optional.empty();
        }

        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        return userRepository.findById(userId);
    }
}
