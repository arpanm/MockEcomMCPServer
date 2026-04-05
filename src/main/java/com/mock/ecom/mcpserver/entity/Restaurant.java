package com.mock.ecom.mcpserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurant", indexes = {
    @Index(name = "idx_restaurant_swiggy_id", columnList = "swiggy_id", unique = true),
    @Index(name = "idx_restaurant_city", columnList = "city_id"),
    @Index(name = "idx_restaurant_name", columnList = "name")
})
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "swiggy_id", nullable = false, unique = true)
    private String swiggyId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "locality")
    private String locality;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "cloudinary_image_id")
    private String cloudinaryImageId;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "total_ratings_string")
    private String totalRatingsString;

    @Column(name = "cost_for_two")
    private Integer costForTwo;

    @Column(name = "cost_for_two_message")
    private String costForTwoMessage;

    @Column(name = "delivery_time")
    private Integer deliveryTime;

    @Column(name = "is_open")
    @Builder.Default
    private Boolean isOpen = true;

    @Column(name = "is_pure_veg")
    @Builder.Default
    private Boolean isPureVeg = false;

    @Column(name = "cuisines", columnDefinition = "TEXT")
    private String cuisines;

    @Column(name = "discount_info")
    private String discountInfo;

    @Column(name = "promoted")
    @Builder.Default
    private Boolean promoted = false;

    @Column(name = "last_mile_travel")
    private Double lastMileTravel;

    @Column(name = "slug")
    private String slug;

    @Column(name = "menu_scraped")
    @Builder.Default
    private Boolean menuScraped = false;

    @Column(name = "menu_scraped_at")
    private LocalDateTime menuScrapedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
