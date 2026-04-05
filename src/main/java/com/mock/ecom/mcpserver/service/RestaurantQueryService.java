package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.City;
import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.repository.CityRepository;
import com.mock.ecom.mcpserver.repository.MenuCategoryRepository;
import com.mock.ecom.mcpserver.repository.MenuItemRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.*;

/**
 * Service for querying scraped restaurant and menu data.
 * Provides structured data for MCP tools.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantQueryService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public List<City> getAllCities() {
        return cityRepository.findAllOrderByRestaurantCountDesc();
    }

    public Optional<City> getCityByName(String name) {
        return cityRepository.findByName(name);
    }

    public Page<Restaurant> searchRestaurants(String cityName, String cuisine, String name,
                                               Boolean isPureVeg, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page, Math.min(pageSize, 50),
                Sort.by(Sort.Direction.DESC, "avgRating"));
        return restaurantRepository.searchRestaurants(
                cityName != null && !cityName.isBlank() ? cityName : null,
                cuisine != null && !cuisine.isBlank() ? cuisine : null,
                name != null && !name.isBlank() ? name : null,
                isPureVeg,
                pageable);
    }

    public Optional<Restaurant> getRestaurantBySwiggyId(String swiggyId) {
        return restaurantRepository.findBySwiggyId(swiggyId);
    }

    public Optional<Restaurant> getRestaurantById(Long id) {
        return restaurantRepository.findById(id);
    }

    /**
     * Returns the full menu for a restaurant grouped by category.
     * Each entry has: categoryName, items (list of item maps).
     */
    public List<Map<String, Object>> getRestaurantMenu(Restaurant restaurant) {
        List<MenuCategory> categories = menuCategoryRepository
                .findByRestaurantOrderByDisplayOrderAsc(restaurant);

        List<Map<String, Object>> menu = new ArrayList<>();
        for (MenuCategory cat : categories) {
            List<MenuItem> items = menuItemRepository.findByMenuCategoryOrderByIdAsc(cat);
            if (items.isEmpty()) continue;

            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("category", cat.getName());
            catMap.put("itemCount", items.size());
            catMap.put("items", items.stream().map(this::menuItemToMap).toList());
            menu.add(catMap);
        }
        return menu;
    }

    public List<MenuItem> getMenuItems(Restaurant restaurant, Boolean isVeg, Boolean inStock) {
        return menuItemRepository.findByRestaurantAndFilters(restaurant, isVeg, inStock);
    }

    public Page<MenuItem> searchRestaurantsByMenuItem(String query, String cityName, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, Math.min(pageSize, 50));
        if (cityName != null && !cityName.isBlank()) {
            return menuItemRepository.searchMenuItemsByCityName(query, cityName, pageable);
        }
        return menuItemRepository.searchMenuItemsByName(query, pageable);
    }

    public Map<String, Object> getScraperStats() {
        long totalCities = cityRepository.count();
        long scrapedCities = cityRepository.findByRestaurantsScrapedTrueOrderByNameAsc().size();
        long totalRestaurants = restaurantRepository.count();
        long menusScraped = restaurantRepository.countByMenuScrapedTrue();
        long menusPending = restaurantRepository.countByMenuScrapedFalse();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCities", totalCities);
        stats.put("scrapedCities", scrapedCities);
        stats.put("pendingCities", totalCities - scrapedCities);
        stats.put("totalRestaurants", totalRestaurants);
        stats.put("restaurantsWithMenu", menusScraped);
        stats.put("restaurantsPendingMenu", menusPending);
        return stats;
    }

    public Map<String, Object> restaurantToMap(Restaurant r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("swiggyId", r.getSwiggyId());
        map.put("name", r.getName());
        map.put("city", r.getCity() != null ? r.getCity().getName() : null);
        map.put("locality", r.getLocality());
        map.put("areaName", r.getAreaName());
        map.put("cuisines", r.getCuisines());
        map.put("avgRating", r.getAvgRating());
        map.put("totalRatings", r.getTotalRatingsString());
        map.put("costForTwo", r.getCostForTwo() != null ? r.getCostForTwo() / 100 : null);
        map.put("costForTwoMessage", r.getCostForTwoMessage());
        map.put("deliveryTime", r.getDeliveryTime());
        map.put("isOpen", r.getIsOpen());
        map.put("isPureVeg", r.getIsPureVeg());
        map.put("discount", r.getDiscountInfo());
        map.put("imageUrl", buildImageUrl(r.getCloudinaryImageId()));
        map.put("menuScraped", r.getMenuScraped());
        return map;
    }

    public Map<String, Object> menuItemToMap(MenuItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("description", item.getDescription());
        map.put("price", item.getPrice() != null ? item.getPrice() / 100.0 : null);
        map.put("isVeg", item.getIsVeg());
        map.put("inStock", item.getInStock());
        map.put("isBestSeller", item.getIsBestSeller());
        map.put("imageUrl", buildImageUrl(item.getCloudinaryImageId()));
        map.put("avgRating", item.getAvgRatingString());
        return map;
    }

    public Map<String, Object> cityToMap(City city) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", city.getId());
        map.put("name", city.getName());
        map.put("state", city.getState());
        map.put("restaurantCount", city.getRestaurantCount());
        map.put("restaurantsScraped", city.getRestaurantsScraped());
        map.put("lastScrapedAt", city.getLastScrapedAt());
        return map;
    }

    private String buildImageUrl(String cloudinaryImageId) {
        if (cloudinaryImageId == null || cloudinaryImageId.isBlank()) return null;
        return "https://media-assets.swiggy.com/swiggy/image/upload/fl_lossy,f_auto,q_auto,w_660/" + cloudinaryImageId;
    }
}
