package com.mock.ecom.mcpserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HTTP client for Swiggy's public restaurant listing and menu APIs.
 * Uses Java's built-in HttpClient with browser-like headers to fetch restaurant
 * and menu data for incremental storage.
 */
@Service
@Slf4j
public class SwiggyApiClient {

    private static final String RESTAURANT_LIST_URL =
            "https://www.swiggy.com/dapi/restaurants/list/v5?lat=%s&lng=%s&is-seo-homepage-enabled=true&page_type=DESKTOP_WEB_LISTING";

    private static final String RESTAURANT_LIST_OFFSET_URL =
            "https://www.swiggy.com/dapi/restaurants/list/update";

    private static final String MENU_URL =
            "https://www.swiggy.com/dapi/menu/pl?page-type=REGULAR_MENU&complete-menu=true&lat=%s&lng=%s&restaurantId=%s";

    private static final String RESTAURANT_TYPE =
            "type.googleapis.com/swiggy.presentation.food.v2.Restaurant";

    private static final String ITEM_CATEGORY_TYPE =
            "type.googleapis.com/swiggy.presentation.food.v2.ItemCategory";

    @Value("${app.scraper.request-delay-ms:1500}")
    private long requestDelayMs;

    @Value("${app.scraper.user-agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36}")
    private String userAgent;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SwiggyApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch the first page of restaurant listings for a given lat/lng.
     * Returns the raw JsonNode response, or empty if the request fails.
     */
    public Optional<JsonNode> fetchRestaurantListPage(double lat, double lng) {
        String url = String.format(RESTAURANT_LIST_URL, lat, lng);
        return fetchJson(url);
    }

