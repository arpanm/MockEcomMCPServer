package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByName(String name);

    boolean existsByName(String name);

    List<City> findByRestaurantsScrapedFalseOrderByNameAsc();

    List<City> findByRestaurantsScrapedTrueOrderByNameAsc();

    @Query("SELECT c FROM City c ORDER BY c.restaurantCount DESC")
    List<City> findAllOrderByRestaurantCountDesc();
}
