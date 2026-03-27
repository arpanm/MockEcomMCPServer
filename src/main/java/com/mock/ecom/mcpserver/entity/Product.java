package com.mock.ecom.mcpserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "sub_category")
    private String subCategory;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "additional_images", columnDefinition = "TEXT")
    private String additionalImages;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "mrp", precision = 19, scale = 2)
    private BigDecimal mrp;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "size")
    private String size;

    @Column(name = "color")
    private String color;

    @Column(name = "material")
    private String material;

    @Column(name = "weight")
    private String weight;

    @Column(name = "search_key", unique = true, nullable = false)
    private String searchKey;

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 100;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product")
    private List<ProductAttribute> attributes;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
