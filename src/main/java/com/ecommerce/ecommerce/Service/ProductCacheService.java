package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Category;
import com.ecommerce.ecommerce.entity.Product;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);
    private static final String PRODUCT_IDS_KEY = "products:ids";
    private static final String STOCK_KEY = "products:stock";
    public static final String PLACEHOLDER_IMAGE =
            "https://placehold.co/600x600/e9eef5/1f2937?text=Product+Image";

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public ProductCacheService(ProductRepository productRepository,
                               RedisTemplate<String, String> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    @Transactional(readOnly = true)
    public void warmOnStartup() {
        refreshProductCache();
    }

    @Scheduled(fixedRateString = "${app.product-cache-refresh-ms:1800000}")
    @Transactional(readOnly = true)
    public void refreshProductCache() {
        Set<String> existingIds = redisTemplate.opsForSet().members(PRODUCT_IDS_KEY);
        if (existingIds != null) {
            existingIds.forEach(id -> redisTemplate.delete(productKey(Long.valueOf(id))));
        }
        redisTemplate.delete(PRODUCT_IDS_KEY);
        redisTemplate.delete(STOCK_KEY);

        List<Product> products = productRepository.findAll();
        products.forEach(this::cacheProduct);
        log.info("Refreshed Redis product cache with {} products", products.size());
    }

    public List<Product> getAllProducts() {
        Set<String> ids = redisTemplate.opsForSet().members(PRODUCT_IDS_KEY);
        if (ids == null || ids.isEmpty()) {
            refreshProductCache();
            ids = redisTemplate.opsForSet().members(PRODUCT_IDS_KEY);
        }

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return ids.stream()
                .map(Long::valueOf)
                .map(this::getProduct)
                .sorted(Comparator.comparing(Product::getId))
                .toList();
    }

    public Product getProduct(Long productId) {
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(productKey(productId));
        if (!fields.isEmpty()) {
            return fromCache(fields);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        cacheProduct(product);
        return product;
    }

    public int getAvailableStock(Long productId) {
        Object stock = redisTemplate.opsForHash().get(STOCK_KEY, productId.toString());
        if (stock != null) {
            return Integer.parseInt(stock.toString());
        }
        return getProduct(productId).getQuantity();
    }

    public void cacheProduct(Product product) {
        normalizeImage(product);
        String id = product.getId().toString();
        redisTemplate.opsForSet().add(PRODUCT_IDS_KEY, id);
        redisTemplate.opsForHash().put(productKey(product.getId()), "id", id);
        redisTemplate.opsForHash().put(productKey(product.getId()), "name", value(product.getName()));
        redisTemplate.opsForHash().put(productKey(product.getId()), "description", value(product.getDescription()));
        redisTemplate.opsForHash().put(productKey(product.getId()), "price", String.valueOf(product.getPrice()));
        redisTemplate.opsForHash().put(productKey(product.getId()), "quantity", String.valueOf(product.getQuantity()));
        redisTemplate.opsForHash().put(productKey(product.getId()), "imageUrl", value(product.getImageUrl()));
        redisTemplate.opsForHash().put(productKey(product.getId()), "category",
                product.getCategory() == null ? "" : product.getCategory().name());

        if (product.getBrand() != null) {
            redisTemplate.opsForHash().put(productKey(product.getId()), "brandId", String.valueOf(product.getBrand().getId()));
            redisTemplate.opsForHash().put(productKey(product.getId()), "brandName", value(product.getBrand().getName()));
            redisTemplate.opsForHash().put(productKey(product.getId()), "brandLogoUrl", value(product.getBrand().getLogoUrl()));
        }

        redisTemplate.opsForHash().put(STOCK_KEY, id, String.valueOf(product.getQuantity()));
    }

    private Product fromCache(Map<Object, Object> fields) {
        Product product = new Product();
        product.setId(Long.valueOf(read(fields, "id")));
        product.setName(read(fields, "name"));
        product.setDescription(read(fields, "description"));
        product.setPrice(Double.parseDouble(read(fields, "price")));
        product.setQuantity(Integer.parseInt(read(fields, "quantity")));
        product.setImageUrl(read(fields, "imageUrl"));

        String category = read(fields, "category");
        if (!category.isBlank()) {
            product.setCategory(Category.valueOf(category));
        }

        String brandId = read(fields, "brandId");
        if (!brandId.isBlank()) {
            Brand brand = new Brand();
            brand.setId(Long.valueOf(brandId));
            brand.setName(read(fields, "brandName"));
            brand.setLogoUrl(read(fields, "brandLogoUrl"));
            product.setBrand(brand);
        }

        normalizeImage(product);
        return product;
    }

    private String productKey(Long productId) {
        return "product:" + productId;
    }

    private String read(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? "" : value.toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void normalizeImage(Product product) {
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl(PLACEHOLDER_IMAGE);
        }
    }
}
