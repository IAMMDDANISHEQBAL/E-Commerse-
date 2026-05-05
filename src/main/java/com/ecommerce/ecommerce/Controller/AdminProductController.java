package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Service.AdminProductService;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminProductController {

    private final AdminProductService adminService;

    public AdminProductController(AdminProductService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/brands")
    public List<Brand> listBrands() {
        return adminService.listBrands();
    }


    // Create Brand
    @PostMapping("/brand")
    public Brand createBrand(@RequestBody Brand brand) {
        return adminService.createBrand(brand);
    }

    @PutMapping("/brand/{brandId}")
    public Brand updateBrand(@PathVariable Long brandId, @RequestBody Brand brand) {
        return adminService.updateBrand(brandId, brand);
    }

    @DeleteMapping("/brand/{brandId}")
    public void deleteBrand(@PathVariable Long brandId) {
        adminService.deleteBrand(brandId);
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return adminService.listProducts();
    }

    // Create Product (JSON for now, image later)
    @PostMapping("/product")
    public Product createProduct(@RequestBody Product product) {
        return adminService.createProduct(product);
    }

    @PutMapping("/product/{productId}")
    public Product updateProduct(@PathVariable Long productId, @RequestBody Product product) {
        return adminService.updateProduct(productId, product);
    }

    @PutMapping("/product/{productId}/inventory")
    public Product updateInventory(@PathVariable Long productId,
                                   @RequestParam @Min(0) int quantity) {
        return adminService.updateInventory(productId, quantity);
    }

    @DeleteMapping("/product/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        adminService.deleteProduct(productId);
    }

}
