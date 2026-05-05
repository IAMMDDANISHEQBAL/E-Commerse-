package com.ecommerce.ecommerce.config;

import com.ecommerce.ecommerce.Repository.BrandRepository;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.Service.ProductCacheService;
import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Category;
import com.ecommerce.ecommerce.entity.Product;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogBootstrap implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    public CatalogBootstrap(BrandRepository brandRepository,
                            ProductRepository productRepository,
                            ProductCacheService productCacheService) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            productCacheService.refreshProductCache();
            return;
        }

        Brand apex = saveBrand("Apex", "https://placehold.co/240x120/0f766e/ffffff?text=Apex");
        Brand nova = saveBrand("Nova", "https://placehold.co/240x120/1f2937/ffffff?text=Nova");
        Brand urban = saveBrand("UrbanCraft", "https://placehold.co/240x120/be6d00/ffffff?text=UrbanCraft");

        List<Product> products = List.of(
                product("Apex Wireless Headphones", "Active noise cancellation, 40-hour battery, low-latency gaming mode.",
                        3499, 24, Category.ELECTRONICS, apex, "https://placehold.co/800x700/e6f4f1/0f766e?text=Wireless+Headphones"),
                product("Nova Smart Watch", "AMOLED display, health tracking, GPS workouts, and seven-day battery life.",
                        4999, 18, Category.ELECTRONICS, nova, "https://placehold.co/800x700/eef2ff/3730a3?text=Smart+Watch"),
                product("Urban Runner Shoes", "Lightweight daily trainers with breathable mesh and cushioned sole.",
                        2999, 30, Category.FOOTWEAR, urban, "https://placehold.co/800x700/fef3c7/92400e?text=Running+Shoes"),
                product("Everyday Travel Backpack", "Water-resistant 24L backpack with laptop sleeve and organizer pockets.",
                        1899, 40, Category.ACCESSORIES, urban, "https://placehold.co/800x700/ecfeff/155e75?text=Backpack"),
                product("Classic Cotton Hoodie", "Soft fleece hoodie with relaxed fit and durable ribbed cuffs.",
                        1499, 35, Category.CLOTHING, urban, "https://placehold.co/800x700/f1f5f9/334155?text=Cotton+Hoodie"),
                product("Sterling Minimal Bracelet", "Adjustable sterling bracelet with a clean everyday profile.",
                        999, 16, Category.JWELLERY, nova, "https://placehold.co/800x700/fdf2f8/9d174d?text=Bracelet")
        );

        productRepository.saveAll(products);
        productCacheService.refreshProductCache();
    }

    private Brand saveBrand(String name, String logoUrl) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setLogoUrl(logoUrl);
        return brandRepository.save(brand);
    }

    private Product product(String name,
                            String description,
                            double price,
                            int quantity,
                            Category category,
                            Brand brand,
                            String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCategory(category);
        product.setBrand(brand);
        product.setImageUrl(imageUrl);
        return product;
    }
}
