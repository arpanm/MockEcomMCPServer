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
@Table(name = "menu_item", indexes = {
    @Index(name = "idx_menu_item_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_menu_item_category", columnList = "menu_category_id"),
    @Index(name = "idx_menu_item_swiggy_id", columnList = "swiggy_item_id")
})
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "swiggy_item_id")
    private String swiggyItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_category_id")
    private MenuCategory menuCategory;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price")
    private Integer price;

    @Column(name = "default_price")
    private Integer defaultPrice;

    @Column(name = "is_veg")
    @Builder.Default
    private Boolean isVeg = false;

    @Column(name = "cloudinary_image_id")
    private String cloudinaryImageId;

    @Column(name = "in_stock")
    @Builder.Default
    private Boolean inStock = true;

    @Column(name = "is_best_seller")
    @Builder.Default
    private Boolean isBestSeller = false;

    @Column(name = "ratings_count")
    private Integer ratingsCount;

    @Column(name = "avg_rating_string")
    private String avgRatingString;

    @Column(name = "item_attribute")
    private String itemAttribute;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
