package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySearchKey(String searchKey);
    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Product> searchByQuery(@Param("q") String query, Pageable pageable);
    @Query("SELECT p FROM Product p WHERE (:cat IS NULL OR LOWER(p.category)=LOWER(:cat)) AND (:sub IS NULL OR LOWER(p.subCategory)=LOWER(:sub)) AND (:brand IS NULL OR LOWER(p.brand)=LOWER(:brand))")
    Page<Product> filterProducts(@Param("cat") String category, @Param("sub") String subCategory, @Param("brand") String brand, Pageable pageable);
    @Query("SELECT DISTINCT p.category FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<String> findDistinctCategoriesByQuery(@Param("q") String query);
    @Query("SELECT DISTINCT p.subCategory FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) AND p.subCategory IS NOT NULL")
    List<String> findDistinctSubCategoriesByQuery(@Param("q") String query);
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) AND p.brand IS NOT NULL")
    List<String> findDistinctBrandsByQuery(@Param("q") String query);
}
