package com.kushwahacafe.cafe.repository;

import com.kushwahacafe.cafe.model.Order;
import com.kushwahacafe.cafe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByPaymentStatus(String paymentStatus);
}
