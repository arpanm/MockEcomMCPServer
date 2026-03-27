package com.mock.ecom.mcpserver.service;

import com.mock.ecom.mcpserver.entity.Product;
import com.mock.ecom.mcpserver.entity.ProductAttribute;
import com.mock.ecom.mcpserver.repository.ProductAttributeRepository;
import com.mock.ecom.mcpserver.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MockDataGeneratorService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository productAttributeRepository;

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String[]> CATEGORY_BRANDS = new HashMap<>();
    private static final Map<String, long[]> CATEGORY_PRICE_RANGES = new HashMap<>();
    private static final Map<String, String[]> CATEGORY_SUBCATEGORIES = new HashMap<>();
    private static final Map<String, String[]> CATEGORY_DESCRIPTORS = new HashMap<>();
    private static final Map<String, List<String[]>> CATEGORY_ATTRIBUTES = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put("GROCERY", new String[]{"rice","dal","flour","atta","oil","ghee","sugar","salt","tea","coffee","biscuit","bread","milk","paneer","butter","spice","masala","pickle","sauce","noodle","pasta","cereal","oats","honey","jam","pulses","lentil","wheat","basmati","mustard","coconut"});
        CATEGORY_KEYWORDS.put("ELECTRONICS", new String[]{"phone","mobile","laptop","computer","tablet","tv","television","camera","headphone","earphone","speaker","bluetooth","charger","router","keyboard","mouse","monitor","printer","watch","smartwatch","refrigerator","fridge","washing","microwave","ac","fan","iron","gaming","ssd","pendrive"});
        CATEGORY_KEYWORDS.put("FASHION", new String[]{"shirt","tshirt","trouser","pant","jeans","dress","saree","kurta","lehenga","suit","blazer","jacket","coat","shoes","sandal","slipper","boot","socks","underwear","bra","belt","wallet","bag","purse","handbag","backpack","cap","hat","scarf","dupatta","legging","shorts","skirt","kurti","ethnic","sneaker"});
        CATEGORY_KEYWORDS.put("BEAUTY", new String[]{"cream","lotion","moisturizer","sunscreen","serum","toner","facewash","shampoo","conditioner","hairoil","lipstick","foundation","concealer","mascara","eyeliner","kajal","blush","perfume","deodorant","bodywash","soap","facemask","scrub","nailpolish","makeup","skincare","haircare"});
        CATEGORY_KEYWORDS.put("HOME", new String[]{"pillow","bedsheet","blanket","curtain","carpet","mat","towel","container","bottle","plate","bowl","pan","pot","cooker","mixer","grinder","lamp","bulb","sofa","chair","table","shelf","rack","mirror","clock","vase","storage","organizer","cleaning","mop"});

        CATEGORY_BRANDS.put("GROCERY", new String[]{"Tata","Aashirvaad","Fortune","Saffola","Amul","Mother Dairy","Nestle","Britannia","Parle","MDH","Everest","Patanjali","Dabur","ITC","HUL"});
        CATEGORY_BRANDS.put("ELECTRONICS", new String[]{"Samsung","Apple","OnePlus","Xiaomi","Realme","Oppo","Vivo","Sony","LG","Panasonic","Philips","Lenovo","HP","Dell","Asus","JBL","boAt","Noise","Fire-Boltt","Canon"});
        CATEGORY_BRANDS.put("FASHION", new String[]{"Nike","Adidas","Puma","Reebok","Levis","Zara","H&M","FabIndia","Biba","Allen Solly","Van Heusen","Peter England","Raymond","Bata","Woodland","Roadster","HRX","US Polo","Tommy Hilfiger","Arrow"});
        CATEGORY_BRANDS.put("BEAUTY", new String[]{"Lakme","Maybelline","LOreal","Nivea","Dove","Himalaya","Biotique","Mamaearth","WOW","Plum","The Ordinary","Cetaphil","Neutrogena","Ponds","Garnier","Olay","Kama Ayurveda","Sugar","Nykaa","Innisfree"});
        CATEGORY_BRANDS.put("HOME", new String[]{"IKEA","Pepperfry","Urban Ladder","Nilkamal","Godrej","Cello","Milton","Prestige","Hawkins","Bajaj","Havells","Philips","Pigeon","Amazon Basics","Solimo"});

        CATEGORY_PRICE_RANGES.put("GROCERY", new long[]{25, 800});
        CATEGORY_PRICE_RANGES.put("ELECTRONICS", new long[]{499, 200000});
        CATEGORY_PRICE_RANGES.put("FASHION", new long[]{199, 20000});
        CATEGORY_PRICE_RANGES.put("BEAUTY", new long[]{99, 5000});
        CATEGORY_PRICE_RANGES.put("HOME", new long[]{99, 30000});

        CATEGORY_SUBCATEGORIES.put("GROCERY", new String[]{"Staples","Dairy & Eggs","Bakery","Beverages","Snacks","Spices & Condiments","Frozen Foods","Organic","Ready to Cook","Dry Fruits"});
        CATEGORY_SUBCATEGORIES.put("ELECTRONICS", new String[]{"Smartphones","Laptops","Televisions","Audio & Headphones","Cameras","Wearables","Accessories","Home Appliances","Kitchen Appliances","Tablets","Gaming"});
        CATEGORY_SUBCATEGORIES.put("FASHION", new String[]{"Men's Clothing","Women's Clothing","Kids' Clothing","Footwear","Accessories","Ethnic Wear","Sportswear","Innerwear","Bags & Wallets","Watches"});
        CATEGORY_SUBCATEGORIES.put("BEAUTY", new String[]{"Skincare","Haircare","Makeup","Fragrances","Body Care","Men's Grooming","Nail Care","Beauty Tools","Organic & Natural","Suncare"});
        CATEGORY_SUBCATEGORIES.put("HOME", new String[]{"Bedding","Bath","Kitchen & Dining","Furniture","Lighting","Home Decor","Storage","Cleaning","Garden & Outdoor"});

        CATEGORY_DESCRIPTORS.put("GROCERY", new String[]{"Premium Quality","Pure & Natural","100% Organic","Farm Fresh","Traditional Recipe","Authentic Taste","Rich & Nutritious"});
        CATEGORY_DESCRIPTORS.put("ELECTRONICS", new String[]{"Pro Series","Ultra HD","5G Ready","AI Powered","Smart Series","Premium Edition","Gaming Edition","Lite Edition","Plus Edition"});
        CATEGORY_DESCRIPTORS.put("FASHION", new String[]{"Classic Fit","Slim Fit","Regular Fit","Premium Collection","Casual Wear","Formal Wear","Festive Collection","Everyday Wear"});
        CATEGORY_DESCRIPTORS.put("BEAUTY", new String[]{"Hydrating Formula","Anti-Aging","SPF 50+","Vitamin C Enriched","Paraben Free","Dermatologist Tested","Natural Ingredients","Advanced Formula"});
        CATEGORY_DESCRIPTORS.put("HOME", new String[]{"Premium Quality","Durable Design","Space Saving","Modern Style","Eco-Friendly","Multi-Purpose","Heavy Duty"});

        CATEGORY_ATTRIBUTES.put("GROCERY", List.of(
            new String[]{"Weight","250g","500g","1kg","2kg","5kg"},
            new String[]{"Pack Size","1 Pack","2 Pack","5 Pack"},
            new String[]{"Type","Premium","Standard","Organic","Classic"},
            new String[]{"Country of Origin","India"}));
        CATEGORY_ATTRIBUTES.put("ELECTRONICS", List.of(
            new String[]{"Color","Black","White","Silver","Gold","Blue"},
            new String[]{"Warranty","1 Year","2 Years","6 Months"},
            new String[]{"In the Box","1 Unit, User Manual, Warranty Card"},
            new String[]{"Country of Origin","India","China","South Korea"}));
        CATEGORY_ATTRIBUTES.put("FASHION", List.of(
            new String[]{"Size","XS","S","M","L","XL","XXL"},
            new String[]{"Color","Black","White","Blue","Red","Green","Grey","Brown"},
            new String[]{"Material","Cotton","Polyester","Cotton Blend","Linen","Rayon"},
            new String[]{"Fit","Regular Fit","Slim Fit","Loose Fit","Comfort Fit"},
            new String[]{"Care","Machine Wash","Hand Wash Only","Dry Clean Only"}));
        CATEGORY_ATTRIBUTES.put("BEAUTY", List.of(
            new String[]{"Skin Type","All Skin Types","Oily Skin","Dry Skin","Sensitive Skin"},
            new String[]{"Volume","50ml","100ml","150ml","200ml","50g","100g"},
            new String[]{"Key Ingredient","Hyaluronic Acid","Vitamin C","Niacinamide","Retinol","SPF 50"},
            new String[]{"Form","Gel","Cream","Serum","Lotion","Oil"},
            new String[]{"Suitable For","Men & Women","Women","Men"}));
        CATEGORY_ATTRIBUTES.put("HOME", List.of(
            new String[]{"Material","Stainless Steel","Plastic","Wood","Aluminium","Cotton"},
            new String[]{"Color","White","Black","Grey","Brown","Beige","Blue"},
            new String[]{"Dimensions","30x20x10 cm","45x30x15 cm","60x40x20 cm"},
            new String[]{"Warranty","6 Months","1 Year","2 Years"}));
    }

    public String detectCategory(String query) {
        if (query == null || query.isBlank()) return "ELECTRONICS";
        String lower = query.toLowerCase().replaceAll("\\s+", "");
        int maxScore = 0;
        String detected = "ELECTRONICS";
        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) score += kw.length();
            }
            if (score > maxScore) { maxScore = score; detected = entry.getKey(); }
        }
        return detected;
    }

    public String normalizeSearchKey(String query) {
        return query.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "_");
    }

    private <T> T pick(T[] arr, long seed) { return arr[(int)(Math.abs(seed) % arr.length)]; }

    public Product generateProduct(String searchKey, String query) {
        long seed = Math.abs(searchKey.hashCode());
        String category = detectCategory(query);
        String brand    = pick(CATEGORY_BRANDS.get(category), seed);
        String subCat   = pick(CATEGORY_SUBCATEGORIES.get(category), seed + 1);
        String desc_    = pick(CATEGORY_DESCRIPTORS.get(category), seed + 2);
        String model    = generateModel(seed);
        long[] pr = CATEGORY_PRICE_RANGES.get(category);
        long rawPrice = pr[0] + Math.abs(seed * 7919L) % (pr[1] - pr[0]);
        rawPrice = rawPrice > 1000 ? (rawPrice / 100) * 100 + 99 : rawPrice > 100 ? (rawPrice / 10) * 10 + 9 : rawPrice;
        BigDecimal price = BigDecimal.valueOf(rawPrice);
        BigDecimal mrp   = price.multiply(BigDecimal.valueOf(1.1 + (seed % 30) / 100.0)).setScale(0, RoundingMode.CEILING);
        String title = brand + " " + capitalize(query) + " " + desc_ + " " + model;
        String description = generateDescription(query, category, brand, desc_);
        double rating = Math.round((3.5 + (seed % 15) / 10.0) * 10.0) / 10.0;
        int reviewCount = (int)(seed % 5000 + 10);
        int stock = (int)(seed % 990 + 10);
        return Product.builder()
            .title(title).description(description).category(category).subCategory(subCat)
            .brand(brand).model(model)
            .imageUrl("https://picsum.photos/seed/" + searchKey + "/400/400")
            .additionalImages("[\"https://picsum.photos/seed/" + searchKey + "a/400/400\",\"https://picsum.photos/seed/" + searchKey + "b/400/400\"]")
            .price(price).mrp(mrp).currency("INR").searchKey(searchKey)
            .averageRating(rating).reviewCount(reviewCount).stockQuantity(stock)
            .build();
    }

    private String generateModel(long seed) {
        String[] formats = {"PRO-" + (2022 + seed % 3) + "-X","ULTRA-" + (char)('A' + seed % 26) + (seed % 9 + 1),"PLUS-" + (100 + seed % 900),"ELITE-" + (char)('A' + seed % 10) + (seed % 99)};
        return formats[(int)(Math.abs(seed) % formats.length)];
    }

    private String generateDescription(String query, String category, String brand, String descriptor) {
        Map<String, String> templates = new HashMap<>();
        templates.put("GROCERY",  brand + " " + capitalize(query) + " (" + descriptor + ") is crafted with the finest ingredients for authentic taste and superior quality. Carefully sourced and processed to retain maximum nutrition. Free from artificial additives. Perfect for everyday healthy cooking.");
        templates.put("ELECTRONICS", brand + " " + capitalize(query) + " " + descriptor + " is engineered for peak performance with cutting-edge technology. Features advanced processing, premium build quality, and smart connectivity. Backed by " + brand + "'s industry-leading after-sales service and " + (new long[]{1, 2}[(int)(Math.abs(query.hashCode()) % 2)]) + " year warranty.");
        templates.put("FASHION",   "Elevate your wardrobe with " + brand + " " + capitalize(query) + " (" + descriptor + "). Crafted from premium materials for superior comfort and style, perfect for casual and semi-formal occasions. Versatile design ensures you look your best wherever you go. Machine washable and easy to maintain.");
        templates.put("BEAUTY",    "Transform your routine with " + brand + " " + capitalize(query) + " - " + descriptor + ". Formulated with scientifically proven ingredients that nourish and protect. Dermatologist-tested and suitable for all skin types. Paraben-free, cruelty-free. See visible results in 4 weeks.");
        templates.put("HOME",      "Transform your living space with " + brand + " " + capitalize(query) + " (" + descriptor + "). Designed with a blend of functionality and aesthetics. Made from durable, high-quality materials. Smart investment that enhances both the beauty and utility of your home.");
        return templates.getOrDefault(category, templates.get("ELECTRONICS"));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public List<ProductAttribute> generateAttributes(String category, Product product) {
        List<ProductAttribute> attrs = new ArrayList<>();
        long seed = Math.abs(product.getSearchKey().hashCode());
        List<String[]> groups = CATEGORY_ATTRIBUTES.getOrDefault(category, CATEGORY_ATTRIBUTES.get("ELECTRONICS"));
        int s = (int) seed;
        for (String[] g : groups) {
            String value = g.length == 2 ? g[1] : g[1 + (int)(Math.abs(s++) % (g.length - 1))];
            attrs.add(ProductAttribute.builder().product(product).name(g[0]).value(value).build());
        }
        return attrs;
    }

    public Product getOrCreateProduct(String searchKey, String query) {
        return productRepository.findBySearchKey(searchKey).orElseGet(() -> {
            Product saved = productRepository.save(generateProduct(searchKey, query));
            List<ProductAttribute> attrs = generateAttributes(saved.getCategory(), saved);
            productAttributeRepository.saveAll(attrs);
            return saved;
        });
    }

    public List<Product> generateProductList(String query, int count) {
        List<Product> products = new ArrayList<>();
        String base = normalizeSearchKey(query);
        for (int i = 0; i < count; i++) {
            final int idx = i;
            final String key = base + (idx == 0 ? "" : "_v" + idx);
            products.add(productRepository.findBySearchKey(key).orElseGet(() -> generateProduct(key, query + (idx > 0 ? " variant " + idx : ""))));
        }
        saveProductsAsync(products);
        return products;
    }

    @Async("asyncExecutor")
    public void saveProductsAsync(List<Product> products) {
        for (Product p : products) {
            if (p.getId() == null) {
                try {
                    Product saved = productRepository.save(p);
                    List<ProductAttribute> attrs = generateAttributes(saved.getCategory(), saved);
                    attrs.forEach(a -> a.setProduct(saved));
                    productAttributeRepository.saveAll(attrs);
                } catch (Exception e) {
                    log.warn("Async save failed for {}: {}", p.getSearchKey(), e.getMessage());
                }
            }
        }
    }
}
