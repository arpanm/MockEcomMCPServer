package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.service.RestaurantQueryService;
import com.mock.ecom.mcpserver.service.SeedDataExportService;
import com.mock.ecom.mcpserver.service.SwiggyScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tools for controlling and monitoring the Swiggy data scraper.
 * All scraping runs asynchronously to avoid blocking MCP responses.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScraperControlTools {

    private final SwiggyScraperService scraperService;
    private final RestaurantQueryService queryService;
    private final SeedDataExportService exportService;
    private final ToolResponseHelper helper;

    @Tool(description = "Get the current status of the Swiggy scraper including total cities, scraped cities, total restaurants collected, and how many restaurants have menu data. Use this to monitor scraping progress.")
    public String getScraperStatus() {
        try {
            log.info("[Tool] getScraperStatus");
            Map<String, Object> stats = queryService.getScraperStats();
            stats.put("restaurantScrapingRunning", scraperService.isRestaurantScrapingRunning());
            stats.put("menuScrapingRunning", scraperService.isMenuScrapingRunning());
            return helper.toJson(stats);
        } catch (Exception e) {
            log.error("[Tool] getScraperStatus error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Start scraping restaurants for ALL cities that haven't been scraped yet. Runs asynchronously in the background. Skips cities already scraped (incremental). Returns immediately with a status message. Check progress with getScraperStatus. Each city may take 30-120 seconds due to rate limiting.")
    public String startRestaurantScraping() {
        try {
            log.info("[Tool] startRestaurantScraping");
            if (scraperService.isRestaurantScrapingRunning()) {
                return helper.toJson(Map.of(
                        "status", "already_running",
                        "message", "Restaurant scraping is already running in the background."));
            }
            scraperService.scrapeAllCitiesAsync();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "started");
            response.put("message", "Restaurant scraping started for all pending cities. Check getScraperStatus for progress.");
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] startRestaurantScraping error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Scrape restaurants for a specific city by name (e.g. 'Mumbai', 'Bangalore'). Runs synchronously and returns the count of new restaurants saved. If already scraped, re-scrapes to get fresh data. Use listRestaurantCities to get valid city names.")
    public String scrapeRestaurantsForCity(String cityName) {
        try {
            log.info("[Tool] scrapeRestaurantsForCity city={}", cityName);
            int saved = scraperService.scrapeRestaurantsForCityByName(cityName);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("city", cityName);
            response.put("newRestaurantsSaved", saved);
            response.put("status", "completed");
            response.put("message", saved + " new restaurants scraped for " + cityName + ". Use searchRestaurants to query them.");
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] scrapeRestaurantsForCity error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Start scraping menus for ALL restaurants that don't have menu data yet. Runs asynchronously in the background. Incremental - skips restaurants already scraped. Can take several hours for thousands of restaurants due to rate limiting. Check progress with getScraperStatus.")
    public String startMenuScraping() {
        try {
            log.info("[Tool] startMenuScraping");
            if (scraperService.isMenuScrapingRunning()) {
                return helper.toJson(Map.of(
                        "status", "already_running",
                        "message", "Menu scraping is already running in the background."));
            }
            scraperService.scrapeAllMenusAsync();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "started");
            response.put("message", "Menu scraping started for all restaurants without menu data. Check getScraperStatus for progress.");
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] startMenuScraping error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Export all scraped restaurant and menu data to a portable JSON seed file at ./seed-export/seed-data.json. After running the scraper, call this tool, then copy the file to src/main/resources/db/seed/seed-data.json and commit it. New environments will automatically load this data on startup without needing to re-scrape.")
    public String exportSeedData() {
        try {
            log.info("[Tool] exportSeedData");
            Map<String, Object> stats = queryService.getScraperStats();
            String filePath = exportService.exportToFile();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("exportedFile", filePath);
            response.put("totalRestaurants", stats.get("totalRestaurants"));
            response.put("restaurantsWithMenu", stats.get("restaurantsWithMenu"));
            response.put("nextSteps", java.util.List.of(
                    "1. Copy " + filePath + " to src/main/resources/db/seed/seed-data.json",
                    "2. Rebuild the project: mvn clean package -DskipTests",
                    "3. Commit and push: git add src/main/resources/db/seed/seed-data.json && git commit -m 'Add scraped restaurant seed data'",
                    "4. New environments will auto-load this data on startup"
            ));
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] exportSeedData error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Scrape the menu for a single restaurant by its Swiggy restaurant ID. Runs synchronously and returns the count of menu items saved. Useful for fetching menu data for a specific restaurant without running the full menu scraper. Use getRestaurantBySwiggyId first to find the swiggyId.")
    public String scrapeRestaurantMenu(String swiggyId) {
        try {
            log.info("[Tool] scrapeRestaurantMenu swiggyId={}", swiggyId);
            int itemsSaved = scraperService.scrapeMenuForRestaurantBySwiggyId(swiggyId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("swiggyId", swiggyId);
            response.put("menuItemsSaved", itemsSaved);
            response.put("status", "completed");
            response.put("message", itemsSaved + " menu items scraped. Use getRestaurantMenu to view them.");
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] scrapeRestaurantMenu error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }
}
