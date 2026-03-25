package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Customer;
import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.Review;
import com.mock.ecom.mcpserver.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductOrderByCreatedAtDesc(Product product, Pageable pageable);
    Page<Review> findByShipmentOrderByCreatedAtDesc(Shipment shipment, Pageable pageable);
    Optional<Review> findByProductAndCustomer(Product product, Customer customer);
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product")
    Double findAverageRatingByProduct(@Param("product") Product product);
    long countByProduct(Product product);
}
