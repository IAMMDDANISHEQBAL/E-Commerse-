package com.ecommerce.ecommerce.dto;

public class GoogleLoginRequest {

    private String email;

    private String name;
    private String idToken;
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

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getGuestCartId() {
        return guestCartId;
    }

    public void setGuestCartId(String guestCartId) {
        this.guestCartId = guestCartId;
    }
}
