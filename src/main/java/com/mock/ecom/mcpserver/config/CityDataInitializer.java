package com.mock.ecom.mcpserver.config;

import com.mock.ecom.mcpserver.entity.City;
import com.mock.ecom.mcpserver.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database with 50 major Indian cities and their coordinates
 * on application startup. Idempotent - skips cities that already exist.
 */
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class CityDataInitializer implements ApplicationRunner {

    private final CityRepository cityRepository;

    private static final List<Object[]> CITIES = List.of(
            // {name, state, latitude, longitude}
            new Object[]{"Mumbai", "Maharashtra", 19.0760, 72.8777},
            new Object[]{"Delhi", "Delhi", 28.6139, 77.2090},
            new Object[]{"Bangalore", "Karnataka", 12.9716, 77.5946},
            new Object[]{"Hyderabad", "Telangana", 17.3850, 78.4867},
            new Object[]{"Chennai", "Tamil Nadu", 13.0827, 80.2707},
            new Object[]{"Kolkata", "West Bengal", 22.5726, 88.3639},
            new Object[]{"Pune", "Maharashtra", 18.5204, 73.8567},
            new Object[]{"Ahmedabad", "Gujarat", 23.0225, 72.5714},
            new Object[]{"Jaipur", "Rajasthan", 26.9124, 75.7873},
            new Object[]{"Surat", "Gujarat", 21.1702, 72.8311},
            new Object[]{"Lucknow", "Uttar Pradesh", 26.8467, 80.9462},
            new Object[]{"Kanpur", "Uttar Pradesh", 26.4499, 80.3319},
            new Object[]{"Nagpur", "Maharashtra", 21.1458, 79.0882},
            new Object[]{"Indore", "Madhya Pradesh", 22.7196, 75.8577},
            new Object[]{"Thane", "Maharashtra", 19.2183, 72.9781},
            new Object[]{"Bhopal", "Madhya Pradesh", 23.2599, 77.4126},
            new Object[]{"Visakhapatnam", "Andhra Pradesh", 17.6868, 83.2185},
            new Object[]{"Patna", "Bihar", 25.5941, 85.1376},
            new Object[]{"Vadodara", "Gujarat", 22.3072, 73.1812},
            new Object[]{"Ghaziabad", "Uttar Pradesh", 28.6692, 77.4538},
            new Object[]{"Ludhiana", "Punjab", 30.9009, 75.8573},
            new Object[]{"Agra", "Uttar Pradesh", 27.1767, 78.0081},
            new Object[]{"Nashik", "Maharashtra", 19.9975, 73.7898},
            new Object[]{"Faridabad", "Haryana", 28.4082, 77.3178},
            new Object[]{"Meerut", "Uttar Pradesh", 28.9845, 77.7064},
            new Object[]{"Rajkot", "Gujarat", 22.3039, 70.8022},
            new Object[]{"Varanasi", "Uttar Pradesh", 25.3176, 82.9739},
            new Object[]{"Srinagar", "Jammu & Kashmir", 34.0837, 74.7973},
            new Object[]{"Aurangabad", "Maharashtra", 19.8762, 75.3433},
            new Object[]{"Amritsar", "Punjab", 31.6340, 74.8723},
            new Object[]{"Navi Mumbai", "Maharashtra", 19.0330, 73.0297},
            new Object[]{"Allahabad", "Uttar Pradesh", 25.4358, 81.8463},
            new Object[]{"Ranchi", "Jharkhand", 23.3441, 85.3096},
            new Object[]{"Coimbatore", "Tamil Nadu", 11.0168, 76.9558},
            new Object[]{"Jabalpur", "Madhya Pradesh", 23.1815, 79.9864},
            new Object[]{"Gwalior", "Madhya Pradesh", 26.2183, 78.1828},
            new Object[]{"Vijayawada", "Andhra Pradesh", 16.5062, 80.6480},
            new Object[]{"Jodhpur", "Rajasthan", 26.2389, 73.0243},
            new Object[]{"Madurai", "Tamil Nadu", 9.9252, 78.1198},
            new Object[]{"Raipur", "Chhattisgarh", 21.2514, 81.6296},
            new Object[]{"Kota", "Rajasthan", 25.2138, 75.8648},
            new Object[]{"Chandigarh", "Punjab", 30.7333, 76.7794},
            new Object[]{"Guwahati", "Assam", 26.1445, 91.7362},
            new Object[]{"Solapur", "Maharashtra", 17.6869, 75.9064},
            new Object[]{"Hubballi", "Karnataka", 15.3647, 75.1239},
            new Object[]{"Mysore", "Karnataka", 12.2958, 76.6394},
            new Object[]{"Tiruchirappalli", "Tamil Nadu", 10.7905, 78.7047},
            new Object[]{"Bareilly", "Uttar Pradesh", 28.3670, 79.4304},
            new Object[]{"Aligarh", "Uttar Pradesh", 27.8974, 78.0880},
            new Object[]{"Moradabad", "Uttar Pradesh", 28.8386, 78.7733}
    );

    @Override
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (Object[] cityData : CITIES) {
            String name = (String) cityData[0];
            if (!cityRepository.existsByName(name)) {
                City city = City.builder()
                        .name(name)
                        .state((String) cityData[1])
                        .latitude((Double) cityData[2])
                        .longitude((Double) cityData[3])
                        .restaurantsScraped(false)
                        .restaurantCount(0)
                        .build();
                cityRepository.save(city);
                seeded++;
            }
        }
        if (seeded > 0) {
            log.info("Seeded {} new cities. Total cities: {}", seeded, cityRepository.count());
        } else {
            log.info("City data already seeded. Total cities: {}", cityRepository.count());
        }
    }
}
