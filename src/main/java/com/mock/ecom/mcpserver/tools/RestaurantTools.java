package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.City;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.service.RestaurantQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP tools for querying scraped Swiggy restaurant and menu data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantTools {

    private final RestaurantQueryService queryService;
    private final ToolResponseHelper helper;

    @Tool(description = "List all Indian cities available in the restaurant database. Returns city name, state, total restaurant count, and whether restaurants have been scraped. Use this to discover which cities have data before searching for restaurants.")
    public String listRestaurantCities() {
        try {
            log.info("[Tool] listRestaurantCities");
            List<City> cities = queryService.getAllCities();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalCities", cities.size());
            response.put("cities", cities.stream().map(queryService::cityToMap).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] listRestaurantCities error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Search for restaurants by city, cuisine, name, or veg/non-veg preference. All parameters are optional. cityName: e.g. 'Mumbai', 'Bangalore'. cuisine: e.g. 'Indian', 'Chinese', 'Pizza'. name: partial restaurant name. isPureVeg: true/false. Returns restaurant details including rating, delivery time, cost for two, cuisines, and discount offers.")
    public String searchRestaurants(String cityName, String cuisine, String name,
                                     Boolean isPureVeg, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] searchRestaurants city={} cuisine={} name={} veg={}", cityName, cuisine, name, isPureVeg);
            int p = page != null ? page : 0;
            int s = pageSize != null ? Math.min(pageSize, 50) : 20;
            Page<Restaurant> result = queryService.searchRestaurants(cityName, cuisine, name, isPureVeg, p, s);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("page", p);
            response.put("pageSize", s);
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("restaurants", result.getContent().stream().map(queryService::restaurantToMap).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] searchRestaurants error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get detailed information about a specific restaurant by its Swiggy restaurant ID. Returns full details including name, city, locality, cuisines, rating, cost for two, delivery time, and whether menu data is available.")
    public String getRestaurantBySwiggyId(String swiggyId) {
        try {
            log.info("[Tool] getRestaurantBySwiggyId swiggyId={}", swiggyId);
            Optional<Restaurant> r = queryService.getRestaurantBySwiggyId(swiggyId);
            if (r.isEmpty()) return helper.error("Restaurant not found: " + swiggyId);
            return helper.toJson(queryService.restaurantToMap(r.get()));
        } catch (Exception e) {
            log.error("[Tool] getRestaurantBySwiggyId error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get the full menu of a restaurant by its Swiggy restaurant ID. Returns menu organized by categories (e.g. Starters, Main Course, Desserts) with each item's name, description, price in INR, veg/non-veg flag, availability, and bestseller status. Menu data must be scraped first using scrapeRestaurantMenu tool.")
    public String getRestaurantMenu(String swiggyId) {
        try {
            log.info("[Tool] getRestaurantMenu swiggyId={}", swiggyId);
            Optional<Restaurant> r = queryService.getRestaurantBySwiggyId(swiggyId);
            if (r.isEmpty()) return helper.error("Restaurant not found: " + swiggyId);

            Restaurant restaurant = r.get();
            if (!Boolean.TRUE.equals(restaurant.getMenuScraped())) {
                return helper.error("Menu not yet scraped for this restaurant. Use scrapeRestaurantMenu tool first.");
            }

            List<Map<String, Object>> menu = queryService.getRestaurantMenu(restaurant);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("restaurantId", swiggyId);
            response.put("restaurantName", restaurant.getName());
            response.put("city", restaurant.getCity() != null ? restaurant.getCity().getName() : null);
            response.put("totalCategories", menu.size());
            response.put("totalItems", menu.stream().mapToInt(c -> (Integer) c.get("itemCount")).sum());
            response.put("menu", menu);
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] getRestaurantMenu error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Search for specific menu items across all restaurants in a city. Useful for finding which restaurants serve a particular dish. query: item name to search (e.g. 'biryani', 'pizza'). cityName: limit to a specific city. Returns matching items with restaurant name, price, veg flag, and availability.")
    public String searchMenuItems(String query, String cityName, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] searchMenuItems query={} city={}", query, cityName);
            int p = page != null ? page : 0;
            int s = pageSize != null ? Math.min(pageSize, 50) : 20;
            var result = queryService.searchRestaurantsByMenuItem(query, cityName, p, s);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("city", cityName);
            response.put("page", p);
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("items", result.getContent().stream().map(item -> {
                Map<String, Object> m = queryService.menuItemToMap(item);
                m.put("restaurantName", item.getRestaurant() != null ? item.getRestaurant().getName() : null);
                m.put("restaurantSwiggyId", item.getRestaurant() != null ? item.getRestaurant().getSwiggyId() : null);
                return m;
            }).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] searchMenuItems error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Filter menu items for a specific restaurant by veg/non-veg and availability. swiggyId: the restaurant's Swiggy ID. isVeg: true for veg only, false for non-veg only, null for all. inStock: true for available items only. Returns filtered list of menu items with price and category.")
    public String filterRestaurantMenuItems(String swiggyId, Boolean isVeg, Boolean inStock) {
        try {
            log.info("[Tool] filterRestaurantMenuItems swiggyId={} veg={} inStock={}", swiggyId, isVeg, inStock);
            Optional<Restaurant> r = queryService.getRestaurantBySwiggyId(swiggyId);
            if (r.isEmpty()) return helper.error("Restaurant not found: " + swiggyId);

            List<MenuItem> items = queryService.getMenuItems(r.get(), isVeg, inStock);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("restaurantName", r.get().getName());
            response.put("filters", Map.of(
                    "isVeg", isVeg != null ? isVeg : "all",
                    "inStock", inStock != null ? inStock : "all"));
            response.put("totalItems", items.size());
            response.put("items", items.stream().map(item -> {
                Map<String, Object> m = queryService.menuItemToMap(item);
                m.put("category", item.getMenuCategory() != null ? item.getMenuCategory().getName() : null);
                return m;
            }).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] filterRestaurantMenuItems error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
