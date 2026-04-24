package com.ecommerce.ecommerce.Repository;

import com.ecommerce.ecommerce.entity.CustomerOrder;
import com.ecommerce.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(CustomerOrder order);
}
