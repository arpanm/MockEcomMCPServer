package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByMenuCategoryOrderByIdAsc(MenuCategory menuCategory);

    List<MenuItem> findByRestaurantOrderByMenuCategoryIdAscIdAsc(Restaurant restaurant);

    void deleteByRestaurant(Restaurant restaurant);

    long countByRestaurant(Restaurant restaurant);

    @Query("SELECT mi FROM MenuItem mi WHERE " +
           "mi.restaurant = :restaurant AND " +
           "(:isVeg IS NULL OR mi.isVeg = :isVeg) AND " +
           "(:inStock IS NULL OR mi.inStock = :inStock)")
    List<MenuItem> findByRestaurantAndFilters(
            @Param("restaurant") Restaurant restaurant,
            @Param("isVeg") Boolean isVeg,
            @Param("inStock") Boolean inStock);

    @Query("SELECT mi FROM MenuItem mi WHERE " +
           "LOWER(mi.name) LIKE LOWER(CONCAT('%', :query, '%')) AND " +
           "mi.restaurant.city.name = :cityName")
    Page<MenuItem> searchMenuItemsByCityName(
            @Param("query") String query,
            @Param("cityName") String cityName,
            Pageable pageable);

    @Query("SELECT mi FROM MenuItem mi WHERE " +
           "LOWER(mi.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<MenuItem> searchMenuItemsByName(
            @Param("query") String query,
            Pageable pageable);
}
