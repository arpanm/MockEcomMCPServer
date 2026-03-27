package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Order;
import com.mock.ecom.mcpserver.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByCustomerOrderByCreatedAtDesc(Customer customer, Pageable pageable);
}
