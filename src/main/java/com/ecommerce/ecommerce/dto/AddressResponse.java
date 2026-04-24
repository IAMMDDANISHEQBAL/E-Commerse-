package com.ecommerce.ecommerce.dto;

import com.ecommerce.ecommerce.entity.Address;

public class AddressResponse {

    private final Long id;
    private final String fullName;
    private final String phone;
    private final String line1;
    private final String line2;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;

    public AddressResponse(Address address) {
        this.id = address.getId();
        this.fullName = address.getFullName();
        this.phone = address.getPhone();
        this.line1 = address.getLine1();
        this.line2 = address.getLine2();
        this.city = address.getCity();
        this.state = address.getState();
        this.postalCode = address.getPostalCode();
        this.country = address.getCountry();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
