package com.mock.ecom.mcpserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shipment")
public class Shipment {

    public enum ShipmentStatus {
        PENDING, PROCESSING, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, RETURNED, CANCELLED
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @Column(name = "carrier_name")
    private String carrierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShipmentStatus status;

    @Column(name = "delivery_otp")
    private String deliveryOtp;

    @Column(name = "otp_verified")
    @Builder.Default
    private boolean otpVerified = false;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "delivery_pincode")
    private String deliveryPincode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shipment")
    private List<OrderItem> items;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
