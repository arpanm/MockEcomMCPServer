package com.mock.ecom.mcpserver.config;

import com.mock.ecom.mcpserver.entity.City;
import com.mock.ecom.mcpserver.entity.MenuCategory;
import com.mock.ecom.mcpserver.entity.MenuItem;
import com.mock.ecom.mcpserver.entity.Restaurant;
import com.mock.ecom.mcpserver.repository.CityRepository;
import com.mock.ecom.mcpserver.repository.MenuCategoryRepository;
import com.mock.ecom.mcpserver.repository.MenuItemRepository;
import com.mock.ecom.mcpserver.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds a representative set of real-world Swiggy restaurant data so the
 * system is immediately queryable even before the live scraper runs.
 * Idempotent - skips restaurants that already exist by swiggyId.
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class SampleRestaurantDataSeeder implements ApplicationRunner {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedRestaurantsWithMenus();
    }

    private void seedRestaurantsWithMenus() {
        int seeded = 0;

        // Bangalore restaurants
        City bangalore = cityRepository.findByName("Bangalore").orElse(null);
        if (bangalore != null) {
            seeded += seed(bangalore, "100001", "Truffles", "Koramangala", "Koramangala 5th Block",
                    "Indian, Burgers, Sandwiches", 4.5, "5K+ ratings", 60000, "₹600 for two", 30, false, false,
                    "40% off up to ₹80", "fl_lossy,f_auto,q_auto,w_660/rngajaja7fdcqehnuuxi",
                    List.of(
                            new MenuCatData("Burgers", List.of(
                                    new MenuItemData("sw101", "The Truffles Burger", "Juicy beef patty with truffle aioli", 28000, 28000, false, true),
                                    new MenuItemData("sw102", "Chicken BBQ Burger", "Smoky BBQ chicken with crispy bacon", 22000, 22000, false, true),
                                    new MenuItemData("sw103", "Veggie Burger", "Black bean patty with avocado", 18000, 18000, true, false)
                            )),
                            new MenuCatData("Pasta & Mains", List.of(
                                    new MenuItemData("sw104", "Spaghetti Aglio e Olio", "Classic garlic olive oil pasta", 25000, 25000, true, true),
                                    new MenuItemData("sw105", "Penne Arrabbiata", "Spicy tomato sauce pasta", 22000, 22000, true, false),
                                    new MenuItemData("sw106", "Grilled Chicken Steak", "250g chicken with herbed butter", 32000, 32000, false, false)
                            )),
                            new MenuCatData("Desserts", List.of(
                                    new MenuItemData("sw107", "Dark Chocolate Fondant", "Warm molten cake with vanilla ice cream", 18000, 18000, true, true),
                                    new MenuItemData("sw108", "NY Cheesecake", "Classic New York style cheesecake", 15000, 15000, true, false)
                            ))
                    ));

            seeded += seed(bangalore, "100002", "CTR (Central Tiffin Room)", "Malleshwaram", "Malleshwaram 8th Cross",
                    "South Indian, Breakfast", 4.7, "10K+ ratings", 15000, "₹150 for two", 20, false, false,
                    "20% off", "fl_lossy,f_auto,q_auto,w_660/k6pnxbpnmlpcmbz0bwzv",
                    List.of(
                            new MenuCatData("Breakfast Specials", List.of(
                                    new MenuItemData("sw201", "Benne Masala Dosa", "Crispy dosa with butter and spiced potato filling", 8000, 8000, true, true),
                                    new MenuItemData("sw202", "Set Dosa", "Soft fluffy set of 3 dosas with chutney and sambar", 6000, 6000, true, true),
                                    new MenuItemData("sw203", "Idli Sambar", "Steamed rice cakes with lentil soup", 5000, 5000, true, true),
                                    new MenuItemData("sw204", "Vada Sambar", "Crispy lentil donuts with sambar", 5000, 5000, true, false)
                            )),
                            new MenuCatData("Beverages", List.of(
                                    new MenuItemData("sw205", "Filter Coffee", "Traditional South Indian filter coffee", 2500, 2500, true, true),
                                    new MenuItemData("sw206", "Badam Milk", "Warm almond milk with saffron", 4000, 4000, true, false)
                            ))
                    ));

            seeded += seed(bangalore, "100003", "Meghana Foods", "Koramangala", "Koramangala 6th Block",
                    "Biryani, Andhra, South Indian", 4.4, "8K+ ratings", 40000, "₹400 for two", 35, false, false,
                    "₹125 off above ₹249", "fl_lossy,f_auto,q_auto,w_660/meghana_img",
                    List.of(
                            new MenuCatData("Biryani", List.of(
                                    new MenuItemData("sw301", "Chicken Dum Biryani", "Aromatic basmati rice with tender chicken pieces", 22000, 22000, false, true),
                                    new MenuItemData("sw302", "Mutton Biryani", "Slow-cooked mutton biryani", 28000, 28000, false, true),
                                    new MenuItemData("sw303", "Veg Biryani", "Fragrant vegetable biryani", 18000, 18000, true, false)
                            )),
                            new MenuCatData("Curries", List.of(
                                    new MenuItemData("sw304", "Gongura Chicken Curry", "Tangy sorrel leaves chicken curry", 22000, 22000, false, false),
                                    new MenuItemData("sw305", "Paneer Butter Masala", "Creamy tomato-based paneer curry", 20000, 20000, true, false)
                            ))
                    ));
        }

        // Mumbai restaurants
        City mumbai = cityRepository.findByName("Mumbai").orElse(null);
        if (mumbai != null) {
            seeded += seed(mumbai, "200001", "Bademiya", "Colaba", "Colaba Causeway",
                    "Mughlai, Kebabs, Biryani", 4.3, "7K+ ratings", 35000, "₹350 for two", 25, false, false,
                    "30% off up to ₹100", "fl_lossy,f_auto,q_auto,w_660/bademiya_img",
                    List.of(
                            new MenuCatData("Kebabs", List.of(
                                    new MenuItemData("sw401", "Seekh Kebab (6 pcs)", "Minced lamb kebabs cooked on sigri", 28000, 28000, false, true),
                                    new MenuItemData("sw402", "Chicken Tikka (6 pcs)", "Marinated chicken pieces char-grilled", 25000, 25000, false, true),
                                    new MenuItemData("sw403", "Boti Kebab", "Tender mutton boti cooked on coal", 30000, 30000, false, false)
                            )),
                            new MenuCatData("Rolls & Breads", List.of(
                                    new MenuItemData("sw404", "Chicken Reshmi Roll", "Creamy chicken in rumali roti", 18000, 18000, false, false),
                                    new MenuItemData("sw405", "Pav Bhaji Special", "Mumbai-style spiced mashed vegetables with butter pav", 15000, 15000, true, true)
                            ))
                    ));

            seeded += seed(mumbai, "200002", "Sarvi Restaurant", "Byculla", "Mohammed Ali Road",
                    "Mughlai, Biryani, Haleem", 4.5, "9K+ ratings", 50000, "₹500 for two", 40, false, false,
                    "Free delivery on first order", "fl_lossy,f_auto,q_auto,w_660/sarvi_img",
                    List.of(
                            new MenuCatData("Biryani", List.of(
                                    new MenuItemData("sw501", "Mutton Biryani (Full)", "Slow-cooked dum biryani with whole spices", 45000, 45000, false, true),
                                    new MenuItemData("sw502", "Chicken Biryani (Full)", "Aromatic chicken dum biryani", 38000, 38000, false, true),
                                    new MenuItemData("sw503", "Egg Biryani", "Biryani with hard-boiled eggs", 28000, 28000, false, false)
                            )),
                            new MenuCatData("Haleem & Nihari", List.of(
                                    new MenuItemData("sw504", "Mutton Haleem", "Slow-cooked mutton and lentil porridge", 35000, 35000, false, true),
                                    new MenuItemData("sw505", "Chicken Nihari", "Slow-braised chicken stew with khameeri roti", 32000, 32000, false, false)
                            ))
                    ));

            seeded += seed(mumbai, "200003", "Cream Centre", "Nariman Point", "Churchgate",
                    "Continental, Italian, Indian, Pizza", 4.2, "5K+ ratings", 70000, "₹700 for two", 45, true, false,
                    "20% off", "fl_lossy,f_auto,q_auto,w_660/cream_centre_img",
                    List.of(
                            new MenuCatData("Starters", List.of(
                                    new MenuItemData("sw601", "Chilli Paneer Dry", "Crispy paneer tossed in chilli sauce", 32000, 32000, true, true),
                                    new MenuItemData("sw602", "Corn & Cheese Balls", "Crispy golden balls with sweet corn and cheese", 28000, 28000, true, true),
                                    new MenuItemData("sw603", "Spring Rolls (4 pcs)", "Crispy veg spring rolls", 25000, 25000, true, false)
                            )),
                            new MenuCatData("Pizza", List.of(
                                    new MenuItemData("sw604", "Margherita Pizza", "Classic tomato base with mozzarella", 38000, 38000, true, false),
                                    new MenuItemData("sw605", "Paneer Tikka Pizza", "Spiced paneer on pizza base", 45000, 45000, true, true)
                            )),
                            new MenuCatData("Desserts", List.of(
                                    new MenuItemData("sw606", "Brownie with Ice Cream", "Warm fudge brownie with vanilla ice cream", 32000, 32000, true, true),
                                    new MenuItemData("sw607", "Gulab Jamun", "Soft milk-solid dumplings in sugar syrup", 18000, 18000, true, false)
                            ))
                    ));
        }

        // Delhi restaurants
        City delhi = cityRepository.findByName("Delhi").orElse(null);
        if (delhi != null) {
            seeded += seed(delhi, "300001", "Karim's", "Old Delhi", "Jama Masjid",
                    "Mughlai, Non-Veg, Kebabs", 4.6, "12K+ ratings", 40000, "₹400 for two", 30, false, false,
                    "Flat ₹100 off", "fl_lossy,f_auto,q_auto,w_660/karims_img",
                    List.of(
                            new MenuCatData("Kebabs & Starters", List.of(
                                    new MenuItemData("sw701", "Mutton Seekh Kebab", "Minced mutton kebabs on iron skewer", 30000, 30000, false, true),
                                    new MenuItemData("sw702", "Burra Kabab", "Marinated mutton chops with special spices", 45000, 45000, false, true),
                                    new MenuItemData("sw703", "Chicken Tangdi Kebab", "Spiced chicken drumstick kebab", 28000, 28000, false, false)
                            )),
                            new MenuCatData("Main Course", List.of(
                                    new MenuItemData("sw704", "Mutton Korma", "Rich Mughal-style mutton in creamy gravy", 40000, 40000, false, true),
                                    new MenuItemData("sw705", "Chicken Jahangiri", "Stuffed chicken in royal gravy", 38000, 38000, false, false),
                                    new MenuItemData("sw706", "Dal Bukhara", "Black lentils slow-cooked overnight", 25000, 25000, true, false)
                            )),
                            new MenuCatData("Biryani", List.of(
                                    new MenuItemData("sw707", "Mutton Biryani", "Traditional Delhi-style dum biryani", 40000, 40000, false, true),
                                    new MenuItemData("sw708", "Chicken Biryani", "Fragrant chicken biryani", 35000, 35000, false, false)
                            ))
                    ));

            seeded += seed(delhi, "300002", "Paranthe Wali Gali", "Chandni Chowk", "Old Delhi",
                    "North Indian, Breakfast, Street Food", 4.4, "8K+ ratings", 20000, "₹200 for two", 20, false, false,
                    "Buy 2 get 1 free", "fl_lossy,f_auto,q_auto,w_660/paranthe_img",
                    List.of(
                            new MenuCatData("Paranthas", List.of(
                                    new MenuItemData("sw801", "Aloo Parantha", "Stuffed potato paratha with butter and curd", 8000, 8000, true, true),
                                    new MenuItemData("sw802", "Gobhi Parantha", "Cauliflower-stuffed crispy paratha", 8000, 8000, true, true),
                                    new MenuItemData("sw803", "Paneer Parantha", "Fresh cottage cheese stuffed paratha", 10000, 10000, true, false),
                                    new MenuItemData("sw804", "Mixed Veg Parantha", "Assorted vegetable stuffed paratha", 10000, 10000, true, false)
                            )),
                            new MenuCatData("Accompaniments", List.of(
                                    new MenuItemData("sw805", "Raita", "Yogurt with cucumber and cumin", 4000, 4000, true, true),
                                    new MenuItemData("sw806", "Homemade Pickle", "Traditional achar", 3000, 3000, true, false)
                            ))
                    ));
        }

        // Hyderabad restaurants
        City hyderabad = cityRepository.findByName("Hyderabad").orElse(null);
        if (hyderabad != null) {
            seeded += seed(hyderabad, "400001", "Paradise Biryani", "Secunderabad", "MG Road",
                    "Biryani, Hyderabadi", 4.6, "15K+ ratings", 45000, "₹450 for two", 35, false, false,
                    "₹75 off on ₹199+", "fl_lossy,f_auto,q_auto,w_660/paradise_img",
                    List.of(
                            new MenuCatData("Biryani", List.of(
                                    new MenuItemData("sw901", "Mutton Dum Biryani", "Original Hyderabadi dum biryani with tender mutton", 35000, 35000, false, true),
                                    new MenuItemData("sw902", "Chicken Biryani Full Plate", "Hyderabadi-style chicken biryani with raita", 28000, 28000, false, true),
                                    new MenuItemData("sw903", "Veg Dum Biryani", "Aromatic vegetable biryani", 22000, 22000, true, false)
                            )),
                            new MenuCatData("Starters", List.of(
                                    new MenuItemData("sw904", "Chicken 65", "Spicy deep-fried chicken bites", 22000, 22000, false, true),
                                    new MenuItemData("sw905", "Mirchi Bajji", "Stuffed green chilli fritters", 12000, 12000, true, false)
                            )),
                            new MenuCatData("Desserts", List.of(
                                    new MenuItemData("sw906", "Double Ka Meetha", "Hyderabadi bread pudding with cream", 12000, 12000, true, true),
                                    new MenuItemData("sw907", "Qubani Ka Meetha", "Apricot dessert with cream", 10000, 10000, true, false)
                            ))
                    ));
        }

        // Chennai restaurants
        City chennai = cityRepository.findByName("Chennai").orElse(null);
        if (chennai != null) {
            seeded += seed(chennai, "500001", "Saravana Bhavan", "T Nagar", "Pondy Bazaar",
                    "South Indian, Tamil, Breakfast", 4.5, "20K+ ratings", 25000, "₹250 for two", 25, false, true,
                    "Free delivery", "fl_lossy,f_auto,q_auto,w_660/saravana_img",
                    List.of(
                            new MenuCatData("Breakfast", List.of(
                                    new MenuItemData("sw1001", "Masala Dosa", "Crispy golden dosa with potato masala", 10000, 10000, true, true),
                                    new MenuItemData("sw1002", "Pongal", "Soft rice and lentil dish with ghee", 8000, 8000, true, true),
                                    new MenuItemData("sw1003", "Vada (2 pcs)", "Crispy urad dal fritters", 7000, 7000, true, true),
                                    new MenuItemData("sw1004", "Uthappam", "Thick rice pancake with toppings", 9000, 9000, true, false)
                            )),
                            new MenuCatData("Meals & Thali", List.of(
                                    new MenuItemData("sw1005", "Mini Meals", "Rice, sambar, rasam, curd, and 2 curries", 18000, 18000, true, true),
                                    new MenuItemData("sw1006", "Full Meals", "Complete Tamil thali with unlimited rice", 25000, 25000, true, false)
                            )),
                            new MenuCatData("Sweets", List.of(
                                    new MenuItemData("sw1007", "Kesari", "Semolina halwa with saffron", 6000, 6000, true, true),
                                    new MenuItemData("sw1008", "Payasam", "Rice pudding with jaggery", 7000, 7000, true, false)
                            ))
                    ));
        }

        if (seeded > 0) {
            log.info("Seeded {} sample restaurants with full menus across Bangalore, Mumbai, Delhi, Hyderabad, Chennai", seeded);
        } else {
            log.info("Sample restaurants already in DB, skipping seed");
        }
    }

    private int seed(City city, String swiggyId, String name, String locality, String area,
                     String cuisines, double rating, String totalRatings, int costForTwo,
                     String costMsg, int deliveryTime, boolean isOpen, boolean isPureVeg,
                     String discount, String imageId, List<MenuCatData> categories) {
        if (restaurantRepository.existsBySwiggyId(swiggyId)) return 0;

        Restaurant restaurant = Restaurant.builder()
                .swiggyId(swiggyId)
                .name(name)
                .city(city)
                .locality(locality)
                .areaName(area)
                .cuisines(cuisines)
                .avgRating(rating)
                .totalRatingsString(totalRatings)
                .costForTwo(costForTwo)
                .costForTwoMessage(costMsg)
                .deliveryTime(deliveryTime)
                .isOpen(isOpen)
                .isPureVeg(isPureVeg)
                .discountInfo(discount)
                .cloudinaryImageId(imageId)
                .menuScraped(true)
                .menuScrapedAt(LocalDateTime.now())
                .build();
        restaurantRepository.save(restaurant);

        int order = 0;
        for (MenuCatData cat : categories) {
            MenuCategory menuCategory = MenuCategory.builder()
                    .restaurant(restaurant)
                    .name(cat.name)
                    .displayOrder(order++)
                    .build();
            menuCategoryRepository.save(menuCategory);

            for (MenuItemData item : cat.items) {
                menuItemRepository.save(MenuItem.builder()
                        .swiggyItemId(item.id)
                        .restaurant(restaurant)
                        .menuCategory(menuCategory)
                        .name(item.name)
                        .description(item.desc)
                        .price(item.price)
                        .defaultPrice(item.defaultPrice)
                        .isVeg(item.isVeg)
                        .inStock(true)
                        .isBestSeller(item.isBestseller)
                        .build());
            }
        }

        return 1;
    }

    record MenuCatData(String name, List<MenuItemData> items) {}
    record MenuItemData(String id, String name, String desc, int price, int defaultPrice, boolean isVeg, boolean isBestseller) {}
}
