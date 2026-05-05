package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Service.ProductCacheService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductCacheService productCacheService;

    public ProductController(ProductCacheService productCacheService) {
        this.productCacheService = productCacheService;
    }

    // Get all products (public)
    @GetMapping
    public List<Product> getAllProducts() {
        return productCacheService.getAllProducts();
    }

    @GetMapping("/{productId}")
    public Product getProduct(@PathVariable Long productId) {
        return productCacheService.getProduct(productId);
    }
}
