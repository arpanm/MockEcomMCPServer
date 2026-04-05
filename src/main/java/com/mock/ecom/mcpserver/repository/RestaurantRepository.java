package com.mock.ecom.mcpserver.repository;

import com.mock.ecom.mcpserver.entity.City;
import com.mock.ecom.mcpserver.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findBySwiggyId(String swiggyId);

    boolean existsBySwiggyId(String swiggyId);

    Page<Restaurant> findByCityOrderByAvgRatingDesc(City city, Pageable pageable);

    Page<Restaurant> findByMenuScrapedFalseAndCityOrderByIdAsc(City city, Pageable pageable);

    List<Restaurant> findByMenuScrapedFalseOrderByIdAsc(Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.menuScraped = false ORDER BY r.id ASC")
    List<Restaurant> findUnscrapedMenuRestaurants(Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE " +
           "(:cityName IS NULL OR r.city.name = :cityName) AND " +
           "(:cuisine IS NULL OR LOWER(r.cuisines) LIKE LOWER(CONCAT('%', :cuisine, '%'))) AND " +
           "(:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:isPureVeg IS NULL OR r.isPureVeg = :isPureVeg)")
    Page<Restaurant> searchRestaurants(
            @Param("cityName") String cityName,
            @Param("cuisine") String cuisine,
            @Param("name") String name,
            @Param("isPureVeg") Boolean isPureVeg,
            Pageable pageable);

    @Query("SELECT DISTINCT r.cuisines FROM Restaurant r WHERE r.city.name = :cityName")
    List<String> findDistinctCuisinesByCityName(@Param("cityName") String cityName);

    long countByCity(City city);

    long countByMenuScrapedTrue();

    long countByMenuScrapedFalse();
}
