package com.mock.ecom.mcpserver.config;

import com.mock.ecom.mcpserver.service.SwiggyScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Optionally auto-starts restaurant scraping on application startup.
 * Enable via app.scraper.auto-start=true in application.yml or environment variable.
 * Runs after CityDataInitializer (order 2 vs default order).
 */
@Component
@Order(5)
@Slf4j
@RequiredArgsConstructor
public class ScraperAutoStartRunner implements ApplicationRunner {

    private final SwiggyScraperService scraperService;

    @Value("${app.scraper.auto-start:false}")
    private boolean autoStart;

    @Override
    public void run(ApplicationArguments args) {
        if (autoStart) {
            log.info("Auto-start enabled: launching restaurant scraping in background...");
            scraperService.scrapeAllCitiesAsync();
        } else {
            log.info("Scraper auto-start disabled. Use MCP tool 'startRestaurantScraping' to begin.");
        }
    }
}
