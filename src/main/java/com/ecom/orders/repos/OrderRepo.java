package com.ecom.orders.repos;

import com.ecom.orders.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
