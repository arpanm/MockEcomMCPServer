package com.mock.ecom.mcpserver.tools;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSearchTools {

    private final ProductService productService;
    private final ToolResponseHelper helper;

    @Tool(description = "Search for products across all ecommerce categories (grocery, fashion, electronics, beauty, home) using a keyword query. Returns paginated list of matching products with title, description, price, brand, category, rating, and image URL. Use this as the primary product discovery tool.")
    public String searchProducts(String query, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] searchProducts query={} page={} size={}", query, page, pageSize);
            int p = page != null ? page : 0;
            int s = pageSize != null ? Math.min(pageSize, 50) : 10;
            Page<Product> result = productService.searchProducts(query, p, s);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("page", p);
            response.put("pageSize", s);
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("products", result.getContent().stream().map(helper::productToMap).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] searchProducts error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Search and filter products by category, subcategory, and/or brand. Provide any combination of 1, 2, or 3 filter criteria along with an optional search query. Category examples: GROCERY, ELECTRONICS, FASHION, BEAUTY, HOME. Returns paginated filtered product list.")
    public String filterProducts(String query, String category, String subCategory, String brand, Integer page, Integer pageSize) {
        try {
            log.info("[Tool] filterProducts query={} cat={} sub={} brand={}", query, category, subCategory, brand);
            int p = page != null ? page : 0;
            int s = pageSize != null ? Math.min(pageSize, 50) : 10;
            Page<Product> result = productService.filterProducts(query, category, subCategory, brand, p, s);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("filters", Map.of("category", category != null ? category : "", "subCategory", subCategory != null ? subCategory : "", "brand", brand != null ? brand : ""));
            response.put("page", p);
            response.put("totalElements", result.getTotalElements());
            response.put("totalPages", result.getTotalPages());
            response.put("products", result.getContent().stream().map(helper::productToMap).toList());
            return helper.toJson(response);
        } catch (Exception e) {
            log.error("[Tool] filterProducts error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get available filter options (categories, subcategories, brands with counts) for a search query. Call this before showing filter UI to the user so they can narrow down results. Returns structured filter options.")
    public String getFilters(String query) {
        try {
            log.info("[Tool] getFilters query={}", query);
            Map<String, Object> filters = productService.getFilters(query);
            return helper.toJson(filters);
        } catch (Exception e) {
            log.error("[Tool] getFilters error: {}", e.getMessage(), e);
            return helper.error(e.getMessage());
        }
    }

    @Tool(description = "Get available sorting options for product search results. Returns sort keys and labels. Use the sort key with searchProducts or filterProducts to order results by price, rating, or delivery time.")
    public String getSortOptions(String query) {
        log.info("[Tool] getSortOptions query={}", query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("sortOptions", List.of(
            Map.of("key", "price_asc",    "label", "Price: Low to High"),
            Map.of("key", "price_desc",   "label", "Price: High to Low"),
            Map.of("key", "rating_desc",  "label", "Rating: High to Low"),
            Map.of("key", "newest",       "label", "Newest First"),
            Map.of("key", "popularity",   "label", "Popularity"),
            Map.of("key", "delivery_asc", "label", "Fastest Delivery First")
        ));
        return helper.toJson(result);
    }
}
