package com.mock.ecom.mcpserver.api;

import com.mock.ecom.mcpserver.repository.MenuItemRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import com.mock.ecom.mcpserver.service.RestaurantQueryService;
import com.mock.ecom.mcpserver.service.SwiggyScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ScraperApiController {

    private final RestaurantQueryService queryService;
    private final SwiggyScraperService scraperService;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    @GetMapping("/scraper/status")
    public ResponseEntity<Map<String, Object>> getScraperStatus() {
        Map<String, Object> stats = new LinkedHashMap<>(queryService.getScraperStats());
        stats.put("restaurantScrapingRunning", scraperService.isRestaurantScrapingRunning());
        stats.put("menuScrapingRunning", scraperService.isMenuScrapingRunning());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> scraperStats = queryService.getScraperStats();

        long totalCities = (long) scraperStats.get("totalCities");
        long scrapedCities = (long) scraperStats.get("scrapedCities");
        long totalRestaurants = (long) scraperStats.get("totalRestaurants");
        long restaurantsWithMenu = (long) scraperStats.get("restaurantsWithMenu");
        long totalMenuItems = menuItemRepository.count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCities", totalCities);
        result.put("totalRestaurants", totalRestaurants);
        result.put("totalMenuItems", totalMenuItems);
        result.put("restaurantsWithMenu", restaurantsWithMenu);
        result.put("scrapedCities", scrapedCities);
        return ResponseEntity.ok(result);
    }
}
