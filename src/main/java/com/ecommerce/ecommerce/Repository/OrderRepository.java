package com.ecommerce.ecommerce.Repository;

import com.ecommerce.ecommerce.entity.CustomerOrder;
import com.ecommerce.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserOrderByCreatedAtDesc(User user);

    Optional<CustomerOrder> findByIdAndUser(Long id, User user);
}
