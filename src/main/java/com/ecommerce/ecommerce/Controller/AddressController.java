package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.Service.AddressService;
import com.ecommerce.ecommerce.dto.AddressRequest;
import com.ecommerce.ecommerce.dto.AddressResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public AddressResponse create(@Valid @RequestBody AddressRequest request) {
        return addressService.create(request);
    }

    @GetMapping
    public List<AddressResponse> list() {
        return addressService.list();
    }
}
