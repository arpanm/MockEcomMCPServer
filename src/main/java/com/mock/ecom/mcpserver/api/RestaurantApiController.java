package com.mock.ecom.mcpserver.api;

import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.repository.CityRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import com.mock.ecom.mcpserver.service.RestaurantQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RestaurantApiController {

    private final RestaurantQueryService queryService;
    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;

    @GetMapping("/cities")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCities() {
        var cities = queryService.getAllCities();
        var cityList = cities.stream().map(queryService::cityToMap).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cities", cityList);
        result.put("total", cityList.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/restaurants")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getRestaurants(
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isPureVeg,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Restaurant> results = queryService.searchRestaurants(cityName, cuisine, name, isPureVeg, page, size);
        var restaurants = results.getContent().stream().map(queryService::restaurantToMap).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", results.getNumber());
        result.put("size", results.getSize());
        result.put("totalElements", results.getTotalElements());
        result.put("totalPages", results.getTotalPages());
        result.put("restaurants", restaurants);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/restaurants/{swiggyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getRestaurantBySwiggyId(@PathVariable String swiggyId) {
        Optional<Restaurant> restaurant = queryService.getRestaurantBySwiggyId(swiggyId);
        if (restaurant.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }
        return ResponseEntity.ok(queryService.restaurantToMap(restaurant.get()));
    }

    @GetMapping("/restaurants/{swiggyId}/menu")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getRestaurantMenu(@PathVariable String swiggyId) {
        Optional<Restaurant> optRestaurant = queryService.getRestaurantBySwiggyId(swiggyId);
        if (optRestaurant.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }
        Restaurant restaurant = optRestaurant.get();
        List<Map<String, Object>> menu = queryService.getRestaurantMenu(restaurant);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("restaurantName", restaurant.getName());
        result.put("city", restaurant.getCity() != null ? restaurant.getCity().getName() : null);
        result.put("swiggyId", restaurant.getSwiggyId());
        result.put("menu", menu);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/menu-items")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> searchMenuItems(
            @RequestParam String q,
            @RequestParam(required = false) String cityName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MenuItem> results = queryService.searchRestaurantsByMenuItem(q, cityName, page, Math.min(size, 50));

        var items = results.getContent().stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("name", item.getName());
            map.put("description", item.getDescription());
            map.put("price", item.getPrice() != null ? item.getPrice() / 100.0 : null);
            map.put("isVeg", item.getIsVeg());
            map.put("inStock", item.getInStock());
            map.put("isBestSeller", item.getIsBestSeller());
            map.put("restaurantName", item.getRestaurant().getName());
            map.put("restaurantSwiggyId", item.getRestaurant().getSwiggyId());
            map.put("cityName", item.getRestaurant().getCity() != null
                    ? item.getRestaurant().getCity().getName() : null);
            return map;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", results.getNumber());
        result.put("totalElements", results.getTotalElements());
        result.put("totalPages", results.getTotalPages());
        result.put("items", items);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cuisines")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> getCuisines(
            @RequestParam(required = false) String cityName) {

        List<Restaurant> allRestaurants = restaurantRepository.findAll();

        var stream = allRestaurants.stream();
        if (cityName != null && !cityName.isBlank()) {
            stream = stream.filter(r -> r.getCity() != null
                    && r.getCity().getName().equalsIgnoreCase(cityName));
        }

        List<String> cuisines = stream
                .map(Restaurant::getCuisines)
                .filter(c -> c != null && !c.isBlank())
                .flatMap(c -> Arrays.stream(c.split(", ")))
                .map(String::trim)
                .filter(c -> !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return ResponseEntity.ok(cuisines);
    }
}
