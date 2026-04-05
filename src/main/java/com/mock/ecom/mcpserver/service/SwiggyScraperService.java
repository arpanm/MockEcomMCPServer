package com.mock.ecom.mcpserver.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates incremental scraping of restaurant and menu data from Swiggy.
 * Tracks progress in the database so scraping can be resumed without re-fetching
 * already-scraped cities or restaurants.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SwiggyScraperService {

    private final SwiggyApiClient apiClient;
    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    private static final int MAX_PAGES_PER_CITY = 10;
    private static final int MENU_BATCH_SIZE = 20;

    private final AtomicBoolean restaurantScrapingRunning = new AtomicBoolean(false);
    private final AtomicBoolean menuScrapingRunning = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // Restaurant scraping
    // -------------------------------------------------------------------------

    /**
     * Asynchronously scrape restaurants for all un-scraped cities.
     * Safe to call multiple times - skips already-scraped cities.
     */
    @Async
    public void scrapeAllCitiesAsync() {
        if (!restaurantScrapingRunning.compareAndSet(false, true)) {
            log.info("Restaurant scraping already running, skipping.");
            return;
        }
        try {
            List<City> pending = cityRepository.findByRestaurantsScrapedFalseOrderByNameAsc();
            log.info("Starting restaurant scraping for {} cities", pending.size());
            for (City city : pending) {
                scrapeRestaurantsForCity(city);
            }
            log.info("Completed restaurant scraping for all cities");
        } finally {
            restaurantScrapingRunning.set(false);
        }
    }

    /**
     * Scrape restaurants for a single city by name.
     * Returns the number of new restaurants saved.
     */
    public int scrapeRestaurantsForCityByName(String cityName) {
        City city = cityRepository.findByName(cityName)
                .orElseThrow(() -> new IllegalArgumentException("City not found: " + cityName));
        return scrapeRestaurantsForCity(city);
    }

    public int scrapeRestaurantsForCity(City city) {
        log.info("Scraping restaurants for city: {} ({}, {})", city.getName(), city.getLatitude(), city.getLongitude());
        int totalSaved = 0;
        int page = 0;
        String pageContext = null;

        Optional<JsonNode> firstPage = apiClient.fetchRestaurantListPage(city.getLatitude(), city.getLongitude());
        if (firstPage.isEmpty()) {
            log.warn("No response for city: {}", city.getName());
            return 0;
        }

        totalSaved += processRestaurantPage(firstPage.get(), city);
        pageContext = apiClient.extractPageContextData(firstPage.get()).orElse(null);
        page++;

        while (page < MAX_PAGES_PER_CITY && pageContext != null) {
            Optional<JsonNode> nextPage = apiClient.fetchRestaurantListNextPage(
                    city.getLatitude(), city.getLongitude(), page * 15, pageContext);
            if (nextPage.isEmpty()) break;

            int saved = processRestaurantPage(nextPage.get(), city);
            totalSaved += saved;
            pageContext = apiClient.extractPageContextData(nextPage.get()).orElse(null);

            if (saved == 0) break; // No new restaurants, stop paginating
            page++;
        }

        city.setRestaurantsScraped(true);
        city.setRestaurantCount((int) restaurantRepository.countByCity(city));
        city.setLastScrapedAt(LocalDateTime.now());
        cityRepository.save(city);

        log.info("Saved {} restaurants for city: {}", totalSaved, city.getName());
        return totalSaved;
    }

    protected int processRestaurantPage(JsonNode response, City city) {
        List<JsonNode> restaurantNodes = apiClient.extractRestaurantNodes(response);
        int saved = 0;
        for (JsonNode info : restaurantNodes) {
            try {
                String swiggyId = info.path("id").asText(null);
                if (swiggyId == null || swiggyId.isBlank()) continue;

                if (restaurantRepository.existsBySwiggyId(swiggyId)) {
                    // Update existing restaurant with latest data
                    restaurantRepository.findBySwiggyId(swiggyId).ifPresent(existing -> {
                        updateRestaurantFromNode(existing, info, city);
                        restaurantRepository.save(existing);
                    });
                    continue;
                }

                Restaurant restaurant = buildRestaurantFromNode(info, city);
                restaurantRepository.save(restaurant);
                saved++;
            } catch (Exception e) {
                log.error("Error saving restaurant {}: {}", info.path("name").asText("?"), e.getMessage());
            }
        }
        return saved;
    }

    // -------------------------------------------------------------------------
    // Menu scraping
    // -------------------------------------------------------------------------

    /**
     * Asynchronously scrape menus for all restaurants that don't have menu data yet.
     */
    @Async
    public void scrapeAllMenusAsync() {
        if (!menuScrapingRunning.compareAndSet(false, true)) {
            log.info("Menu scraping already running, skipping.");
            return;
        }
        try {
            log.info("Starting menu scraping for all restaurants without menu data");
            int totalProcessed = 0;
            int batchOffset = 0;

            while (true) {
                List<Restaurant> batch = restaurantRepository.findUnscrapedMenuRestaurants(
                        PageRequest.of(0, MENU_BATCH_SIZE));
                if (batch.isEmpty()) break;

                for (Restaurant restaurant : batch) {
                    scrapeMenuForRestaurant(restaurant);
                    totalProcessed++;
                }
                batchOffset += batch.size();
                log.info("Scraped menus for {} restaurants so far", totalProcessed);
            }
            log.info("Completed menu scraping. Total: {}", totalProcessed);
        } finally {
            menuScrapingRunning.set(false);
        }
    }

    /**
     * Scrape the menu for a single restaurant by its Swiggy ID.
     */
    public int scrapeMenuForRestaurantBySwiggyId(String swiggyId) {
        Restaurant restaurant = restaurantRepository.findBySwiggyId(swiggyId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + swiggyId));
        return scrapeMenuForRestaurant(restaurant);
    }

    public int scrapeMenuForRestaurant(Restaurant restaurant) {
        log.debug("Scraping menu for: {} ({})", restaurant.getName(), restaurant.getSwiggyId());

        City city = restaurant.getCity();
        double lat = city != null ? city.getLatitude() : 12.9716;
        double lng = city != null ? city.getLongitude() : 77.5946;

        Optional<JsonNode> response = apiClient.fetchRestaurantMenu(restaurant.getSwiggyId(), lat, lng);
        if (response.isEmpty()) {
            log.warn("No menu response for restaurant: {}", restaurant.getName());
            return 0;
        }

        List<JsonNode> categoryNodes = apiClient.extractMenuCategoryNodes(response.get());
        if (categoryNodes.isEmpty()) {
            log.warn("No menu categories found for: {}", restaurant.getName());
            // Mark as scraped even if empty to avoid retrying
            restaurant.setMenuScraped(true);
            restaurant.setMenuScrapedAt(LocalDateTime.now());
            restaurantRepository.save(restaurant);
            return 0;
        }

        // Clear existing menu data before re-saving
        menuItemRepository.deleteByRestaurant(restaurant);
        menuCategoryRepository.deleteByRestaurant(restaurant);

        int itemCount = 0;
        int categoryOrder = 0;

        for (JsonNode catNode : categoryNodes) {
            String categoryName = catNode.path("title").asText("Uncategorized");
            MenuCategory menuCategory = MenuCategory.builder()
                    .restaurant(restaurant)
                    .name(categoryName)
                    .displayOrder(categoryOrder++)
                    .build();
            menuCategoryRepository.save(menuCategory);

            JsonNode itemCards = catNode.path("itemCards");
            if (!itemCards.isArray()) continue;

            List<MenuItem> items = new ArrayList<>();
            for (JsonNode itemCard : itemCards) {
                try {
                    JsonNode info = itemCard.path("card").path("info");
                    if (info.isMissingNode()) continue;

                    MenuItem item = buildMenuItemFromNode(info, restaurant, menuCategory);
                    items.add(item);
                } catch (Exception e) {
                    log.error("Error building menu item: {}", e.getMessage());
                }
            }
            menuItemRepository.saveAll(items);
            itemCount += items.size();
        }

        restaurant.setMenuScraped(true);
        restaurant.setMenuScrapedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);

        log.debug("Saved {} menu items for: {}", itemCount, restaurant.getName());
        return itemCount;
    }

    // -------------------------------------------------------------------------
    // Status checks
    // -------------------------------------------------------------------------

    public boolean isRestaurantScrapingRunning() {
        return restaurantScrapingRunning.get();
    }

    public boolean isMenuScrapingRunning() {
        return menuScrapingRunning.get();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Restaurant buildRestaurantFromNode(JsonNode info, City city) {
        Restaurant r = new Restaurant();
        updateRestaurantFromNode(r, info, city);
        return r;
    }

    private void updateRestaurantFromNode(Restaurant r, JsonNode info, City city) {
        r.setSwiggyId(info.path("id").asText());
        r.setName(info.path("name").asText("Unknown"));
        r.setCity(city);
        r.setLocality(info.path("locality").asText(null));
        r.setAreaName(info.path("areaName").asText(null));
        r.setCloudinaryImageId(info.path("cloudinaryImageId").asText(null));
        r.setSlug(info.path("slugs").path("restaurant").asText(null));

        if (!info.path("avgRating").isMissingNode()) {
            r.setAvgRating(info.path("avgRating").asDouble(0.0));
        }
        r.setTotalRatingsString(info.path("totalRatingsString").asText(null));
        r.setCostForTwo(info.path("costForTwo").asInt(0));
        r.setCostForTwoMessage(info.path("costForTwoMessage").asText(null));
        r.setDeliveryTime(info.path("sla").path("deliveryTime").asInt(
                info.path("deliveryTime").asInt(0)));
        r.setIsOpen(info.path("isOpen").asBoolean(true));
        r.setIsPureVeg(info.path("veg").asBoolean(false));
        r.setPromoted(info.path("promoted").asBoolean(false));
        r.setLastMileTravel(info.path("lastMileTravelString").asDouble(
                info.path("lastMileTravel").asDouble(0.0)));

        // Cuisines: array to comma-separated string
        JsonNode cuisinesNode = info.path("cuisines");
        if (cuisinesNode.isArray()) {
            List<String> cuisines = new ArrayList<>();
            cuisinesNode.forEach(c -> cuisines.add(c.asText()));
            r.setCuisines(String.join(", ", cuisines));
        }

        // Discount info
        JsonNode aggregatedDiscountInfoV3 = info.path("aggregatedDiscountInfoV3");
        if (!aggregatedDiscountInfoV3.isMissingNode()) {
            String header = aggregatedDiscountInfoV3.path("header").asText("");
            String subHeader = aggregatedDiscountInfoV3.path("subHeader").asText("");
            r.setDiscountInfo((header + " " + subHeader).trim());
        }

        // Lat/lng from location
        JsonNode location = info.path("location");
        if (!location.isMissingNode()) {
            r.setLatitude(location.path("lat").asDouble(0.0));
            r.setLongitude(location.path("lng").asDouble(0.0));
        }
    }

    private MenuItem buildMenuItemFromNode(JsonNode info, Restaurant restaurant, MenuCategory menuCategory) {
        MenuItem item = new MenuItem();
        item.setSwiggyItemId(info.path("id").asText(null));
        item.setRestaurant(restaurant);
        item.setMenuCategory(menuCategory);
        item.setName(info.path("name").asText("Unknown Item"));
        item.setDescription(info.path("description").asText(null));

        // Price in paise (divide by 100 to get rupees)
        int price = info.path("price").asInt(0);
        int defaultPrice = info.path("defaultPrice").asInt(0);
        item.setPrice(price > 0 ? price : defaultPrice);
        item.setDefaultPrice(defaultPrice > 0 ? defaultPrice : price);

        item.setIsVeg(info.path("isVeg").asInt(0) == 1);
        item.setInStock(info.path("inStock").asInt(1) == 1);
        item.setIsBestSeller(info.path("isBestseller").asBoolean(false));
        item.setCloudinaryImageId(info.path("imageId").asText(null));
        item.setRatingsCount(info.path("ratingsCount").asInt(0));
        item.setAvgRatingString(info.path("avgRatingString").asText(null));

        JsonNode itemAttribute = info.path("itemAttribute");
        if (!itemAttribute.isMissingNode()) {
            item.setItemAttribute(itemAttribute.path("vegClassifier").asText(null));
        }

        return item;
    }
}
