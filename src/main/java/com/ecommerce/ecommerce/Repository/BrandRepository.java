package com.ecommerce.ecommerce.Repository;



import com.ecommerce.ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
