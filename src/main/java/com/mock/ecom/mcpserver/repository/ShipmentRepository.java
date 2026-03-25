package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Order;
import com.mock.ecom.mcpserver.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findByOrder(Order order);
    @Query("SELECT s FROM Shipment s WHERE s.order.customer = :customer ORDER BY s.createdAt DESC")
    Page<Shipment> findByCustomer(@Param("customer") Customer customer, Pageable pageable);
}
