package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Repository.BrandRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminProductService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public AdminProductService(
                               BrandRepository brandRepository,
                               ProductRepository productRepository) {

        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    public Brand createBrand(Brand brand) {

        return brandRepository.save(brand);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
}
