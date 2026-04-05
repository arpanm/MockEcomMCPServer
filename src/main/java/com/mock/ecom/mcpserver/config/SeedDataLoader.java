package com.mock.ecom.mcpserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.repository.CityRepository;
import com.mock.ecom.mcpserver.repository.MenuCategoryRepository;
import com.mock.ecom.mcpserver.repository.MenuItemRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import com.mock.ecom.mcpserver.service.SeedDataExportService.MenuCategoryRecord;
import com.mock.ecom.mcpserver.service.SeedDataExportService.MenuItemRecord;
import com.mock.ecom.mcpserver.service.SeedDataExportService.RestaurantSeed;
import com.mock.ecom.mcpserver.service.SeedDataExportService.SeedExport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * Loads pre-exported seed data from {@code classpath:db/seed/seed-data.json}
 * on application startup if the restaurant table is empty.
 *
 * <p>This enables zero-scraping deployment: commit the exported seed file and
 * every new environment gets the full dataset automatically.
 *
 * <p>Ordering:
 * <ol>
 *   <li>Order 1 – {@code CityDataInitializer} (seeds 50 cities)</li>
 *   <li>Order 2 – This loader (loads scraped restaurant + menu data)</li>
 *   <li>Order 3 – {@code SampleRestaurantDataSeeder} (fallback demo data if still empty)</li>
 *   <li>Order 4 – {@code ScraperAutoStartRunner} (optionally starts live scraper)</li>
 * </ol>
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class SeedDataLoader implements ApplicationRunner {

    private static final String SEED_FILE = "db/seed/seed-data.json";

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        if (!resource.exists()) {
            log.info("No seed file found at classpath:{}. Skipping seed data load.", SEED_FILE);
            return;
        }

        if (restaurantRepository.count() > 0) {
            log.info("Restaurants already present in DB ({}). Skipping seed data load.", restaurantRepository.count());
            return;
        }

        log.info("Loading restaurant seed data from classpath:{}", SEED_FILE);
        try (InputStream is = resource.getInputStream()) {
            SeedExport export = objectMapper.readValue(is, SeedExport.class);
            int loaded = 0;
            int skipped = 0;

            for (RestaurantSeed rs : export.restaurants()) {
                if (rs.cityName() == null) {
                    log.warn("Skipping restaurant '{}' – no city name in seed data", rs.name());
                    skipped++;
                    continue;
                }
                var city = cityRepository.findByName(rs.cityName()).orElse(null);
                if (city == null) {
                    log.warn("Skipping restaurant '{}' – city '{}' not found in DB", rs.name(), rs.cityName());
                    skipped++;
                    continue;
                }
                if (restaurantRepository.existsBySwiggyId(rs.swiggyId())) {
                    skipped++;
                    continue;
                }

                Restaurant restaurant = Restaurant.builder()
                        .swiggyId(rs.swiggyId())
                        .name(rs.name())
                        .city(city)
                        .locality(rs.locality())
                        .areaName(rs.areaName())
                        .latitude(rs.latitude())
                        .longitude(rs.longitude())
                        .cloudinaryImageId(rs.cloudinaryImageId())
                        .avgRating(rs.avgRating())
                        .totalRatingsString(rs.totalRatingsString())
                        .costForTwo(rs.costForTwo())
                        .costForTwoMessage(rs.costForTwoMessage())
                        .deliveryTime(rs.deliveryTime())
                        .isOpen(rs.isOpen() != null ? rs.isOpen() : true)
                        .isPureVeg(rs.isPureVeg() != null ? rs.isPureVeg() : false)
                        .cuisines(rs.cuisines())
                        .discountInfo(rs.discountInfo())
                        .promoted(rs.promoted() != null ? rs.promoted() : false)
                        .lastMileTravel(rs.lastMileTravel())
                        .slug(rs.slug())
                        .menuScraped(rs.menuScraped() != null ? rs.menuScraped() : false)
                        .menuScrapedAt(rs.menuScraped() != null && rs.menuScraped() ? LocalDateTime.now() : null)
                        .build();
                restaurantRepository.save(restaurant);

                if (rs.menuCategories() != null) {
                    loadMenuCategories(restaurant, rs.menuCategories());
                }

                loaded++;
            }

            // Update city restaurant counts
            cityRepository.findAll().forEach(c -> {
                c.setRestaurantCount((int) restaurantRepository.countByCity(c));
                if (c.getRestaurantCount() > 0) {
                    c.setRestaurantsScraped(true);
                }
                cityRepository.save(c);
            });

            log.info("Seed data loaded: {} restaurants imported, {} skipped. Export was from: {}",
                    loaded, skipped, export.exportedAt());
        } catch (Exception e) {
            log.error("Failed to load seed data: {}", e.getMessage(), e);
        }
    }

    private void loadMenuCategories(Restaurant restaurant, java.util.List<MenuCategoryRecord> categories) {
        int order = 0;
        for (MenuCategoryRecord catRecord : categories) {
            MenuCategory category = MenuCategory.builder()
                    .restaurant(restaurant)
                    .name(catRecord.name())
                    .displayOrder(catRecord.displayOrder() != null ? catRecord.displayOrder() : order)
                    .build();
            menuCategoryRepository.save(category);

            if (catRecord.items() != null) {
                for (MenuItemRecord itemRecord : catRecord.items()) {
                    menuItemRepository.save(MenuItem.builder()
                            .swiggyItemId(itemRecord.swiggyItemId())
                            .restaurant(restaurant)
                            .menuCategory(category)
                            .name(itemRecord.name())
                            .description(itemRecord.description())
                            .price(itemRecord.price())
                            .defaultPrice(itemRecord.defaultPrice())
                            .isVeg(itemRecord.isVeg() != null ? itemRecord.isVeg() : false)
                            .inStock(itemRecord.inStock() != null ? itemRecord.inStock() : true)
                            .isBestSeller(itemRecord.isBestSeller() != null ? itemRecord.isBestSeller() : false)
                            .ratingsCount(itemRecord.ratingsCount())
                            .avgRatingString(itemRecord.avgRatingString())
                            .itemAttribute(itemRecord.itemAttribute())
                            .cloudinaryImageId(itemRecord.cloudinaryImageId())
                            .build());
                }
            }
            order++;
        }
    }
}
