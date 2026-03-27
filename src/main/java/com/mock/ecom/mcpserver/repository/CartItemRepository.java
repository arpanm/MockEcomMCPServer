package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Cart;
import com.mock.ecom.mcpserver.entity.CartItem;
import com.mock.ecom.mcpserver.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
