package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Order;
import com.mock.ecom.mcpserver.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrder(Order order);
}
