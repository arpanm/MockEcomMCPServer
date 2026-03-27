package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Cart;
import com.mock.ecom.mcpserver.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findFirstByCustomerAndStatus(Customer customer, Cart.CartStatus status);
    List<Cart> findByCustomerAndStatus(Customer customer, Cart.CartStatus status);
}
