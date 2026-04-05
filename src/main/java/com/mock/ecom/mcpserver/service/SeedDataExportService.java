package com.mock.ecom.mcpserver.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.repository.MenuCategoryRepository;
import com.mock.ecom.mcpserver.repository.MenuItemRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports all scraped restaurant and menu data to a portable JSON seed file.
 *
 * <p>Usage flow:
 * <ol>
 *   <li>Run the Swiggy scraper (startRestaurantScraping + startMenuScraping MCP tools)</li>
 *   <li>Call exportSeedData MCP tool → writes {@code ./seed-export/seed-data.json}</li>
 *   <li>Copy the file to {@code src/main/resources/db/seed/seed-data.json}</li>
 *   <li>Commit and push – new environments auto-load the data on startup</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeedDataExportService {

    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    private static final String EXPORT_DIR = "./seed-export";
    private static final String EXPORT_FILE = "seed-data.json";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Exports all restaurants with full menu data to {@code ./seed-export/seed-data.json}.
     *
     * @return absolute path of the written file
     */
    @Transactional(readOnly = true)
    public String exportToFile() throws IOException {
        List<Restaurant> all = restaurantRepository.findAll();
        log.info("Exporting {} restaurants to seed file", all.size());

        List<RestaurantSeed> seeds = new ArrayList<>();
        int totalItems = 0;

        for (Restaurant r : all) {
            List<MenuCategoryRecord> cats = buildMenuCategories(r);
            int items = cats.stream().mapToInt(c -> c.items().size()).sum();
            totalItems += items;

            seeds.add(new RestaurantSeed(
                    r.getSwiggyId(),
                    r.getName(),
                    r.getCity() != null ? r.getCity().getName() : null,
                    r.getLocality(),
                    r.getAreaName(),
                    r.getLatitude(),
                    r.getLongitude(),
                    r.getCloudinaryImageId(),
                    r.getAvgRating(),
                    r.getTotalRatingsString(),
                    r.getCostForTwo(),
                    r.getCostForTwoMessage(),
                    r.getDeliveryTime(),
                    r.getIsOpen(),
                    r.getIsPureVeg(),
                    r.getCuisines(),
                    r.getDiscountInfo(),
                    r.getPromoted(),
                    r.getLastMileTravel(),
                    r.getSlug(),
                    r.getMenuScraped(),
                    cats
            ));
        }

        SeedExport export = new SeedExport(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "1.0",
                seeds.size(),
                totalItems,
                seeds
        );

        Path exportDir = Path.of(EXPORT_DIR);
        Files.createDirectories(exportDir);
        Path exportFile = exportDir.resolve(EXPORT_FILE);

        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(exportFile.toFile(), export);

        log.info("Seed data exported: {} restaurants, {} menu items → {}", seeds.size(), totalItems, exportFile.toAbsolutePath());
        return exportFile.toAbsolutePath().toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<MenuCategoryRecord> buildMenuCategories(Restaurant restaurant) {
        List<MenuCategory> categories = menuCategoryRepository.findByRestaurantOrderByDisplayOrderAsc(restaurant);
        List<MenuCategoryRecord> result = new ArrayList<>();
        for (MenuCategory cat : categories) {
            List<MenuItem> items = menuItemRepository.findByMenuCategoryOrderByIdAsc(cat);
            List<MenuItemRecord> itemRecords = items.stream().map(i -> new MenuItemRecord(
                    i.getSwiggyItemId(),
                    i.getName(),
                    i.getDescription(),
                    i.getPrice(),
                    i.getDefaultPrice(),
                    i.getIsVeg(),
                    i.getInStock(),
                    i.getIsBestSeller(),
                    i.getRatingsCount(),
                    i.getAvgRatingString(),
                    i.getItemAttribute(),
                    i.getCloudinaryImageId()
            )).toList();
            result.add(new MenuCategoryRecord(cat.getName(), cat.getDisplayOrder(), itemRecords));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Seed data record types (portable JSON format)
    // -------------------------------------------------------------------------

    public record SeedExport(
            String exportedAt,
            String version,
            int totalRestaurants,
            int totalMenuItems,
            List<RestaurantSeed> restaurants
    ) {}

    public record RestaurantSeed(
            String swiggyId,
            String name,
            String cityName,
            String locality,
            String areaName,
            Double latitude,
            Double longitude,
            String cloudinaryImageId,
            Double avgRating,
            String totalRatingsString,
            Integer costForTwo,
            String costForTwoMessage,
            Integer deliveryTime,
            Boolean isOpen,
            Boolean isPureVeg,
            String cuisines,
            String discountInfo,
            Boolean promoted,
            Double lastMileTravel,
            String slug,
            Boolean menuScraped,
            List<MenuCategoryRecord> menuCategories
    ) {}

    public record MenuCategoryRecord(
            String name,
            Integer displayOrder,
            List<MenuItemRecord> items
    ) {}

    public record MenuItemRecord(
            String swiggyItemId,
            String name,
            String description,
            Integer price,
            Integer defaultPrice,
            Boolean isVeg,
            Boolean inStock,
            Boolean isBestSeller,
            Integer ratingsCount,
            String avgRatingString,
            String itemAttribute,
            String cloudinaryImageId
    ) {}
}
