package com.mock.ecom.mcpserver.integration;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.service.MockDataGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MockDataGeneratorService category detection and product generation.
 */
@SpringBootTest
@ActiveProfiles("test")
class MockDataGeneratorTest {

    @Autowired
    private MockDataGeneratorService mockDataGeneratorService;

    @Test
    void generateProduct_groceryKeyword_createsGroceryProduct() {
        Product product = mockDataGeneratorService.generateProduct("rice_premium", "rice");
        assertNotNull(product);
        assertNotNull(product.getTitle());
        assertNotNull(product.getCategory());
        assertEquals("GROCERY", product.getCategory());
        assertNotNull(product.getPrice());
        assertTrue(product.getPrice().doubleValue() > 0, "Price should be positive");
    }

    @Test
    void generateProduct_electronicsKeyword_createsElectronicsProduct() {
        Product product = mockDataGeneratorService.generateProduct("laptop_dell", "laptop");
        assertNotNull(product);
        assertEquals("ELECTRONICS", product.getCategory());
        assertNotNull(product.getBrand());
    }

    @Test
    void generateProduct_fashionKeyword_createsFashionProduct() {
        Product product = mockDataGeneratorService.generateProduct("shirt_men", "shirt");
        assertNotNull(product);
        assertEquals("FASHION", product.getCategory());
    }

    @Test
    void generateProduct_beautyKeyword_createsBeautyProduct() {
        Product product = mockDataGeneratorService.generateProduct("lipstick_red", "lipstick");
        assertNotNull(product);
        assertEquals("BEAUTY", product.getCategory());
    }

    @Test
    void generateProduct_homeKeyword_createsHomeProduct() {
        Product product = mockDataGeneratorService.generateProduct("sofa_wooden", "sofa");
        assertNotNull(product);
        assertEquals("HOME", product.getCategory());
    }

    @Test
    void generateProduct_isDeterministic() {
        Product p1 = mockDataGeneratorService.generateProduct("test_product", "test product");
        Product p2 = mockDataGeneratorService.generateProduct("test_product", "test product");
        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals(p1.getTitle(), p2.getTitle(), "Same searchKey should produce same title");
        assertEquals(p1.getPrice(), p2.getPrice(), "Same searchKey should produce same price");
    }

    @Test
    void generateProduct_hasAllRequiredFields() {
        Product product = mockDataGeneratorService.generateProduct("phone_samsung", "samsung phone");
        assertNotNull(product.getTitle());
        assertNotNull(product.getDescription());
        assertNotNull(product.getCategory());
        assertNotNull(product.getBrand());
        assertNotNull(product.getPrice());
        assertNotNull(product.getMrp());
        assertNotNull(product.getImageUrl());
        assertTrue(product.getMrp().compareTo(product.getPrice()) >= 0, "MRP should be >= price");
    }

    @Test
    void generateProductList_returnsRequestedCount() {
        List<Product> products = mockDataGeneratorService.generateProductList("laptop ultrabook", 5);
        assertNotNull(products);
        assertEquals(5, products.size(), "Should return exactly 5 products");
    }

    @Test
    void generateProductList_productsHaveUniqueSearchKeys() {
        List<Product> products = mockDataGeneratorService.generateProductList("mobile device", 3);
        long uniqueKeys = products.stream()
            .map(Product::getSearchKey)
            .distinct()
            .count();
        assertEquals(products.size(), uniqueKeys, "Each product should have a unique search key");
    }

    @Test
    void getOrCreateProduct_cachesBehavior() {
        String searchKey = "unique_product_for_cache_test";
        Product p1 = mockDataGeneratorService.getOrCreateProduct(searchKey, "cache test product");
        Product p2 = mockDataGeneratorService.getOrCreateProduct(searchKey, "cache test product");

        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals(p1.getId(), p2.getId(), "Same searchKey should return same product from DB");
    }

    @Test
    void detectCategory_groceryTerms_returnsGrocery() {
        String[] groceryQueries = {"wheat flour", "dal lentils", "cooking oil", "basmati rice", "coconut oil"};
        for (String query : groceryQueries) {
            Product product = mockDataGeneratorService.generateProduct(
                mockDataGeneratorService.normalizeSearchKey(query), query
            );
            assertEquals("GROCERY", product.getCategory(), "Should detect GROCERY for: " + query);
        }
    }

    @Test
    void detectCategory_electronicTerms_returnsElectronics() {
        String[] queries = {"laptop computer", "smartphone android", "television led"};
        for (String query : queries) {
            Product product = mockDataGeneratorService.generateProduct(
                mockDataGeneratorService.normalizeSearchKey(query), query
            );
            assertEquals("ELECTRONICS", product.getCategory(), "Should detect ELECTRONICS for: " + query);
        }
    }

    @Test
    void normalizeSearchKey_removesSpecialChars() {
        String normalized = mockDataGeneratorService.normalizeSearchKey("Hello World! 123");
        assertNotNull(normalized);
        assertFalse(normalized.contains(" "), "Should not contain spaces");
        assertFalse(normalized.contains("!"), "Should not contain special chars");
        assertEquals(normalized.toLowerCase(), normalized, "Should be lowercase");
    }

    @Test
    void generateProduct_priceInCorrectRange() {
        // Grocery prices should be lower than electronics
        Product grocery = mockDataGeneratorService.generateProduct("salt_iodized", "salt iodized");
        Product electronics = mockDataGeneratorService.generateProduct("television_4k", "television 4k");

        assertEquals("GROCERY", grocery.getCategory());
        assertEquals("ELECTRONICS", electronics.getCategory());

        // Electronics prices should generally be higher than grocery
        assertTrue(electronics.getPrice().compareTo(grocery.getPrice()) > 0 ||
                   electronics.getPrice().doubleValue() >= 499,
            "Electronics should have prices in expected range");
    }
}
