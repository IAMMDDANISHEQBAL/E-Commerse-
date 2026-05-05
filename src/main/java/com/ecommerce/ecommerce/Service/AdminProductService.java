package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Repository.BrandRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminProductService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    public AdminProductService(
                               BrandRepository brandRepository,
                               ProductRepository productRepository,
                               ProductCacheService productCacheService) {

        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
    }

    @Transactional(readOnly = true)
    public List<Brand> listBrands() {
        return brandRepository.findAll();
    }

    public Brand createBrand(Brand brand) {

        return brandRepository.save(brand);
    }

    public Brand updateBrand(Long brandId, Brand request) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        brand.setName(request.getName());
        brand.setLogoUrl(request.getLogoUrl());
        Brand saved = brandRepository.save(brand);
        productCacheService.refreshProductCache();
        return saved;
    }

    public void deleteBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        brandRepository.delete(brand);
        productCacheService.refreshProductCache();
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    public Product createProduct(Product product) {
        normalizeProduct(product);
        Product saved = productRepository.save(product);
        productCacheService.cacheProduct(saved);
        return saved;
    }

    public Product updateProduct(Long productId, Product request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        normalizeProduct(product);

        Product saved = productRepository.save(product);
        productCacheService.cacheProduct(saved);
        return saved;
    }

    public Product updateInventory(Long productId, int quantity) {
        if (quantity < 0) {
            throw new RuntimeException("Inventory cannot be negative");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setQuantity(quantity);
        Product saved = productRepository.save(product);
        productCacheService.cacheProduct(saved);
        return saved;
    }

    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);
        productCacheService.evictProduct(productId);
    }

    private void normalizeProduct(Product product) {
        if (product.getQuantity() < 0) {
            throw new RuntimeException("Inventory cannot be negative");
        }
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl(ProductCacheService.PLACEHOLDER_IMAGE);
        }
        if (product.getBrand() != null && product.getBrand().getId() != null) {
            product.setBrand(brandRepository.findById(product.getBrand().getId())
                    .orElseThrow(() -> new RuntimeException("Brand not found")));
        }
    }
}
