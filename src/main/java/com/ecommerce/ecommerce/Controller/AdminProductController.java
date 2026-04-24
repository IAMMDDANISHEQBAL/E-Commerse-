package com.ecommerce.ecommerce.Controller;

import com.ecommerce.ecommerce.entity.Brand;
import com.ecommerce.ecommerce.entity.Category;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.Service.AdminProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminProductController {

    private final AdminProductService adminService;

    public AdminProductController(AdminProductService adminService) {
        this.adminService = adminService;
    }


    // Create Brand
    @PostMapping("/brand")
    public Brand createBrand(@RequestBody Brand brand) {
        return adminService.createBrand(brand);
    }

    // Create Product (JSON for now, image later)
    @PostMapping("/product")
    public Product createProduct(@RequestBody Product product) {
        return adminService.createProduct(product);
    }

}
