package com.mock.ecom.mcpserver.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.repository.ProductAttributeRepository;
import com.mock.ecom.mcpserver.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports all products and their attributes to a portable JSON seed file.
 *
 * <p>Usage flow:
 * <ol>
 *   <li>Generate products via LLM or mock generator</li>
 *   <li>Call exportProductSeedData MCP tool → writes {@code ./seed-export/products-seed.json}</li>
 *   <li>Copy the file to {@code src/main/resources/db/seed/products-seed.json}</li>
 *   <li>Commit and push – new environments auto-load the data on startup</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSeedExportService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;

    private static final String EXPORT_DIR = "./seed-export";
    private static final String EXPORT_FILE = "products-seed.json";

    /**
     * Exports all products with their attributes to {@code ./seed-export/products-seed.json}.
     *
     * @return absolute path of the written file
     */
    @Transactional(readOnly = true)
    public String exportToFile() throws IOException {
        List<Product> all = productRepository.findAll();
        log.info("Exporting {} products to seed file", all.size());

        List<Map<String, Object>> productSeeds = new ArrayList<>();

        for (Product product : all) {
            List<ProductAttribute> attrs = productAttributeRepository.findByProduct(product);
            List<Map<String, String>> attrList = attrs.stream().map(a -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("attributeKey", a.getName());
                m.put("attributeValue", a.getValue());
                return m;
            }).toList();

            Map<String, Object> seed = new LinkedHashMap<>();
            seed.put("title", product.getTitle());
            seed.put("description", product.getDescription());
            seed.put("category", product.getCategory());
            seed.put("subCategory", product.getSubCategory());
            seed.put("brand", product.getBrand());
            seed.put("model", product.getModel());
            seed.put("imageUrl", product.getImageUrl());
            seed.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : null);
            seed.put("mrp", product.getMrp() != null ? product.getMrp().doubleValue() : null);
            seed.put("currency", product.getCurrency() != null ? product.getCurrency() : "INR");
            seed.put("size", product.getSize());
            seed.put("color", product.getColor());
            seed.put("material", product.getMaterial());
            seed.put("weight", product.getWeight());
            seed.put("stockQuantity", product.getStockQuantity());
            seed.put("averageRating", product.getAverageRating());
            seed.put("reviewCount", product.getReviewCount());
            seed.put("attributes", attrList);
            productSeeds.add(seed);
        }

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("products", productSeeds);

        Path exportDir = Path.of(EXPORT_DIR);
        Files.createDirectories(exportDir);
        Path exportFile = exportDir.resolve(EXPORT_FILE);

        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(exportFile.toFile(), export);

        log.info("Product seed data exported: {} products → {}", productSeeds.size(), exportFile.toAbsolutePath());
        return exportFile.toAbsolutePath().toString();
    }
}
