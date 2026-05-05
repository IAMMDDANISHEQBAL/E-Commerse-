package com.ecommerce.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequest {

    @Email
    @NotBlank
    private String email;

    private String name;
    private String googleToken;
    private String guestCartId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGoogleToken() {
        return googleToken;
    }

    public void setGoogleToken(String googleToken) {
        this.googleToken = googleToken;
    }

    public String getGuestCartId() {
        return guestCartId;
    }

    public void setGuestCartId(String guestCartId) {
        this.guestCartId = guestCartId;
    }
}
