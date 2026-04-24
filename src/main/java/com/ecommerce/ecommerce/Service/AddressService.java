package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.AddressRepository;
import com.ecommerce.ecommerce.dto.AddressRequest;
import com.ecommerce.ecommerce.dto.AddressResponse;
import com.ecommerce.ecommerce.entity.Address;
import com.ecommerce.ecommerce.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    public AddressService(AddressRepository addressRepository,
                          CurrentUserService currentUserService) {
        this.addressRepository = addressRepository;
        this.currentUserService = currentUserService;
    }

    public AddressResponse create(AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        return new AddressResponse(addressRepository.save(toAddress(request, user)));
    }

    public List<AddressResponse> list() {
        User user = currentUserService.getCurrentUser();
        return addressRepository.findByUser(user).stream()
                .map(AddressResponse::new)
                .toList();
    }

    Address toAddress(AddressRequest request, User user) {
        Address address = new Address();
        address.setUser(user);
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        return address;
    }
}
