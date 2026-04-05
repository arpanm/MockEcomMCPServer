package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByRestaurantOrderByDisplayOrderAsc(Restaurant restaurant);

    void deleteByRestaurant(Restaurant restaurant);
}
