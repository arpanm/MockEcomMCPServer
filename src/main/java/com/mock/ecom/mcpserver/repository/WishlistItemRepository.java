package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    Page<WishlistItem> findByCustomerOrderByAddedAtDesc(Customer customer, Pageable pageable);
    Optional<WishlistItem> findByCustomerAndProduct(Customer customer, Product product);
    boolean existsByCustomerAndProduct(Customer customer, Product product);
}
