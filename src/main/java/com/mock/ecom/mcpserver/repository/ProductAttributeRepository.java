package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    List<ProductAttribute> findByProduct(Product product);
    List<ProductAttribute> findByProductId(UUID productId);
}
