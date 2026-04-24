package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all products (public)
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