    /**
     * Fetch a subsequent page of restaurants using Swiggy's update endpoint.
     * widgetOffset tracks pagination state returned in the initial response.
     */
    public Optional<JsonNode> fetchRestaurantListNextPage(double lat, double lng, int offset, String pageContext) {
        try {
            sleepBeforeRequest();
            String body = buildNextPageBody(lat, lng, offset, pageContext);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESTAURANT_LIST_OFFSET_URL))
                    .header("User-Agent", userAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://www.swiggy.com/")
                    .header("Origin", "https://www.swiggy.com")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Optional.of(objectMapper.readTree(response.body()));
            }
            log.warn("Swiggy next-page returned HTTP {}", response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while fetching next page");
        } catch (Exception e) {
            log.error("Error fetching next page lat={} lng={} offset={}: {}", lat, lng, offset, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Fetch the full menu for a restaurant by its Swiggy restaurant ID.
     */
    public Optional<JsonNode> fetchRestaurantMenu(String restaurantId, double lat, double lng) {
        String url = String.format(MENU_URL, lat, lng, restaurantId);
        return fetchJson(url);
    }

    /**
     * Extract the list of restaurant info nodes from a restaurant list API response.
     */
    public List<JsonNode> extractRestaurantNodes(JsonNode response) {
        List<JsonNode> restaurants = new ArrayList<>();
        try {
            JsonNode cards = response.path("data").path("cards");
            if (!cards.isArray()) return restaurants;
            for (JsonNode card : cards) {
                // Direct restaurant card
                JsonNode innerCard = card.path("card").path("card");
                String type = innerCard.path("@type").asText("");
                if (RESTAURANT_TYPE.equals(type)) {
                    JsonNode info = innerCard.path("info");
                    if (!info.isMissingNode()) {
                        restaurants.add(info);
                    }
                }
                // Restaurant inside a group/collection card
                JsonNode groupedRestaurants = card.path("card").path("card").path("restaurants");
                if (groupedRestaurants.isArray()) {
                    for (JsonNode r : groupedRestaurants) {
                        JsonNode info = r.path("info");
                        if (!info.isMissingNode()) {
                            restaurants.add(info);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting restaurant nodes: {}", e.getMessage());
        }
        return restaurants;
    }

    /**
     * Extract menu categories and items from a menu API response.
     * Returns a list of category nodes, each with "title" and "itemCards" fields.
     */
    public List<JsonNode> extractMenuCategoryNodes(JsonNode response) {
        List<JsonNode> categories = new ArrayList<>();
        try {
            JsonNode cards = response.path("data").path("cards");
            if (!cards.isArray()) return categories;

            for (JsonNode card : cards) {
                // Try groupedCard path first (standard Swiggy menu format)
                JsonNode regularCards = card.path("groupedCard")
                        .path("cardGroupMap").path("REGULAR").path("cards");
                if (regularCards.isArray()) {
                    for (JsonNode rc : regularCards) {
                        JsonNode innerCard = rc.path("card").path("card");
                        String type = innerCard.path("@type").asText("");
                        if (ITEM_CATEGORY_TYPE.equals(type)) {
                            categories.add(innerCard);
                        }
                        // Sub-categories nested inside a category
                        JsonNode subCategories = innerCard.path("categories");
                        if (subCategories.isArray()) {
                            for (JsonNode sub : subCategories) {
                                categories.add(sub);
                            }
                        }
                    }
                }
                // Also check direct card path
                JsonNode directCard = card.path("card").path("card");
                String type = directCard.path("@type").asText("");
                if (ITEM_CATEGORY_TYPE.equals(type)) {
                    categories.add(directCard);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting menu categories: {}", e.getMessage());
        }
        return categories;
    }

    /**
     * Extract the pagination context string from a restaurant list response
     * to use for fetching subsequent pages.
     */
    public Optional<String> extractPageContextData(JsonNode response) {
        try {
            JsonNode cards = response.path("data").path("cards");
            if (!cards.isArray()) return Optional.empty();
            for (JsonNode card : cards) {
                JsonNode pageOffset = card.path("card").path("card").path("pageOffset");
                if (!pageOffset.isMissingNode()) {
                    return Optional.of(pageOffset.toString());
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract pageContextData: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Check if the API response indicates no more pages are available.
     */
    public boolean hasMorePages(JsonNode response) {
        try {
            JsonNode pagination = response.path("data").path("pageOffset");
            if (!pagination.isMissingNode()) {
                return !pagination.isEmpty();
            }
            // Some responses wrap it differently
            JsonNode cards = response.path("data").path("cards");
            if (cards.isArray()) {
                for (JsonNode card : cards) {
                    JsonNode pageOffset = card.path("card").path("card").path("pageOffset");
                    if (!pageOffset.isMissingNode() && !pageOffset.isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error checking for more pages: {}", e.getMessage());
        }
        return false;
    }

    private Optional<JsonNode> fetchJson(String url) {
        try {
            sleepBeforeRequest();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://www.swiggy.com/")
                    .header("Origin", "https://www.swiggy.com")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                int statusCode = json.path("statusCode").asInt(-1);
                if (statusCode == 0) {
                    return Optional.of(json);
                }
                log.warn("Swiggy API statusCode={} for URL: {}", statusCode, url);
            } else if (response.statusCode() == 429) {
                log.warn("Rate limited by Swiggy (429). Backing off 60s.");
                Thread.sleep(60_000);
            } else {
                log.warn("Swiggy API HTTP {} for URL: {}", response.statusCode(), url);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted during fetchJson");
        } catch (Exception e) {
            log.error("Error fetching URL {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }

    private String buildNextPageBody(double lat, double lng, int offset, String pageContext) {
        return String.format(
                "{\"lat\":\"%s\",\"lng\":\"%s\",\"nextOffset\":\"%s\"," +
                "\"widgetOffset\":{\"Restaurant_Group_WebView_PB_Theme\":\"\",\"Restaurant_Group_WebView_SEO_PB_Theme\":\"\"," +
                "\"collectionV5RestaurantListWidget_SimRestoRelevance_food_seo\":\"%d\"," +
                "\"inlineFacetSection\":\"%d\"},\"filters\":{},\"sortBy\":\"\",\"pageType\":\"SEO\",\"offset\":%d}",
                lat, lng, pageContext != null ? pageContext : "", offset, offset, offset
        );
    }

    private void sleepBeforeRequest() throws InterruptedException {
        if (requestDelayMs > 0) {
            Thread.sleep(requestDelayMs);
        }
    }
}
